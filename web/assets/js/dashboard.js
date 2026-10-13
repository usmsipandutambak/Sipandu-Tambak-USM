// dashboard.js — Home page logic (sensor display + charts)
'use strict';

// ── Water quality thresholds (aquaculture references) ─────────────────────
const THRESHOLDS = {
  ph:          { warn_lo: 6.5, crit_lo: 6.0, warn_hi: 8.5, crit_hi: 9.0, unit: '' },
  do_mg_l:     { warn_lo: 4.0, crit_lo: 3.0, warn_hi: 14.0, crit_hi: 20.0, unit: 'mg/L' },
  tds_ppm:     { warn_lo: 0,   crit_lo: 0,   warn_hi: 800,  crit_hi: 1200, unit: 'ppm' },
  turbidity_ntu:{ warn_lo: 0,  crit_lo: 0,   warn_hi: 50,   crit_hi: 100,  unit: 'NTU' },
  temperature_c:{ warn_lo: 22, crit_lo: 18,  warn_hi: 32,   crit_hi: 35,   unit: '°C' },
  battery_voltage:{ warn_lo: 11.5, crit_lo: 11.0, warn_hi: 14.0, crit_hi: 15.0, unit: 'V' },
};

function getSensorStatus(key, val) {
  const t = THRESHOLDS[key];
  if (!t) return 'normal';
  if (val <= t.crit_lo || val >= t.crit_hi) return 'danger';
  if (val <= t.warn_lo || val >= t.warn_hi) return 'warning';
  return 'normal';
}

// ── Sensor Card Definitions ───────────────────────────────────────────────
const SENSOR_DEFS = [
  { id: 'ph',             key: 'ph',              label: 'pH',         unit: '',     decimals: 2, hwKey: 'ads_a' },
  { id: 'do',             key: 'do_mg_l',         label: 'DO',         unit: 'mg/L', decimals: 2, hwKey: 'ads_a' },
  { id: 'tds',            key: 'tds_ppm',         label: 'TDS',        unit: 'ppm',  decimals: 0, hwKey: 'ads_b' },
  { id: 'turbidity',      key: 'turbidity_ntu',   label: 'Turbidity',  unit: 'NTU',  decimals: 1, hwKey: 'ads_b' },
  { id: 'temperature',    key: 'temperature_c',   label: 'Suhu',       unit: '°C',   decimals: 1, hwKey: 'ds18b20' },
  { id: 'battery',        key: 'battery_voltage', label: 'Baterai',    unit: 'V',    decimals: 2, hwKey: 'ina226' },
];

function buildSensorCards() {
  const grid = document.getElementById('sensor-grid');
  if (!grid) return;
  grid.innerHTML = SENSOR_DEFS.map(s => `
    <div class="sensor-card" id="sc-${s.id}">
      <div class="sensor-card-top">
        <span class="sensor-name">${s.label}</span>
        <span class="sensor-indicator" id="si-${s.id}"></span>
      </div>
      <div class="sensor-value-wrap">
        <span class="sensor-value" id="sv-${s.id}">—</span>
        <span class="sensor-unit">${s.unit}</span>
      </div>
      <span class="sensor-badge badge-offline" id="sb-${s.id}">Offline</span>
    </div>
  `).join('');
}

function updateSensorCards(sensorData, sensorStatus, isConnected) {
  SENSOR_DEFS.forEach(s => {
    const card      = document.getElementById(`sc-${s.id}`);
    const valEl     = document.getElementById(`sv-${s.id}`);
    const indEl     = document.getElementById(`si-${s.id}`);
    const badgeEl   = document.getElementById(`sb-${s.id}`);
    if (!card || !valEl || !indEl || !badgeEl) return;

    // Determine hardware sensor OK status
    const hwMap = { ads_a: 'ads_a', ads_b: 'ads_b', ds18b20: 'ds18b20', ina226: 'ina226' };
    const hwOK = sensorStatus ? sensorStatus[hwMap[s.hwKey]] : false;

    if (!isConnected || !sensorData) {
      valEl.textContent = '—';
      indEl.className = 'sensor-indicator error';
      badgeEl.className = 'sensor-badge badge-offline';
      badgeEl.textContent = 'Offline';
      return;
    }

    const val = sensorData[s.key];
    if (val === undefined || val === null) {
      valEl.textContent = '—';
      indEl.className = 'sensor-indicator ' + (hwOK ? 'ok' : 'error');
      badgeEl.className = 'sensor-badge badge-offline';
      badgeEl.textContent = hwOK ? 'No Data' : 'Error';
      return;
    }

    const status = getSensorStatus(s.key, val);
    valEl.textContent = Number(val).toFixed(s.decimals);
    indEl.className = 'sensor-indicator ' + (hwOK ? 'ok' : 'error');

    const badgeMap = { normal: 'badge-normal Normal', warning: 'badge-warning Peringatan', danger: 'badge-danger Kritis' };
    const [cls, txt] = (badgeMap[status] || 'badge-normal Normal').split(' ');
    badgeEl.className = `sensor-badge ${cls}`;
    badgeEl.textContent = txt;
  });
}

function updateESP32Status(statusData, isConnected) {
  const dot      = document.getElementById('conn-dot');
  const badge    = document.getElementById('conn-badge');
  const badgeText = document.getElementById('conn-badge-text');
  const ipEl     = document.getElementById('header-ip');
  const uptimeEl = document.getElementById('esp-uptime');

  if (badge) badge.className = `connection-badge ${isConnected ? 'online' : 'offline'}`;
  if (badgeText) badgeText.textContent = isConnected ? 'ESP32 Online' : 'ESP32 Offline';
  if (ipEl) ipEl.textContent = (isConnected && statusData) ? statusData.ip_address || '—' : '—';
  if (uptimeEl && isConnected && statusData) uptimeEl.textContent = 'Uptime: ' + Utils.fmtUptime(statusData.uptime_ms);
}

function updateLastUpdate() {
  const el = document.getElementById('last-update');
  if (el) el.textContent = 'Update: ' + Utils.fmtTime(Date.now());
}

// ── History buffer for mini charts ───────────────────────────────────────
const MAX_HISTORY = 20;
const _history = {};
SENSOR_DEFS.forEach(s => { _history[s.key] = []; });

function pushHistory(sensorData) {
  if (!sensorData) return;
  SENSOR_DEFS.forEach(s => {
    const v = sensorData[s.key];
    if (v !== null && v !== undefined) {
      _history[s.key].push(v);
      if (_history[s.key].length > MAX_HISTORY) _history[s.key].shift();
    }
  });
}

// ── Module init ───────────────────────────────────────────────────────────
function initDashboard() {
  buildSensorCards();

  Realtime.on('sensors', (data) => {
    const isConn = Realtime.isConnected();
    if (data) {
      updateSensorCards(data.sensors, data.sensor_status, isConn);
      pushHistory(data.sensors);
      updateLastUpdate();
    } else {
      updateSensorCards(null, null, false);
    }
  });

  Realtime.on('status', (data) => {
    updateESP32Status(data, Realtime.isConnected());
    // Sync power button state if status available
    if (data) {
      Control.syncPowerState(data.sensor_power, data.jetson_power);
    }
  });

  Realtime.on('connection', (connected) => {
    updateESP32Status(null, connected);
    if (!connected) {
      updateSensorCards(null, null, false);
    }
  });
}

window.Dashboard = { init: initDashboard };
