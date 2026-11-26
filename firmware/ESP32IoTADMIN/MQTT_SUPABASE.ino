#include <HTTPClient.h>
#include <PubSubClient.h>
#include <WiFiClientSecure.h>

WiFiClient espMqttClient;
PubSubClient mqttClient(espMqttClient);

static unsigned long lastMqttPublishMs = 0;
static unsigned long lastSupabaseUploadMs = 0;
static unsigned long lastMqttReconnectMs = 0;
static bool firstSupabaseUploadDone = false;

static const char *evaluateWaterStatus() {
  if (phLevel < 6.0f || phLevel > 9.0f || doMgL < 3.0f || temperatureC < 22.0f || temperatureC > 34.0f || tdsPpm > 1000.0f) {
    return "BAHAYA";
  }
  if (phLevel < 6.5f || phLevel > 8.5f || doMgL < 5.0f || temperatureC < 25.0f || temperatureC > 32.0f || tdsPpm > 500.0f) {
    return "WASPADA";
  }
  return "NORMAL";
}

void initMQTT() {
  TOPIC_TELEMETRY = String("sipandu/devices/") + DEVICE_ID + "/telemetry";
  TOPIC_COMMAND = String("sipandu/devices/") + DEVICE_ID + "/command";
  TOPIC_STATUS = String("sipandu/devices/") + DEVICE_ID + "/status";

  mqttClient.setServer(MQTT_BROKER, MQTT_PORT);
  mqttClient.setCallback(mqttCallback);
  mqttClient.setBufferSize(768); // Buffer JSON telemetri
  mqttClient.setSocketTimeout(1); // Cegah blocking lama jika broker Jetson offline

  Serial.println(F("[MQTT] Inisialisasi MQTT Client (Paho compatible)"));
  Serial.printf("[MQTT] Broker: %s:%d\n", MQTT_BROKER, MQTT_PORT);
  Serial.printf("[MQTT] Subscribing: %s\n", TOPIC_COMMAND.c_str());
}

void connectMQTT() {
  if (WiFi.status() != WL_CONNECTED)
    return;
  if (mqttClient.connected())
    return;

  unsigned long now = millis();
  if (now - lastMqttReconnectMs < 15000)
    return; // Jeda 15 detik antar-percobaan reconnect jika Jetson offline
  lastMqttReconnectMs = now;

  Serial.print(F("[MQTT] Menghubungkan ke broker MQTT... "));
  String clientId =
      String("ESP32_") + DEVICE_ID + "_" + String(random(0xffff), HEX);

  bool ok = false;
  if (strlen(MQTT_USER) > 0) {
    ok = mqttClient.connect(clientId.c_str(), MQTT_USER, MQTT_PASS);
  } else {
    ok = mqttClient.connect(clientId.c_str());
  }

  if (ok) {
    Serial.println(F("TERHUBUNG!"));
    mqttClient.subscribe(TOPIC_COMMAND.c_str());

    // Publish status awal online
    publishDeviceStatus();
  } else {
    Serial.printf("GAGAL (rc=%d)\n", mqttClient.state());
  }
}

void handleMQTT() {
  if (!mqttClient.connected()) {
    connectMQTT();
  } else {
    mqttClient.loop();
  }
}

