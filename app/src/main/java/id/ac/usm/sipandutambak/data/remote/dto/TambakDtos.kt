package id.ac.usm.sipandutambak.data.remote.dto

import com.google.gson.annotations.SerializedName

// --- GET /devices & GET /devices/{id} ---
data class DeviceDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("pond_id") val pondId: String,
    @SerializedName("pond_name") val pondName: String,
    @SerializedName("status") val status: String,
    @SerializedName("last_seen") val lastSeen: String,
    @SerializedName("parameters") val parameters: List<SensorParameterDto>? = null
)

data class SensorParameterDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("value") val value: Double,
    @SerializedName("unit") val unit: String,
    @SerializedName("status") val status: String,
    @SerializedName("sparkline") val sparkline: List<Double>? = null
)

// --- GET /ponds ---
data class PondDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("clarity") val clarity: String,
    @SerializedName("device_count") val deviceCount: Int
)

// --- GET /alerts?active=true ---
data class AlertDto(
    @SerializedName("id") val id: String,
    @SerializedName("device_id") val deviceId: String,
    @SerializedName("pond_name") val pondName: String,
    @SerializedName("message") val message: String,
    @SerializedName("severity") val severity: String,
    @SerializedName("timestamp") val timestamp: String,
    @SerializedName("snapshot") val snapshot: Map<String, Double>? = null
)

// --- GET /devices/{id}/history ---
data class HistoryDto(
    @SerializedName("device_id") val deviceId: String,
    @SerializedName("parameter") val parameter: String,
    @SerializedName("unit") val unit: String,
    @SerializedName("threshold_low") val thresholdLow: Double?,
    @SerializedName("threshold_high") val thresholdHigh: Double?,
    @SerializedName("points") val points: List<HistoryPointDto>
)

data class HistoryPointDto(
    @SerializedName("timestamp") val timestamp: String,
    @SerializedName("value") val value: Double
)

// --- GET /devices/{id}/readings ---
data class ReadingDto(
    @SerializedName("id") val id: String,
    @SerializedName("timestamp") val timestamp: String,
    @SerializedName("do") val doValue: Double?,
    @SerializedName("ph") val phValue: Double?,
    @SerializedName("tds") val tdsValue: Double?,
    @SerializedName("temp") val tempValue: Double?,
    @SerializedName("nh4") val nh4Value: Double?,
    @SerializedName("no3") val no3Value: Double?
)

// --- GET & PUT /devices/{id}/calibration ---
data class CalibrationDto(
    @SerializedName("device_id") val deviceId: String,
    @SerializedName("param") val param: String,
    @SerializedName("slope") val slope: Double,
    @SerializedName("offset") val offset: Double
)

data class UpdateCalibrationRequest(
    @SerializedName("param") val param: String,
    @SerializedName("slope") val slope: Double,
    @SerializedName("offset") val offset: Double
)

// --- PUT /devices/{id}/pond ---
data class UpdatePondRequest(
    @SerializedName("pondId") val pondId: String
)