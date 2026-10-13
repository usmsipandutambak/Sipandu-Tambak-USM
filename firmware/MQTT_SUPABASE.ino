#include <PubSubClient.h>
#include <HTTPClient.h>
#include <WiFiClientSecure.h>

WiFiClient espMqttClient;
PubSubClient mqttClient(espMqttClient);

static unsigned long lastMqttPublishMs   = 0;
static unsigned long lastSupabaseUploadMs = 0;
static unsigned long lastMqttReconnectMs  = 0;

void initMQTT() {
    TOPIC_TELEMETRY = String("sipandu/devices/") + DEVICE_ID + "/telemetry";
    TOPIC_COMMAND   = String("sipandu/devices/") + DEVICE_ID + "/command";
    TOPIC_STATUS    = String("sipandu/devices/") + DEVICE_ID + "/status";

    mqttClient.setServer(MQTT_BROKER, MQTT_PORT);
    mqttClient.setCallback(mqttCallback);
    mqttClient.setBufferSize(768); // Buffer JSON telemetri

    Serial.println(F("[MQTT] Inisialisasi MQTT Client (Paho compatible)"));
    Serial.printf("[MQTT] Broker: %s:%d\n", MQTT_BROKER, MQTT_PORT);
    Serial.printf("[MQTT] Subscribing: %s\n", TOPIC_COMMAND.c_str());
}

void connectMQTT() {
    if (WiFi.status() != WL_CONNECTED) return;
    if (mqttClient.connected()) return;

    unsigned long now = millis();
    if (now - lastMqttReconnectMs < 5000) return; // Coba reconnect tiap 5 detik
    lastMqttReconnectMs = now;

    Serial.print(F("[MQTT] Menghubungkan ke broker MQTT... "));
    String clientId = String("ESP32_") + DEVICE_ID + "_" + String(random(0xffff), HEX);

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
    if (!mqttClient.connected()) return;

    unsigned long now = millis();
    if (now - lastMqttPublishMs < MQTT_PUBLISH_INTERVAL_MS) return;
    lastMqttPublishMs = now;

    StaticJsonDocument<512> doc;
    doc["device_id"]      = DEVICE_ID;
    doc["pond_number"]    = POND_NUMBER;
    doc["timestamp_ms"]   = millis();
    doc["uptime_ms"]      = millis();
    doc["sensor_power"]   = sensorPowerState;
    doc["jetson_power"]   = jetsonPowerState;
    doc["jetson_shutdown_pending"] = jetsonShutdownPending;

    JsonObject sens = doc.createNestedObject("sensors");
    sens["ph"]              = isnan(phLevel)      ? 0.0f : phLevel;
    sens["do_mg_l"]         = isnan(doMgL)        ? 0.0f : doMgL;
    sens["tds_ppm"]         = isnan(tdsPpm)       ? 0.0f : tdsPpm;
    sens["turbidity_ntu"]   = isnan(turbidityNTU) ? 0.0f : turbidityNTU;
    sens["temperature_c"]   = isnan(temperatureC) ? 0.0f : temperatureC;
    sens["battery_voltage"] = isnan(batVoltage)   ? 0.0f : batVoltage;

    JsonObject stat = doc.createNestedObject("sensor_status");
    stat["ds18b20"] = isDs18b20OK;
    stat["ads_a"]   = isAdsAOK;
    stat["ads_b"]   = isAdsBOK;
    stat["ina226"]  = isIna226OK;

    String jsonStr;
    serializeJson(doc, jsonStr);

    mqttClient.publish(TOPIC_TELEMETRY.c_str(), jsonStr.c_str());
}

void publishDeviceStatus() {
    if (!mqttClient.connected()) return;

    StaticJsonDocument<256> doc;
    doc["device_id"]      = DEVICE_ID;
    doc["pond_number"]    = POND_NUMBER;
    doc["online"]         = true;
    doc["ip_address"]     = WiFi.localIP().toString();
    doc["sensor_power"]   = sensorPowerState;
    doc["jetson_power"]   = jetsonPowerState;
    doc["jetson_shutdown_pending"] = jetsonShutdownPending;

    String jsonStr;
    serializeJson(doc, jsonStr);
    mqttClient.publish(TOPIC_STATUS.c_str(), jsonStr.c_str(), true); // Retain flag
}

void publishJetsonShutdownNotice(int secondsRemaining) {
    if (!mqttClient.connected()) return;

    StaticJsonDocument<192> doc;
    doc["device_id"]       = DEVICE_ID;
    doc["event"]           = "jetson_shutdown_countdown";
    doc["seconds_left"]    = secondsRemaining;
    doc["status"]          = "WAITING_SHUTDOWN";

    String jsonStr;
    serializeJson(doc, jsonStr);
    mqttClient.publish(TOPIC_STATUS.c_str(), jsonStr.c_str());
}

