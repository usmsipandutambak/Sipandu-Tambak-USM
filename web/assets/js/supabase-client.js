// supabase-client.js — Sinkronisasi Kalibrasi & Telemetri Web Admin <-> Supabase
'use strict';

const SupabaseConfig = {
  restURL: 'https://tidmdjnjhqptszyubvue.supabase.co/rest/v1',
  anonKey: 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InRpZG1kam5qaHFwdHN6eXVidnVlIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODk1NDMwMzEsImV4cCI6MjEwNTExOTAzMX0.G2VeWnkN5nexmaYKdA1jI9AjwM-sw1yPnF7uHlnkT20',
  deviceId: 'SIPANDU_A',
};

const DEFAULT_CALIBRATION = {
  ph:        { slope: -5.70,   offset: 21.34 },
  do:        { slope: 5.5,     offset: 0.0 },
  tds:       { slope: 500.0,   offset: 0.0 },
  turbidity: { slope: -1120.4, offset: 4895.87 },
};

const SupabaseSync = (() => {
  const LS_KEY = `sipandu_calibration_${SupabaseConfig.deviceId}`;

  function _headers(extra = {}) {
    return {
      'Content-Type': 'application/json',
      'apikey': SupabaseConfig.anonKey,
      'Authorization': `Bearer ${SupabaseConfig.anonKey}`,
      ...extra,
    };
  }

  function _readLocal() {
    try {
      const raw = localStorage.getItem(LS_KEY);
      if (!raw) return null;
      return JSON.parse(raw);
    } catch (_) {
      return null;
    }
  }

  function _writeLocal(calib) {
    try {
      localStorage.setItem(LS_KEY, JSON.stringify({
        ...calib,
        updated_at: new Date().toISOString(),
      }));
    } catch (_) {}
  }

  function _cloneDefaults() {
    return JSON.parse(JSON.stringify(DEFAULT_CALIBRATION));
  }

  function _mergeCalib(base, incoming) {
    const out = _cloneDefaults();
    ['ph', 'do', 'tds', 'turbidity'].forEach((k) => {
      if (base && base[k]) {
        if (Number.isFinite(Number(base[k].slope)))  out[k].slope  = Number(base[k].slope);
        if (Number.isFinite(Number(base[k].offset))) out[k].offset = Number(base[k].offset);
      }
      if (incoming && incoming[k]) {
        if (Number.isFinite(Number(incoming[k].slope)))  out[k].slope  = Number(incoming[k].slope);
        if (Number.isFinite(Number(incoming[k].offset))) out[k].offset = Number(incoming[k].offset);
      }
    });
    return out;
  }

  async function getCalibration(deviceId = SupabaseConfig.deviceId) {
    const localCalib = _readLocal();
    try {
      const res = await fetch(
        `${SupabaseConfig.restURL}/devices?id=eq.${encodeURIComponent(deviceId)}&select=*`,
        { headers: _headers() }
      );
      if (!res.ok) throw new Error(`HTTP ${res.status}`);
      const rows = await res.json();
      if (Array.isArray(rows) && rows.length > 0) {
        const d = rows[0];
        const remoteCalib = {
          ph:        { slope: d.ph_slope,   offset: d.ph_offset },
          do:        { slope: d.do_slope,   offset: d.do_offset },
          tds:       { slope: d.tds_slope,  offset: d.tds_offset },
          turbidity: { slope: d.turb_slope, offset: d.turb_offset },
        };
        const merged = _mergeCalib(localCalib, remoteCalib);
        _writeLocal(merged);
        return merged;
      }
    } catch (err) {
      console.warn('[SupabaseSync] Gagal ambil kalibrasi dari Supabase:', err.message);
    }
    return _mergeCalib(null, localCalib);
  }

  async function saveCalibration(partialCalib, deviceId = SupabaseConfig.deviceId) {
    const current = _mergeCalib(_readLocal(), partialCalib);
    _writeLocal(current);

    const corePayload = {
      ph_slope:    current.ph.slope,
      ph_offset:   current.ph.offset,
      tds_slope:   current.tds.slope,
      tds_offset:  current.tds.offset,
      turb_slope:  current.turbidity.slope,
      turb_offset: current.turbidity.offset,
    };
    const extendedPayload = {
      ...corePayload,
      do_slope:  current.do.slope,
      do_offset: current.do.offset,
    };

    const url = `${SupabaseConfig.restURL}/devices?id=eq.${encodeURIComponent(deviceId)}`;
    const reqInit = (body) => ({
      method: 'PATCH',
      headers: _headers({ Prefer: 'return=minimal' }),
      body: JSON.stringify(body),
    });

    let res = await fetch(url, reqInit(extendedPayload));
    if (res.status === 400) {
      // Tabel devices belum memiliki kolom do_slope/do_offset — simpan 6 kolom standar agar sinkronisasi tetap berhasil
      res = await fetch(url, reqInit(corePayload));
    }
    if (!res.ok) {
      const txt = await res.text().catch(() => '');
      throw new Error(`Supabase HTTP ${res.status}: ${txt}`);
    }
    return current;
  }

  async function getLatestSensorLogs(deviceId = SupabaseConfig.deviceId, limit = 1) {
    const res = await fetch(
      `${SupabaseConfig.restURL}/sensor_logs?device_id=eq.${encodeURIComponent(deviceId)}&select=*&order=created_at.desc&limit=${limit}`,
      { headers: _headers() }
    );
    if (!res.ok) throw new Error(`Supabase HTTP ${res.status}`);
    return await res.json();
  }

  return {
    DEFAULT_CALIBRATION,
    getCalibration,
    saveCalibration,
    getLatestSensorLogs,
    readLocalCalibration: _readLocal,
  };
})();

window.SupabaseConfig = SupabaseConfig;
window.SupabaseSync   = SupabaseSync;