void publishTelemetryMQTT() {
  if (!mqttClient.connected())
    return;

  unsigned long now = millis();
  if (now - lastMqttPublishMs < MQTT_PUBLISH_INTERVAL_MS)
    return;
  lastMqttPublishMs = now;

  StaticJsonDocument<512> doc;
  doc["device_id"] = DEVICE_ID;
  doc["pond_number"] = POND_NUMBER;
  doc["timestamp_ms"] = millis();
  doc["uptime_ms"] = millis();
  doc["sensor_power"] = sensorPowerState;
  doc["jetson_power"] = jetsonPowerState;
  doc["jetson_shutdown_pending"] = jetsonShutdownPending;

  JsonObject sens = doc.createNestedObject("sensors");
  sens["ph"] = isnan(phLevel) ? 0.0f : phLevel;
  sens["do_mg_l"] = isnan(doMgL) ? 0.0f : doMgL;
  sens["tds_ppm"] = isnan(tdsPpm) ? 0.0f : tdsPpm;
  sens["turbidity_ntu"] = isnan(turbidityNTU) ? 0.0f : turbidityNTU;
  sens["temperature_c"] = isnan(temperatureC) ? 0.0f : temperatureC;
  sens["battery_voltage"] = isnan(batVoltage) ? 0.0f : batVoltage;

  JsonObject stat = doc.createNestedObject("sensor_status");
  stat["ds18b20"] = isDs18b20OK;
  stat["ads_a"] = isAdsAOK;
  stat["ads_b"] = isAdsBOK;
  stat["ina226"] = isIna226OK;

  String jsonStr;
  serializeJson(doc, jsonStr);

  bool ok = mqttClient.publish(TOPIC_TELEMETRY.c_str(), jsonStr.c_str());
  if (ok) {
    Serial.printf("[MQTT] Publish telemetry OK -> %s (%u bytes)\n",
                  TOPIC_TELEMETRY.c_str(), jsonStr.length());
  } else {
    Serial.printf("[MQTT] Publish telemetry GAGAL -> %s (state=%d)\n",
                  TOPIC_TELEMETRY.c_str(), mqttClient.state());
  }
}

void publishDeviceStatus() {
  if (!mqttClient.connected())
    return;

  StaticJsonDocument<256> doc;
  doc["device_id"] = DEVICE_ID;
  doc["pond_number"] = POND_NUMBER;
  doc["online"] = true;
  doc["ip_address"] = WiFi.localIP().toString();
  doc["sensor_power"] = sensorPowerState;
  doc["jetson_power"] = jetsonPowerState;
  doc["jetson_shutdown_pending"] = jetsonShutdownPending;

  String jsonStr;
  serializeJson(doc, jsonStr);
  mqttClient.publish(TOPIC_STATUS.c_str(), jsonStr.c_str(),
                     true); // Retain flag
}

void publishJetsonShutdownNotice(int secondsRemaining) {
  if (!mqttClient.connected())
    return;

  StaticJsonDocument<192> doc;
  doc["device_id"] = DEVICE_ID;
  doc["event"] = "jetson_shutdown_countdown";
  doc["seconds_left"] = secondsRemaining;
  doc["status"] = "WAITING_SHUTDOWN";

  String jsonStr;
  serializeJson(doc, jsonStr);
  mqttClient.publish(TOPIC_STATUS.c_str(), jsonStr.c_str());
}

