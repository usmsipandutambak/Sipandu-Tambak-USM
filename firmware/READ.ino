//  Tegangan aktual = raw_voltage × DIVIDER_CORRECTION_FACTOR
//  NTU = (turbSlope × voltage_actual) + turbOffset
//  Default turbSlope = -1120.4, turbOffset = 4895.87
float readTurbidity() {
    if (!isAdsTurbidityOK) return 0.0f;

    float accumulator = 0.0f;

    for (int i = 0; i < TURBIDITY_SAMPLE_COUNT; i++) {
        int16_t raw     = adsTurbidity.readADC_SingleEnded(0);  // A0
        float   voltage = (float)raw * ADS_LSB_V;

        // voltage divider (R1=10kΩ, R2=20kΩ → ×1.5)
        float voltageActual = voltage * DIVIDER_CORRECTION_FACTOR;

        float ntu = (turbSlope * voltageActual) + turbOffset;
        if (ntu < 0.0f)    ntu = 0.0f;
        if (ntu > 4000.0f) ntu = 4000.0f;

        accumulator += ntu;
        delay(SAMPLE_INTERVAL_MS);
    }

    return accumulator / (float)TURBIDITY_SAMPLE_COUNT;
}


//  ppm = (voltage × tdsSlope) + tdsOffset
//  Default tdsSlope = 500 ppm/V, tdsOffset = 0
float readTDS() {
    if (!isAdsTdsOK) return 0.0f;

    float accumulator = 0.0f;

    for (int i = 0; i < TDS_SAMPLE_COUNT; i++) {
        int16_t raw     = adsTDS.readADC_SingleEnded(1);   // A1
        float   voltage = (float)raw * ADS_LSB_V;

        float ppm = (voltage * tdsSlope) + tdsOffset;
        if (ppm < 0.0f)    ppm = 0.0f;
        if (ppm > 9999.0f) ppm = 9999.0f;

        accumulator += ppm;
        delay(SAMPLE_INTERVAL_MS);
    }

    return accumulator / (float)TDS_SAMPLE_COUNT;
}

//  Tegangan aktual = raw_voltage × DIVIDER_CORRECTION_FACTOR
//  pH = (voltage_actual × phSlope) + phOffset
//  Sampling 10x + bubble sort + median filter
float readPH() {
    if (!isAdsPhOK) return 0.0f;

    float samples[PH_SAMPLE_COUNT];

    for (int i = 0; i < PH_SAMPLE_COUNT; i++) {
        adsPH.readADC_SingleEnded(1);
        int16_t raw     = adsPH.readADC_SingleEnded(1);  // A1
        float   voltage = (float)raw * ADS_LSB_V;

        // voltage divider (R1=10kΩ, R2=20kΩ → ×1.5)
        float voltageActual = voltage * DIVIDER_CORRECTION_FACTOR;
        phVoltage           = voltageActual;  

        float ph = (voltageActual * phSlope) + phOffset;
        if (ph < 0.0f)  ph = 0.0f;
        if (ph > 14.0f) ph = 14.0f;

        samples[i] = ph;
        delay(SAMPLE_INTERVAL_MS);
    }

    for (int i = 0; i < PH_SAMPLE_COUNT - 1; i++) {
        for (int j = 0; j < PH_SAMPLE_COUNT - 1 - i; j++) {
            if (samples[j] > samples[j + 1]) {
                float tmp      = samples[j];
                samples[j]     = samples[j + 1];
                samples[j + 1] = tmp;
            }
        }
    }

    float median = (samples[4] + samples[5]) / 2.0f;

    if (median < 0.0f)  median = 0.0f;
    if (median > 14.0f) median = 14.0f;

    return median;
}

//  mg/L = (voltage × doSlope) + doOffset
//  Default: doSlope = 5.5, doOffset = 0.0 
float readDO() {
    if (!isAdsDoOK) return 0.0f;

    float accumulator = 0.0f;

    for (int i = 0; i < DO_SAMPLE_COUNT; i++) {
        int16_t raw     = adsDO.readADC_SingleEnded(0);  // A0
        float   voltage = (float)raw * ADS_LSB_V;

        float mgL = (voltage * doSlope) + doOffset;
        if (mgL < 0.0f)   mgL = 0.0f;
        if (mgL > 20.0f)  mgL = 20.0f;   // DO jenuh air ~14 mg/L, batas max 20

        doVoltage = voltage; 

        accumulator += mgL;
        delay(SAMPLE_INTERVAL_MS);
    }

    return accumulator / (float)DO_SAMPLE_COUNT;
}

// --- DS18B20 Suhu Air (12-bit, ~750ms conversion time) ---
float readTemperature() {
  // Jika saat booting gagal, coba inisialisasi ulang di latar belakang
  if (!isDs18b20OK) {
    sensors.begin();
    if (sensors.getDeviceCount() > 0) {
      isDs18b20OK = true;
      sensors.setResolution(12);
    } else {
      return 0.0f;
    }
  }

  // Kirim perintah konversi ke seluruh sensor pada jalur OneWire
  sensors.requestTemperatures();
  delay(750); // Waktu konversi presisi 12-bit

  float raw = sensors.getTempCByIndex(0);

  // Filter error: -127 C (kabel putus) atau 85 C (power-on reset glitch)
  if (raw == DEVICE_DISCONNECTED_C || raw == 85.0f || raw < -20.0f || raw > 85.0f) {
    Serial.println(F("DS18B20 pembacaan tidak valid"));
    return (temperatureC > 0.0f) ? temperatureC : 0.0f;
  }

  temperatureC = raw;
  return raw;
}

// CJMCU-226 VBS ke jalur 12V+
void readINA226() {
    if (!isIna226OK) {
        batVoltage = 0.0f;
        return;
    }

    float rawVoltage = ina226.getBusVoltage_V();
    batVoltage = rawVoltage + offsetINA;
    if (batVoltage < 0.0f) batVoltage = 0.0f;
}

void readAllSensors() {
    Serial.print(F("Turbidity"));
    turbidityNTU = readTurbidity();

    Serial.print(F(" - TDS"));
    tdsPpm = readTDS();

    Serial.print(F(" - pH"));
    phLevel = readPH();

    Serial.print(F(" - DO"));
    doMgL = readDO();

    Serial.print(F(" - Temp"));
    temperatureC = readTemperature();

    Serial.println(F(" - Battery"));
    readINA226();
}

// JSON OUTPUT Format:
//  {"ph":7.20,"do":6.80,"tds":150.5,"turbidity":12.3,"temp":28.50,"bat_v":12.80}
void printJSON() {
    char buf[180];
    snprintf(buf, sizeof(buf),
        "{\"ph\":%.2f,\"do\":%.2f,\"tds\":%.1f,\"turbidity\":%.1f,"
        "\"temp\":%.2f,\"bat_v\":%.2f}",
        phLevel,
        doMgL,
        tdsPpm,
        turbidityNTU,
        temperatureC,
        batVoltage
    );
    Serial.println(buf);
}
