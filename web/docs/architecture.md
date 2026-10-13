# Arsitektur Sistem SIPANDU TAMBAK

## Diagram Komunikasi

```
[Laptop/Smartphone]
     Browser
       |
       | HTTP (LAN)
       |
[Router Wi-Fi Lokal]
       |
       | Wi-Fi
       |
   [ESP32 DevKit]
   HTTP Server :80
       |
   ┌───┴───────────────┐
   │    Sensor I2C     │
   ├───────────────────┤
   │ ADS1115 A (0x48)  │ → pH (A1), DO (A0)
   │ ADS1115 B (0x49)  │ → TDS (A1), Turbidity (A0)
   │ INA226 (0x44)     │ → Tegangan Baterai
   │ OLED SH1106 (0x3C)│ → Display lokal
   ├───────────────────┤
   │ DS18B20 (GPIO 25) │ → Suhu Air
   │ MOSFET (GPIO 33)  │ → Daya Sensor
   │ MOSFET (GPIO 32)  │ → Daya Jetson (mendatang)
   └───────────────────┘
```

## Alur Data Real-Time
1. Browser melakukan HTTP GET ke `/api/sensors` setiap 3 detik
2. ESP32 mengembalikan JSON dengan semua nilai sensor terkini
3. Browser mengupdate tampilan kartu sensor tanpa reload halaman
4. Jika ESP32 tidak merespons dalam 5 detik, status berubah ke Offline

## Format JSON Respons Sensor
```json
{
  "device_id": "Sipandu_A",
  "pond_number": 1,
  "timestamp_ms": 12345678,
  "sensors": {
    "ph": 7.20,
    "do_mg_l": 6.80,
    "tds_ppm": 250.0,
    "turbidity_ntu": 12.3,
    "temperature_c": 28.50,
    "battery_voltage": 12.80
  },
  "voltages": {
    "ph_voltage": 2.1450,
    "do_voltage": 1.2364
  },
  "sensor_status": {
    "ds18b20": true,
    "ads_a": true,
    "ads_b": true,
    "ina226": true
  }
}
```
