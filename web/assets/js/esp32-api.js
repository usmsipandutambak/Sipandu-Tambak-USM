// esp32-api.js — Centralized ESP32 HTTP API client
'use strict';

const ESP32Config = {
  baseURL: 'http://192.168.1.100',
  timeout: 5000,
  updateInterval: 2000,
};

const ESP32API = (() => {
  let _baseURL = ESP32Config.baseURL;

  /** Internal fetch with timeout + error handling */
  async function _fetch(path, options = {}) {
    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), ESP32Config.timeout);

    try {
      const res = await fetch(`${_baseURL}${path}`, {
        ...options,
        signal: controller.signal,
        headers: { 'Content-Type': 'application/json', ...(options.headers || {}) },
      });
      clearTimeout(timer);

      if (!res.ok) {
        const errText = await res.text().catch(() => '');
        throw new Error(`HTTP ${res.status}: ${errText}`);
      }
      return await res.json();
    } catch (err) {
      clearTimeout(timer);
      if (err.name === 'AbortError') throw new Error('Request timeout — ESP32 tidak merespons');
      throw err;
    }
  }

  return {
    setBaseURL(url) { _baseURL = url.replace(/\/+$/, ''); },
    getBaseURL()    { return _baseURL; },

    /** GET /api/status */
    getStatus()       { return _fetch('/api/status'); },

    /** GET /api/sensors */
    getSensors()      { return _fetch('/api/sensors'); },

    /** GET /api/calibration */
    getCalibration()  { return _fetch('/api/calibration'); },

    /** POST /api/calibration — {ph:{slope,offset}, do:{slope,offset}, ...} */
    postCalibration(payload) {
      if (window.MQTTClient && window.MQTTClient.isConnected()) {
        const ok = window.MQTTClient.sendCommand({ command: 'calibrate', ...payload });
        if (ok) return Promise.resolve({ ok: true, via: 'mqtt' });
      }
      return _fetch('/api/calibration', { method: 'POST', body: JSON.stringify(payload) });
    },

    /** POST /api/calibration/reset — {sensor:"ph"|"do"|"tds"|"turbidity"|"all"} */
    resetCalibration(sensor) {
      if (window.MQTTClient && window.MQTTClient.isConnected()) {
        const ok = window.MQTTClient.sendCommand({ command: 'reset_calibration', sensor });
        if (ok) return Promise.resolve({ ok: true, via: 'mqtt' });
      }
      return _fetch('/api/calibration/reset', { method: 'POST', body: JSON.stringify({ sensor }) });
    },

    /** POST /api/control/sensors — {power: true/false} */
    controlSensors(power) {
      if (window.MQTTClient && window.MQTTClient.isConnected()) {
        const ok = window.MQTTClient.sendCommand({ command: 'power_sensors', power });
        if (ok) return Promise.resolve({ ok: true, sensor_power: power, via: 'mqtt' });
      }
      return _fetch('/api/control/sensors', { method: 'POST', body: JSON.stringify({ power }) });
    },

    /** POST /api/control/jetson — {power: true/false} */
    controlJetson(power) {
      if (window.MQTTClient && window.MQTTClient.isConnected()) {
        const ok = window.MQTTClient.sendCommand({ command: 'power_jetson', power });
        if (ok) return Promise.resolve({ ok: true, jetson_power: power, via: 'mqtt' });
      }
      return _fetch('/api/control/jetson', { method: 'POST', body: JSON.stringify({ power }) });
    },
  };
})();

window.ESP32Config = ESP32Config;
window.ESP32API    = ESP32API;
