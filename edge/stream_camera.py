import argparse
import collections
import json
import logging
import platform
import signal
import socket
import socketserver
import sys
import threading
import time
from http.server import BaseHTTPRequestHandler, HTTPServer
from urllib.parse import urlparse

try:
    import cv2
    import numpy as np
except ImportError as exc:
    sys.stderr.write(
        "[ERROR] OpenCV/NumPy belum terpasang di Jetson.\n"
        "        Jalankan : sudo apt-get install python3-opencv\n"
        "        (biasanya sudah bawaan JetPack)\n"
        "        Detail   : %s\n" % exc
    )
    sys.exit(1)

LOG = logging.getLogger("sipandu.cam")


class FrameStore(object):

    def __init__(self):
        self._cond = threading.Condition()
        self._jpeg = None
        self._seq = 0
        self._ts = 0.0
        self._width = 0
        self._height = 0

    def update(self, jpeg, width, height):
        with self._cond:
            self._jpeg = jpeg
            self._width = width
            self._height = height
            self._seq += 1
            self._ts = time.time()
            self._cond.notify_all()

    def snapshot(self):
        with self._cond:
            return self._jpeg, self._seq, self._ts, self._width, self._height

    def wait_next(self, last_seq, timeout=5.0):
        with self._cond:
            if self._seq <= last_seq:
                self._cond.wait(timeout)
            return self._jpeg, self._seq, self._ts, self._width, self._height


class StreamStats(object):

    def __init__(self):
        self._lock = threading.Lock()
        self._times = collections.deque(maxlen=90)
        self.started = time.time()
        self.frames = 0
        self.clients = 0
        self.width = 0
        self.height = 0
        self.camera_ok = False
        self.last_error = None

    def tick(self, width, height):
        with self._lock:
            self.frames += 1
            self.width = width
            self.height = height
            self._times.append(time.monotonic())

    def fps(self):
        with self._lock:
            if len(self._times) < 2:
                return 0.0
            span = self._times[-1] - self._times[0]
            if span <= 0:
                return 0.0
            return round((len(self._times) - 1) / span, 1)

    def client_add(self):
        with self._lock:
            self.clients += 1

    def client_remove(self):
        with self._lock:
            self.clients = max(0, self.clients - 1)

    def set_camera(self, ok, error=None):
        with self._lock:
            self.camera_ok = ok
            if error is not None:
                self.last_error = error

def csi_pipeline(args):
    return (
        "nvarguscamerasrc sensor-id={sid} ! "
        "video/x-raw(memory:NV12),width={w},height={h},framerate={fps}/1 ! "
        "nvvidconv ! video/x-raw,format=BGRx ! "
        "videoconvert ! video/x-raw,format=BGR ! "
        "appsink drop=1 max-buffers=1 sync=false"
    ).format(sid=args.sensor_id, w=args.width, h=args.height, fps=args.fps)


def usb_gstreamer_pipeline(args):
    device = args.camera
    if str(device).isdigit():
        device = "/dev/video" + str(device)
    return (
        "v4l2src device={dev} ! "
        "video/x-raw,width={w},height={h},framerate={fps}/1 ! "
        "videoconvert ! appsink drop=1 max-buffers=1 sync=false"
    ).format(dev=device, w=args.width, h=args.height, fps=args.fps)


def open_camera(args):
    cap = None
    if args.camera_type == "csi":
        pipeline = csi_pipeline(args)
        LOG.info("Buka kamera CSI (GStreamer): %s", pipeline)
        cap = cv2.VideoCapture(pipeline, cv2.CAP_GSTREAMER)
    elif args.gstreamer_usb:
        pipeline = usb_gstreamer_pipeline(args)
        LOG.info("Buka kamera USB (GStreamer): %s", pipeline)
        cap = cv2.VideoCapture(pipeline, cv2.CAP_GSTREAMER)
    else:
        src = args.camera
        if str(src).startswith("/dev/video"):
            src = int(str(src).replace("/dev/video", ""))
        elif str(src).isdigit():
            src = int(src)
        LOG.info("Buka kamera USB (V4L2): %s", src)
        cap = cv2.VideoCapture(src, cv2.CAP_V4L2)
        if cap is not None and cap.isOpened():
            cap.set(cv2.CAP_PROP_FOURCC, cv2.VideoWriter_fourcc(*"MJPG"))
            cap.set(cv2.CAP_PROP_FRAME_WIDTH, args.width)
            cap.set(cv2.CAP_PROP_FRAME_HEIGHT, args.height)
            cap.set(cv2.CAP_PROP_FPS, args.fps)
            cap.set(cv2.CAP_PROP_BUFFERSIZE, 1)

    if cap is None or not cap.isOpened():
        if cap is not None:
            cap.release()
        LOG.error(
            "Kamera tidak bisa dibuka (%s). Cek: ls /dev/video*, "
            "jenis kamera, dan resolusi yang didukung sensor.",
            args.camera,
        )
        return None
    return cap


