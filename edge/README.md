# jetson-stream — Server Kamera MJPEG untuk Jetson Nano

Server streaming kamera **headless** (tanpa GUI) untuk SIPANDU TAMBAK.
Didesain untuk Jetson Nano dengan **Python 3.6.9** (bawaan JetPack) dan diakses lewat SSH.

## Kebutuhan

| Item | Keterangan |
|------|------------|
| Python | 3.6.9 (bawaan Jetson Nano) — tidak perlu upgrade |
| OpenCV | `python3-opencv` (bawaan JetPack). Cek: `python3 -c "import cv2; print(cv2.__version__)"` |
| NumPy | bawaan JetPack |
| Paho MQTT | `pip3 install paho-mqtt` (untuk listener shutdown & heartbeat) |
| GUI | **TIDAK dibutuhkan** — murni terminal/SSH |

Instalasi dependency tambahan (jika belum ada):
```bash
sudo apt-get update && sudo apt-get install -y python3-opencv
pip3 install paho-mqtt
```

## Menjalankan

```bash
# 1. Jalankan streaming dengan koneksi broker MQTT Paho:
python3 stream_camera.py --mqtt-broker broker.hivemq.com --device-id Sipandu_A

# 2. Kamera CSI (Raspberry Pi v2 / IMX219) + MQTT:
python3 stream_camera.py --camera-type csi --mqtt-broker broker.hivemq.com

# 3. Mode tes tanpa kamera fisik (diagnostic):
python3 stream_camera.py --test-pattern --mqtt-broker broker.hivemq.com

# 4. Membuka streaming ke internet publik (Cloudflare Tunnel):
bash tunnel.sh
# Salin URL https://...trycloudflare.com/stream ke aplikasi Android / Web Admin!
```

Stop: `Ctrl+C` (ditangani rapi — kamera dilepas, thread berhenti).

## Endpoint

| URL | Deskripsi |
|-----|-----------|
| `http://<ip-jetson>:8080/` | Halaman tes sederhana (langsung menampilkan stream) |
| `http://<ip-jetson>:8080/stream` | Stream MJPEG (`multipart/x-mixed-replace`) |
| `http://<ip-jetson>:8080/snapshot.jpg` | Satu frame JPEG |
| `http://<ip-jetson>:8080/api/status` | Status JSON (CORS `*`, dipakai Web Admin) |

Respons `/api/status` (contoh):
```json
{
  "device": "jetson-nano",
  "hostname": "jetson",
  "python": "3.6.9",
  "opencv": "4.1.1",
  "camera": { "type": "usb", "source": "0", "width": 1280, "height": 720, "fps_target": 30 },
  "stream": { "width": 1280, "height": 720, "fps": 29.8, "jpeg_quality": 80, "frames": 1234 },
  "clients": 1,
  "camera_ok": true
}
```

## Opsi CLI Lengkap

```
--host            Bind address (default: 0.0.0.0)
--port            Port HTTP (default: 8080)
--camera          Index/perangkat USB: 0 / 1 / /dev/video0 (default: 0)
--camera-type     usb | csi (default: usb)
--sensor-id       Sensor ID untuk CSI (default: 0)
--gstreamer-usb   Pakai pipeline GStreamer v4l2src untuk USB
--width           Lebar frame (default: 1280)
--height          Tinggi frame (default: 720)
--fps             Target FPS (default: 30)
--quality         Kualitas JPEG 1-100 (default: 80)
--flip            none | h | v | hv (balik gambar)
--test-pattern    Frame sintetis tanpa kamera
--log-level       DEBUG | INFO | WARNING | ERROR (default: INFO)
```

> **Catatan CSI:** gunakan resolusi/framerate yang didukung sensor
> (mis. IMX219: 1280x720@30, 1920x1080@30, 3280x2464@21).

## Autostart saat Boot (systemd)

```bash
sudo tee /etc/systemd/system/sipandu-cam.service <<'EOF'
[Unit]
Description=SIPANDU CAM - Jetson MJPEG server
After=network.target

[Service]
User=jetson
WorkingDirectory=/home/jetson
ExecStart=/usr/bin/python3 /home/jetson/stream_camera.py --camera-type usb
Restart=on-failure
RestartSec=3

[Install]
WantedBy=multi-user.target
EOF

sudo systemctl daemon-reload
sudo systemctl enable --now sipandu-cam
journalctl -u sipandu-cam -f   # lihat log
```

(Ganti `User=` dan path sesuai username/lokasi file di Jetson Anda.)

## Integrasi Web Admin

1. Jalankan server di Jetson (lihat di atas).
2. Buka Web Admin → sidebar **IP Jetson Nano** → masukkan IP → **Terapkan IP Jetson**.
3. Frame kamera Dashboard & halaman Camera menampilkan stream otomatis.

## Troubleshooting

| Gejala | Solusi |
|--------|--------|
| `Kamera tidak bisa dibuka` | Cek `ls /dev/video*`, colok-ulang kamera, tutup aplikasi lain yang memakai kamera |
| Frame sangat lambat | Turunkan `--width/--height`, naikkan `--quality` ke atas? (justru turunkan quality ke 60), pakai `--gstreamer-usb` |
| Tidak bisa diakses dari laptop | Pastikan satu jaringan, cek `ping <ip-jetson>`, buka port: `sudo ufw allow 8080` |
| CSI hitam/erroneous | Cek pipeline `nvidia-l4t-multimedia` terpasang, resolusi didukung sensor |
| Stream putus-putus | Kurangi resolusi/FPS; pastikan tidak terlalu banyak klien (`clients` di `/api/status`) |
