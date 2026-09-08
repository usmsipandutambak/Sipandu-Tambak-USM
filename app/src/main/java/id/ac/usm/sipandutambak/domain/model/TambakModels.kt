package id.ac.usm.sipandutambak.domain.model

enum class DeviceStatus { NORMAL, WASPADA, BAHAYA, OFFLINE }
enum class WaterClarity { JERNIH, KERUH }

data class SensorParameter(
    val id: String,          // contoh: "do", "ph", "tds", "temp", "nh4", "no3"
    val name: String,        // "DO", "pH", "Suhu"
    val value: Double,
    val unit: String,        // "mg/L", "°C", "ppm"
    val status: DeviceStatus,
    val sparklineData: List<Double> = emptyList()
)

data class Device(
    val id: String,
    val name: String,
    val pondId: String,
    val pondName: String,
    val status: DeviceStatus,
    val lastSeen: String,
    val parameters: List<SensorParameter> = emptyList()
)

data class Alert(
    val id: String,
    val deviceId: String,
    val pondName: String,
    val message: String,
    val severity: DeviceStatus,
    val timestamp: String,
    val snapshot: Map<String, Double> = emptyMap()
)

data class PondSummary(
    val totalPonds: Int,
    val clearPonds: Int,
    val turbidPonds: Int
)