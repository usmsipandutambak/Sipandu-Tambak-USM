// calibration.js — Calibration form handling
'use strict';

const CALIB_DEFS = [
  {
    id: 'ph', label: 'pH',
    slopeId: 'ph-slope', offsetId: 'ph-offset',
    slopeRange: [-50, 0], offsetRange: [0, 100],
    apiKey: 'ph',
  },
  {
    id: 'do', label: 'DO',
    slopeId: 'do-slope', offsetId: 'do-offset',
    slopeRange: [0, 100], offsetRange: [-50, 50],
    apiKey: 'do',
  },
  {
    id: 'tds', label: 'TDS',
    slopeId: 'tds-slope', offsetId: 'tds-offset',
    slopeRange: [0, 5000], offsetRange: [-1000, 1000],
    apiKey: 'tds',
  },
  {
    id: 'turbidity', label: 'Turbidity',
    slopeId: 'turb-slope', offsetId: 'turb-offset',
    slopeRange: [-10000, 0], offsetRange: [0, 20000],
    apiKey: 'turbidity',
  },
];

function _applyCalibrationToInputs(data) {
  if (!data) return;
  CALIB_DEFS.forEach(def => {
    const s = data[def.apiKey];
    if (!s) return;
    const slopeEl  = document.getElementById(def.slopeId);
    const offsetEl = document.getElementById(def.offsetId);
    if (slopeEl && s.slope !== undefined && s.slope !== null)  slopeEl.value  = s.slope;
    if (offsetEl && s.offset !== undefined && s.offset !== null) offsetEl.value = s.offset;
    updateLiveDisplay(def.id, s.slope, s.offset);
  });
}

async function loadCalibration() {
  if (window.SupabaseSync) {
    try {
      const supaData = await SupabaseSync.getCalibration();
      _applyCalibrationToInputs(supaData);
    } catch (_) {}
  }

  try {
    const espData = await ESP32API.getCalibration();
    _applyCalibrationToInputs(espData);
    if (window.SupabaseSync) {
      await SupabaseSync.saveCalibration(espData).catch(() => {});
    }
  } catch (err) {
    if (!window.SupabaseSync) {
      Utils.toast('Gagal memuat kalibrasi: ' + err.message, 'error');
    }
  }
}

function updateLiveDisplay(id, slope, offset) {
  const sEl = document.getElementById(`live-slope-${id}`);
  const oEl = document.getElementById(`live-offset-${id}`);
  if (sEl) sEl.textContent = Number(slope).toFixed(4);
  if (oEl) oEl.textContent = Number(offset).toFixed(4);
}

function validateAndGetValues(def) {
  const sRes = Utils.validateFloat(document.getElementById(def.slopeId)?.value,  def.slopeRange[0],  def.slopeRange[1]);
  const oRes = Utils.validateFloat(document.getElementById(def.offsetId)?.value, def.offsetRange[0], def.offsetRange[1]);

  const sErr = document.getElementById(`${def.id}-slope-err`);
  const oErr = document.getElementById(`${def.id}-offset-err`);

  if (sErr) { sErr.style.display = sRes.ok ? 'none' : 'block'; sErr.textContent = sRes.msg || ''; }
  if (oErr) { oErr.style.display = oRes.ok ? 'none' : 'block'; oErr.textContent = oRes.msg || ''; }

  if (!sRes.ok || !oRes.ok) return null;
  return { slope: sRes.val, offset: oRes.val };
}

async function _syncCalib(def, btn, actionLabel) {
  const values = validateAndGetValues(def);
  if (!values) { Utils.toast('Validasi gagal — periksa input', 'error'); return; }

  Utils.btnLoading(btn, true);
  let supabaseOk = false;
  let esp32Ok = false;
  let espError = '';

  try {
    if (window.SupabaseSync) {
      await SupabaseSync.saveCalibration({ [def.apiKey]: values });
      supabaseOk = true;
    }
  } catch (err) {
    console.warn('[Calibration] Gagal sinkron ke Supabase:', err.message);
  }

  try {
    await ESP32API.postCalibration({ [def.apiKey]: values });
    esp32Ok = true;
  } catch (err) {
    espError = err.message;
  }

  updateLiveDisplay(def.id, values.slope, values.offset);

  if (supabaseOk && esp32Ok) {
    Utils.toast(`Kalibrasi ${def.label} ${actionLabel} ke Supabase & ESP32`, 'success');
    setStatusMsg(def.id, 'Tersimpan (Supabase + ESP32)', 'success');
  } else if (supabaseOk) {
    Utils.toast(`Kalibrasi ${def.label} disimpan ke Supabase (ESP32 offline: ${espError})`, 'info');
    setStatusMsg(def.id, 'Tersimpan di Supabase (ESP32 offline)', 'info');
  } else if (esp32Ok) {
    Utils.toast(`Kalibrasi ${def.label} ${actionLabel} ke ESP32`, 'success');
    setStatusMsg(def.id, 'Tersimpan di ESP32', 'success');
  } else {
    Utils.toast(`Gagal menyimpan: ${espError || 'Periksa koneksi Supabase/ESP32'}`, 'error');
    setStatusMsg(def.id, espError || 'Gagal menyimpan', 'error');
  }

  Utils.btnLoading(btn, false);
}

