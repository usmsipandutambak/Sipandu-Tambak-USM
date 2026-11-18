# SIPANDU-TAMBAK — Edge Camera & AI Service (`edge/`)

Direktori `edge/` berisi skrip layanan kamera *headless* untuk **NVIDIA Jetson Nano (`192.168.100.41`)** agar dapat menyajikan *live MJPEG stream* ke **Web Admin** dan **Aplikasi Mobile Android**, sekaligus mendengarkan instruksi *graceful shutdown* melalui MQTT.

---

## Arsitektur Streaming Kamera Jetson Nano

1. **Endpoint Aktif Produksi (`:5000/video_feed`)**:
   - Web Admin (`web/`) dan Aplikasi Android (`mobile/`) dikonfigurasi secara default mengarah ke **`http://192.168.100.41:5000/video_feed`** (layanan Flask/OpenCV MJPEG & inferensi objek ikan pada Jetson Nano).
2. **Server Stream Standalone (`stream_camera.py`)**:
   - Skrip Python *headless* (kompatibel **Python 3.6.9+** bawaan JetPack tanpa GUI) yang membaca kamera **USB (`V4L2`)**, **CSI (`nvarguscamerasrc`)**, atau **`--test-pattern`**.
   - Terintegrasi dengan `paho-mqtt` pada topik `sipandu/devices/SIPANDU_A/command` (`192.168.100.41:1883`) untuk mengeksekusi `sudo shutdown -h now` saat instruksi `power_jetson: false` dikirim dari ESP32/Web Admin.

---

## Kebutuhan Sistem

| Komponen | Keterangan |
| :--- | :--- |
| Python | 3.6.9+ (bawaan NVIDIA JetPack) |
| OpenCV & NumPy | `python3-opencv` & `numpy` |
| Paho MQTT | `pip3 install paho-mqtt` |

Instalasi dependensi di Jetson Nano:
```bash
sudo apt-get update && sudo apt-get install -y python3-opencv
pip3 install paho-mqtt
```

---

## Menjalankan Server Kamera (`stream_camera.py`)

```bash
cd edge

# 1. Jalankan stream USB kamera + listener MQTT (broker Jetson lokal)
python3 stream_camera.py --port 5000 --mqtt-broker 192.168.100.41 --device-id SIPANDU_A

# 2. Kamera CSI (IMX219 / Raspberry Pi Cam v2)
python3 stream_camera.py --camera-type csi --port 5000 --mqtt-broker 192.168.100.41

# 3. Mode uji tanpa kamera fisik (test pattern)
python3 stream_camera.py --test-pattern --port 5000 --no-auto-shutdown
```

> Catatan: Default bawaan CLI `--port` di `stream_camera.py` adalah `8080` dengan endpoint `/stream`, `/snapshot.jpg`, dan `/api/status`. Gunakan `--port 5000` atau sesuaikan port/path stream pada Web Admin & Android jika menjalankan `stream_camera.py` langsung.

### Opsi CLI Utama
| Flag | Default | Deskripsi |
| :--- | :--- | :--- |
| `--host` | `0.0.0.0` | Bind address HTTP server |
| `--port` | `8080` | Port HTTP server |
| `--camera` | `0` | Index/device kamera USB (`0`, `/dev/video0`, dll.) |
| `--camera-type` | `usb` | Jenis input kamera: `usb` atau `csi` |
| `--width` / `--height` | `1280` / `720` | Resolusi frame |
| `--fps` / `--quality` | `30` / `80` | Target FPS dan kualitas JPEG (`1–100`) |
| `--mqtt-broker` | `192.168.100.41` | Alamat broker MQTT |
| `--mqtt-port` | `1883` | Port broker MQTT |
| `--device-id` | `SIPANDU_A` | ID perangkat topik MQTT |
| `--no-auto-shutdown` | `false` | Nonaktifkan eksekusi `sudo shutdown` (*dry run*) |

---

## Publikasi Tunnel & Autostart (`systemd`)

- **Akses Publik (`tunnel.sh`)**:
  ```bash
  bash tunnel.sh
  ```
  Untuk mengekspos stream MJPEG lokal melalui Cloudflare Tunnel (`trycloudflare.com`) / Ngrok.
- **Autostart Systemd**:
  ```bash
  sudo tee /etc/systemd/system/sipandu-cam.service <<'EOF'
  [Unit]
  Description=SIPANDU CAM - Jetson MJPEG server
  After=network.target

  [Service]
  User=jetson
  WorkingDirectory=/home/jetson/SIPANDU-TAMBAK/edge
  ExecStart=/usr/bin/python3 stream_camera.py --camera-type usb --port 5000
  Restart=on-failure
  RestartSec=3

  [Install]
  WantedBy=multi-user.target
  EOF

  sudo systemctl daemon-reload
  sudo systemctl enable --now sipandu-cam
  ```
