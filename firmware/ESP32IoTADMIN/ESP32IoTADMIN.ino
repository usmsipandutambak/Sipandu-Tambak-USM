#include "esp_sleep.h"
#include <Adafruit_ADS1X15.h>
#include <Adafruit_GFX.h>
#include <Adafruit_SH110X.h>
#include <Arduino.h>
#include <ArduinoJson.h>
#include <DallasTemperature.h>
#include <INA226_WE.h>
#include <OneWire.h>
#include <Preferences.h>
#include <WebServer.h>
#include <WiFi.h>
#include <Wire.h>

const char *WIFI_SSID = "Sipandu Tambak";
const char *WIFI_PASSWORD = "1234567890";

const char *DEVICE_ID = "SIPANDU_A";
const int POND_NUMBER = 5; // ALAT A - KOLAM 5

#define MODE_CONTINUOUS 1
#define MODE_DUTY_CYCLE 2
#define OPERATION_MODE MODE_CONTINUOUS

#define PIN_MOSFET_JETSON 33
#define PIN_MOSFET_SENSOR 32
#define PIN_DS18B20 25
#define PIN_SDA 21
#define PIN_SCL 22

#define PIN_MOSFET PIN_MOSFET_JETSON

#define WARMUP_DELAY_MS 15000UL
#define CONTINUOUS_INTERVAL_MS 3000UL
#define DEEP_SLEEP_US 240000000ULL

#define PH_SAMPLE_COUNT 10
#define DO_SAMPLE_COUNT 5
#define TDS_SAMPLE_COUNT 5
#define TURBIDITY_SAMPLE_COUNT 5
#define SAMPLE_INTERVAL_MS 50

#define INA226_I2C_ADDR 0x44
#define ADS_A_ADDR 0x48
#define ADS_B_ADDR 0x49

#define ADS_PH_ADDR ADS_A_ADDR
#define ADS_DO_ADDR ADS_A_ADDR
#define ADS_TDS_ADDR ADS_B_ADDR
#define ADS_TURBIDITY_ADDR ADS_B_ADDR

// voltage divider R1=10kΩ, R2=20kΩ koreksi = (R1+R2)/R2 = 1.5
#define DIVIDER_CORRECTION_FACTOR 1.5f

#define SCREEN_WIDTH 128
#define SCREEN_HEIGHT 64
#define OLED_RESET -1

// GAIN_TWOTHIRDS → ±6.144V, LSB = 6.144/32768 = 0.0001875V
#define ADS_LSB_V 0.0001875f
OneWire oneWire(PIN_DS18B20);
DallasTemperature sensors(&oneWire);

Adafruit_ADS1115 adsA;
Adafruit_ADS1115 adsB;

#define adsPH adsA
#define adsDO adsA
#define adsTDS adsB
#define adsTurbidity adsB

INA226_WE ina226(INA226_I2C_ADDR);
Adafruit_SH1106G display =
    Adafruit_SH1106G(SCREEN_WIDTH, SCREEN_HEIGHT, &Wire, OLED_RESET);
Preferences prefs;

WebServer server(80);

float phLevel = 0.0f;
float phVoltage = 0.0f;
float doMgL = 0.0f;
float doVoltage = 0.0f;
float tdsPpm = 0.0f;
float turbidityNTU = 0.0f;
float temperatureC = 0.0f;
float batVoltage = 0.0f;

bool isDs18b20OK = false;
bool isAdsAOK = false;
bool isAdsBOK = false;
bool isIna226OK = false;

static bool sensorPowerState = false;
static bool jetsonPowerState = false;
static bool jetsonShutdownPending = false;
static unsigned long jetsonShutdownStartMs = 0;
const unsigned long JETSON_SHUTDOWN_GRACE_PERIOD_MS = 30000UL;

// ─── MQTT & SUPABASE SETTINGS (PAHO BROKER ECOSYSTEM) ─────────
const char *MQTT_BROKER =
    "192.168.100.41"; // Broker MQTT lokal (Jetson Nano). Ganti ke
                      // "broker.hivemq.com" jika pakai broker publik.
const int MQTT_PORT = 1883;
const char *MQTT_USER = "";
const char *MQTT_PASS = "";

String TOPIC_TELEMETRY;
String TOPIC_COMMAND;
String TOPIC_STATUS;

const char *SUPABASE_REST_URL =
    "https://tidmdjnjhqptszyubvue.supabase.co/rest/v1";
const char *SUPABASE_KEY =
    "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9."
    "eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InRpZG1kam5qaHFwdHN6eXVidnVlIiwicm9sZSI6Im"
    "Fub24iLCJpYXQiOjE3ODk1NDMwMzEsImV4cCI6MjEwNTExOTAzMX0."
    "G2VeWnkN5nexmaYKdA1jI9AjwM-sw1yPnF7uHlnkT20";

#define MQTT_PUBLISH_INTERVAL_MS 3000UL     // 3 detik real-time
#define SUPABASE_UPLOAD_INTERVAL_MS 60000UL // 60 detik (1 menit) riwayat log

// Prototipe fungsi dari MQTT_SUPABASE.ino & CONTROL.ino
void initMQTT();
void handleMQTT();
void publishTelemetryMQTT();
void uploadToSupabasePeriodic();
void checkJetsonShutdownTimer();
void publishDeviceStatus();
void publishJetsonShutdownNotice(int secondsRemaining);

