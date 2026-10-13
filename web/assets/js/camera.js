// camera.js — Integrasi kamera Jetson Nano (MJPEG over HTTP)
'use strict';

const CameraModule = (() => {
  const STREAM_IDS = ['cam-stream', 'dash-cam-stream'];
  let _initialized = false;
  let _statusTimer = null;
  let _snapTimer   = null;
  let _started     = false;
  let _state       = 'standby';   // standby | live | offline

  const $ = (id) => document.getElementById(id);

  // ── UI state ───────────────────────────────────────────────────────────
  function _applyState(state) {
    const chip = {
      standby: { cls: 'camera-chip yellow', text: '● STANDBY' },
      live:    { cls: 'camera-chip green',  text: '● LIVE' },
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
        sig.textContent = '● LIVE';
      } else {
        sig.className = 'camera-chip red';
        sig.textContent = 'NO SIGNAL';
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
        standby: ['Belum Terintegrasi', 'var(--color-warning)'],
        live:    ['Online',             'var(--color-success)'],
        offline: ['Offline',            'var(--color-danger)'],
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
  function _startStreams() {
    if (!JetsonAPI.isConfigured()) return;
    STREAM_IDS.forEach((id) => {
      const img = $(id);
      if (!img) return;
      img.classList.remove('hidden');
      img.src = JetsonAPI.getStreamURL();
    });
  }

  function _stopStreams() {
    STREAM_IDS.forEach((id) => {
      const img = $(id);
      if (!img) return;
      img.removeAttribute('src');   // putuskan koneksi MJPEG
      img.classList.add('hidden');
    });
  }

  function _onImgError(id) {
    if (!_started || _state !== 'live') return;
    console.warn('[Camera] Stream error pada', id, '— fallback snapshot 1 FPS');
    _startSnapshotFallback();
  }

  function _startSnapshotFallback() {
    if (_snapTimer) return;
    _snapTimer = setInterval(() => {
      STREAM_IDS.forEach((id) => {
        const img = $(id);
        if (img) img.src = JetsonAPI.getSnapshotURL();
      });
    }, 1000);
  }

  function _stopSnapshotFallback() {
    if (_snapTimer) { clearInterval(_snapTimer); _snapTimer = null; }
  }


  // ── Status polling ─────────────────────────────────────────────────────
  async function _poll() {
    if (!_started || !JetsonAPI.isConfigured()) return;
    try {
      const st = await JetsonAPI.getStatus();
      _updateInfo(st);
      if (st.camera_ok) {
        const recovered = _state === 'offline';
        _setState('live');
        if (recovered) {          // kembali online -> sambungkan ulang stream
          _stopSnapshotFallback();
          _startStreams();
        }
      } else {
        // server OK tapi kamera di Jetson bermasalah
        _setState('offline');
      }
    } catch (err) {
      if (_state !== 'offline') {
        console.warn('[Camera] Jetson tidak merespons:', err.message);
      }
      _setState('offline');
    }
  }

  function _updateInfo(st) {
    const set = (id, text) => { const el = $(id); if (el) el.textContent = text; };
    const cam = st.camera || {};
    const stream = st.stream || {};
    set('cam-info-device',
      'Jetson Nano — ' + (st.hostname || 'online') +
      (cam.type ? ' · ' + cam.type : ''));
    set('cam-info-resolution',
      stream.width
        ? `${stream.width}×${stream.height} @ ${Utils.fmt(stream.fps, 1)} FPS`
        : 'Menunggu koneksi…');
    set('cam-info-protocol', 'MJPEG — multipart/x-mixed-replace');
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
      if (img) img.addEventListener('error', () => _onImgError(id));
    });

    _applyState('standby');
    if (JetsonAPI.isConfigured()) start();
  }

  function start() {
    if (!JetsonAPI.isConfigured()) { _setState('standby'); return; }
    _started = true;
    if (_statusTimer) return;       // sudah berjalan
    _stopSnapshotFallback();
    _startStreams();
    _poll();
    _statusTimer = setInterval(_poll, JetsonConfig.statusInterval);
  }

  function pause() {
    _started = false;
    if (_statusTimer) { clearInterval(_statusTimer); _statusTimer = null; }
    _stopSnapshotFallback();
    _stopStreams();
  }

  function restart() {
    pause();
    _state = 'standby';             // paksa transisi state
    _applyState('standby');
    start();
  }

  return { init, start, pause, restart };
})();

window.CameraModule = CameraModule;

