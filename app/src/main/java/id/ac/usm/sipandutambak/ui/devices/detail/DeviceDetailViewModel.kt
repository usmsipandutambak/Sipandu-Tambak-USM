package id.ac.usm.sipandutambak.ui.devices.detail

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import id.ac.usm.sipandutambak.data.api.SupabaseApi
import id.ac.usm.sipandutambak.util.CsvExportHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

data class SensorLogRow(
    val timeLabel: String,
    val ph: Double,
    val tds: Int,
    val cod: Int = 28,
    val nh4: Double = 0.15,
    val no3: Double = 3.2,
    val doLevel: Double = 6.8,
    val status: String = "NORMAL"
)

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
    DAYS_30("30 Hari"),
    HOUR_24("24 Jam"),
    DAY_7("7 Hari"),
    DAY_30("30 Hari")
}

data class DeviceDetailUiState(
    val deviceId: String = "",
    val deviceName: String = "Sipandu A",
    val pondName: String = "Kolam 1",
    val commodity: String = "LELE",
    val status: String = "STANDBY",
    val currentPh: Double = 0.0,
    val currentDo: Double = 0.0,
    val currentTds: Int = 0,
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

    init {
        loadTelemetryData()
    }

    fun loadTelemetryData() {
        if (deviceId.isBlank()) return

        viewModelScope.launch {
            try {
                val jsonLogs = SupabaseApi.getSensorLogs(deviceId, limit = 15)
                val list = mutableListOf<SensorLogRow>()

                val inFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                val outFormat = SimpleDateFormat("HH:mm", Locale("id", "ID"))

                for (i in 0 until jsonLogs.length()) {
                    val obj = jsonLogs.getJSONObject(i)
                    val rawDate = obj.optString("created_at", "")
                    val formattedTime = try {
                        val parsed = inFormat.parse(rawDate.take(19))
                        if (parsed != null) outFormat.format(parsed) else rawDate.takeLast(8).take(5)
                    } catch (e: Exception) {
                        "Log #${i + 1}"
                    }

                    list.add(
                        SensorLogRow(
                            timeLabel = formattedTime,
                            ph = obj.optDouble("ph_level", 0.0),
                            tds = obj.optDouble("tds_ppm", 0.0).toInt(),
                            status = obj.optString("status", "NORMAL")
                        )
                    )
                }

                if (list.isNotEmpty()) {
                    val latest = list.first()
                    _uiState.update { current ->
                        current.copy(
                            hasData = true,
                            currentPh = latest.ph,
                            currentTds = latest.tds,
                            currentDo = latest.doLevel,
                            status = latest.status,
                            logs = list,
                            trendPoints = list.map { it.ph }.reversed()
                        )
                    }
                } else {
                    _uiState.update { current ->
                        current.copy(hasData = false, logs = emptyList())
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun setDeviceInfo(name: String, pond: String, commodity: String) {
        _uiState.update {
            it.copy(
                deviceName = name,
                pondName = pond,
                commodity = commodity
            )
        }
    }

    // =========================================================================
    // FUNGSI EKSPOR CSV AKTUAL UNTUK DETAIL KOLAM
    // =========================================================================
    fun exportCsvLog(context: Context) {
        val currentState = _uiState.value
        val headers = listOf("WAKTU", "NAMA ALAT", "KOLAM", "KOMODITAS", "PH", "TDS (PPM)", "DO (MG/L)", "STATUS")
        val rows = currentState.logs.map { log ->
            listOf(
                log.timeLabel,
                currentState.deviceName,
                currentState.pondName,
                currentState.commodity,
                log.ph.toString(),
                log.tds.toString(),
                log.doLevel.toString(),
                log.status
            )
        }

        val cleanPondName = currentState.pondName.replace(" ", "_")
        CsvExportHelper.exportAndShareCsv(
            context = context,
            fileName = "SIPANDU_Log_${cleanPondName}",
            headers = headers,
            rows = rows
        )
    }

    fun updateTdsConfig(slope: String, offset: String) {
        _uiState.update { it.copy(tdsConfig = SensorParamConfig(slope, offset)) }
        recalculateSensors()
    }

    fun updatePhConfig(slope: String, offset: String) {
        _uiState.update { it.copy(phConfig = SensorParamConfig(slope, offset)) }
        recalculateSensors()
    }

    fun updateCodConfig(slope: String, offset: String) {
        _uiState.update { it.copy(codConfig = SensorParamConfig(slope, offset)) }
        recalculateSensors()
    }

    fun updateNh4Config(slope: String, offset: String) {
        _uiState.update { it.copy(nh4Config = SensorParamConfig(slope, offset)) }
        recalculateSensors()
    }

    fun updateNo3Config(slope: String, offset: String) {
        _uiState.update { it.copy(no3Config = SensorParamConfig(slope, offset)) }
        recalculateSensors()
    }

    fun updateDoConfig(slope: String, offset: String) {
        _uiState.update { it.copy(doConfig = SensorParamConfig(slope, offset)) }
        recalculateSensors()
    }

    private fun recalculateSensors() {
        val s = _uiState.value
        fun parseNum(str: String): Double = str.replace(',', '.').toDoubleOrNull() ?: 0.0

        val phSlope = parseNum(s.phConfig.slope)
        val phOffset = parseNum(s.phConfig.offset)
        val calcPh = if (phSlope != 0.0) Math.abs(phSlope * 1.0 + phOffset) else s.currentPh

        val doSlope = parseNum(s.doConfig.slope)
        val doOffset = parseNum(s.doConfig.offset)
        val calcDo = if (doSlope != 0.0) Math.abs(doSlope * 1.0 + doOffset) else s.currentDo

        val tdsSlope = parseNum(s.tdsConfig.slope)
        val tdsOffset = parseNum(s.tdsConfig.offset)
        val calcTds = if (tdsSlope != 0.0) Math.abs((tdsSlope * 2.0 + tdsOffset).toInt()) else s.currentTds

        val newStatus = when {
            calcPh < 6.0 || calcPh > 9.0 || calcDo < 3.0 || calcTds > 1000 -> "BAHAYA"
            (calcPh in 6.0..6.4) || (calcPh in 8.6..9.0) || (calcDo in 3.0..4.9) || (calcTds in 500..1000) -> "WASPADA"
            else -> "NORMAL"
        }

        _uiState.update {
            it.copy(
                currentPh = Math.round(calcPh * 10.0) / 10.0,
                currentDo = Math.round(calcDo * 10.0) / 10.0,
                currentTds = calcTds,
                status = newStatus
            )
        }
    }

    fun getPageTitle(): String = if (deviceId.isNotBlank()) "Detail - $deviceId" else "Detail Alat"

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