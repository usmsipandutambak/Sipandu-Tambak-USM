// app.js — Application entry point
'use strict';

document.addEventListener('DOMContentLoaded', () => {

  // ── Mobile sidebar toggle ──────────────────────────────────────────────
  const menuToggle = document.getElementById('menu-toggle');
  const sidebar    = document.querySelector('.sidebar');
  const overlay    = document.querySelector('.sidebar-overlay');

  if (menuToggle && sidebar && overlay) {
    menuToggle.addEventListener('click', () => {
      sidebar.classList.toggle('open');
      overlay.classList.toggle('active');
    });
    overlay.addEventListener('click', () => {
      sidebar.classList.remove('open');
      overlay.classList.remove('active');
    });
  }

  // ── ESP32 IP Configuration ─────────────────────────────────────────────
  const ipInput    = document.getElementById('esp32-ip-input');
  const ipSaveBtn  = document.getElementById('esp32-ip-save');
  const headerIpEl = document.getElementById('header-ip');
  let rawSavedIP   = (localStorage.getItem('esp32_ip') || sessionStorage.getItem('esp32_ip') || '192.168.100.213').trim();
  if (rawSavedIP === '10.146.87.127') {
    rawSavedIP = '192.168.100.213';
    localStorage.setItem('esp32_ip', rawSavedIP);
    sessionStorage.setItem('esp32_ip', rawSavedIP);
  }
  const savedIP    = rawSavedIP;

  if (savedIP) {
    const cleanIP = savedIP.replace(/^https?:\/\//i, '').replace(/\/+$/, '');
    ESP32API.setBaseURL('http://' + cleanIP);
    if (ipInput) ipInput.value = cleanIP;
    if (headerIpEl) headerIpEl.textContent = cleanIP;
  }

  if (ipSaveBtn && ipInput) {
    ipSaveBtn.addEventListener('click', () => {
      const ip = ipInput.value.trim().replace(/^https?:\/\//i, '').replace(/\/+$/, '');
      if (!ip) { Utils.toast('Masukkan alamat IP ESP32', 'error'); return; }
      ESP32API.setBaseURL('http://' + ip);
      localStorage.setItem('esp32_ip', ip);
      sessionStorage.setItem('esp32_ip', ip);
      if (headerIpEl) headerIpEl.textContent = ip;
      Realtime.restart();
      Utils.toast('IP ESP32 diperbarui: ' + ip, 'success');
    });
  }

  // ── Jetson Nano IP Configuration (camera stream) ─────────────────────────
  const jetsonInput    = document.getElementById('jetson-ip-input');
  const jetsonSaveBtn  = document.getElementById('jetson-ip-save');
  let rawSavedJetson   = (localStorage.getItem('jetson_host') || sessionStorage.getItem('jetson_host') || '192.168.100.41').trim();
  if (rawSavedJetson === '10.146.87.247' || rawSavedJetson === 'http://10.146.87.247:5000/video_feed') {
    rawSavedJetson = '192.168.100.41';
    localStorage.setItem('jetson_host', rawSavedJetson);
    sessionStorage.setItem('jetson_host', rawSavedJetson);
  }
  const savedJetson    = rawSavedJetson;

  if (savedJetson) {
    JetsonAPI.setHost(savedJetson);
    if (jetsonInput) jetsonInput.value = savedJetson;
  }

  if (jetsonSaveBtn && jetsonInput) {
    jetsonSaveBtn.addEventListener('click', () => {
      const host = jetsonInput.value.trim();
      if (!host) { Utils.toast('Masukkan alamat IP Jetson Nano', 'error'); return; }
      JetsonAPI.setHost(host);
      localStorage.setItem('jetson_host', host);
      sessionStorage.setItem('jetson_host', host);
      CameraModule.restart();
      Utils.toast('Stream Jetson dimuat: ' + JetsonAPI.getStreamURL(), 'success');
    });
  }

  // ── Initialize modules ─────────────────────────────────────────────────
  Dashboard.init();
  Control.init();
  Calibration.init();
  CameraModule.init();
  DataModule.init();

  // ── Router setup ───────────────────────────────────────────────────────
  Router.on('home',    () => { CameraModule.start(); });
  Router.on('control', () => { Calibration.load(); CameraModule.pause(); });
  Router.on('camera',  () => { CameraModule.start(); });
  Router.on('data',    () => { CameraModule.pause(); });
  Router.init();

  // ── Realtime sensor updates to calibration page ────────────────────────
  Realtime.on('sensors', (data) => {
    if (data && data.sensors) {
      Calibration.updateLiveReadings(data.sensors, data.voltages);
    }
  });

  // ── Start polling ──────────────────────────────────────────────────────
  Realtime.start();

  console.log('[SIPANDU] App initialized.');
});