def _post_process(frame, args):
    if args.flip in ("h", "hv"):
        frame = cv2.flip(frame, 1)
    if args.flip in ("v", "hv"):
        frame = cv2.flip(frame, 0)
    return frame


def capture_loop(args, store, stats, stop_event):
    """Loop utama: baca frame -> encode JPEG -> publish ke FrameStore."""
    LOG.info("Capture thread dimulai (kamera %s)...", args.camera_type)
    while not stop_event.is_set():
        cap = None
        try:
            cap = open_camera(args)
            if cap is None:
                stats.set_camera(False, "kamera tidak bisa dibuka")
                stop_event.wait(2.0)
                continue
            stats.set_camera(True)
            LOG.info(
                "Kamera siap — target %dx%d @ %d FPS, JPEG quality %d",
                args.width, args.height, args.fps, args.quality,
            )
            while not stop_event.is_set():
                ok, frame = cap.read()
                if not ok or frame is None:
                    LOG.warning("Gagal membaca frame — membuka ulang kamera...")
                    stats.set_camera(False, "gagal membaca frame")
                    break
                frame = _post_process(frame, args)
                h, w = frame.shape[:2]
                ok_enc, buf = cv2.imencode(
                    ".jpg", frame, [cv2.IMWRITE_JPEG_QUALITY, args.quality]
                )
                if not ok_enc:
                    continue
                store.update(buf.tobytes(), w, h)
                stats.tick(w, h)
        except Exception as exc:  # jangan mati gara-gara error kamera
            LOG.error("Capture error: %s", exc)
            stats.set_camera(False, str(exc))
            stop_event.wait(2.0)
        finally:
            if cap is not None:
                try:
                    cap.release()
                except Exception:
                    pass
        if not stop_event.is_set():
            stop_event.wait(1.0)
    LOG.info("Capture thread berhenti.")


def test_pattern_loop(args, store, stats, stop_event):
    LOG.info("Mode --test-pattern aktif (tanpa kamera fisik).")
    stats.set_camera(True)
    w, h = args.width, args.height
    period = 1.0 / max(1, args.fps)
    seq = 0
    while not stop_event.is_set():
        t0 = time.monotonic()
        frame = np.zeros((h, w, 3), dtype=np.uint8)
        cv2.rectangle(frame, (0, 0), (w, h), (15, 23, 42), -1)
        for x in range(0, w, 80):
            cv2.line(frame, (x, 0), (x, h), (30, 41, 59), 1)
        bx = int((w - 140) * (0.5 + 0.5 * np.sin(seq / 15.0)))
        by = int((h - 140) * (0.5 + 0.5 * np.cos(seq / 23.0)))
        cv2.rectangle(frame, (bx, by), (bx + 120, by + 120), (96, 165, 250), 3)
        cv2.putText(
            frame, "SIPANDU CAM - TEST PATTERN", (24, 46),
            cv2.FONT_HERSHEY_SIMPLEX, 1.0, (226, 232, 240), 2, cv2.LINE_AA,
        )
        cv2.putText(
            frame, time.strftime("%Y-%m-%d %H:%M:%S"), (24, h - 32),
            cv2.FONT_HERSHEY_SIMPLEX, 0.9, (34, 197, 94), 2, cv2.LINE_AA,
        )
        cv2.putText(
            frame, "FRAME %d" % seq, (w - 260, 46),
            cv2.FONT_HERSHEY_SIMPLEX, 0.9, (148, 163, 184), 2, cv2.LINE_AA,
        )
        ok_enc, buf = cv2.imencode(
            ".jpg", frame, [cv2.IMWRITE_JPEG_QUALITY, args.quality]
        )
        if ok_enc:
            store.update(buf.tobytes(), w, h)
            stats.tick(w, h)
        seq += 1
        spent = time.monotonic() - t0
        stop_event.wait(max(0.0, period - spent))
    LOG.info("Test pattern berhenti.")