static unsigned long lastSensorReadMs = 0;
static unsigned long _lastContinuousMs = 0UL;

#define isAdsPhOK isAdsAOK
#define isAdsDoOK isAdsAOK
#define isAdsTdsOK isAdsBOK
#define isAdsTurbidityOK isAdsBOK

const float minVolt = 11.0f;
const float maxVolt = 14.1f;
const float offsetINA = -0.50f;

float phSlope = -11.43f;
float phOffset = 52.00f;
float doSlope = 5.5f;
float doOffset = 0.0f;
float tdsSlope = 500.0f;
float tdsOffset = 0.0f;
float turbSlope = -1120.4f;
float turbOffset = 4895.87f;

void setup() {

  Serial.begin(115200);
  delay(300);
  Serial.println();
  Serial.flush();

  Wire.begin(PIN_SDA, PIN_SCL);
  Wire.setClock(50000);
  delay(800);

  pinMode(PIN_MOSFET_JETSON, OUTPUT);
  powerOnJetson(); // Aktifkan MOSFET Jetson secara default saat booting (12V ->
                   // buck converter 5V)
  pinMode(PIN_MOSFET_SENSOR, OUTPUT);
  digitalWrite(PIN_MOSFET_SENSOR, LOW);
  sensorPowerState = false;
  delay(800);

  loadCalibration();
  delay(500);

  if (OPERATION_MODE == MODE_DUTY_CYCLE) {
    runDutyCycle();
  } else {
    powerOnSensors();
    delay(250);

    initOLED();
    showOLEDI2CScan();
    delay(400);

    initSensors();
    delay(300);
    showOLEDStatus();
    delay(2000);

    showOLEDMessage("Connecting WiFi...");
    WiFi.setSleep(false);
    WiFi.setAutoReconnect(true);
    connectWiFi();
    bool wifiOK = (WiFi.status() == WL_CONNECTED);
    String ipStr = wifiOK ? WiFi.localIP().toString() : "";
    showOLEDWifi(wifiOK, ipStr.c_str());

    if (wifiOK) {
      initMQTT();
    }

    const int warmupSecs = WARMUP_DELAY_MS / 1000;
    Serial.print(F("Warm: "));
    for (int i = warmupSecs; i > 0; i--) {
      showOLEDWarmup(i);
      delay(1000);
      Serial.print(F("."));
    }
    Serial.println();

    // Baca seluruh sensor sekali setelah warm-up agar nilai awal tidak 0
    readAllSensors();
    markSensorReadTime();
    updateOLED();
  }
}

void loop() {
  if (OPERATION_MODE == MODE_CONTINUOUS) {
    static unsigned long lastWifiRetryMs = 0;
    if (WiFi.status() != WL_CONNECTED &&
        (millis() - lastWifiRetryMs >= 15000UL)) {
      lastWifiRetryMs = millis();
      Serial.println(F("[WiFi] Sinyal terputus - mencoba reconnect..."));
      WiFi.reconnect();
    }
    handleHTTPServer();
    handleSerialCommands();
    handleMQTT();
    publishTelemetryMQTT();
    uploadToSupabasePeriodic();
    checkJetsonShutdownTimer();
    runContinuous();
  }
}

void initSensors() {
  sensors.begin();
  delay(100); // Beri waktu stabilisasi bus OneWire

  // Periksa ketersediaan perangkat
  int devCount = sensors.getDeviceCount();
  if (devCount > 0) {
    isDs18b20OK = true;
    sensors.setResolution(12);
    Serial.printf("DS18B20 OK - Ditemukan %d sensor\n", devCount);
  } else {
    isDs18b20OK = false;
    Serial.println(F("ERROR DS18B20 - Perangkat tidak terdeteksi"));
  }
  delay(500);

  if (!adsA.begin(ADS_A_ADDR)) {
    isAdsAOK = false;
    Serial.println(F("ERROR ADS1115 A (0x48)"));
  } else {
    isAdsAOK = true;
    adsA.setGain(GAIN_TWOTHIRDS);
    Serial.println(F("ADS1115 A (0x48) OK - pH:A1, DO:A0"));
  }
  delay(500);

  if (!adsB.begin(ADS_B_ADDR)) {
    isAdsBOK = false;
    Serial.println(F("ERROR ADS1115 B (0x49)"));
  } else {
    isAdsBOK = true;
    adsB.setGain(GAIN_TWOTHIRDS);
    Serial.println(F("ADS1115 B (0x49) OK - TDS:A1, Turbidity:A0"));
  }
  delay(500);

  debugADSPhOLED();
  delay(2000);

  Wire.beginTransmission(INA226_I2C_ADDR);
  if (Wire.endTransmission() == 0) {
    isIna226OK = true;
    ina226.init();
    ina226.setAverage(INA226_AVERAGE_4);
    ina226.setConversionTime(INA226_CONV_TIME_1100);
    ina226.setMeasureMode(INA226_CONTINUOUS);
    Serial.println(F("INA226 OK - Voltage-only mode"));
  } else {
    isIna226OK = false;
    Serial.println(F("ERROR INA226"));
  }
  delay(500);
}