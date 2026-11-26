void addCORSHeaders() {
  server.sendHeader("Access-Control-Allow-Origin", "*");
  server.sendHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
  server.sendHeader("Access-Control-Allow-Headers", "Content-Type");
  server.sendHeader("Access-Control-Allow-Private-Network", "true");
}

void handleOptions() {
  addCORSHeaders();
  server.send(204);
}

void handleGetStatus() {
  addCORSHeaders();

  bool wifiOK = (WiFi.status() == WL_CONNECTED);

  StaticJsonDocument<384> doc; // perbesar buffer dari 256 ke 384
  doc["device_id"] = DEVICE_ID;
  doc["pond_number"] = POND_NUMBER;
  doc["esp32_online"] = true;
  doc["wifi_connected"] = wifiOK;
  doc["ip_address"] = wifiOK ? WiFi.localIP().toString() : "";
  doc["uptime_ms"] = millis();
  doc["sensor_power"] = sensorPowerState;
  doc["jetson_power"] = jetsonPowerState;
  doc["last_read_ms"] = lastSensorReadMs;

  // Tambahkan blok ini:
  JsonObject stat = doc.createNestedObject("sensor_status");
  stat["ds18b20"] = isDs18b20OK;
  stat["ads_a"] = isAdsAOK;
  stat["ads_b"] = isAdsBOK;
  stat["ina226"] = isIna226OK;

  String json;
  serializeJson(doc, json);
  server.send(200, "application/json", json);
}

void handleGetSensors() {
  addCORSHeaders();

  StaticJsonDocument<512> doc;
  doc["device_id"] = DEVICE_ID;
  doc["pond_number"] = POND_NUMBER;
  doc["timestamp_ms"] = millis();

  JsonObject sens = doc.createNestedObject("sensors");
  sens["ph"] = isnan(phLevel) ? 0.0f : phLevel;
  sens["do_mg_l"] = isnan(doMgL) ? 0.0f : doMgL;
  sens["tds_ppm"] = isnan(tdsPpm) ? 0.0f : tdsPpm;
  sens["turbidity_ntu"] = isnan(turbidityNTU) ? 0.0f : turbidityNTU;
  sens["temperature_c"] = isnan(temperatureC) ? 0.0f : temperatureC;
  sens["battery_voltage"] = isnan(batVoltage) ? 0.0f : batVoltage;

  JsonObject volt = doc.createNestedObject("voltages");
  volt["ph_voltage"] = isnan(phVoltage) ? 0.0f : phVoltage;
  volt["do_voltage"] = isnan(doVoltage) ? 0.0f : doVoltage;

  JsonObject stat = doc.createNestedObject("sensor_status");
  stat["ds18b20"] = isDs18b20OK;
  stat["ads_a"] = isAdsAOK;
  stat["ads_b"] = isAdsBOK;
  stat["ina226"] = isIna226OK;

  String json;
  serializeJson(doc, json);
  server.send(200, "application/json", json);
}

void handleGetCalibration() {
  addCORSHeaders();

  StaticJsonDocument<384> doc;
  JsonObject ph = doc.createNestedObject("ph");
  ph["slope"] = phSlope;
  ph["offset"] = phOffset;

  JsonObject doObj = doc.createNestedObject("do");
  doObj["slope"] = doSlope;
  doObj["offset"] = doOffset;

  JsonObject tds = doc.createNestedObject("tds");
  tds["slope"] = tdsSlope;
  tds["offset"] = tdsOffset;

  JsonObject turb = doc.createNestedObject("turbidity");
  turb["slope"] = turbSlope;
  turb["offset"] = turbOffset;

  String json;
  serializeJson(doc, json);
  server.send(200, "application/json", json);
}

//  Body JSON: {"ph":{"slope":-5.70,"offset":21.34}, ...}
void handlePostCalibration() {
  addCORSHeaders();

  if (!server.hasArg("plain")) {
    server.send(400, "application/json", "{\"error\":\"No body\"}");
    return;
  }

  StaticJsonDocument<384> doc;
  DeserializationError err = deserializeJson(doc, server.arg("plain"));
  if (err) {
    server.send(400, "application/json", "{\"error\":\"Invalid JSON\"}");
    return;
  }

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
    server.send(200, "application/json", "{\"ok\":true,\"saved\":true}");
  } else {
    server.send(400, "application/json",
                "{\"error\":\"No valid calibration fields\"}");
  }
}

//  Body: {"power": true/false}
void handleControlSensors() {
  addCORSHeaders();

  if (!server.hasArg("plain")) {
    server.send(400, "application/json", "{\"error\":\"No body\"}");
    return;
  }

  StaticJsonDocument<64> doc;
  DeserializationError err = deserializeJson(doc, server.arg("plain"));
  if (err) {
    server.send(400, "application/json", "{\"error\":\"Invalid JSON\"}");
    return;
  }

  if (!doc.containsKey("power")) {
    server.send(400, "application/json",
                "{\"error\":\"Missing 'power' field\"}");
    return;
  }

  bool power = doc["power"].as<bool>();
  if (power) {
    powerOnSensors();
    sensorPowerState = true;
  } else {
    powerOffSensors();
    sensorPowerState = false;
  }

  StaticJsonDocument<64> resp;
  resp["ok"] = true;
  resp["sensor_power"] = sensorPowerState;
  String json;
  serializeJson(resp, json);
  server.send(200, "application/json", json);
}

