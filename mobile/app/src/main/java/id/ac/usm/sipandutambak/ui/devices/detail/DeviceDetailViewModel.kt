package id.ac.usm.sipandutambak.ui.devices.detail

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import id.ac.usm.sipandutambak.data.api.SupabaseApi
import id.ac.usm.sipandutambak.util.CsvExportHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private fun org.json.JSONObject.optNullableDouble(key: String): Double? {
    if (!has(key) || isNull(key)) return null
    val v = optDouble(key, Double.NaN)
    return if (v.isNaN()) null else v
}

data class SensorLogRow(
    val timeLabel: String,
    val ph: Double?,
    val tds: Double?,
    val temperature: Double? = null,
    val turbidity: Double? = null,
    val cod: String = "-",
    val nh4: String = "-",
    val no3: String = "-",
    val doLevel: Double? = null,
    val status: String = "NORMAL",
    val fullTime: String = ""
)

enum class CsvFormat(val label: String, val decimalComma: Boolean) {
    EXCEL_ID("Excel Indonesia (desimal koma)", true),
    STANDAR("Standar (desimal titik)", false)
}

data class SensorParamConfig(
    val slope: String = "-6,78",
    val offset: String = "0,5"
)

data class TrendStats(
    val min: Double = 6.8,
    val avg: Double = 7.2,
    val max: Double = 7.6
)

enum class TrendTimeRange(val label: String) {
    HOURS_24("24 Jam"),
    DAYS_7("7 Hari"),
    DAYS_30("30 Hari")
}

data class DeviceDetailUiState(
    val deviceId: String = "",
    val deviceName: String = "Sipandu A",
    val pondName: String = "Kolam 1",
    val commodity: String = "LELE",
    val status: String = "NORMAL",
    val currentDateFormatted: String = "",
    val currentPh: Double? = null,
    val currentTds: Double? = null,
    val currentTemp: Double? = null,
    val currentTurbidity: Double? = null,
    val currentDo: Double? = null,
    val currentCod: String = "-",
    val currentNh4: String = "-",
    val currentNo3: String = "-",
    val hasData: Boolean = false,
    val logs: List<SensorLogRow> = emptyList(),
    val trendPoints: List<Double> = emptyList(),
    val stats: TrendStats = TrendStats(),
    val selectedRange: TrendTimeRange = TrendTimeRange.HOURS_24,
    val tdsConfig: SensorParamConfig = SensorParamConfig(slope = "", offset = ""),
    val phConfig: SensorParamConfig = SensorParamConfig(slope = "-6,78", offset = "0,5"),
    val codConfig: SensorParamConfig = SensorParamConfig(slope = "-6,78", offset = "0,5"),
    val nh4Config: SensorParamConfig = SensorParamConfig(slope = "-6,78", offset = "0,5"),
    val no3Config: SensorParamConfig = SensorParamConfig(slope = "-6,78", offset = "0,5"),
    val doConfig: SensorParamConfig = SensorParamConfig(slope = "-6,78", offset = "0,5")
)

