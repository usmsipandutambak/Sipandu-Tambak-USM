static bool ensureAdsAReady() {
    if (isAdsAOK) return true;
    if (adsA.begin(ADS_A_ADDR)) {
        isAdsAOK = true;
        adsA.setGain(GAIN_TWOTHIRDS);
        Serial.println(F("[SENSOR] ADS1115 A (0x48) re-inisialisasi OK"));
        return true;
    }
    return false;
}

static bool ensureAdsBReady() {
    if (isAdsBOK) return true;
    if (adsB.begin(ADS_B_ADDR)) {
        isAdsBOK = true;
        adsB.setGain(GAIN_TWOTHIRDS);
        Serial.println(F("[SENSOR] ADS1115 B (0x49) re-inisialisasi OK"));
        return true;
    }
    return false;
}

static float sanitizeReading(float val, float minVal, float maxVal, float fallback) {
    if (isnan(val) || isinf(val)) return fallback;
    if (val < minVal) return minVal;
    if (val > maxVal) return maxVal;
    return val;
}

//  Tegangan aktual = raw_voltage × DIVIDER_CORRECTION_FACTOR
//  NTU = (turbSlope × voltage_actual) + turbOffset
//  Default turbSlope = -1120.4, turbOffset = 4895.87
float readTurbidity() {
    if (!ensureAdsBReady()) {
        Serial.println(F("[SENSOR] ADS1115 B (Turbidity) belum siap"));
        return sanitizeReading(turbidityNTU, 0.0f, 4000.0f, 0.0f);
    }

    adsTurbidity.readADC_SingleEnded(0); // dummy read untuk settling mux A0
    delay(10);

    float accumulator = 0.0f;

    for (int i = 0; i < TURBIDITY_SAMPLE_COUNT; i++) {
        int16_t raw = adsTurbidity.readADC_SingleEnded(0);  // A0
        if (raw < 0) raw = 0;
        float voltage = adsTurbidity.computeVolts(raw);

        // voltage divider (R1=10kΩ, R2=20kΩ → ×1.5)
        float voltageActual = voltage * DIVIDER_CORRECTION_FACTOR;

        float ntu = sanitizeReading((turbSlope * voltageActual) + turbOffset, 0.0f, 4000.0f, 0.0f);
        accumulator += ntu;
        delay(SAMPLE_INTERVAL_MS);
    }

    return accumulator / (float)TURBIDITY_SAMPLE_COUNT;
}


//  ppm = (voltage × tdsSlope) + tdsOffset
//  Default tdsSlope = 500 ppm/V, tdsOffset = 0
float readTDS() {
    if (!ensureAdsBReady()) {
        Serial.println(F("[SENSOR] ADS1115 B (TDS) belum siap"));
        return sanitizeReading(tdsPpm, 0.0f, 9999.0f, 0.0f);
    }

    adsTDS.readADC_SingleEnded(1); // dummy read untuk settling mux A1
    delay(10);

    float accumulator = 0.0f;

    for (int i = 0; i < TDS_SAMPLE_COUNT; i++) {
        int16_t raw = adsTDS.readADC_SingleEnded(1);   // A1
        if (raw < 0) raw = 0;
        float voltage = adsTDS.computeVolts(raw);

        float ppm = sanitizeReading((voltage * tdsSlope) + tdsOffset, 0.0f, 9999.0f, 0.0f);
        accumulator += ppm;
        delay(SAMPLE_INTERVAL_MS);
    }

    return accumulator / (float)TDS_SAMPLE_COUNT;
}

//  Tegangan aktual = raw_voltage × DIVIDER_CORRECTION_FACTOR
//  pH = (voltage_actual × phSlope) + phOffset
//  Sampling 10x + bubble sort + median filter
float readPH() {
    if (!ensureAdsAReady()) {
        Serial.println(F("[SENSOR] ADS1115 A (pH) belum siap"));
        return sanitizeReading(phLevel, 0.0f, 14.0f, 7.0f);
    }

    adsPH.readADC_SingleEnded(1); // dummy read untuk settling mux A1
    delay(10);

    float samples[PH_SAMPLE_COUNT];
    int16_t lastRaw = 0;

    for (int i = 0; i < PH_SAMPLE_COUNT; i++) {
        adsPH.readADC_SingleEnded(1);
        int16_t raw = adsPH.readADC_SingleEnded(1);  // A1
        if (raw < 0) raw = 0;
        lastRaw = raw;
        float voltage = adsPH.computeVolts(raw);

        // voltage divider (R1=10kΩ, R2=20kΩ → ×1.5)
        float voltageActual = voltage * DIVIDER_CORRECTION_FACTOR;
        phVoltage           = voltageActual;

        float ph = sanitizeReading((voltageActual * phSlope) + phOffset, 0.0f, 14.0f, 7.0f);
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

    float median = sanitizeReading((samples[4] + samples[5]) / 2.0f, 0.0f, 14.0f, 7.0f);
    Serial.printf(" [pH raw=%d V=%.4fV -> %.2f]", lastRaw, phVoltage, median);
    return median;
}

//  mg/L = (voltage × doSlope) + doOffset
//  Default: doSlope = 5.5, doOffset = 0.0 
float readDO() {
    if (!ensureAdsAReady()) {
        Serial.println(F("[SENSOR] ADS1115 A (DO) belum siap"));
        return sanitizeReading(doMgL, 0.0f, 20.0f, 0.0f);
    }

    adsDO.readADC_SingleEnded(0); // dummy read untuk settling mux A0
    delay(10);

    float accumulator = 0.0f;
    int16_t lastRaw = 0;

    for (int i = 0; i < DO_SAMPLE_COUNT; i++) {
        adsDO.readADC_SingleEnded(0);
        int16_t raw = adsDO.readADC_SingleEnded(0);  // A0
        if (raw < 0) raw = 0;
        lastRaw = raw;
        float voltage = adsDO.computeVolts(raw);

        float mgL = sanitizeReading((voltage * doSlope) + doOffset, 0.0f, 20.0f, 0.0f);
        doVoltage = voltage;

        accumulator += mgL;
        delay(SAMPLE_INTERVAL_MS);
    }

    float avgDo = accumulator / (float)DO_SAMPLE_COUNT;
    Serial.printf(" [DO raw=%d V=%.4fV -> %.2f mg/L]", lastRaw, doVoltage, avgDo);
    return avgDo;
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
        Wire.beginTransmission(INA226_I2C_ADDR);
        if (Wire.endTransmission() == 0) {
            isIna226OK = true;
            ina226.init();
            ina226.setAverage(INA226_AVERAGE_4);
            ina226.setConversionTime(INA226_CONV_TIME_1100);
            ina226.setMeasureMode(INA226_CONTINUOUS);
            Serial.println(F("[SENSOR] INA226 re-inisialisasi OK"));
        } else {
            batVoltage = 0.0f;
            return;
        }
    }

    float rawVoltage = ina226.getBusVoltage_V();
    batVoltage = sanitizeReading(rawVoltage + offsetINA, 0.0f, 30.0f, 0.0f);
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
