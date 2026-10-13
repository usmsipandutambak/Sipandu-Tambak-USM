void powerOnSensors() {
    digitalWrite(PIN_MOSFET_SENSOR, HIGH);
}
void powerOffSensors() {
    digitalWrite(PIN_MOSFET_SENSOR, LOW);
}

void powerOnJetson() {
    digitalWrite(PIN_MOSFET_JETSON, HIGH);
}
void powerOffJetson() {
    digitalWrite(PIN_MOSFET_JETSON, LOW);
}

void enterDeepSleep() {
    Serial.printf("deep sleep %llu ...\n",
                  DEEP_SLEEP_US / 1000000ULL);
    Serial.flush();
    delay(50);   

    esp_sleep_enable_timer_wakeup(DEEP_SLEEP_US);
    esp_deep_sleep_start();
}

void runDutyCycle() {

    powerOnSensors();
    delay(150); 
    initSensors(); 

    const int warmupSecs = WARMUP_DELAY_MS / 1000;
    Serial.print(F("Warm-up: "));
    showOLEDMessage("Warming up..."); 
    for (int i = 1; i <= warmupSecs; i++) {
        delay(1000);
        Serial.print(i);
        Serial.print(F("s "));
        if (i % 5 == 0 && i < warmupSecs) Serial.print(F("| "));
    }
    Serial.println();
    readAllSensors();

    Serial.print(F("DATA "));
    printJSON();
    
    updateOLED();
    delay(3000);  

    powerOffSensors();
    enterDeepSleep();
}

void runContinuous() {
    unsigned long now = millis();

    if (now - _lastContinuousMs >= CONTINUOUS_INTERVAL_MS) {
        _lastContinuousMs = now;

        readAllSensors();
        markSensorReadTime();  

        Serial.print(F("DATA "));
        printJSON();

        updateOLED();
    }
}

void handleSerialCommands() {

    static char   cmdBuf[32];
    static uint8_t cmdLen = 0;

    while (Serial.available()) {
        char c = (char)Serial.read();

        if (c == '\n' || c == '\r') {
            if (cmdLen == 0) continue;   

            cmdBuf[cmdLen] = '\0';     
            cmdLen = 0;                 

            if (strncmp(cmdBuf, "CL:", 4) == 0) {
                float val = atof(cmdBuf + 4);
                calibratePH(val);

            } else if (strcmp(cmdBuf, "SAVE") == 0) {
                saveCalibration();

            } else if (strcmp(cmdBuf, "STATUS") == 0) {
                Serial.println(F("STATUS calibration:"));
                Serial.printf( "  pH    slope=%.4f  offset=%.4f\n", phSlope,   phOffset);
                Serial.printf( "  DO    slope=%.4f  offset=%.4f\n", doSlope,   doOffset);
                Serial.printf( "  TDS   slope=%.2f  offset=%.2f\n",  tdsSlope,  tdsOffset);
                Serial.printf( "  Turb  slope=%.2f  offset=%.2f\n",  turbSlope, turbOffset);
                Serial.printf( "  [pH voltage (actual): %.4f V]\n",  phVoltage);
                Serial.printf( "  [DO voltage         : %.4f V]\n",  doVoltage);

            } else {
                Serial.print(F("Unknown cmd: "));
                Serial.println(cmdBuf);
                Serial.println(F("Valid"));
            }
        } else {
            if (cmdLen < (sizeof(cmdBuf) - 1)) {
                cmdBuf[cmdLen++] = c;
            }
        }
    }
}