class DeviceDetailViewModel(
    val deviceId: String,
    private val repository: Any? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(DeviceDetailUiState(deviceId = deviceId))
    val uiState: StateFlow<DeviceDetailUiState> = _uiState.asStateFlow()

    private var pollingJob: Job? = null

    init {
        val dateStr = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID")).format(Date())
        _uiState.update { it.copy(currentDateFormatted = dateStr) }
        startRealtimePolling()
    }

    private fun startRealtimePolling() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch(Dispatchers.IO) {
            var tickCount = 0
            while (isActive) {
                try {
                    val liveObj = SupabaseApi.getLiveDeviceTelemetry(deviceId)
                    if (liveObj != null) {
                        _uiState.update {
                            it.copy(
                                hasData = true,
                                currentPh = liveObj.optNullableDouble("live_ph"),
                                currentTds = liveObj.optNullableDouble("live_tds"),
                                currentTemp = liveObj.optNullableDouble("live_temp"),
                                currentTurbidity = liveObj.optNullableDouble("live_turb"),
                                currentDo = liveObj.optNullableDouble("live_do"),
                                status = liveObj.optString("status", it.status)
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                hasData = false,
                                currentPh = null,
                                currentTds = null,
                                currentTemp = null,
                                currentTurbidity = null,
                                currentDo = null
                            )
                        }
                    }

                    if (tickCount % 15 == 0) {
                        loadTelemetryDataInternal()
                    }
                    tickCount++
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                delay(1000)
            }
        }
    }

    private suspend fun loadTelemetryDataInternal() {
        if (deviceId.isBlank()) return
        try {
            val jsonLogs = SupabaseApi.getSensorLogs(deviceId, limit = 25)
            val list = mutableListOf<SensorLogRow>()

            val inFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val outFormat = SimpleDateFormat("HH:mm", Locale("id", "ID"))
            val fullFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale("id", "ID"))

            for (i in 0 until jsonLogs.length()) {
                val obj = jsonLogs.getJSONObject(i)
                val rawDate: String = obj.optString("created_at", "")
                val parsedDate = try {
                    if (rawDate.length >= 19) inFormat.parse(rawDate.substring(0, 19)) else null
                } catch (e: Exception) {
                    null
                }
                val formattedTime = parsedDate?.let { outFormat.format(it) } ?: "-"
                val fullTime = parsedDate?.let { fullFormat.format(it) } ?: "-"

                list.add(
                    SensorLogRow(
                        timeLabel = formattedTime,
                        fullTime = fullTime,
                        ph = obj.optNullableDouble("ph_level"),
                        tds = obj.optNullableDouble("tds_ppm"),
                        temperature = obj.optNullableDouble("temperature_c"),
                        turbidity = obj.optNullableDouble("turbidity_ntu"),
                        doLevel = obj.optNullableDouble("do_level"),
                        status = obj.optString("status", "NORMAL")
                    )
                )
            }

            if (list.isNotEmpty()) {
                _uiState.update { current ->
                    current.copy(
                        logs = list,
                        trendPoints = list.mapNotNull { it.ph }.reversed()
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setDeviceInfo(name: String, pond: String, commodity: String) {
        _uiState.update { it.copy(deviceName = name, pondName = pond, commodity = commodity) }
    }

    fun exportCsvLog(context: Context, format: CsvFormat = CsvFormat.EXCEL_ID) {
        val currentState = _uiState.value

        fun num(d: Double?, digits: Int = 2): String {
            if (d == null) return "-"
            val s = String.format(Locale.US, "%." + digits + "f", d)
            return if (format.decimalComma) s.replace('.', ',') else s
        }

        val headers = listOf(
            "WAKTU", "PH", "TDS (PPM)", "TEMPERATURE (°C)", "TURBIDITY (NTU)",
            "COD", "NH4+", "NO3-", "DO (MG/L)", "STATUS"
        )
        val rows = currentState.logs.map { log ->
            listOf(
                log.fullTime.ifBlank { log.timeLabel },
                num(log.ph),
                num(log.tds, 0),
                num(log.temperature, 1),
                num(log.turbidity, 1),
                log.cod,
                log.nh4,
                log.no3,
                num(log.doLevel, 1),
                log.status
            )
        }

        val cleanPond = currentState.pondName.replace(" ", "_")
        CsvExportHelper.exportAndShareCsv(
            context = context,
            fileName = "SIPANDU_Log_" + cleanPond,
            headers = headers,
            rows = rows
        )
    }

    fun updateTdsConfig(slope: String, offset: String) {
        _uiState.update { it.copy(tdsConfig = SensorParamConfig(slope, offset)) }
    }

    fun updatePhConfig(slope: String, offset: String) {
        _uiState.update { it.copy(phConfig = SensorParamConfig(slope, offset)) }
    }

    fun updateCodConfig(slope: String, offset: String) {
        _uiState.update { it.copy(codConfig = SensorParamConfig(slope, offset)) }
    }

    fun updateNh4Config(slope: String, offset: String) {
        _uiState.update { it.copy(nh4Config = SensorParamConfig(slope, offset)) }
    }

    fun updateNo3Config(slope: String, offset: String) {
        _uiState.update { it.copy(no3Config = SensorParamConfig(slope, offset)) }
    }

    fun updateDoConfig(slope: String, offset: String) {
        _uiState.update { it.copy(doConfig = SensorParamConfig(slope, offset)) }
    }

    fun saveCalibrationToDevice(onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            val state = _uiState.value
            fun parse(s: String): Double = s.replace(',', '.').toDoubleOrNull() ?: 0.0

            val phSlope = parse(state.phConfig.slope)
            val phOffset = parse(state.phConfig.offset)
            val doSlope = parse(state.doConfig.slope)
            val doOffset = parse(state.doConfig.offset)
            val tdsSlope = parse(state.tdsConfig.slope)
            val tdsOffset = parse(state.tdsConfig.offset)

            val payload = org.json.JSONObject().apply {
                put("command", "calibrate")
                put("ph", org.json.JSONObject().put("slope", phSlope).put("offset", phOffset))
                put("do", org.json.JSONObject().put("slope", doSlope).put("offset", doOffset))
                put("tds", org.json.JSONObject().put("slope", tdsSlope).put("offset", tdsOffset))
            }

            val ok = if (deviceId.isNotBlank()) SupabaseApi.updateDeviceCalibration(deviceId, payload) else false
            withContext(Dispatchers.Main) {
                onComplete(ok)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        pollingJob?.cancel()
    }

    class Factory(
        private val deviceId: String,
        private val repository: Any? = null
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DeviceDetailViewModel(deviceId, repository) as T
        }
    }
}