// camera.js — Integrasi kamera Jetson Nano (MJPEG over HTTP port 5000 /video_feed)
'use strict';

const CameraModule = (() => {
  const STREAM_IDS = ['cam-stream', 'dash-cam-stream'];
  let _initialized  = false;
  let _statusTimer  = null;
  let _retryTimer   = null;
  let _started      = false;
  let _streamLoaded = false;
  let _state        = 'standby';   // standby | live | offline

  const $ = (id) => document.getElementById(id);

  // ── UI state ───────────────────────────────────────────────────────────
  function _applyState(state) {
    const chip = {
      standby: { cls: 'camera-chip yellow', text: '● STANDBY' },
      live:    { cls: 'camera-chip green',  text: '● ONLINE' },
      offline: { cls: 'camera-chip red',    text: '● OFFLINE' },
    }[state];

    ['dash-cam-chip', 'cam-status-chip'].forEach((id) => {
      const el = $(id);
      if (el) { el.className = chip.cls; el.textContent = chip.text; }
    });

    const sig = $('cam-signal-chip');
    if (sig) {
      if (state === 'live') {
        sig.className = 'camera-chip green';
        sig.textContent = '● ONLINE';
      } else if (state === 'standby') {
        sig.className = 'camera-chip yellow';
        sig.textContent = 'CONNECTING...';
      } else {
        sig.className = 'camera-chip red';
        sig.textContent = 'OFFLINE';
      }
    }

    // overlay "no signal" vs gambar stream
    const showNoSignal = state !== 'live';
    ['dash-cam-nosignal', 'cam-nosignal'].forEach((id) => {
      const el = $(id);
      if (el) el.classList.toggle('hidden', !showNoSignal);
    });
    STREAM_IDS.forEach((id) => {
      const img = $(id);
      if (img) img.classList.toggle('hidden', showNoSignal);
    });

    // teks status di halaman kamera
    const src = $('cam-source-status');
    if (src) {
      const map = {
        standby: ['Menghubungkan...', 'var(--color-warning)'],
        live:    ['Online',           'var(--color-success)'],
        offline: ['Offline',          'var(--color-danger)'],
      }[state];
      src.textContent = map[0];
      src.style.color = map[1];
    }
  }

  function _setState(state) {
    if (_state === state) return;
    _state = state;
    _applyState(state);
  }

  // ── Stream control ─────────────────────────────────────────────────────
  function _startStreams(cacheBust = false) {
    if (!JetsonAPI.isConfigured()) return;
    const url = JetsonAPI.getStreamURL(cacheBust);
    _streamLoaded = false;
    STREAM_IDS.forEach((id) => {
      const img = $(id);
      if (!img) return;
      img.src = url;
    });
    const set = (id, text) => { const el = $(id); if (el) el.textContent = text; };
    set('cam-info-device', `Jetson Nano — ${JetsonAPI.getBaseURL()}`);
    set('cam-info-protocol', `MJPEG — ${url}`);
  }

  function _stopStreams() {
    _streamLoaded = false;
    STREAM_IDS.forEach((id) => {
      const img = $(id);
      if (!img) return;
      img.removeAttribute('src');   // putuskan koneksi MJPEG
      img.classList.add('hidden');
    });
  }

  function _onImgLoad(id) {
    if (!_started) return;
    _streamLoaded = true;
    if (_retryTimer) { clearTimeout(_retryTimer); _retryTimer = null; }
    _setState('live');

    const img = $(id);
    if (img && img.naturalWidth > 0) {
      const resEl = $('cam-info-resolution');
      if (resEl && (!resEl.textContent || resEl.textContent.includes('Menunggu'))) {
        resEl.textContent = `${img.naturalWidth}×${img.naturalHeight} (Live MJPEG)`;
      }
    }
  }

  function _onImgError(id) {
    if (!_started) return;
    _streamLoaded = false;
    console.warn('[Camera] Stream gagal dimuat pada', id, '— URL:', JetsonAPI.getStreamURL());
    _setState('offline');

    if (!_retryTimer) {
      _retryTimer = setTimeout(() => {
        _retryTimer = null;
        if (_started && !_streamLoaded) {
          _startStreams(true);
        }
      }, 5000);
    }
  }

  // ── Status polling (opsional /api/status) ──────────────────────────────
  async function _poll() {
    if (!_started || !JetsonAPI.isConfigured()) return;
    try {
      const st = await JetsonAPI.getStatus();
      _updateInfo(st);
      if (st.camera_ok && !_streamLoaded) {
        _setState('live');
        _startStreams(true);
      }
    } catch (_) {
      // Server Flask port 5000 (/video_feed) tidak wajib memiliki /api/status;
      // status ONLINE/OFFLINE ditentukan langsung oleh event onload / onerror pada <img>.
    }
  }

  function _updateInfo(st) {
    const set = (id, text) => { const el = $(id); if (el) el.textContent = text; };
    const cam = st.camera || {};
    const stream = st.stream || {};
    set('cam-info-device',
      'Jetson Nano — ' + (st.hostname || JetsonAPI.getBaseURL()) +
      (cam.type ? ' · ' + cam.type : ''));
    if (stream.width) {
      set('cam-info-resolution', `${stream.width}×${stream.height} @ ${Utils.fmt(stream.fps, 1)} FPS`);
    }
    set('cam-info-protocol', `MJPEG — ${JetsonAPI.getStreamURL()}`);
  }

  // ── Fullscreen ─────────────────────────────────────────────────────────
  function _toggleFullscreen() {
    const wrap = $('cam-frame-wrap');
    if (!wrap) return;
    if (document.fullscreenElement) {
      document.exitFullscreen().catch(() => {});
    } else if (wrap.requestFullscreen) {
      wrap.requestFullscreen().catch(() => {
        Utils.toast('Fullscreen tidak didukung browser', 'info');
      });
    } else {
      Utils.toast('Fullscreen tidak didukung browser', 'info');
    }
  }

  // ── Lifecycle ──────────────────────────────────────────────────────────
  function init() {
    if (_initialized) return;
    _initialized = true;

    const fsBtn = $('camera-fullscreen-btn');
    if (fsBtn) fsBtn.addEventListener('click', _toggleFullscreen);

    const refreshBtn = $('camera-refresh-btn');
    if (refreshBtn) {
      refreshBtn.addEventListener('click', () => {
        if (!JetsonAPI.isConfigured()) {
          Utils.toast('Masukkan IP Jetson Nano di sidebar terlebih dahulu', 'info');
          return;
        }
        restart();
        Utils.toast('Stream dimuat ulang', 'success');
      });
    }

    STREAM_IDS.forEach((id) => {
      const img = $(id);
      if (!img) return;
      img.addEventListener('load', () => _onImgLoad(id));
      img.addEventListener('error', () => _onImgError(id));
    });

    _applyState('standby');
    if (JetsonAPI.isConfigured()) start();
  }

  function start() {
    if (!JetsonAPI.isConfigured()) { _setState('standby'); return; }
    _started = true;
    if (_statusTimer) return;       // sudah berjalan
    _startStreams(false);
    _poll();
    _statusTimer = setInterval(_poll, JetsonConfig.statusInterval);
  }

  function pause() {
    _started = false;
    if (_statusTimer) { clearInterval(_statusTimer); _statusTimer = null; }
    if (_retryTimer)  { clearTimeout(_retryTimer);   _retryTimer = null; }
    _stopStreams();
  }

  function restart() {
    pause();
    _state = 'standby';             // paksa transisi state
    _applyState('standby');
    if (JetsonAPI.isConfigured()) {
      _started = true;
      _startStreams(true);
      _poll();
      _statusTimer = setInterval(_poll, JetsonConfig.statusInterval);
    }
  }

  return { init, start, pause, restart };
})();

window.CameraModule = CameraModule;

