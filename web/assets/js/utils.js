// utils.js — Common utility functions
'use strict';

const Utils = {
  /** Format float with fixed decimal places */
  fmt(val, decimals = 2) {
    if (val === null || val === undefined || isNaN(val)) return '—';
    return Number(val).toFixed(decimals);
  },

  /** Format timestamp (ms) to HH:MM:SS */
  fmtTime(ms) {
    const d = new Date(ms);
    return d.toLocaleTimeString('id-ID', { hour12: false });
  },

  /** Format uptime ms to Xh Xm Xs */
  fmtUptime(ms) {
    const s = Math.floor(ms / 1000);
    const h = Math.floor(s / 3600);
    const m = Math.floor((s % 3600) / 60);
    const sec = s % 60;
    if (h > 0) return `${h}h ${m}m ${sec}s`;
    if (m > 0) return `${m}m ${sec}s`;
    return `${sec}s`;
  },

  /** Clamp value */
  clamp(val, min, max) {
    return Math.min(Math.max(val, min), max);
  },

  /** Validate float input — returns {ok, val, msg} */
  validateFloat(str, min = -Infinity, max = Infinity) {
    if (!str || str.trim() === '') return { ok: false, msg: 'Tidak boleh kosong' };
    const v = parseFloat(str);
    if (isNaN(v)) return { ok: false, msg: 'Bukan angka valid' };
    if (!isFinite(v)) return { ok: false, msg: 'Nilai tidak valid (Inf)' };
    if (v < min) return { ok: false, msg: `Minimum ${min}` };
    if (v > max) return { ok: false, msg: `Maksimum ${max}` };
    return { ok: true, val: v };
  },

  /** Show toast notification */
  toast(msg, type = 'info', duration = 3500) {
    const container = document.getElementById('toast-container');
    if (!container) return;
    const el = document.createElement('div');
    el.className = `toast toast-${type}`;
    el.innerHTML = `
      <svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor">
        ${type === 'success' ? '<path d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"/>' :
          type === 'error'   ? '<path d="M10 14l2-2m0 0l2-2m-2 2l-2-2m2 2l2 2m7-2a9 9 0 11-18 0 9 9 0 0118 0z"/>' :
                               '<path d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/>'}
      </svg>
      <span>${msg}</span>`;
    container.appendChild(el);
    setTimeout(() => {
      el.style.transition = 'opacity 200ms';
      el.style.opacity = '0';
      setTimeout(() => el.remove(), 200);
    }, duration);
  },

  /** Set button loading state */
  btnLoading(btn, loading, text = null) {
    if (!btn) return;
    if (loading) {
      btn._originalHTML = btn.innerHTML;
      btn.innerHTML = `<span class="spinner"></span> ${text || 'Memproses...'}`;
      btn.disabled = true;
    } else {
      btn.innerHTML = btn._originalHTML || '';
      btn.disabled = false;
    }
  },

  /** Update element text safely */
  setText(id, val) {
    const el = document.getElementById(id);
    if (el) el.textContent = val;
  },

  /** Show/hide element by id */
  show(id) { const el = document.getElementById(id); if (el) el.classList.remove('hidden'); },
  hide(id) { const el = document.getElementById(id); if (el) el.classList.add('hidden'); },
};

window.Utils = Utils;
