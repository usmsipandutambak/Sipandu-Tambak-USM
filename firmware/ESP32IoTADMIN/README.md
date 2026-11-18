# SIPANDU TAMBAK — ESP32 Firmware

Firmware ESP32 DevKit untuk pemantauan kualitas air tambak secara **Dual-Stream** (Real-Time via MQTT Broker & Riwayat Berkala via Supabase REST API).

## Fitur Utama

1. **Real-time Telemetry via MQTT Broker (Paho Compatible)**:
   - Mengirim pembacaan sensor setiap **3 detik** ke topic:  
     `sipandu/devices/{DEVICE_ID}/telemetry`
2. **Pencatatan Riwayat Berkala ke Supabase (1 Menit)**:
   - Mengunggah row data baru setiap **60 detik (1 menit)** ke tabel `sensor_logs` di Supabase.
   - Memperbarui status snapshot perangkat di tabel `devices`.
3. **Kendali Jarak Jauh & Kalibrasi 2-Arah via MQTT**:
   - Mendengarkan perintah di topic:  
     `sipandu/devices/{DEVICE_ID}/command`
   - Mendukung saklar daya sensor (MOSFET GPIO 32) dan daya Jetson Nano (MOSFET GPIO 33).
   - Mendukung pembaruan parameter kalibrasi sensor ke memori NVS permanen.
4. **Graceful Shutdown Jetson Nano**:
   - Memberi jeda hitung mundur **30 detik** saat instruksi mematikan Jetson Nano diterima agar OS Jetson sempat melakukan `shutdown -h now` sebelum aliran listrik diputus oleh MOSFET.
5. **Local Web Server Fail-Safe (Port 80)**:
   - Server HTTP bawaan tetap aktif di jaringan lokal untuk diagnostik offline saat berada di tambak.

## Library Arduino yang Dibutuhkan

Pastikan library berikut terpasang di Arduino IDE / PlatformIO:
- `PubSubClient` (oleh Nick O'Leary)
- `ArduinoJson` (v6+)
- `Adafruit ADS1X15`
- `Adafruit SH110X` & `Adafruit GFX`
- `DallasTemperature` & `OneWire`
- `INA226_WE`

## Pinout Hardware

| Komponen | Pin ESP32 | Keterangan |
| :--- | :--- | :--- |
| **DS18B20** | GPIO 25 | Sensor Suhu Air (OneWire) |
| **I2C SDA** | GPIO 21 | Bus komunikasi sensor analog & OLED |
| **I2C SCL** | GPIO 22 | Bus komunikasi sensor analog & OLED |
| **MOSFET Sensor** | GPIO 32 | Saklar daya ke seluruh sensor |
| **MOSFET Jetson** | GPIO 33 | Saklar daya ke Jetson Nano |
| **ADS1115 A** (0x48) | I2C | pH (A1), DO (A0) |
| **ADS1115 B** (0x49) | I2C | TDS (A1), Turbidity (A0) |
| **INA226** (0x44) | I2C | Sensor Tegangan Baterai |
| **OLED SH1106** (0x3C) | I2C | Layar lokal 128x64 |

## Format Perintah MQTT Command

Kirim JSON ke topic `sipandu/devices/Sipandu_A/command`:

- **Nyalakan Sensor**: `{"command": "power_sensors", "power": true}`
- **Matikan Sensor**: `{"command": "power_sensors", "power": false}`
- **Nyalakan Jetson**: `{"command": "power_jetson", "power": true}`
- **Matikan Jetson (Graceful)**: `{"command": "power_jetson", "power": false}`
- **Kalibrasi Sensor**:
  ```json
  {
    "command": "calibrate",
    "ph": { "slope": -5.70, "offset": 21.34 },
    "tds": { "slope": 500.0, "offset": 0.0 }
  }
  ```
- **Reset Kalibrasi**: `{"command": "reset_calibration", "sensor": "all"}`
