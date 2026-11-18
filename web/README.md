# SIPANDU-TAMBAK — Web Admin Dashboard (`web/`)

Dashboard administrasi dan pemantauan *real-time* berbasis **HTML5, CSS3, dan Vanilla JavaScript** untuk mengelola node ESP32 (`192.168.100.213`), live stream kamera Jetson Nano (`192.168.100.41:5000/video_feed`), dan kalibrasi sensor tersinkronisasi ke Supabase.

---

## Fitur Utama

1. **Dashboard Kualitas Air Real-Time (`dashboard.js`, `realtime.js`)**:
   - Menampilkan pembacaan **pH, Dissolved Oxygen (DO), TDS, Turbidity, Suhu Air, dan Baterai**.
   - Membaca data secara langsung dari HTTP ESP32 / MQTT WebSocket, dengan **fallback polling otomatis ke Supabase** (`sensor_logs`) melalui `supabase-client.js` saat perangkat LAN tidak terjangkau.
2. **Live Stream Kamera Jetson Nano (`camera.js`, `jetson-api.js`)**:
   - Default endpoint mengarah ke `http://192.168.100.41:5000/video_feed` di tampilan Dashboard (`#dash-cam-stream`) dan halaman Camera (`#cam-stream`).
   - Otomatis mengubah badge status menjadi **`● ONLINE`** saat stream berhasil dimuat (`onload`) serta melakukan percobaan ulang otomatis tiap 5 detik saat terputus.
3. **Manajemen Kalibrasi Tersinkronisasi (`calibration.js`, `supabase-client.js`)**:
   - Menyimpan dan menyinkronkan parameter kalibrasi **pH, DO, TDS, dan Turbidity** secara bersamaan ke **`localStorage`**, tabel **`devices` Supabase**, serta **ESP32 HTTP/MQTT** (`/api/calibration`).
   - Mendukung aksi **Simpan / Terapkan** maupun **Reset ke Default** firmware.
4. **Kontrol Daya Jarak Jauh (`control.js`, `esp32-api.js`, `mqtt-client.js`)**:
   - Kendali saklar daya modul sensor (`GPIO 32`) dan Jetson Nano (`GPIO 33`) melalui REST API lokal ESP32 maupun Paho MQTT WebSocket.

---

## Struktur File `web/`

```text
web/
├── index.html                   # Kerangka SPA (Home, Control, Camera, Data)
├── assets/
│   ├── css/                     # style.css, dashboard.css, camera.css, control.css, responsive.css
│   └── js/
│       ├── app.js               # Inisialisasi modul & konfigurasi IP sidebar
│       ├── supabase-client.js   # Klien REST Supabase (sensor_logs & kalibrasi devices)
│       ├── jetson-api.js        # Normalisasi host/port/path stream kamera Jetson (:5000/video_feed)
│       ├── camera.js            # Siklus hidup live stream & badge ONLINE/OFFLINE
│       ├── esp32-api.js         # Klien REST API lokal ESP32 (default 192.168.100.213)
│       ├── mqtt-client.js       # Klien Paho MQTT WebSocket (default 192.168.100.41, SIPANDU_A)
│       ├── realtime.js          # Polling telemetri & fallback Supabase
│       ├── calibration.js       # Form & sinkronisasi kalibrasi 4 sensor
│       ├── dashboard.js         # Kartu metrik & status ambang batas
│       ├── control.js           # Kontrol relay daya sensor & Jetson
│       ├── data.js              # Tabel riwayat telemetri & ekspor CSV
│       ├── router.js            # Navigasi halaman SPA
│       └── utils.js             # Helper format & toast notifikasi
└── docs/architecture.md
```

---

## Cara Menjalankan

```bash
cd web
python -m http.server 3000
```
Buka **`http://localhost:3000`** di browser modern, lalu sesuaikan IP di panel sidebar (default: **ESP32** `192.168.100.213`, **Jetson & MQTT** `192.168.100.41`, **Device ID** `SIPANDU_A`).
