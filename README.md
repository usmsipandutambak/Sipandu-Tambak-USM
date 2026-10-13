# SIPANDU TAMBAK — Sistem Terpadu Pemantauan & Kontrol Kualitas Air Tambak

Sistem IoT Terpadu Pemantauan dan Pengendalian Kualitas Air Tambak berbasis **ESP32 DevKit**, **Edge AI Jetson Nano**, **Cloud MQTT Broker (Eclipse Paho)**, **Database Supabase Cloud**, **Web Admin Dashboard SPA**, dan **Aplikasi Android Mobile (Jetpack Compose)**.

---

## Arsitektur Monorepo

Proyek ini terstruktur ke dalam 4 modul utama:

```
SIPANDU-TAMBAK/
├── firmware/         # Firmware Microcontroller ESP32 (Arduino C++)
├── edge/             # Server Video Streaming & Edge Daemon Jetson Nano (Python)
├── web/              # Web Admin Dashboard SPA (HTML5, Vanilla CSS, JS)
├── mobile/           # Aplikasi Android Native (Kotlin, Jetpack Compose, MVVM)
└── README.md         # Dokumentasi Utama
```

---

## Diagram Aliran Data & Integrasi

```
                               ┌───────────────────────────┐
                               │   CLOUD INFRASTRUCTURE    │
                               │                           │
                               │  Broker MQTT   Supabase   │
                               │   (Paho)       Database   │
                               └─────▲───┬─────────▲───────┘
                                     │   │         │
                 Real-time Telemetry │   │ Command │ Upload Riwayat
                        (tiap 2-3s)  │   │ 2-Arah  │ (tiap 1 Menit)
                                     │   │         │
       ┌─────────────────────────────┴───▼─┐       │
       │                                   │       │
       │    ESP32 DevKit (firmware/)       ├───────┘
       │    • Multi-Sensor: pH, DO, TDS,   │
       │      Turbidity, Temp, Baterai     │
       │    • MOSFET Daya Sensor & Jetson  │
       │    • Graceful Shutdown Jetson 30s │
       │                                   │
       └─────────────────▲─────────────────┘
                         │ Relai Daya (GPIO 33)
                         ▼
       ┌───────────────────────────────────┐
       │     Jetson Nano (edge/)           │
       │     • Stream Kamera MJPEG (8080)  ├───── Public HTTPS Tunnel ────┐
       │     • Listener Shutdown Paho MQTT │     (Cloudflare / Ngrok)     │
       └───────────────────────────────────┘                              │
                                                                          ▼
                                                       ┌──────────────────────────────┐
                                                       │       APLIKASI KLIEN         │
                                                       │                              │
                                                       │ 1. Web Admin (web/)          │
                                                       │ 2. Android Mobile (mobile/)  │
                                                       │                              │
                                                       │ • Live Telemetry Realtime    │
                                                       │ • Live Stream Video Kamera   │
                                                       │ • Kontrol & Kalibrasi Jarak  │
                                                       │   Jauh dari Mana Saja        │
                                                       └──────────────────────────────┘
```

---

## Ringkasan Per Modul

### 1. `firmware/` (ESP32 DevKit)
- **Komunikasi Dual-Stream**:
  - **MQTT Real-Time (tiap 3s)**: Mengirim telemetri ke `sipandu/devices/Sipandu_A/telemetry`.
  - **Supabase Riwayat (tiap 1 menit)**: Mengunggah log permanen ke tabel `sensor_logs` dan snapshot ke tabel `devices`.
- **Command Subscriber**: Menerima perintah kendali daya dan kalibrasi sensor dari topic `sipandu/devices/Sipandu_A/command`.
- **Proteksi Jetson Nano**: Menerapkan jeda hitung mundur 30 detik saat perintah shutdown diterima sebelum memutus relai MOSFET fisik.

### 2. `edge/` (Jetson Nano)
- **Streaming Kamera**: Skrip `stream_camera.py` melayani video MJPEG di port 8080 (kamera CSI, USB, atau test pattern).
- **Paho MQTT Daemon**: Mendengarkan instruksi shutdown dan otomatis memicu `sudo shutdown -h now` secara aman.
- **Tunneling**: Skrip `tunnel.sh` untuk mengekspos kamera ke internet publik lewat Cloudflare Tunnel / Ngrok.

### 3. `web/` (Web Admin Dashboard)
- Berbasis Vanilla JS modern + Lucide Icons tanpa framework runtime berat.
- Terintegrasi dengan **Paho MQTT WebSocket** (`broker.hivemq.com:8000`) untuk pemantauan real-time & kendali jarak jauh.
- Tetap memiliki fallback mode direct HTTP LAN (`192.168.1.xxx`) untuk pengujian lokal di tepi tambak.

### 4. `mobile/` (Android App)
- Dibangun dengan Jetpack Compose, Coroutine, StateFlow, dan Retrofit.
- **Kamera Terpadu**: `CameraMonitoringScreen` menampilkan feed video real-time dari Jetson Nano (IP lokal atau Cloudflare Tunnel).
- **Kalibrasi Terpadu**: `CalibrationScreen` menyimpan dan menyinkronkan parameter sensor langsung ke database Supabase dan perangkat.

---

## Cara Menjalankan

### Menjalankan Web Admin:
```bash
cd web
python -m http.server 8080
# Buka di browser: http://localhost:8080
```

### Menjalankan Jetson Camera:
```bash
cd edge
pip3 install paho-mqtt
python3 stream_camera.py --mqtt-broker broker.hivemq.com --device-id Sipandu_A
```

### Menjalankan Android App:
Buka folder `mobile/` di **Android Studio**, lalu jalankan di perangkat fisik atau emulator Android.
