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
  const savedIP    = sessionStorage.getItem('esp32_ip');

  if (savedIP) {
    ESP32API.setBaseURL('http://' + savedIP);
    if (ipInput) ipInput.value = savedIP;
  }

  if (ipSaveBtn && ipInput) {
    ipSaveBtn.addEventListener('click', () => {
      const ip = ipInput.value.trim();
      if (!ip) { Utils.toast('Masukkan alamat IP ESP32', 'error'); return; }
      ESP32API.setBaseURL('http://' + ip);
      sessionStorage.setItem('esp32_ip', ip);
      Realtime.restart();
      Utils.toast('IP ESP32 diperbarui: ' + ip, 'success');
    });
  }

  // ── Jetson Nano IP Configuration (camera stream) ─────────────────────────
  const jetsonInput    = document.getElementById('jetson-ip-input');
  const jetsonSaveBtn  = document.getElementById('jetson-ip-save');
  const savedJetson    = sessionStorage.getItem('jetson_host');

  if (savedJetson) {
    JetsonAPI.setHost(savedJetson);
    if (jetsonInput) jetsonInput.value = savedJetson;
  }

  if (jetsonSaveBtn && jetsonInput) {
    jetsonSaveBtn.addEventListener('click', () => {
      const host = jetsonInput.value.trim();
      if (!host) { Utils.toast('Masukkan alamat IP Jetson Nano', 'error'); return; }
      JetsonAPI.setHost(host);
      sessionStorage.setItem('jetson_host', host);
      CameraModule.restart();
      Utils.toast('IP Jetson diperbarui: ' + JetsonAPI.getBaseURL(), 'success');
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
