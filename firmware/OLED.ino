void initOLED() {
    if (!display.begin(0x3C, false)) {  
        Serial.println(F("OLED SH1106 Gagal inisialisasi!"));
    } else {
        Serial.println(F("OLED SH1106 OK"));
        display.clearDisplay();
        display.setRotation(2);
        display.setTextSize(1);
        display.setTextColor(SH110X_WHITE);
        display.setCursor(0, 0);
        display.println(F("SIPANDU TAMBAK"));
        display.println(F("Starting..."));
        display.display();
    }
}

void showOLEDI2CScan() {
    uint8_t found[16];
    uint8_t count = 0;

    for (uint8_t addr = 0x08; addr <= 0x77; addr++) {
        Wire.beginTransmission(addr);
        if (Wire.endTransmission() == 0 && count < 16) {
            found[count++] = addr;
        }
    }

    display.clearDisplay();
    display.setTextSize(1);
    display.setTextColor(SH110X_WHITE);

    display.setCursor(0, 0);
    display.println(F("== I2C SCAN =="));

    if (count == 0) {
        display.setCursor(0, 16);
        display.println(F("Tdk ada device!"));
        display.setCursor(0, 28);
        display.println(F("Cek:"));
        display.setCursor(0, 38);
        display.println(F(" - Kabel SDA/SCL"));
        display.setCursor(0, 48);
        display.println(F(" - Tegangan 3.3V"));
    } else {
        display.setCursor(0, 10);
        display.print(F("Found: "));
        display.print(count);
        display.println(F(" device"));

        for (uint8_t i = 0; i < count && i < 8; i++) {
            int col = (i % 2) * 64;
            int row = 20 + (i / 2) * 10;
            display.setCursor(col, row);
            display.print(F("0x"));
            if (found[i] < 0x10) display.print(F("0"));
            display.print(found[i], HEX);
        }
    }

    display.display();
    delay(4000); 
}


void showOLEDStatus() {
    display.clearDisplay();
    display.setTextSize(1);
    display.setTextColor(SH110X_WHITE);

    display.setCursor(0, 0);
    display.println(F("== SENSOR INIT =="));

    display.setCursor(0, 10);
    display.print(F("DS18B20  : "));
    display.println(isDs18b20OK    ? F("OK")  : F("ERROR"));

    display.setCursor(0, 20);
    display.print(F("ADS pH   : "));
    display.println(isAdsPhOK      ? F("OK")  : F("ERROR"));

    display.setCursor(0, 30);
    display.print(F("ADS TDS  : "));
    display.println(isAdsTdsOK     ? F("OK")  : F("ERROR"));

    display.setCursor(0, 40);
    display.print(F("ADS Turb : "));
    display.println(isAdsTurbidityOK ? F("OK") : F("ERROR"));

    display.setCursor(0, 50);
    display.print(F("INA226   : "));
    display.println(isIna226OK     ? F("OK")  : F("ERROR"));

    display.display();
}

void showOLEDWifi(bool connected, const char* ipStr) {
    display.clearDisplay();
    display.setTextSize(1);
    display.setTextColor(SH110X_WHITE);

    display.setCursor(0, 0);
    display.println(F("== WIFI STATUS =="));

    display.setCursor(0, 16);
    if (connected) {
        display.println(F("Status : CONNECTED"));
        display.setCursor(0, 28);
        display.print(F("IP : "));
        display.println(ipStr);
        display.setCursor(0, 40);
        display.println(F("Supabase : OK"));
    } else {
        display.println(F("Status : GAGAL!"));
        display.setCursor(0, 28);
        display.println(F("Cek SSID/Password"));
        display.setCursor(0, 40);
        display.println(F("Offline Mode"));
    }

    display.display();
    delay(2500);
}

void showOLEDWarmup(int secsLeft) {
    display.clearDisplay();
    display.setTextSize(1);
    display.setTextColor(SH110X_WHITE);

    display.setCursor(0, 0);
    display.println(F("== WARM UP =="));

    display.setTextSize(2);
    display.setCursor(44, 16);
    if (secsLeft < 10) display.print(F("0"));
    display.print(secsLeft);
    display.print(F("s"));

    display.setTextSize(1);
    display.setCursor(0, 42);
    display.print(isAdsPhOK      ? F("pH:OK ") : F("pH:ERR "));
    display.print(isAdsTdsOK     ? F("TDS:OK ") : F("TDS:ERR "));

    display.setCursor(0, 54);
    display.print(isAdsTurbidityOK ? F("Tb:OK ") : F("Tb:ERR "));
    display.print(isDs18b20OK    ? F("T:OK") : F("T:ERR"));

    display.display();
}

void updateOLED() {
    display.clearDisplay();
    display.setTextSize(1);
    display.setTextColor(SH110X_WHITE);
    
    display.setCursor(0, 0);
    display.printf("pH  : %.2f", phLevel);

    display.setCursor(0, 10);
    display.printf("DO  : %.1f doMgL", doMgL);

    display.setCursor(0, 20);
    display.printf("T   : %.1f C", temperatureC);
    
    display.setCursor(0, 30);
    display.printf("TDS : %.0f ppm", tdsPpm);
    
    display.setCursor(0, 40);
    display.printf("Turb: %.1f NTU", turbidityNTU);
    
    display.setCursor(0, 50);
    display.printf("Bat : %.1f V", batVoltage);
    
    display.display();
}

void showOLEDMessage(const char* msg) {
    display.clearDisplay();
    display.setCursor(0, 0);
    display.setTextSize(1);
    display.setTextColor(SH110X_WHITE);
    display.println(F("SIPANDU TAMBAK"));
    display.println(F("----------------"));
    display.println(msg);
    display.display();
}

void debugADSPhOLED() {
    int16_t adc0 = adsPH.readADC_SingleEnded(0);
    int16_t adc1 = adsPH.readADC_SingleEnded(1);
    int16_t adc2 = adsPH.readADC_SingleEnded(2);
    int16_t adc3 = adsPH.readADC_SingleEnded(3);

    float voltage0 = adsPH.computeVolts(adc0);

    display.clearDisplay();
    display.setTextSize(1);
    display.setTextColor(SH110X_WHITE);

    display.setCursor(0, 0);
    display.println(F("== ADS pH DEBUG =="));

    display.setCursor(0, 12);
    display.print(F("INIT : "));
    display.println(isAdsPhOK ? F("OK") : F("ERROR"));

    display.setCursor(0, 22);
    display.print(F("A0 : "));
    display.println(adc0);

    display.setCursor(0, 32);
    display.print(F("V  : "));
    display.print(voltage0, 4);
    display.println(F(" V"));

    display.setCursor(0, 42);
    display.print(F("A1:"));
    display.print(adc1);

    display.print(F(" A2:"));
    display.print(adc2);

    display.setCursor(0, 52);
    display.print(F("A3:"));
    display.print(adc3);

    display.display();
}
