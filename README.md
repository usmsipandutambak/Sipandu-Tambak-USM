# SIPANDU-TAMBAK — Sistem Pemantauan Kualitas Air Tambak & Deteksi Ikan Real-Time

**SIPANDU-TAMBAK** adalah sistem pemantauan kualitas air tambak dan pemantauan kamera bawah/permukaan air secara *real-time* yang mengintegrasikan perangkat **IoT ESP32**, **Edge AI NVIDIA Jetson Nano**, **Cloud Database Supabase**, **MQTT Broker**, **Web Admin Dashboard**, dan **Aplikasi Mobile Android**.

---

## Arsitektur & Struktur Monorepo

```text
SIPANDU-TAMBAK/
├── firmware/
│   ├── ESP32IoTADMIN/          # Sketch Arduino lengkap (ESP32IoTADMIN.ino + 6 modul .ino)
│   └── README.md               # Panduan hardware, pinout, ADC ADS1115, NVS, & Supabase/MQTT
├── edge/
│   ├── stream_camera.py        # Server MJPEG & MQTT daemon headless untuk Jetson Nano
│   ├── tunnel.sh               # Skrip publikasi tunnel (Cloudflare / Ngrok)
│   └── README.md               # Panduan Edge AI Jetson Nano & layanan kamera
├── web/
│   ├── index.html              # Single Page Application (Dashboard, Control, Camera, Data)
│   ├── assets/                 # CSS, JS modular (ESP32, Jetson, Supabase, MQTT, Kalibrasi)
│   ├── docs/                   # Dokumentasi arsitektur Web Admin
│   └── README.md               # Panduan menjalankan Web Admin & endpoint konfigurasi
├── mobile/
│   ├── app/                    # Aplikasi Android Kotlin + Jetpack Compose (id.ac.usm.sipandutambak)
│   ├── gradle/                 # Gradle wrapper & katalog dependensi
│   └── README.md               # Panduan build APK, fitur Compose, & sinkronisasi Supabase
├── tests/
│   └── stress_test_suite.py    # Skrip pengujian integrasi (MQTT, Supabase, Shutdown, MJPEG load)
├── .gitignore
└── README.md                   # Dokumentasi utama monorepo
```

---

## Ringkasan Komponen Sistem

### 1. `firmware/` (`ESP32IoTADMIN` & `MQTT_SUPABASE`)
- **Mikrokontroler**: ESP32 DevKit (`DEVICE_ID = "SIPANDU_A"`, `POND_NUMBER = 5`, IP LAN `192.168.100.213`).
- **Akuisisi Sensor**:
  - `ADS1115 A` (`0x48`): **pH** (`A1`, dengan *voltage divider* koreksi `×1.5`) & **DO / Dissolved Oxygen** (`A0`).
  - `ADS1115 B` (`0x49`): **TDS** (`A1`) & **Turbidity** (`A0`, dengan *voltage divider* koreksi `×1.5`).
  - `DS18B20` (`GPIO 25`): Suhu air (°C).
  - `INA226` (`0x44`): Tegangan baterai (V).
  - `OLED SH1106` (`0x3C`): Tampilan status lokal `128×64`.
- **Kalibrasi & API Lokal**: Parameter kalibrasi (`ph`, `do`, `tds`, `turbidity`) disimpan permanen pada **NVS (`Preferences` `"sipandu"`)** dan dapat diakses/diubah melalui **REST API lokal Port 80** (`/api/status`, `/api/sensors`, `/api/calibration`, `/api/calibration/reset`, `/api/control/sensors`, `/api/control/jetson`).
- **Dual-Stream Telemetri**:
  - **MQTT Broker** (`192.168.100.41:1883`): Publikasi tiap 3 detik ke `sipandu/devices/SIPANDU_A/telemetry` dan menerima perintah di `sipandu/devices/SIPANDU_A/command`.
  - **Supabase REST** (tiap 60 detik): `POST /sensor_logs` via HTTPS (`WiFiClientSecure` dengan `setInsecure()`) serta sinkronisasi status `PATCH /devices`.

### 2. `edge/` (`stream_camera.py` di NVIDIA Jetson Nano)
- Berjalan di **NVIDIA Jetson Nano (`192.168.100.41`)**.
- Layanan utama kamera & inferensi deteksi objek ikan menyajikan **live MJPEG stream** di `http://192.168.100.41:5000/video_feed`.
- `edge/stream_camera.py` menyediakan server streaming MJPEG *headless* (USB / CSI / `--test-pattern`) dengan listener MQTT `sipandu/devices/SIPANDU_A/command` untuk *graceful shutdown* Jetson Nano.

### 3. `web/` (Web Admin Dashboard)
- Berbasis **HTML5 / CSS3 / Vanilla JavaScript** tanpa ketergantungan build framework.
- Terhubung ke **ESP32** (`192.168.100.213`), **Kamera Jetson Nano** (`http://192.168.100.41:5000/video_feed` dengan indikator status otomatis `● ONLINE` saat stream termuat), **MQTT WebSocket** (`192.168.100.41`), dan **Supabase REST** (`supabase-client.js`).
- Menyimpan dan menyinkronkan kalibrasi **pH, DO, TDS, dan Turbidity** secara bersamaan ke `localStorage`, tabel `devices` Supabase, dan HTTP/MQTT ESP32.

