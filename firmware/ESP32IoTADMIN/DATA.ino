static const char NVS_NS[]          = "sipandu";
static const char KEY_PH_SLOPE[]    = "ph_slope";
static const char KEY_PH_OFFSET[]   = "ph_offset";
static const char KEY_DO_SLOPE[]    = "do_slope";
static const char KEY_DO_OFFSET[]   = "do_offset";
static const char KEY_TDS_SLOPE[]   = "tds_slope";
static const char KEY_TDS_OFFSET[]  = "tds_offset";
static const char KEY_TURB_SLOPE[]  = "turb_slope";
static const char KEY_TURB_OFFSET[] = "turb_offset";


void loadCalibration() {
    prefs.begin(NVS_NS, true);

    phSlope    = prefs.getFloat(KEY_PH_SLOPE,    -5.70f);
    phOffset   = prefs.getFloat(KEY_PH_OFFSET,   21.34f);
    doSlope    = prefs.getFloat(KEY_DO_SLOPE,      5.5f);
    doOffset   = prefs.getFloat(KEY_DO_OFFSET,     0.0f);
    tdsSlope   = prefs.getFloat(KEY_TDS_SLOPE,  500.00f);
    tdsOffset  = prefs.getFloat(KEY_TDS_OFFSET,   0.00f);
    turbSlope  = prefs.getFloat(KEY_TURB_SLOPE, -1120.4f);
    turbOffset = prefs.getFloat(KEY_TURB_OFFSET, 4895.87f);

    prefs.end();

    // Guard against corrupted / NaN / zero-slope calibration stored in NVS
    if (isnan(phSlope) || isinf(phSlope) || phSlope == 0.0f)       phSlope = -5.70f;
    if (isnan(phOffset) || isinf(phOffset))                        phOffset = 21.34f;
    if (isnan(doSlope) || isinf(doSlope) || doSlope <= 0.0f)       doSlope = 5.5f;
    if (isnan(doOffset) || isinf(doOffset))                        doOffset = 0.0f;
    if (isnan(tdsSlope) || isinf(tdsSlope) || tdsSlope <= 0.0f)    tdsSlope = 500.00f;
    if (isnan(tdsOffset) || isinf(tdsOffset))                      tdsOffset = 0.00f;
    if (isnan(turbSlope) || isinf(turbSlope) || turbSlope == 0.0f) turbSlope = -1120.4f;
    if (isnan(turbOffset) || isinf(turbOffset))                    turbOffset = 4895.87f;

    Serial.println(F("CL:"));
    Serial.printf( "      pH    slope=%.4f  offset=%.4f\n", phSlope,   phOffset);
    Serial.printf( "      DO    slope=%.4f  offset=%.4f\n", doSlope,   doOffset);
    Serial.printf( "      TDS   slope=%.2f  offset=%.2f\n",  tdsSlope,  tdsOffset);
    Serial.printf( "      Turb  slope=%.2f  offset=%.2f\n",  turbSlope, turbOffset);
}

void saveCalibration() {
    prefs.begin(NVS_NS, false);

    prefs.putFloat(KEY_PH_SLOPE,    phSlope);
    prefs.putFloat(KEY_PH_OFFSET,   phOffset);
    prefs.putFloat(KEY_DO_SLOPE,    doSlope);
    prefs.putFloat(KEY_DO_OFFSET,   doOffset);
    prefs.putFloat(KEY_TDS_SLOPE,   tdsSlope);
    prefs.putFloat(KEY_TDS_OFFSET,  tdsOffset);
    prefs.putFloat(KEY_TURB_SLOPE,  turbSlope);
    prefs.putFloat(KEY_TURB_OFFSET, turbOffset);

    prefs.end();
    Serial.println(F("[NVS] Kalibrasi disimpan ke NVS."));
}

void calibratePH(float stdValue) {
    if (stdValue < 0.0f || stdValue > 14.0f) {
        Serial.println(F("ERROR must 0.0, 14.0"));
        return;
    }

    Serial.printf("Calibrating pH = %.2f ...\n", stdValue);

    float voltageSamples[10];

    for (int i = 0; i < 10; i++) {
        adsPH.readADC_SingleEnded(1);           // dummy read untuk settling
        int16_t raw         = adsPH.readADC_SingleEnded(1);   // A1 = pH
        voltageSamples[i]   = (float)raw * ADS_LSB_V * DIVIDER_CORRECTION_FACTOR;
        delay(50);
    }

    for (int i = 0; i < 9; i++) {
        for (int j = 0; j < 9 - i; j++) {
            if (voltageSamples[j] > voltageSamples[j + 1]) {
                float tmp              = voltageSamples[j];
                voltageSamples[j]     = voltageSamples[j + 1];
                voltageSamples[j + 1] = tmp;
            }
        }
    }

    // Median 10 sample (voltageActual setelah koreksi divider)
    float medianVoltage = (voltageSamples[4] + voltageSamples[5]) / 2.0f;

    // offset:  phOffset = stdValue − (voltageActual × phSlope)
    float newOffset = stdValue - (medianVoltage * phSlope);

    Serial.printf("[CAL] Buffer pH = %.2f\n",               stdValue);
    Serial.printf("[CAL] Median voltage (actual) = %.4f V\n", medianVoltage);
    Serial.printf("[CAL] Old offset = %.4f\n",               phOffset);
    Serial.printf("[CAL] New offset = %.4f\n",               newOffset);

    phOffset = newOffset;
    saveCalibration();
}
