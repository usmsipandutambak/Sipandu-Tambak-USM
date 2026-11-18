// jetson-api.js — Centralized Jetson Nano camera stream client
'use strict';

const JetsonConfig = {
  baseURL: 'http://192.168.100.41:5000',
  defaultPort: 5000,
  defaultStreamPath: '/video_feed',
  timeout: 5000,
  statusInterval: 5000,
};

const JetsonAPI = (() => {
  let _baseURL = JetsonConfig.baseURL;
  let _streamPath = JetsonConfig.defaultStreamPath;

  /**
   * Normalisasi input user:
   *   "192.168.100.41"                        -> base: http://192.168.100.41:5000, path: /video_feed
   *   "192.168.100.41:5000"                   -> base: http://192.168.100.41:5000, path: /video_feed
   *   "http://192.168.100.41:5000/video_feed" -> base: http://192.168.100.41:5000, path: /video_feed
   */
  function _normalize(host) {
    let raw = String(host || '').trim().replace(/\/+$/, '');
    if (!raw) return { base: '', path: JetsonConfig.defaultStreamPath };
    if (!/^https?:\/\//i.test(raw)) raw = 'http://' + raw;
    try {
      const u = new URL(raw);
      const port = u.port || String(JetsonConfig.defaultPort);
      const base = `${u.protocol}//${u.hostname}:${port}`;
      const path = (u.pathname && u.pathname !== '/') ? u.pathname : JetsonConfig.defaultStreamPath;
      return { base, path };
    } catch (_) {
      const rest = raw.replace(/^https?:\/\//i, '');
      const base = /:\d+$/.test(rest) ? raw : `${raw}:${JetsonConfig.defaultPort}`;
      return { base, path: JetsonConfig.defaultStreamPath };
    }
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
    setHost(host) {
      const parsed = _normalize(host);
      _baseURL = parsed.base;
      _streamPath = parsed.path;
    },
    setBaseURL(url) {
      const parsed = _normalize(url);
      _baseURL = parsed.base;
      _streamPath = parsed.path;
    },
    getBaseURL()       { return _baseURL; },
    getStreamPath()    { return _streamPath; },
    isConfigured()     { return !!_baseURL; },

    /** GET /api/status — opsional untuk server dengan endpoint metadata */
    getStatus()        { return _fetch('/api/status'); },

    /** URL stream MJPEG aktual (default: http://192.168.100.41:5000/video_feed) */
    getStreamURL(cacheBust = false) {
      if (!_baseURL) return '';
      const baseStream = `${_baseURL}${_streamPath}`;
      return cacheBust ? `${baseStream}?cb=${Date.now()}` : baseStream;
    },

    /** URL snapshot JPEG tunggal (fallback bila tersedia) */
    getSnapshotURL()   { return _baseURL ? `${_baseURL}/snapshot.jpg?cb=${Date.now()}` : ''; },
  };
})();

window.JetsonConfig = JetsonConfig;
window.JetsonAPI    = JetsonAPI;
