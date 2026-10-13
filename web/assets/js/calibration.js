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

async function loadCalibration() {
  try {
    const data = await ESP32API.getCalibration();
    CALIB_DEFS.forEach(def => {
      const s = data[def.apiKey];
      if (!s) return;
      const slopeEl  = document.getElementById(def.slopeId);
      const offsetEl = document.getElementById(def.offsetId);
      if (slopeEl)  slopeEl.value  = s.slope;
      if (offsetEl) offsetEl.value = s.offset;
      updateLiveDisplay(def.id, s.slope, s.offset);
    });
  } catch (err) {
    Utils.toast('Gagal memuat kalibrasi: ' + err.message, 'error');
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

async function saveCalib(def) {
  const btn = document.getElementById(`save-${def.id}`);
  const values = validateAndGetValues(def);
  if (!values) { Utils.toast('Validasi gagal — periksa input', 'error'); return; }

  Utils.btnLoading(btn, true);
  try {
    await ESP32API.postCalibration({ [def.apiKey]: values });
    updateLiveDisplay(def.id, values.slope, values.offset);
    Utils.toast(`Kalibrasi ${def.label} disimpan ke NVS`, 'success');
    setStatusMsg(def.id, 'Tersimpan', 'success');
  } catch (err) {
    Utils.toast(`Gagal menyimpan: ${err.message}`, 'error');
    setStatusMsg(def.id, err.message, 'error');
  } finally {
    Utils.btnLoading(btn, false);
  }
}

async function applyCalib(def) {
  const btn = document.getElementById(`apply-${def.id}`);
  const values = validateAndGetValues(def);
  if (!values) { Utils.toast('Validasi gagal — periksa input', 'error'); return; }

  Utils.btnLoading(btn, true);
  try {
    await ESP32API.postCalibration({ [def.apiKey]: values });
    updateLiveDisplay(def.id, values.slope, values.offset);
    Utils.toast(`Kalibrasi ${def.label} diterapkan`, 'success');
    setStatusMsg(def.id, 'Diterapkan', 'success');
  } catch (err) {
    Utils.toast(`Gagal menerapkan: ${err.message}`, 'error');
    setStatusMsg(def.id, err.message, 'error');
  } finally {
    Utils.btnLoading(btn, false);
  }
}

async function resetCalib(def) {
  const btn = document.getElementById(`reset-${def.id}`);
  if (!confirm(`Reset kalibrasi ${def.label} ke nilai default?`)) return;

  Utils.btnLoading(btn, true);
  try {
    await ESP32API.resetCalibration(def.apiKey);
    await loadCalibration();
    Utils.toast(`Kalibrasi ${def.label} direset`, 'info');
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
  const liveMap = {
    ph:         { valId: 'live-reading-ph',   val: sensorData.ph,            volt: voltageData?.ph_voltage },
    do:         { valId: 'live-reading-do',   val: sensorData.do_mg_l,       volt: voltageData?.do_voltage },
    tds:        { valId: 'live-reading-tds',  val: sensorData.tds_ppm,       volt: null },
    turbidity:  { valId: 'live-reading-turb', val: sensorData.turbidity_ntu, volt: null },
    temperature:{ valId: 'live-reading-temp', val: sensorData.temperature_c, volt: null },
    battery:    { valId: 'live-reading-bat',  val: sensorData.battery_voltage,volt: null },
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