async function saveCalib(def) {
  return _syncCalib(def, document.getElementById(`save-${def.id}`), 'disimpan');
}

async function applyCalib(def) {
  return _syncCalib(def, document.getElementById(`apply-${def.id}`), 'diterapkan');
}

async function resetCalib(def) {
  const btn = document.getElementById(`reset-${def.id}`);
  if (!confirm(`Reset kalibrasi ${def.label} ke nilai default?`)) return;

  Utils.btnLoading(btn, true);
  try {
    const defValues = window.SupabaseSync?.DEFAULT_CALIBRATION?.[def.apiKey];
    if (window.SupabaseSync && defValues) {
      await SupabaseSync.saveCalibration({ [def.apiKey]: defValues }).catch(() => {});
    }
    try {
      await ESP32API.resetCalibration(def.apiKey);
    } catch (_) {}
    await loadCalibration();
    Utils.toast(`Kalibrasi ${def.label} direset ke default`, 'info');
    setStatusMsg(def.id, 'Reset ke default', 'info');
  } catch (err) {
    Utils.toast(`Gagal reset: ${err.message}`, 'error');
  } finally {
    Utils.btnLoading(btn, false);
  }
}

function setStatusMsg(id, msg, type) {
  const el = document.getElementById(`calib-status-${id}`);
  if (!el) return;
  const colors = { success: 'var(--color-success)', error: 'var(--color-danger)', info: 'var(--color-primary)' };
  el.style.color = colors[type] || 'inherit';
  el.textContent = msg;
  setTimeout(() => { if (el) el.textContent = ''; }, 4000);
}

function initCalibration() {
  CALIB_DEFS.forEach(def => {
    const saveBtn  = document.getElementById(`save-${def.id}`);
    const applyBtn = document.getElementById(`apply-${def.id}`);
    const resetBtn = document.getElementById(`reset-${def.id}`);
    if (saveBtn)  saveBtn.addEventListener('click',  () => saveCalib(def));
    if (applyBtn) applyBtn.addEventListener('click', () => applyCalib(def));
    if (resetBtn) resetBtn.addEventListener('click', () => resetCalib(def));
  });
}

// Update live readings from Realtime data
function updateCalibLiveReadings(sensorData, voltageData) {
  if (!sensorData) return;
  const doVal = sensorData.do_mg_l ?? sensorData.do_level ?? sensorData.dissolved_oxygen ?? sensorData.do;
  const liveMap = {
    ph:         { valId: 'live-reading-ph',   val: sensorData.ph ?? sensorData.ph_level,     volt: voltageData?.ph_voltage },
    do:         { valId: 'live-reading-do',   val: doVal,                                     volt: voltageData?.do_voltage },
    tds:        { valId: 'live-reading-tds',  val: sensorData.tds_ppm ?? sensorData.tds,      volt: null },
    turbidity:  { valId: 'live-reading-turb', val: sensorData.turbidity_ntu ?? sensorData.turbidity, volt: null },
    temperature:{ valId: 'live-reading-temp', val: sensorData.temperature_c ?? sensorData.temperature, volt: null },
    battery:    { valId: 'live-reading-bat',  val: sensorData.battery_voltage,                volt: null },
  };
  Object.entries(liveMap).forEach(([k, v]) => {
    const el = document.getElementById(v.valId);
    if (el) el.textContent = v.val !== undefined ? Number(v.val).toFixed(2) : '—';
    if (v.volt !== undefined) {
      const ve = document.getElementById(`live-volt-${k}`);
      if (ve) ve.textContent = v.volt !== null ? Number(v.volt).toFixed(4) + ' V' : '—';
    }
  });
}

window.Calibration = { init: initCalibration, load: loadCalibration, updateLiveReadings: updateCalibLiveReadings };
