// jetson-api.js — Centralized Jetson Nano camera stream client
'use strict';

const JetsonConfig = {
  baseURL: 'http://192.168.100.41:8080',          // diisi lewat sidebar (IP Jetson) — contoh: http://192.168.1.101:8080
  defaultPort: 8080,    // port default stream_camera.py di Jetson
  timeout: 5000,
  statusInterval: 3000, // interval polling /api/status (ms)
};

const JetsonAPI = (() => {
  let _baseURL = JetsonConfig.baseURL;

  /**
   * Normalisasi input user:
   *   "192.168.1.101"        -> http://192.168.1.101:8080
   *   "192.168.1.101:9000"   -> http://192.168.1.101:9000
   *   "http://192.168.1.101" -> http://192.168.1.101:8080
   */
  function _normalize(host) {
    let url = String(host || '').trim().replace(/\/+$/, '');
    if (!url) return '';
    if (!/^https?:\/\//i.test(url)) url = 'http://' + url;
    const rest = url.replace(/^https?:\/\//i, '');
    if (!/:\d+$/.test(rest)) url += ':' + JetsonConfig.defaultPort;
    return url;
  }

  /** GET JSON dari Jetson dengan timeout + error handling (ala esp32-api.js) */
  async function _fetch(path) {
    if (!_baseURL) throw new Error('IP Jetson belum dikonfigurasi');
    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), JetsonConfig.timeout);
    try {
      const res = await fetch(`${_baseURL}${path}`, { signal: controller.signal });
      clearTimeout(timer);
      if (!res.ok) throw new Error(`HTTP ${res.status}`);
      return await res.json();
    } catch (err) {
      clearTimeout(timer);
      if (err.name === 'AbortError') throw new Error('Request timeout — Jetson tidak merespons');
      throw err;
    }
  }

  return {
    setHost(host)      { _baseURL = _normalize(host); },
    setBaseURL(url)    { _baseURL = _normalize(url); },
    getBaseURL()       { return _baseURL; },
    isConfigured()     { return !!_baseURL; },

    /** GET /api/status — status kamera + stream dari Jetson */
    getStatus()        { return _fetch('/api/status'); },

    /** URL stream MJPEG (cache-buster tiap pemanggilan) */
    getStreamURL()     { return _baseURL ? `${_baseURL}/stream?cb=${Date.now()}` : ''; },

    /** URL snapshot JPEG tunggal (fallback bila MJPEG gagal) */
    getSnapshotURL()   { return _baseURL ? `${_baseURL}/snapshot.jpg?cb=${Date.now()}` : ''; },
  };
})();

window.JetsonConfig = JetsonConfig;
window.JetsonAPI    = JetsonAPI;
