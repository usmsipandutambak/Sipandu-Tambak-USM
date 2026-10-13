// mqtt-client.js — Paho MQTT WebSocket Client untuk Web Admin
'use strict';

const MQTTConfig = {
  host: 'broker.hivemq.com',
  port: 8000,             // WebSocket port default HiveMQ (atau 8884 wss)
  path: '/mqtt',
  useSSL: false,
  deviceId: 'Sipandu_A',
  enabled: true,
};

const MQTTClient = (() => {
  let _client = null;
  let _isConnected = false;
  let _reconnectTimer = null;

  function _getTopicTelemetry() { return `sipandu/devices/${MQTTConfig.deviceId}/telemetry`; }
  function _getTopicCommand()   { return `sipandu/devices/${MQTTConfig.deviceId}/command`; }
  function _getTopicStatus()    { return `sipandu/devices/${MQTTConfig.deviceId}/status`; }
  function _getTopicCamera()    { return `sipandu/devices/${MQTTConfig.deviceId}/camera`; }

  function init() {
    if (!window.Paho || !window.Paho.MQTT) {
      console.warn('[MQTT] Library Paho MQTT tidak ditemukan di window.');
      return;
    }
    if (!MQTTConfig.enabled) {
      console.log('[MQTT] Mode MQTT dinonaktifkan.');
      return;
    }
    connect();
  }

  function connect() {
    if (_isConnected || (_client && _client.isConnected && _client.isConnected())) return;

    const clientId = `WebAdmin_${MQTTConfig.deviceId}_${Math.random().toString(16).substr(2, 6)}`;
    console.log(`[MQTT] Menghubungkan ke ${MQTTConfig.host}:${MQTTConfig.port} (WebSocket)...`);

    try {
      _client = new Paho.MQTT.Client(MQTTConfig.host, Number(MQTTConfig.port), MQTTConfig.path, clientId);

      _client.onConnectionLost = (responseObject) => {
        _isConnected = false;
        console.warn('[MQTT] Koneksi terputus:', responseObject.errorMessage);
        _updateUIBadge(false);
        // Coba konek ulang tiap 5 detik
        clearTimeout(_reconnectTimer);
        _reconnectTimer = setTimeout(connect, 5000);
      };

      _client.onMessageArrived = (message) => {
        _handleIncomingMessage(message.destinationName, message.payloadString);
      };

      _client.connect({
        useSSL: MQTTConfig.useSSL,
        timeout: 5,
        onSuccess: () => {
          _isConnected = true;
          console.log('[MQTT] Berhasil terhubung ke broker Paho!');
          _updateUIBadge(true);

          // Subscribe topic telemetri dan status
          _client.subscribe(_getTopicTelemetry());
          _client.subscribe(_getTopicStatus());
          _client.subscribe(_getTopicCamera());
        },
        onFailure: (err) => {
          _isConnected = false;
          console.warn('[MQTT] Gagal terhubung ke broker:', err.errorMessage);
          _updateUIBadge(false);
          clearTimeout(_reconnectTimer);
          _reconnectTimer = setTimeout(connect, 7000);
        }
      });
    } catch (e) {
      console.error('[MQTT] Inisialisasi error:', e);
    }
  }

  function disconnect() {
    clearTimeout(_reconnectTimer);
    if (_client && _isConnected) {
      try { _client.disconnect(); } catch (e) {}
    }
    _isConnected = false;
    _updateUIBadge(false);
  }

  function _handleIncomingMessage(topic, payloadStr) {
    try {
      const data = JSON.parse(payloadStr);

      if (topic === _getTopicTelemetry()) {
        // Teruskan data telemetri real-time langsung ke modul Realtime & Dashboard
        if (window.Realtime) {
          Realtime._feedMqttData(data);
        }
        if (window.Control && data.sensor_power !== undefined) {
          Control.syncPowerState(data.sensor_power, data.jetson_power);
        }
      } else if (topic === _getTopicStatus()) {
        console.log('[MQTT] Status perangkat diperbarui:', data);
        if (data.jetson_power !== undefined && window.Control) {
          Control.syncPowerState(data.sensor_power, data.jetson_power);
        }
      }
    } catch (e) {
      console.warn('[MQTT] Gagal parse JSON payload:', e);
    }
  }

  function sendCommand(payloadObj) {
    if (!_isConnected || !_client) {
      console.warn('[MQTT] Tidak dapat mengirim perintah — MQTT belum terhubung.');
      return false;
    }
    try {
      const message = new Paho.MQTT.Message(JSON.stringify(payloadObj));
      message.destinationName = _getTopicCommand();
      message.qos = 1;
      _client.send(message);
      console.log('[MQTT] Perintah berhasil dikirim ke:', _getTopicCommand(), payloadObj);
      return true;
    } catch (e) {
      console.error('[MQTT] Gagal kirim perintah:', e);
      return false;
    }
  }

  function _updateUIBadge(connected) {
    const badge = document.getElementById('conn-badge');
    const text = document.getElementById('conn-badge-text');
    if (badge && text) {
      if (connected) {
        badge.className = 'connection-badge online';
        text.textContent = 'Cloud MQTT Live';
      }
    }
  }

  return {
    init,
    connect,
    disconnect,
    sendCommand,
    isConnected: () => _isConnected,
    config: MQTTConfig
  };
})();

window.MQTTConfig = MQTTConfig;
window.MQTTClient = MQTTClient;