// Callback saat menerima pesan di TOPIC_COMMAND dari Mobile / Web Admin
void mqttCallback(char* topic, byte* payload, unsigned int length) {
    Serial.printf("[MQTT] Pesan diterima di topic [%s] (len=%d): ", topic, length);
    char buf[length + 1];
    memcpy(buf, payload, length);
    buf[length] = '\0';
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
        bool pwr = doc.containsKey("sensor_power") ? doc["sensor_power"].as<bool>() : doc["power"].as<bool>();
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
        bool pwr = doc.containsKey("jetson_power") ? doc["jetson_power"].as<bool>() : doc["power"].as<bool>();
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
            if (ph.containsKey("slope"))  { phSlope  = ph["slope"].as<float>();  changed = true; }
            if (ph.containsKey("offset")) { phOffset = ph["offset"].as<float>(); changed = true; }
        }
        if (doc.containsKey("do")) {
            JsonObject doObj = doc["do"];
            if (doObj.containsKey("slope"))  { doSlope  = doObj["slope"].as<float>();  changed = true; }
            if (doObj.containsKey("offset")) { doOffset = doObj["offset"].as<float>(); changed = true; }
        }
        if (doc.containsKey("tds")) {
            JsonObject tds = doc["tds"];
            if (tds.containsKey("slope"))  { tdsSlope  = tds["slope"].as<float>();  changed = true; }
            if (tds.containsKey("offset")) { tdsOffset = tds["offset"].as<float>(); changed = true; }
        }
        if (doc.containsKey("turbidity")) {
            JsonObject turb = doc["turbidity"];
            if (turb.containsKey("slope"))  { turbSlope  = turb["slope"].as<float>();  changed = true; }
            if (turb.containsKey("offset")) { turbOffset = turb["offset"].as<float>(); changed = true; }
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
        if (sName == "ph" || sName == "all")        { phSlope = -5.70f; phOffset = 21.34f; }
        if (sName == "do" || sName == "all")        { doSlope = 5.5f;   doOffset = 0.0f; }
        if (sName == "tds" || sName == "all")       { tdsSlope = 500.0f; tdsOffset = 0.0f; }
        if (sName == "turbidity" || sName == "all") { turbSlope = -1120.4f; turbOffset = 4895.87f; }
        saveCalibration();
        publishDeviceStatus();
    }
    // 5. QUERY STATUS
    else if (action == "get_status") {
        publishDeviceStatus();
    }
}

// ─── PENGUNGGAH BERKALA 1 MENIT KE SUPABASE ───────────────────
void uploadToSupabasePeriodic() {
    if (WiFi.status() != WL_CONNECTED) return;

    unsigned long now = millis();
    if (now - lastSupabaseUploadMs < SUPABASE_UPLOAD_INTERVAL_MS) return;
    lastSupabaseUploadMs = now;

    Serial.println(F("\n[SUPABASE] Memulai upload riwayat berkala (1 Menit)..."));

    // Gunakan WiFiClientSecure tanpa verifikasi SSL fingerprint agar hemat memori heap
    WiFiClientSecure secureClient;
    secureClient.setInsecure();

    HTTPClient https;
    String endpointLogs = String(SUPABASE_REST_URL) + "/sensor_logs";

    if (https.begin(secureClient, endpointLogs)) {
        https.addHeader("Content-Type", "application/json");
        https.addHeader("apikey", SUPABASE_KEY);
        https.addHeader("Authorization", String("Bearer ") + SUPABASE_KEY);
        https.addHeader("Prefer", "return=minimal");

        // 1. Simpan baris baru ke 'sensor_logs'
        StaticJsonDocument<384> docLog;
        docLog["device_id"]      = DEVICE_ID;
        docLog["pond_number"]    = POND_NUMBER;
        docLog["ph_level"]       = isnan(phLevel)      ? 0.0f : phLevel;
        docLog["do_level"]       = isnan(doMgL)        ? 0.0f : doMgL;
        docLog["tds_ppm"]        = isnan(tdsPpm)       ? 0.0f : tdsPpm;
        docLog["turbidity_ntu"]  = isnan(turbidityNTU) ? 0.0f : turbidityNTU;
        docLog["temperature_c"]  = isnan(temperatureC) ? 0.0f : temperatureC;
        docLog["status"]         = "NORMAL";

        String jsonLog;
        serializeJson(docLog, jsonLog);

        int httpCode = https.POST(jsonLog);
        if (httpCode >= 200 && httpCode < 300) {
            Serial.printf("[SUPABASE] Riwayat sensor_logs berhasil diunggah (HTTP %d)\n", httpCode);
        } else {
            Serial.printf("[SUPABASE] Gagal simpan log (HTTP %d): %s\n", httpCode, https.getString().c_str());
        }
        https.end();
    }

    // 2. Perbarui snapshot telemetri live di tabel 'devices'
    String endpointDevice = String(SUPABASE_REST_URL) + "/devices?id=eq." + DEVICE_ID;
    if (https.begin(secureClient, endpointDevice)) {
        https.addHeader("Content-Type", "application/json");
        https.addHeader("apikey", SUPABASE_KEY);
        https.addHeader("Authorization", String("Bearer ") + SUPABASE_KEY);

        StaticJsonDocument<384> docDev;
        docDev["live_ph"]         = isnan(phLevel)      ? 0.0f : phLevel;
        docDev["live_do"]         = isnan(doMgL)        ? 0.0f : doMgL;
        docDev["live_tds"]        = isnan(tdsPpm)       ? 0.0f : tdsPpm;
        docDev["live_turb"]       = isnan(turbidityNTU) ? 0.0f : turbidityNTU;
        docDev["live_temp"]       = isnan(temperatureC) ? 0.0f : temperatureC;
        docDev["battery_percent"] = 100;
        docDev["status"]          = "NORMAL";
        docDev["is_connected"]    = true;

        String jsonDev;
        serializeJson(docDev, jsonDev);

        int httpCode = https.PATCH(jsonDev);
        if (httpCode >= 200 && httpCode < 300) {
            Serial.printf("[SUPABASE] Telemetri devices berhasil disinkronkan (HTTP %d)\n", httpCode);
        } else {
            Serial.printf("[SUPABASE] Gagal update devices (HTTP %d)\n", httpCode);
        }
        https.end();
    }
}