INDEX_HTML = """<!DOCTYPE html>
<html lang="id"><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>SIPANDU CAM - Jetson Nano</title>
<style>
 body{margin:0;background:#0F172A;color:#CBD5E1;font-family:monospace}
 header{padding:12px 16px;background:#1E293B;font-size:14px}
 header b{color:#60A5FA}
 img{display:block;width:100%;background:#000}
 a{color:#60A5FA}
</style></head><body>
<header><b>SIPANDU CAM</b> - Jetson Nano (headless)
 &middot; <a href="/stream">/stream</a>
 &middot; <a href="/snapshot.jpg">/snapshot.jpg</a>
 &middot; <a href="/api/status">/api/status</a></header>
<img src="/stream" alt="live stream">
</body></html>
"""


class CamHTTPServer(socketserver.ThreadingMixIn, HTTPServer):
    """HTTP server multi-thread — kompatibel Python 3.6
    (ThreadingHTTPServer baru ada di Python 3.7)."""
    daemon_threads = True
    allow_reuse_address = True


class CamHandler(BaseHTTPRequestHandler):
    server_version = "SipanduCam/1.0"

    # --- util -------------------------------------------------------------
    def _cors(self):
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Access-Control-Allow-Methods", "GET, OPTIONS")
        self.send_header("Access-Control-Allow-Headers", "Content-Type")

    def _send(self, code, ctype, body, cache=False):
        self.send_response(code)
        self.send_header("Content-Type", ctype)
        self.send_header("Content-Length", str(len(body)))
        if not cache:
            self.send_header("Cache-Control", "no-cache, no-store, must-revalidate")
        self._cors()
        self.end_headers()
        self.wfile.write(body)

    def do_OPTIONS(self):  # preflight CORS
        self.send_response(204)
        self._cors()
        self.end_headers()

    def do_GET(self):
        path = urlparse(self.path).path
        if path == "/":
            self._send(200, "text/html; charset=utf-8", INDEX_HTML.encode("utf-8"))
        elif path == "/stream":
            self._stream()
        elif path == "/snapshot.jpg":
            jpeg, _seq, _ts, _w, _h = self.server.store.snapshot()
            if not jpeg:
                self._send(503, "text/plain", b"no frame yet")
            else:
                self._send(200, "image/jpeg", jpeg, cache=True)
        elif path == "/api/status":
            body = json.dumps(self._status()).encode("utf-8")
            self._send(200, "application/json; charset=utf-8", body)
        else:
            self._send(404, "text/plain", b"404 Not Found")

    def _status(self):
        st = self.server.stats
        args = self.server.args
        return {
            "device": "jetson-nano",
            "hostname": socket.gethostname(),
            "uptime_s": round(time.time() - st.started, 1),
            "python": platform.python_version(),
            "opencv": getattr(cv2, "__version__", "n/a"),
            "camera": {
                "type": "test-pattern" if args.test_pattern else args.camera_type,
                "source": "test-pattern" if args.test_pattern else str(args.camera),
                "width": args.width,
                "height": args.height,
                "fps_target": args.fps,
            },
            "stream": {
                "width": st.width,
                "height": st.height,
                "fps": st.fps(),
                "jpeg_quality": args.quality,
                "frames": st.frames,
            },
            "clients": st.clients,
            "camera_ok": st.camera_ok,
            "last_error": st.last_error,
        }

    def _stream(self):
        store = self.server.store
        stats = self.server.stats
        stop_event = self.server.stop_event
        try:
            self.send_response(200)
            self.send_header(
                "Content-Type", "multipart/x-mixed-replace; boundary=frame"
            )
            self.send_header("Cache-Control", "no-cache, no-store, must-revalidate")
            self.send_header("Pragma", "no-cache")
            self._cors()
            self.end_headers()
            self.close_connection = True
        except OSError:
            return  # klien sudah putus

        stats.client_add()
        LOG.info("Klien stream terhubung (%d aktif)", stats.clients)
        last = 0
        try:
            while not stop_event.is_set():
                jpeg, seq, _ts, _w, _h = store.wait_next(last, timeout=5.0)
                if jpeg is None or seq <= last:
                    continue  # belum ada frame baru / kamera belum siap
                last = seq
                header = (
                    b"--frame\r\n"
                    b"Content-Type: image/jpeg\r\n"
                    b"Content-Length: %d\r\n\r\n" % len(jpeg)
                )
                self.wfile.write(header)
                self.wfile.write(jpeg)
                self.wfile.write(b"\r\n")
                self.wfile.flush()
        except (BrokenPipeError, ConnectionResetError, OSError):
            pass  # klien menutup tab — hal yang normal
        finally:
            stats.client_remove()
            LOG.info("Klien stream terputus (%d aktif)", stats.clients)

    def log_message(self, fmt, *args):
        LOG.debug("%s - %s", self.address_string(), fmt % args)



