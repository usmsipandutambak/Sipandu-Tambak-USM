// realtime.js — HTTP polling for real-time sensor data
'use strict';

const Realtime = (() => {
  let _intervalId = null;
  let _isConnected = false;
  let _lastDataMs = null;
  let _staleMs = 10000;   // declare stale after 10s without data
  const _listeners = { status: [], sensors: [], connection: [] };

  function _emit(event, data) {
    (_listeners[event] || []).forEach(fn => { try { fn(data); } catch (e) { } });
  }

  function _setConnected(val) {
    if (_isConnected !== val) {
      _isConnected = val;
      _emit('connection', val);
    }
  }

  async function _poll() {
    const [statusRes, sensorsRes] = await Promise.allSettled([
      ESP32API.getStatus(),
      ESP32API.getSensors(),
    ]);

    const hasStatus = statusRes.status === 'fulfilled' && statusRes.value;
    const hasSensors = sensorsRes.status === 'fulfilled' && sensorsRes.value;

    if (hasStatus || hasSensors) {
      _lastDataMs = Date.now();
      _setConnected(true);
      if (hasStatus) _emit('status', statusRes.value);
      if (hasSensors) _emit('sensors', sensorsRes.value);
      return;
    }

    const errMsg = statusRes.reason?.message || sensorsRes.reason?.message || 'ESP32 unreachable';
    if (window.SupabaseSync) {
      try {
        const rows = await SupabaseSync.getLatestSensorLogs(undefined, 1);
        if (Array.isArray(rows) && rows.length > 0) {
          const r = rows[0];
          _lastDataMs = Date.now();
          _setConnected(true);
          _emit('sensors', {
            source: 'supabase',
            sensors: {
              ph: r.ph_level,
              do_mg_l: r.do_level ?? r.dissolved_oxygen ?? r.do ?? null,
              tds_ppm: r.tds_ppm,
              turbidity_ntu: r.turbidity_ntu,
              temperature_c: r.temperature_c,
              battery_voltage: r.battery_voltage,
            },
            sensor_status: { ads_a: true, ads_b: true, ds18b20: true, ina226: true },
          });
          return;
        }
      } catch (_) { }
    }
    _setConnected(false);
    _emit('sensors', null);
    _emit('status', null);
    console.warn('[Realtime] Poll failed:', errMsg);
  }

  function _checkStale() {
    if (_isConnected && _lastDataMs && Date.now() - _lastDataMs > _staleMs) {
      _setConnected(false);
      _emit('sensors', null);
    }
  }

  return {
    start() {
      if (_intervalId) return;
      _poll();   // immediate first poll
      _intervalId = setInterval(() => {
        _poll();
        _checkStale();
      }, ESP32Config.updateInterval);
      console.log('[Realtime] Polling started, interval:', ESP32Config.updateInterval, 'ms');
    },

    stop() {
      if (_intervalId) { clearInterval(_intervalId); _intervalId = null; }
    },

    restart() { this.stop(); this.start(); },

    isConnected() { return _isConnected; },
    lastDataMs() { return _lastDataMs; },

    _feedMqttData(data) {
      if (!data) return;
      _lastDataMs = Date.now();
      _setConnected(true);
      _emit('sensors', data);
      _emit('status', data);
    },

    on(event, fn) {
      if (_listeners[event]) _listeners[event].push(fn);
    },
    off(event, fn) {
      if (_listeners[event]) _listeners[event] = _listeners[event].filter(f => f !== fn);
    },
  };
})();

window.Realtime = Realtime;
