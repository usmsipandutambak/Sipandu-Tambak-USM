# SIPANDU TAMBAK — Web Admin

Sistem monitoring dan kontrol kualitas air tambak berbasis ESP32 + Local Network.

## Cara Menjalankan

### 1. Upload Firmware ESP32
- Buka `Code/ESP32IoTADMIN/ESP32IoTADMIN.ino` di Arduino IDE.
- Pastikan library terinstall:
  - `Adafruit ADS1X15`
  - `Adafruit SH110X` + `Adafruit GFX`
  - `DallasTemperature` + `OneWire`
  - `INA226_WE`
  - `WebServer` (built-in ESP32)
  - `ArduinoJson` (v6+)
- Upload ke ESP32 DevKit.
- Buka Serial Monitor → catat IP yang tampil, contoh: `192.168.1.105`

### 2. Jalankan Web Admin

**Cara paling mudah — Python HTTP Server:**
```bash
cd web-admin/
python3 -m http.server 8080
```
Lalu buka browser: `http://localhost:8080`

**Atau gunakan VS Code Live Server / any static server.**

### 3. Konfigurasi IP ESP32
- Di sidebar Web Admin, masukkan IP ESP32 (contoh: `192.168.1.105`)
- Klik **Terapkan IP**
- Status header akan berubah menjadi **ESP32 Online**

### 4. Streaming Kamera Jetson Nano (MJPEG)

**Di Jetson Nano (headless, via SSH):**
```bash
scp Code/ADMIN/jetson-stream/stream_camera.py jetson@<ip-jetson>:~/
ssh jetson@<ip-jetson>

python3 stream_camera.py                    # USB camera (/dev/video0)
python3 stream_camera.py --camera-type csi  # kamera CSI (Raspberry Pi v2, dll.)
python3 stream_camera.py --test-pattern     # tes stream tanpa kamera fisik
```

- Stream : `http://<ip-jetson>:8080/stream`
- Status : `http://<ip-jetson>:8080/api/status`
- Tes    : buka `http://<ip-jetson>:8080/` langsung di browser laptop

**Di Web Admin:**
- Sidebar → **IP Jetson Nano** → masukkan IP (contoh: `192.168.1.101`) → klik **Terapkan IP Jetson**
- Frame kamera di Dashboard dan halaman **Camera** akan menampilkan live stream
- Chip status berubah: `STANDBY` → `LIVE` (hijau) / `OFFLINE` (merah)

**Catatan:**
- Server bersifat **headless** (tanpa GUI/display) — aman dijalankan lewat SSH
- Dependency hanya OpenCV + NumPy (bawaan JetPack) — kompatibel **Python 3.6.9**
- Port default `8080` — pastikan terbuka jika firewall aktif: `sudo ufw allow 8080`
- Detail opsi CLI, endpoint, dan auto-start saat boot: lihat `jetson-stream/README.md`

## Struktur API ESP32

| Method | Endpoint               | Deskripsi                       |
|--------|------------------------|---------------------------------|
| GET    | /api/status            | Status perangkat dan koneksi    |
| GET    | /api/sensors           | Semua nilai sensor terbaru      |
| GET    | /api/calibration       | Konfigurasi kalibrasi aktif     |
| POST   | /api/calibration       | Simpan kalibrasi baru ke NVS    |
| POST   | /api/calibration/reset | Reset kalibrasi ke default      |
| POST   | /api/control/sensors   | ON/OFF daya sensor (GPIO 33)    |
| POST   | /api/control/jetson    | ON/OFF daya Jetson (GPIO 32)    |

## Catatan CORS
ESP32 sudah dikonfigurasi dengan header CORS `Access-Control-Allow-Origin: *`.
Browser dapat mengakses langsung selama website dan ESP32 berada dalam jaringan yang sama.

## Perangkat Keras
- ESP32 DevKit
- DS18B20 → GPIO 25
- I2C SDA → GPIO 21, SCL → GPIO 22
- MOSFET Sensor → GPIO 33
- MOSFET Jetson → GPIO 32
- ADS1115 A → 0x48 (pH: A1, DO: A0)
- ADS1115 B → 0x49 (TDS: A1, Turbidity: A0)
- INA226 → 0x44
- OLED SH1106 128×64 → 0x3C
