// data.js — Temporary in-memory data table (no persistence)
'use strict';

const DataModule = (() => {
  let _rows = [];   // in-memory only, cleared on page reload
  const MAX_ROWS = 100;

  const COLS = [
    { key: 'ts',              label: 'Waktu',        fmt: v => Utils.fmtTime(v) },
    { key: 'ph',              label: 'pH',           fmt: v => Number(v).toFixed(2) },
    { key: 'do_mg_l',         label: 'DO (mg/L)',    fmt: v => Number(v).toFixed(2) },
    { key: 'tds_ppm',         label: 'TDS (ppm)',    fmt: v => Number(v).toFixed(0) },
    { key: 'turbidity_ntu',   label: 'Turbidity (NTU)', fmt: v => Number(v).toFixed(1) },
    { key: 'temperature_c',   label: 'Suhu (°C)',    fmt: v => Number(v).toFixed(1) },
    { key: 'battery_voltage', label: 'Baterai (V)',  fmt: v => Number(v).toFixed(2) },
    { key: 'status',          label: 'Status',       fmt: v => v },
  ];

  function _statusText(sensors) {
    const { ph, turbidity_ntu } = sensors;
    if (ph < 6.0 || ph > 9.0 || turbidity_ntu > 100) return 'BAHAYA';
    if (ph < 6.5 || ph > 8.5 || turbidity_ntu > 50)  return 'WASPADA';
    return 'NORMAL';
  }

  function buildTable() {
    const thead = document.getElementById('data-thead');
    const tbody = document.getElementById('data-tbody');
    if (!thead || !tbody) return;

    thead.innerHTML = '<tr>' + COLS.map(c => `<th>${c.label}</th>`).join('') + '</tr>';
    _renderBody(tbody);
  }

  function _renderBody(tbody) {
    if (!tbody) tbody = document.getElementById('data-tbody');
    if (!tbody) return;

    if (_rows.length === 0) {
      tbody.innerHTML = `<tr><td colspan="${COLS.length}" class="text-center text-muted" style="padding:24px">
        Belum ada data — data akan muncul secara otomatis setiap 3 detik</td></tr>`;
      return;
    }

    tbody.innerHTML = [..._rows].reverse().map(row => `
      <tr>
        ${COLS.map(c => {
          const val = row[c.key];
          const formatted = (val !== null && val !== undefined) ? c.fmt(val) : '—';
          const cls = c.key === 'status' ? _statusCls(formatted) : '';
          return `<td class="${cls}">${formatted}</td>`;
        }).join('')}
      </tr>
    `).join('');

    // Row count
    const countEl = document.getElementById('data-row-count');
    if (countEl) countEl.textContent = `${_rows.length} pembacaan (sesi ini)`;
  }

  function _statusCls(s) {
    if (s === 'NORMAL')  return 'status-val' ;
    if (s === 'WASPADA') return 'status-val' ;
    if (s === 'BAHAYA')  return 'status-val' ;
    return '';
  }

  function addRow(sensors) {
    if (!sensors) return;
    const row = {
      ts:              Date.now(),
      ph:              sensors.ph,
      do_mg_l:         sensors.do_mg_l,
      tds_ppm:         sensors.tds_ppm,
      turbidity_ntu:   sensors.turbidity_ntu,
      temperature_c:   sensors.temperature_c,
      battery_voltage: sensors.battery_voltage,
      status:          _statusText(sensors),
    };
    _rows.push(row);
    if (_rows.length > MAX_ROWS) _rows.shift();
    _renderBody();
  }

  return {
    init() {
      buildTable();
      Realtime.on('sensors', (data) => {
        if (data && data.sensors) addRow(data.sensors);
      });
    },
  };
})();

window.DataModule = DataModule;
