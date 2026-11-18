# SIPANDU-TAMBAK — ESP32 Firmware (`firmware/ESP32IoTADMIN`)

Firmware ESP32 DevKit untuk pemantauan kualitas air tambak secara **Dual-Stream** (MQTT *real-time* per 3 detik + Supabase REST per 1 menit), dilengkapi REST API lokal port 80, penyimpanan kalibrasi permanen **NVS (`Preferences`)**, layar **OLED SH1106**, serta kontrol daya sensor & Jetson Nano.

---

## Struktur Sketch (`firmware/ESP32IoTADMIN/`)

Seluruh 7 file `.ino` berada di dalam folder `firmware/ESP32IoTADMIN/` agar dapat dikompilasi langsung oleh Arduino IDE:

| File | Fungsi |
| :--- | :--- |
| `ESP32IoTADMIN.ino` | Entry point `setup()`/`loop()`, konfigurasi WiFi, ID perangkat (`SIPANDU_A`), kolam (`POND_NUMBER = 5`), konstanta pin, dan variabel kalibrasi global |
| `READ.ino` | Akuisisi ADC `ADS1115 A` (`0x48`) & `ADS1115 B` (`0x49`) dengan *mux settling*, `computeVolts()`, auto re-init I2C, `DS18B20`, `INA226`, dan log mentah `[pH raw=...]` / `[DO raw=...]` |
| `DATA.ino` | Load/save kalibrasi NVS (`Preferences` namespace `"sipandu"`) dengan guard `isnan`/`isinf`/zero-slope serta fungsi `calibratePH()` |
| `MQTT_SUPABASE.ino` | Koneksi MQTT broker (`192.168.100.41:1883`), handler perintah `command`, serta upload HTTPS Supabase (`POST /sensor_logs` & `PATCH /devices`) memakai `WiFiClientSecure` (`setInsecure()`) dengan fallback otomatis bila kolom `do_level` belum ada |
| `SERVER.ino` | REST API lokal port 80 (`/api/status`, `/api/sensors`, `/api/calibration`, `/api/calibration/reset`, `/api/control/sensors`, `/api/control/jetson`) dengan header CORS `*` |
| `CONTROL.ino` | Kontrol MOSFET daya sensor (`GPIO 32`), MOSFET Jetson (`GPIO 33`), dan *graceful shutdown* Jetson 30 detik |
| `OLED.ino` | Driver tampilan status sensor & jaringan pada OLED SH1106 `128×64` |

---

## Library Arduino yang Dibutuhkan

Pastikan library berikut terpasang melalui **Library Manager** Arduino IDE:
- `PubSubClient` (Nick O'Leary)
- `ArduinoJson` (v6+)
- `Adafruit ADS1X15`
- `Adafruit SH110X` & `Adafruit GFX`
- `DallasTemperature` & `OneWire`
- `INA226_WE`

---

## Pinout Hardware & Alamat I2C

| Komponen | Pin / Alamat | Keterangan |
| :--- | :--- | :--- |
| **DS18B20** | `GPIO 25` | Sensor suhu air (OneWire, 12-bit) |
| **I2C Bus** | `SDA GPIO 21`, `SCL GPIO 22` | Clock `50 kHz` untuk ADC, INA226, dan OLED |
| **MOSFET Sensor** | `GPIO 32` | Saklar daya ke seluruh modul sensor |
| **MOSFET Jetson** | `GPIO 33` | Saklar daya ke NVIDIA Jetson Nano |
| **ADS1115 A** | I2C `0x48` | `A1`: pH (dengan *voltage divider* `10kΩ/20kΩ`, koreksi `×1.5`), `A0`: DO (`GAIN_TWOTHIRDS`) |
| **ADS1115 B** | I2C `0x49` | `A1`: TDS, `A0`: Turbidity (dengan *voltage divider* `×1.5`, `GAIN_TWOTHIRDS`) |
| **INA226** | I2C `0x44` | Sensor tegangan baterai (`offsetINA = -0.50V`) |
| **OLED SH1106** | I2C `0x3C` | Layar informasi lokal `128×64` |

---

## Parameter Kalibrasi Default

| Sensor | Rumus | Default Slope | Default Offset |
| :--- | :--- | :---: | :---: |
| **pH** | `(voltage × 1.5 × phSlope) + phOffset` | `-5.70` | `21.34` |
| **DO** | `(voltage × doSlope) + doOffset` | `5.5` | `0.0` |
| **TDS** | `(voltage × tdsSlope) + tdsOffset` | `500.0` | `0.0` |
| **Turbidity** | `(voltage × 1.5 × turbSlope) + turbOffset` | `-1120.4` | `4895.87` |

---

## Cara Upload & Diagnosa Serial Monitor

1. Buka `firmware/ESP32IoTADMIN/ESP32IoTADMIN.ino` di Arduino IDE dan pastikan **7 tab `.ino`** tampil di editor.
2. Pilih board **DOIT ESP32 DEVKIT V1** dan port COM USB ESP32 yang aktif (pastikan kabel USB mendukung transfer data dan driver **CP210x**/**CH340** terpasang).
3. Klik **Upload**, lalu buka **Serial Monitor** di baud rate **`115200`**.
4. Periksa baris diagnostik berikut:
   - `CL:` — nilai slope/offset kalibrasi yang dimuat dari NVS saat boot.
   - `[pH raw=... V=... -> ...]` & `[DO raw=... V=... -> ... mg/L]` — nilai mentah ADC `ADS1115 A`. Jika `DO raw` berada di `0–5` (`~0.0008V`), periksa suplai daya/probe/kabel sinyal ke pin `A0` `ADS1115 A`.
   - `[SUPABASE] Riwayat sensor_logs berhasil diunggah (HTTP 201)` — konfirmasi data masuk ke Supabase.
   - `[MQTT] Menghubungkan ke broker MQTT...` — bila muncul `GAGAL (rc=-2)`, pastikan broker `mosquitto` di Jetson Nano (`192.168.100.41:1883`) aktif dan mengizinkan koneksi eksternal.
