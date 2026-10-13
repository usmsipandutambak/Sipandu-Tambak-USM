// realtime.js — HTTP polling for real-time sensor data
'use strict';

const Realtime = (() => {
  let _intervalId  = null;
  let _isConnected = false;
  let _lastDataMs  = null;
  let _staleMs     = 10000;   // declare stale after 10s without data
  const _listeners = { status: [], sensors: [], connection: [] };

  function _emit(event, data) {
    (_listeners[event] || []).forEach(fn => { try { fn(data); } catch(e) {} });
  }

  function _setConnected(val) {
    if (_isConnected !== val) {
      _isConnected = val;
      _emit('connection', val);
    }
  }

  async function _poll() {
    try {
      const [status, sensors] = await Promise.all([
        ESP32API.getStatus(),
        ESP32API.getSensors(),
      ]);
      _lastDataMs = Date.now();
      _setConnected(true);
      _emit('status',  status);
      _emit('sensors', sensors);
    } catch (err) {
      _setConnected(false);
      _emit('sensors', null);
      console.warn('[Realtime] Poll failed:', err.message);
    }
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
    lastDataMs()  { return _lastDataMs; },

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
