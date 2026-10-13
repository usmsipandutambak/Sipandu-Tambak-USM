#!/usr/bin/env bash
# ==============================================================================
# SIPANDU TAMBAK — Script Tunneling Publik untuk Kamera Jetson Nano
# Membuka port 8080 Jetson Nano ke internet via Cloudflare Tunnel atau Ngrok
# ==============================================================================

PORT=8080

echo "=========================================================="
echo " SIPANDU TAMBAK — Public Camera Streaming Tunnel"
echo "=========================================================="
echo "Pilih opsi tunneling:"
echo "1) Cloudflare Tunnel (Gratis, Tanpa Akun, Rekomendasi)"
echo "2) Ngrok (Memerlukan auth token ngrok)"
echo "3) Keluar"
read -p "Pilihan [1/2/3]: " PILIHAN

case $PILIHAN in
  1)
    if ! command -v cloudflared &> /dev/null; then
      echo "[INFO] cloudflared belum terinstall di Jetson. Mengunduh binary ARM64..."
      wget -q https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-linux-arm64 -O cloudflared
      chmod +x cloudflared
      sudo mv cloudflared /usr/local/bin/
    fi
    echo "[INFO] Menjalankan Cloudflare Tunnel untuk port $PORT..."
    echo "[INFO] Salin URL https://...trycloudflare.com dan tempelkan ke Web Admin / Mobile App!"
    cloudflared tunnel --url http://localhost:$PORT
    ;;
  2)
    if ! command -v ngrok &> /dev/null; then
      echo "[ERROR] ngrok belum terpasang. Unduh ngrok ARM64 untuk Jetson dari https://ngrok.com/download"
      exit 1
    fi
    echo "[INFO] Menjalankan Ngrok pada port $PORT..."
    ngrok http $PORT
    ;;
  *)
    echo "Keluar."
    exit 0
    ;;
esac
