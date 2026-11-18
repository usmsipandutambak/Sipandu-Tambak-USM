# SIPANDU-TAMBAK — Aplikasi Android (`mobile/`)

Aplikasi Android native berbasis **Kotlin** dan **Jetpack Compose** (`id.ac.usm.sipandutambak`) untuk pemantauan kualitas air tambak *real-time*, *live stream* kamera Jetson Nano, analisis tren grafik 24 jam, kalibrasi sensor tersinkronisasi Supabase, dan peringatan ambang batas kualitas air.

---

## Spesifikasi & Teknologi

- **Package / Namespace**: `id.ac.usm.sipandutambak`
- **SDK**: `minSdk = 26`, `compileSdk = 35`, `targetSdk = 35` (JVM Target 17)
- **UI & Arsitektur**: Jetpack Compose (Material 3), Navigation Compose, MVVM (`StateFlow`)
- **Jaringan & Background Task**: Retrofit 2, OkHttp, Kotlin Coroutines, WorkManager (`SensorAlertWorker`)
- **Izin Jaringan (`AndroidManifest.xml`)**: `android.permission.INTERNET` & `android:usesCleartextTraffic="true"` (diperlukan untuk akses stream HTTP LAN Jetson Nano)

---

## Fitur Utama

1. **Dashboard Real-Time (`ui/dashboard/`)**:
   - Mengambil pembacaan terbaru dari tabel `sensor_logs` Supabase (mendukung field `do_level`, `dissolved_oxygen`, `do`, `ph_level`, `tds_ppm`, `turbidity_ntu`, `temperature_c`, dan `battery_voltage`).
   - Notifikasi peringatan otomatis melalui `SensorAlertWorker` dan aturan ambang batas pada `WaterQualityRules.kt`.
2. **Monitoring Kamera Jetson Nano (`CameraMonitoringScreen.kt`)**:
   - Menampilkan *live MJPEG stream* dari Jetson Nano di dalam komponen `WebView` (`AndroidView`) dengan `Modifier.fillMaxSize()`, `loadWithOverviewMode = true`, `useWideViewPort = true`, dan `MIXED_CONTENT_ALWAYS_ALLOW`.
   - Dialog **Setting Kamera** tersimpan di `SharedPreferences` meliputi:
     - **Alamat Server Stream** (default `http://192.168.100.41:5000/video_feed`) + tombol **Tes Koneksi** HTTP.
     - **Pilihan Resolusi** (`640×480` / `1280×720`) & **Target FPS** (`5 FPS` / `10 FPS` / `15 FPS` / `30 FPS`).
     - **Mode Sesi Kamera** (Tanpa Batas / Durasi Timer).
   - Header bersih minimalis dengan navigasi kembali melalui tombol sistem Android atau *Bottom Navigation Bar*.
3. **Kalibrasi Sensor Tersinkronisasi (`CalibrationScreen.kt`, `DeviceDetailViewModel.kt`, `SupabaseApi.kt`)**:
   - Memuat konfigurasi kalibrasi terbaru langsung dari tabel `devices` Supabase saat inisialisasi (`loadCalibrationFromSupabase()`).
   - Mendukung kalibrasi **pH, DO, TDS, dan Turbidity** dengan default koefisien yang selaras dengan firmware ESP32, serta melakukan `PATCH` langsung ke kolom tabel `devices`.
4. **Riwayat Grafik 24 Jam & Ekspor CSV (`HistoryDetailScreen.kt`, `SensorTrendChart.kt`, `CsvExportHelper.kt`)**:
   - Visualisasi grafik tren kualitas air dan ekspor laporan telemetri ke format CSV.

---

## Cara Build & Instalasi APK

### Melalui Terminal (PowerShell / Bash)
Pastikan `ANDROID_HOME` (di `local.properties` atau environment) sudah terkonfigurasi dan perangkat Android terhubung via ADB:

```bash
cd mobile
.\gradlew installDebug
adb shell am start -S -n id.ac.usm.sipandutambak/.ui.MainActivity
```

### Melalui Android Studio
1. Buka direktori `mobile/` di **Android Studio**.
2. Tunggu proses **Gradle Sync** selesai.
3. Pilih perangkat fisik / emulator lalu tekan **Run (`Shift+F10`)**.