// Callback saat menerima pesan di TOPIC_COMMAND dari Mobile / Web Admin
void mqttCallback(char *topic, byte *payload, unsigned int length) {
  Serial.printf("[MQTT] Pesan diterima di topic [%s] (len=%u): ", topic,
                length);
  char buf[513];
  memset(buf, 0, sizeof(buf));
  unsigned int copyLen = (length < 512) ? length : 512;
  memcpy(buf, payload, copyLen);
  buf[copyLen] = '\0';
  Serial.println(buf);

  StaticJsonDocument<512> doc;
  DeserializationError err = deserializeJson(doc, buf);
  if (err) {
    Serial.printf("[MQTT] Error parse JSON: %s\n", err.c_str());
    return;
  }

  String action = doc["action"] | doc["command"] | "";

  // 1. KENDALI POWER SENSOR
  if (action == "power_sensors" || doc.containsKey("sensor_power")) {
    bool pwr = doc.containsKey("sensor_power") ? doc["sensor_power"].as<bool>()
                                               : doc["power"].as<bool>();
    if (pwr) {
      powerOnSensors();
      sensorPowerState = true;
    } else {
      powerOffSensors();
      sensorPowerState = false;
    }
    publishDeviceStatus();
  }
  // 2. KENDALI POWER JETSON NANO (DENGAN GRACEFUL SHUTDOWN)
  else if (action == "power_jetson" || doc.containsKey("jetson_power")) {
    bool pwr = doc.containsKey("jetson_power") ? doc["jetson_power"].as<bool>()
                                               : doc["power"].as<bool>();
    if (pwr) {
      cancelJetsonShutdown();
      powerOnJetson();
      jetsonPowerState = true;
      publishDeviceStatus();
    } else {
      // Permintaan matikan Jetson -> beri waktu shutdown OS 30 detik
      requestGracefulJetsonPowerOff();
    }
  }
  // 3. KALIBRASI SENSOR
  else if (action == "calibrate" || action == "calibration") {
    bool changed = false;
    if (doc.containsKey("ph")) {
      JsonObject ph = doc["ph"];
      if (ph.containsKey("slope")) {
        phSlope = ph["slope"].as<float>();
        changed = true;
      }
      if (ph.containsKey("offset")) {
        phOffset = ph["offset"].as<float>();
        changed = true;
      }
    }
    if (doc.containsKey("do")) {
      JsonObject doObj = doc["do"];
      if (doObj.containsKey("slope")) {
        doSlope = doObj["slope"].as<float>();
        changed = true;
      }
      if (doObj.containsKey("offset")) {
        doOffset = doObj["offset"].as<float>();
        changed = true;
      }
    }
    if (doc.containsKey("tds")) {
      JsonObject tds = doc["tds"];
      if (tds.containsKey("slope")) {
        tdsSlope = tds["slope"].as<float>();
        changed = true;
      }
      if (tds.containsKey("offset")) {
        tdsOffset = tds["offset"].as<float>();
        changed = true;
      }
    }
    if (doc.containsKey("turbidity")) {
      JsonObject turb = doc["turbidity"];
      if (turb.containsKey("slope")) {
        turbSlope = turb["slope"].as<float>();
        changed = true;
      }
      if (turb.containsKey("offset")) {
        turbOffset = turb["offset"].as<float>();
        changed = true;
      }
    }

    if (changed) {
      saveCalibration();
      Serial.println(F("[MQTT] Kalibrasi berhasil diperbarui via MQTT."));
      publishDeviceStatus();
    }
  }
  // 4. RESET KALIBRASI
  else if (action == "reset_calibration") {
    String sName = doc["sensor"] | "all";
    if (sName == "ph" || sName == "all") {
      phSlope = -11.43f;
      phOffset = 52.00f;
    }
    if (sName == "do" || sName == "all") {
      doSlope = 5.5f;
      doOffset = 0.0f;
    }
    if (sName == "tds" || sName == "all") {
      tdsSlope = 500.0f;
      tdsOffset = 0.0f;
    }
    if (sName == "turbidity" || sName == "all") {
      turbSlope = -1120.4f;
      turbOffset = 4895.87f;
    }
    saveCalibration();
    publishDeviceStatus();
  }
  // 5. QUERY STATUS
  else if (action == "get_status") {
    publishDeviceStatus();
  }
}

static int postSensorLogToSupabase(const String &endpointLogs, bool includeDoLevel) {
  WiFiClientSecure secureClient;
  secureClient.setInsecure();
  secureClient.setTimeout(4); // Timeout singkat 4 detik agar loop lokal tidak macet di sinyal lemah

  HTTPClient https;
  https.setReuse(false);
  https.setConnectTimeout(4000);
  https.setTimeout(4000);

  if (!https.begin(secureClient, endpointLogs)) {
    Serial.printf("[SUPABASE] Gagal inisialisasi HTTPS ke %s\n", endpointLogs.c_str());
    secureClient.stop();
    return -1;
  }

  https.addHeader("Content-Type", "application/json");
  https.addHeader("apikey", SUPABASE_KEY);
  https.addHeader("Authorization", String("Bearer ") + SUPABASE_KEY);
  https.addHeader("Prefer", "return=minimal");

  StaticJsonDocument<448> docLog;
  docLog["device_id"] = DEVICE_ID;
  docLog["pond_number"] = POND_NUMBER;
  docLog["ph_level"] = (isnan(phLevel) || isinf(phLevel)) ? 0.0f : phLevel;
  if (includeDoLevel) {
    docLog["do_level"] = (isnan(doMgL) || isinf(doMgL)) ? 0.0f : doMgL;
  }
  docLog["tds_ppm"] = (isnan(tdsPpm) || isinf(tdsPpm)) ? 0.0f : tdsPpm;
  docLog["turbidity_ntu"] = (isnan(turbidityNTU) || isinf(turbidityNTU)) ? 0.0f : turbidityNTU;
  docLog["temperature_c"] = (isnan(temperatureC) || isinf(temperatureC)) ? 0.0f : temperatureC;
  docLog["battery_voltage"] = (isnan(batVoltage) || isinf(batVoltage)) ? 12.0f : batVoltage;
  docLog["battery_current_a"] = 0.45f;
  docLog["status"] = evaluateWaterStatus();

  String jsonLog;
  serializeJson(docLog, jsonLog);
  Serial.printf("[SUPABASE] POST %s | payload=%s\n", endpointLogs.c_str(), jsonLog.c_str());

  int httpCode = https.POST(jsonLog);
  if (httpCode >= 200 && httpCode < 300) {
    Serial.printf("[SUPABASE] Riwayat sensor_logs berhasil diunggah (HTTP %d)\n", httpCode);
  } else {
    String errBody = https.getString();
    Serial.printf("[SUPABASE] Gagal simpan log (HTTP %d / %s): %s\n",
                  httpCode,
                  https.errorToString(httpCode).c_str(),
                  errBody.c_str());
  }
  https.end();
  secureClient.stop();
  return httpCode;
}