### 4. `mobile/` (Aplikasi Android `id.ac.usm.sipandutambak`)
- Dibangun dengan **Kotlin & Jetpack Compose** (Material 3, MVVM, Retrofit/OkHttp, WorkManager).
- **Fitur Utama**:
  - Pemantauan 8 parameter sensor *real-time* dari tabel `sensor_logs` Supabase beserta notifikasi ambang batas kualitas air.
  - Grafik tren telemetri 24 jam & ekspor riwayat data ke **CSV**.
  - **Monitoring Kamera Live (`CameraMonitoringScreen`)** berbasis `WebView` untuk stream MJPEG Jetson Nano (`http://192.168.100.41:5000/video_feed`) lengkap dengan dialog **Setting Kamera** (`SharedPreferences`: URL stream, tes koneksi HTTP, pilihan resolusi, FPS, dan durasi sesi).
  - **Kalibrasi 2-Arah (`CalibrationScreen`)** yang membaca dan memperbarui parameter kalibrasi pH, DO, TDS, dan Turbidity di tabel `devices` Supabase.

### 5. `tests/`
- Berisi [stress_test_suite.py](tests/stress_test_suite.py) untuk menguji beban MQTT burst, latensi/timeout Supabase REST, simulasi siklus graceful shutdown Jetson, serta *concurrency load test* stream MJPEG.

---

## Skema Database Supabase

Project Supabase: `tidmdjnjhqptszyubvue` (`https://tidmdjnjhqptszyubvue.supabase.co/rest/v1`).

### Tabel `sensor_logs`
| Kolom | Tipe | Keterangan |
| :--- | :--- | :--- |
| `id` | `bigint` / `uuid` | Primary key auto-generated |
| `device_id` | `text` | ID perangkat, contoh: `SIPANDU_A` |
| `pond_number` | `integer` | Nomor kolam (default Alat A: `5`) |
| `ph_level` | `double precision` | Nilai pH air (`0–14`) |
| `do_level` | `double precision` | Kadar oksigen terlarut / DO (`mg/L`) |
| `tds_ppm` | `double precision` | Total Dissolved Solids (`ppm`) |
| `turbidity_ntu` | `double precision` | Kekeruhan air (`NTU`) |
| `temperature_c` | `double precision` | Suhu air (`°C`) |
| `battery_voltage` | `double precision` | Tegangan baterai sistem (`V`) |
| `battery_current_a` | `double precision` | Arus baterai (`A`) |
| `status` | `text` | Status kualitas air (`NORMAL` / `WASPADA` / `BAHAYA`) |
| `created_at` | `timestamptz` | Waktu pencatatan server Supabase |

### Tabel `devices` (Snapshot & Kalibrasi)
Menyimpan identitas perangkat (`id`, `name`, `pond_number`, `commodity`, `ip_address`, `battery_percent`, `signal_quality`, `status`, `is_connected`, `last_seen`) serta koefisien kalibrasi yang disinkronkan antara Web Admin dan Android:
- `ph_slope`, `ph_offset`
- `do_slope`, `do_offset`
- `tds_slope`, `tds_offset`
- `turb_slope`, `turb_offset`

> **SQL Migrasi Kolom DO** (jalankan di Supabase SQL Editor bila belum tersedia):
> ```sql
> ALTER TABLE sensor_logs ADD COLUMN IF NOT EXISTS do_level DOUBLE PRECISION;
> ALTER TABLE devices ADD COLUMN IF NOT EXISTS do_slope DOUBLE PRECISION DEFAULT 5.5, ADD COLUMN IF NOT EXISTS do_offset DOUBLE PRECISION DEFAULT 0.0;
> ```

---

## Panduan Instalasi & Menjalankan Setiap Modul

### 1. Firmware ESP32 (`firmware/`)
1. Buka `firmware/ESP32IoTADMIN/ESP32IoTADMIN.ino` di **Arduino IDE** (pastikan seluruh 7 file `.ino` berada dalam folder `firmware/ESP32IoTADMIN/`).
2. Pasang library yang tercantum di [`firmware/README.md`](firmware/README.md).
3. Pilih board **DOIT ESP32 DEVKIT V1**, pilih port COM USB ESP32 yang aktif, lalu klik **Upload** dan pantau Serial Monitor (`115200` baud).

### 2. Edge Camera Jetson Nano (`edge/`)
```bash
cd edge
pip3 install paho-mqtt
# Jalankan stream MJPEG pada port 5000 (atau gunakan layanan deteksi objek Jetson pada :5000/video_feed)
python3 stream_camera.py --port 5000 --mqtt-broker 192.168.100.41 --device-id SIPANDU_A
```
Detail opsi CLI dan systemd autostart tersedia di [`edge/README.md`](edge/README.md).

### 3. Web Admin (`web/`)
```bash
cd web
python -m http.server 3000
```
Buka browser ke `http://localhost:3000`. Panduan lengkap tersedia di [`web/README.md`](web/README.md).

### 4. Aplikasi Android (`mobile/`)
```bash
cd mobile
.\gradlew installDebug
```
Atau buka folder `mobile/` menggunakan **Android Studio** dan jalankan ke perangkat Android. Panduan lengkap tersedia di [`mobile/README.md`](mobile/README.md).

### 5. Pengujian Integrasi (`tests/`)
```bash
pip install requests paho-mqtt
python tests/stress_test_suite.py
```