//  Body: {"power": true/false}
void handleControlJetson() {
  addCORSHeaders();

  if (!server.hasArg("plain")) {
    server.send(400, "application/json", "{\"error\":\"No body\"}");
    return;
  }

  StaticJsonDocument<64> doc;
  DeserializationError err = deserializeJson(doc, server.arg("plain"));
  if (err) {
    server.send(400, "application/json", "{\"error\":\"Invalid JSON\"}");
    return;
  }

  if (!doc.containsKey("power")) {
    server.send(400, "application/json",
                "{\"error\":\"Missing 'power' field\"}");
    return;
  }

  bool power = doc["power"].as<bool>();
  if (power) {
    powerOnJetson();
  } else {
    powerOffJetson();
  }

  StaticJsonDocument<96> resp;
  resp["ok"] = true;
  resp["jetson_power"] = jetsonPowerState;
  resp["gpio"] = PIN_MOSFET_JETSON;
  String json;
  serializeJson(resp, json);
  server.send(200, "application/json", json);
}

// ── POST /api/calibration/reset ───────────────────────────
void handleResetCalibration() {
  addCORSHeaders();

  if (!server.hasArg("plain")) {
    server.send(400, "application/json", "{\"error\":\"No body\"}");
    return;
  }

  StaticJsonDocument<64> doc;
  deserializeJson(doc, server.arg("plain"));
  String sensor = doc["sensor"].as<String>();

  if (sensor == "ph") {
    phSlope = -11.43f;
    phOffset = 52.00f;
  } else if (sensor == "do") {
    doSlope = 5.5f;
    doOffset = 0.0f;
  } else if (sensor == "tds") {
    tdsSlope = 500.0f;
    tdsOffset = 0.0f;
  } else if (sensor == "turbidity") {
    turbSlope = -1120.4f;
    turbOffset = 4895.87f;
  } else if (sensor == "all") {
    phSlope = -11.43f;
    phOffset = 52.00f;
    doSlope = 5.5f;
    doOffset = 0.0f;
    tdsSlope = 500.0f;
    tdsOffset = 0.0f;
    turbSlope = -1120.4f;
    turbOffset = 4895.87f;
  } else {
    server.send(400, "application/json", "{\"error\":\"Unknown sensor\"}");
    return;
  }

  saveCalibration();
  server.send(200, "application/json", "{\"ok\":true,\"reset\":true}");
}

void initHTTPServer() {
  server.on("/api/status", HTTP_OPTIONS, handleOptions);
  server.on("/api/sensors", HTTP_OPTIONS, handleOptions);
  server.on("/api/calibration", HTTP_OPTIONS, handleOptions);
  server.on("/api/calibration/reset", HTTP_OPTIONS, handleOptions);
  server.on("/api/control/sensors", HTTP_OPTIONS, handleOptions);
  server.on("/api/control/jetson", HTTP_OPTIONS, handleOptions);

  server.on("/api/status", HTTP_GET, handleGetStatus);
  server.on("/api/sensors", HTTP_GET, handleGetSensors);
  server.on("/api/calibration", HTTP_GET, handleGetCalibration);

  server.on("/api/calibration", HTTP_POST, handlePostCalibration);
  server.on("/api/calibration/reset", HTTP_POST, handleResetCalibration);
  server.on("/api/control/sensors", HTTP_POST, handleControlSensors);
  server.on("/api/control/jetson", HTTP_POST, handleControlJetson);

  server.onNotFound([]() {
    addCORSHeaders();
    if (server.method() == HTTP_OPTIONS) {
      server.send(204);
      return;
    }
    server.send(404, "application/json", "{\"error\":\"Not found\"}");
  });

  server.begin();
  Serial.println(F("HTTP Server started on port 80"));
  if (WiFi.status() == WL_CONNECTED) {
    Serial.print(F("Web Admin: http://"));
    Serial.println(WiFi.localIP());
  }
}

void handleHTTPServer() { server.handleClient(); }

void markSensorReadTime() { lastSensorReadMs = millis(); }

void connectWiFi() {
  if (WiFi.status() == WL_CONNECTED)
    return;

  Serial.print(F("Menghubungkan ke Wi-Fi"));
  WiFi.persistent(false);
  WiFi.mode(WIFI_STA);
  WiFi.setSleep(false);
  WiFi.setAutoReconnect(true);
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);

  unsigned long startAttemptTime = millis();
  while (WiFi.status() != WL_CONNECTED && millis() - startAttemptTime < 12000) {
    delay(500);
    Serial.print(F("."));
  }

  if (WiFi.status() == WL_CONNECTED) {
    Serial.println();
    Serial.print(F("Terhubung! IP: "));
    Serial.print(WiFi.localIP());
    Serial.printf(" (RSSI: %d dBm)\n", WiFi.RSSI());
    initHTTPServer();
  } else {
    Serial.println();
    Serial.println(F("Gagal terhubung ke Wi-Fi (Timeout)"));
  }
}