// ─── PENGUNGGAH BERKALA 1 MENIT KE SUPABASE ───────────────────
void uploadToSupabasePeriodic() {
  unsigned long now = millis();
  if (firstSupabaseUploadDone && (now - lastSupabaseUploadMs < SUPABASE_UPLOAD_INTERVAL_MS))
    return;

  if (WiFi.status() != WL_CONNECTED) {
    lastSupabaseUploadMs = now;
    firstSupabaseUploadDone = true;
    Serial.println(F("[SUPABASE] Lewati upload: WiFi tidak terhubung."));
    return;
  }

  lastSupabaseUploadMs = now;
  firstSupabaseUploadDone = true;

  Serial.println(F("\n[SUPABASE] Memulai upload riwayat ke sensor_logs..."));

  const char *waterStatus = evaluateWaterStatus();
  String endpointLogs = String(SUPABASE_REST_URL) + "/sensor_logs";

  // 1. Kirim ke sensor_logs beserta kolom do_level; fallback otomatis jika kolom do_level belum dibuat di Supabase
  int logCode = postSensorLogToSupabase(endpointLogs, true);
  if (logCode == 400) {
    Serial.println(F("[SUPABASE] Retry tanpa 'do_level' (tambahkan kolom do_level di tabel sensor_logs Supabase agar DO tersimpan)."));
    logCode = postSensorLogToSupabase(endpointLogs, false);
  }
  if (logCode < 0) {
    Serial.println(F("[SUPABASE] Internet/modem lambat atau terputus; lewati PATCH devices siklus ini."));
    return;
  }

  // 2. Perbarui status live di tabel 'devices'
  WiFiClientSecure devClient;
  devClient.setInsecure();
  devClient.setTimeout(4);

  HTTPClient httpsDev;
  httpsDev.setReuse(false);
  httpsDev.setConnectTimeout(4000);
  httpsDev.setTimeout(4000);
  String endpointDevice = String(SUPABASE_REST_URL) + "/devices?id=eq." + DEVICE_ID;
  if (httpsDev.begin(devClient, endpointDevice)) {
    httpsDev.addHeader("Content-Type", "application/json");
    httpsDev.addHeader("apikey", SUPABASE_KEY);
    httpsDev.addHeader("Authorization", String("Bearer ") + SUPABASE_KEY);
    httpsDev.addHeader("Prefer", "return=minimal");

    StaticJsonDocument<256> docDev;
    docDev["pond_number"] = POND_NUMBER;
    docDev["ip_address"] = WiFi.localIP().toString();
    docDev["battery_percent"] = 100;
    docDev["status"] = waterStatus;
    docDev["is_connected"] = true;

    String jsonDev;
    serializeJson(docDev, jsonDev);

    int httpCode = httpsDev.PATCH(jsonDev);
    if (httpCode >= 200 && httpCode < 300) {
      Serial.printf("[SUPABASE] Status devices berhasil disinkronkan (HTTP %d)\n", httpCode);
    } else {
      String errBody = httpsDev.getString();
      Serial.printf("[SUPABASE] Gagal update devices (HTTP %d / %s): %s\n",
                    httpCode,
                    httpsDev.errorToString(httpCode).c_str(),
                    errBody.c_str());
    }
    httpsDev.end();
  } else {
    Serial.printf("[SUPABASE] Gagal inisialisasi HTTPS ke %s\n", endpointDevice.c_str());
  }
  devClient.stop();
}
