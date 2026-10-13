// control.js — Power control logic
'use strict';

let _sensorPower = false;
let _jetsonPower = false;

function updatePowerBtn(id, state) {
  const btn = document.getElementById(id);
  if (!btn) return;
  btn.className = `power-btn ${state ? 'on' : ''}`;
  btn.textContent = state ? 'ON' : 'OFF';

  const statusEl = document.getElementById(id.replace('-btn', '-status'));
  if (statusEl) {
    statusEl.className = `power-status-text ${state ? 'on' : ''}`;
    statusEl.textContent = state ? 'Aktif' : 'Tidak Aktif';
  }
}

async function toggleSensorPower() {
  const btn = document.getElementById('sensor-power-btn');
  const newState = !_sensorPower;
  btn.disabled = true;

  try {
    const res = await ESP32API.controlSensors(newState);
    _sensorPower = res.sensor_power;
    updatePowerBtn('sensor-power-btn', _sensorPower);
    Utils.toast(`Sensor ${_sensorPower ? 'diaktifkan' : 'dimatikan'}`, _sensorPower ? 'success' : 'info');
  } catch (err) {
    Utils.toast('Gagal mengontrol sensor: ' + err.message, 'error');
  } finally {
    btn.disabled = false;
  }
}

async function toggleJetsonPower() {
  const btn = document.getElementById('jetson-power-btn');
  const newState = !_jetsonPower;
  btn.disabled = true;

  try {
    const res = await ESP32API.controlJetson(newState);
    _jetsonPower = res.jetson_power;
    updatePowerBtn('jetson-power-btn', _jetsonPower);
    Utils.toast(`Jetson Nano ${_jetsonPower ? 'diaktifkan' : 'dimatikan'}`, _jetsonPower ? 'success' : 'info');
  } catch (err) {
    Utils.toast('Gagal mengontrol Jetson: ' + err.message, 'error');
  } finally {
    btn.disabled = false;
  }
}

function initControl() {
  const sBtnEl = document.getElementById('sensor-power-btn');
  const jBtnEl = document.getElementById('jetson-power-btn');
  if (sBtnEl) sBtnEl.addEventListener('click', toggleSensorPower);
  if (jBtnEl) jBtnEl.addEventListener('click', toggleJetsonPower);
}

function syncPowerState(sensorPower, jetsonPower) {
  if (sensorPower !== undefined) { _sensorPower = sensorPower; updatePowerBtn('sensor-power-btn', _sensorPower); }
  if (jetsonPower !== undefined) { _jetsonPower = jetsonPower; updatePowerBtn('jetson-power-btn', _jetsonPower); }
}

window.Control = { init: initControl, syncPowerState };