# ---------------------------------------------------------------------------
# CLI + main
# ---------------------------------------------------------------------------
def _lan_ip():
    """IP LAN Jetson (best effort, tanpa koneksi luar)."""
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        try:
            s.connect(("10.255.255.255", 1))  # UDP connect = cuma cari route
            return s.getsockname()[0]
        finally:
            s.close()
    except Exception:
        return "127.0.0.1"


def parse_args(argv=None):
    p = argparse.ArgumentParser(
        description="Server streaming kamera MJPEG untuk Jetson Nano (headless)."
    )
    p.add_argument("--host", default="0.0.0.0",
                   help="Bind address (default: 0.0.0.0)")
    p.add_argument("--port", type=int, default=8080,
                   help="Port HTTP (default: 8080)")
    p.add_argument("--camera", default="0",
                   help="Index/perangkat kamera USB, mis. 0 atau /dev/video0 "
                        "(default: 0)")
    p.add_argument("--camera-type", choices=["usb", "csi"], default="usb",
                   help="Jenis kamera: usb (V4L2) atau csi (nvarguscamerasrc)")
    p.add_argument("--sensor-id", type=int, default=0,
                   help="Sensor ID untuk kamera CSI (default: 0)")
    p.add_argument("--gstreamer-usb", action="store_true",
                   help="Pakai pipeline GStreamer v4l2src untuk kamera USB")
    p.add_argument("--width", type=int, default=1280,
                   help="Lebar frame (default: 1280)")
    p.add_argument("--height", type=int, default=720,
                   help="Tinggi frame (default: 720)")
    p.add_argument("--fps", type=int, default=30,
                   help="Target FPS (default: 30)")
    p.add_argument("--quality", type=int, default=80,
                   help="Kualitas JPEG 1-100 (default: 80)")
    p.add_argument("--flip", choices=["none", "h", "v", "hv"], default="none",
                   help="Flip frame: h (horizontal), v (vertical), hv (keduanya)")
    p.add_argument("--test-pattern", action="store_true",
                   help="Stream frame sintetis tanpa kamera (diagnostic)")
    p.add_argument("--log-level", default="INFO",
                   choices=["DEBUG", "INFO", "WARNING", "ERROR"],
                   help="Level log (default: INFO)")
    return p.parse_args(argv)


def main(argv=None):
    args = parse_args(argv)
    logging.basicConfig(
        level=getattr(logging, args.log_level),
        format="[%(asctime)s] %(levelname)s %(message)s",
        datefmt="%H:%M:%S",
    )

    stop_event = threading.Event()
    store = FrameStore()
    stats = StreamStats()

    worker = test_pattern_loop if args.test_pattern else capture_loop
    t = threading.Thread(
        target=worker, args=(args, store, stats, stop_event),
        name="capture", daemon=True,
    )

    try:
        server = CamHTTPServer((args.host, args.port), CamHandler)
    except OSError as exc:
        LOG.error("Gagal bind %s:%d — %s", args.host, args.port, exc)
        sys.exit(1)
    server.store = store
    server.stats = stats
    server.args = args
    server.stop_event = stop_event

    def _shutdown(signum, _frame):
        LOG.info("Signal %d diterima — menghentikan server...", signum)
        stop_event.set()
        raise SystemExit(0)

    signal.signal(signal.SIGINT, _shutdown)
    signal.signal(signal.SIGTERM, _shutdown)

    ip = _lan_ip()
    LOG.info("SIPANDU CAM server siap:")
    LOG.info("  Test halaman : http://%s:%d/            (buka di browser laptop)", ip, args.port)
    LOG.info("  Stream MJPEG  : http://%s:%d/stream", ip, args.port)
    LOG.info("  Status JSON   : http://%s:%d/api/status", ip, args.port)
    LOG.info("  Tekan Ctrl+C untuk berhenti.")

    t.start()
    try:
        server.serve_forever(poll_interval=0.5)
    except (KeyboardInterrupt, SystemExit):
        pass
    except OSError as exc:
        LOG.error("Server error: %s", exc)
        sys.exit(1)
    finally:
        stop_event.set()
        try:
            server.server_close()
        except Exception:
            pass
        t.join(3.0)
        LOG.info("Server dihentikan. Sampai jumpa.")


if __name__ == "__main__":
    main()

