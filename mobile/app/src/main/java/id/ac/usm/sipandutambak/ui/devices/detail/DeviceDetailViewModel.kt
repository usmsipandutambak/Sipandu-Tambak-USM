package id.ac.usm.sipandutambak.ui.devices.detail

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import id.ac.usm.sipandutambak.data.api.SupabaseApi
import id.ac.usm.sipandutambak.data.api.formatSensorMetric
import id.ac.usm.sipandutambak.data.api.optNullableDoubleAny
import id.ac.usm.sipandutambak.data.api.optNullableIntAny
import id.ac.usm.sipandutambak.data.api.optNullableStringAny
import id.ac.usm.sipandutambak.data.remote.mqtt.MqttTelemetryClient
import id.ac.usm.sipandutambak.ui.util.WaterQualityRules
import id.ac.usm.sipandutambak.util.CsvExportHelper
import id.ac.usm.sipandutambak.util.WibTimeUtil
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
import org.json.JSONObject
import java.util.Locale

data class SensorLogRow(
    val timeLabel: String,
    val ph: Double?,
    val tds: Double?,
    val temperature: Double? = null,
    val turbidity: Double? = null,
    val cod: String = "N/A",
    val nh4: String = "N/A",
    val no3: String = "N/A",
    val doLevel: Double? = null,
    val batteryVoltage: Double? = null,
    val batteryCurrentA: Double? = null,
    val pondNumber: Int? = null,
    val status: String = "NORMAL",
    val fullTime: String = "",
    val isLiveMqtt: Boolean = false
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
    val currentCod: String = "N/A",
    val currentNh4: String = "N/A",
    val currentNo3: String = "N/A",
    val batteryVoltage: Double? = null,
    val batteryCurrentA: Double? = null,
    val networkError: String? = null,
    val isMqttConnected: Boolean = false,
    val hasData: Boolean = false,
    val logs: List<SensorLogRow> = emptyList(),
    val trendPoints: List<Double> = emptyList(),
    val stats: TrendStats = TrendStats(),
    val selectedRange: TrendTimeRange = TrendTimeRange.HOURS_24,
    val tdsConfig: SensorParamConfig = SensorParamConfig(slope = "500.0", offset = "0.0"),
    val phConfig: SensorParamConfig = SensorParamConfig(slope = "-5.70", offset = "21.34"),
    val turbConfig: SensorParamConfig = SensorParamConfig(slope = "-1120.4", offset = "4895.87"),
    val codConfig: SensorParamConfig = SensorParamConfig(slope = "1.0", offset = "0.0"),
    val nh4Config: SensorParamConfig = SensorParamConfig(slope = "1.0", offset = "0.0"),
    val no3Config: SensorParamConfig = SensorParamConfig(slope = "1.0", offset = "0.0"),
    val doConfig: SensorParamConfig = SensorParamConfig(slope = "5.5", offset = "0.0")
)

class DeviceDetailViewModel(
    val deviceId: String,
    private val repository: Any? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(DeviceDetailUiState(deviceId = deviceId))
    val uiState: StateFlow<DeviceDetailUiState> = _uiState.asStateFlow()

    private var pollingJob: Job? = null
    private val mqttClient = MqttTelemetryClient(
        brokerHost = MqttTelemetryClient.DEFAULT_BROKER_HOST,
        brokerPort = MqttTelemetryClient.DEFAULT_BROKER_PORT,
        topicFilter = MqttTelemetryClient.DEFAULT_TELEMETRY_TOPIC
    )

    init {
        val dateStr = WibTimeUtil.formatNowWib("dd MMM yyyy")
        _uiState.update { it.copy(currentDateFormatted = dateStr) }
        loadCalibrationFromSupabase()
        startRealtimePolling()
        startMqttTelemetry()
    }

    private fun startMqttTelemetry() {
        mqttClient.start(
            scope = viewModelScope,
            onConnectionChanged = { connected ->
                _uiState.update { it.copy(isMqttConnected = connected) }
            },
            onTelemetryMessage = { _, payload ->
                handleMqttTelemetryPayload(payload)
            }
        )
    }

    private fun handleMqttTelemetryPayload(payload: String) {
        try {
            val root = JSONObject(payload)
            val msgDevId = root.optNullableStringAny("device_id")
            if (!msgDevId.isNullOrBlank() && deviceId.isNotBlank() && !msgDevId.equals(deviceId, ignoreCase = true)) {
                return
            }

            val sens = root.optJSONObject("sensors") ?: root
            val ph = sens.optNullableDoubleAny("ph", "ph_level", "live_ph")
            val tds = sens.optNullableDoubleAny("tds_ppm", "tds", "live_tds")
            val temp = sens.optNullableDoubleAny("temperature_c", "temperature", "temp", "live_temp")
            val turb = sens.optNullableDoubleAny("turbidity_ntu", "turbidity", "turb", "live_turb")
            val doVal = sens.optNullableDoubleAny("do_mg_l", "do_level", "dissolved_oxygen", "do", "live_do")
            val batV = sens.optNullableDoubleAny("battery_voltage", "bat_v")
            val batI = sens.optNullableDoubleAny("battery_current_a", "bat_i")
            val pondNum = root.optNullableIntAny("pond_number")
            val evalStatus = WaterQualityRules.evaluateStatusLabel(
                ph = ph,
                tds = tds,
                temp = temp,
                doLevel = doVal,
                turbidity = turb,
                fallback = root.optNullableStringAny("status") ?: "NORMAL"
            )

            val shortTime = WibTimeUtil.formatNowWib("HH:mm:ss")
            val fullTime = WibTimeUtil.formatNowWib("yyyy-MM-dd HH:mm:ss")
            val newRow = SensorLogRow(
                timeLabel = shortTime,
                fullTime = fullTime,
                ph = ph,
                tds = tds,
                temperature = temp,
                turbidity = turb,
                cod = _uiState.value.currentCod,
                nh4 = _uiState.value.currentNh4,
                no3 = _uiState.value.currentNo3,
                doLevel = doVal,
                batteryVoltage = batV,
                batteryCurrentA = batI,
                pondNumber = pondNum,
                status = evalStatus,
                isLiveMqtt = true
            )

            _uiState.update { current ->
                val updatedLogs = (listOf(newRow) + current.logs).take(60)
                current.copy(
                    hasData = true,
                    isMqttConnected = true,
                    networkError = null,
                    currentPh = ph ?: current.currentPh,
                    currentTds = tds ?: current.currentTds,
                    currentTemp = temp ?: current.currentTemp,
                    currentTurbidity = turb ?: current.currentTurbidity,
                    currentDo = doVal ?: current.currentDo,
                    batteryVoltage = batV ?: current.batteryVoltage,
                    batteryCurrentA = batI ?: current.batteryCurrentA,
                    status = evalStatus,
                    logs = updatedLogs,
                    trendPoints = updatedLogs.mapNotNull { it.ph }.reversed()
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadCalibrationFromSupabase() {
        if (deviceId.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val dev = SupabaseApi.getDeviceCalibration(deviceId) ?: return@launch
                _uiState.update { current ->
                    current.copy(
                        phConfig = SensorParamConfig(
                            slope = dev.optNullableDoubleAny("ph_slope")?.toString() ?: current.phConfig.slope,
                            offset = dev.optNullableDoubleAny("ph_offset")?.toString() ?: current.phConfig.offset
                        ),
                        tdsConfig = SensorParamConfig(
                            slope = dev.optNullableDoubleAny("tds_slope")?.toString() ?: current.tdsConfig.slope,
                            offset = dev.optNullableDoubleAny("tds_offset")?.toString() ?: current.tdsConfig.offset
                        ),
                        turbConfig = SensorParamConfig(
                            slope = dev.optNullableDoubleAny("turb_slope")?.toString() ?: current.turbConfig.slope,
                            offset = dev.optNullableDoubleAny("turb_offset")?.toString() ?: current.turbConfig.offset
                        ),
                        doConfig = SensorParamConfig(
                            slope = dev.optNullableDoubleAny("do_slope")?.toString() ?: current.doConfig.slope,
                            offset = dev.optNullableDoubleAny("do_offset")?.toString() ?: current.doConfig.offset
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun startRealtimePolling() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                try {
                    loadTelemetryDataInternal()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                delay(30_000)
            }
        }
    }

    private suspend fun loadTelemetryDataInternal() {
        if (deviceId.isBlank()) return
        try {
            var jsonLogs = SupabaseApi.getSensorLogs(deviceId, limit = 25)
            if (jsonLogs.length() == 0) {
                val pondNum = _uiState.value.pondName.filter { it.isDigit() }.toIntOrNull()
                if (pondNum != null) {
                    jsonLogs = SupabaseApi.getSensorLogsByPond(pondNum, limit = 25)
                }
            }
            val list = mutableListOf<SensorLogRow>()

            for (i in 0 until jsonLogs.length()) {
                val obj = jsonLogs.getJSONObject(i)
                val rawDate: String = obj.optNullableStringAny("created_at") ?: ""
                val formattedTime = WibTimeUtil.formatIsoToWibTime(rawDate, "HH:mm")
                val fullTime = WibTimeUtil.formatIsoToWibTime(rawDate, "yyyy-MM-dd HH:mm:ss")

                val codVal = obj.optNullableDoubleAny("cod", "cod_mg_l")?.let { formatSensorMetric(it, 1) }
                    ?: obj.optNullableStringAny("cod")
                    ?: "N/A"
                val nh4Val = obj.optNullableDoubleAny("nh4", "nh4_mg_l", "amonia")?.let { formatSensorMetric(it, 2) }
                    ?: obj.optNullableStringAny("nh4")
                    ?: "N/A"
                val no3Val = obj.optNullableDoubleAny("no3", "no3_mg_l", "nitrat")?.let { formatSensorMetric(it, 2) }
                    ?: obj.optNullableStringAny("no3")
                    ?: "N/A"

                val phVal = obj.optNullableDoubleAny("ph_level", "ph", "live_ph")
                val tdsVal = obj.optNullableDoubleAny("tds_ppm", "tds", "live_tds")
                val tempVal = obj.optNullableDoubleAny("temperature_c", "temperature", "temp", "live_temp")
                val turbVal = obj.optNullableDoubleAny("turbidity_ntu", "turbidity", "turb", "live_turb")
                val doVal = obj.optNullableDoubleAny("do_level", "do_mg_l", "dissolved_oxygen", "do", "live_do")
                val evaluatedStatus = WaterQualityRules.evaluateStatusLabel(
                    ph = phVal,
                    tds = tdsVal,
                    temp = tempVal,
                    doLevel = doVal,
                    turbidity = turbVal,
                    fallback = obj.optNullableStringAny("status") ?: "NORMAL"
                )

                list.add(
                    SensorLogRow(
                        timeLabel = formattedTime,
                        fullTime = fullTime,
                        ph = phVal,
                        tds = tdsVal,
                        temperature = tempVal,
                        turbidity = turbVal,
                        cod = codVal,
                        nh4 = nh4Val,
                        no3 = no3Val,
                        doLevel = doVal,
                        batteryVoltage = obj.optNullableDoubleAny("battery_voltage", "bat_v"),
                        batteryCurrentA = obj.optNullableDoubleAny("battery_current_a", "bat_i"),
                        pondNumber = obj.optNullableIntAny("pond_number"),
                        status = evaluatedStatus
                    )
                )
            }

            val latest = list.firstOrNull()
            val netErr = SupabaseApi.lastNetworkError
            if (latest != null) {
                _uiState.update { current ->
                    val liveMqttPrefix = current.logs.filter { it.isLiveMqtt }.take(30)
                    val mergedLogs = (liveMqttPrefix + list).take(60)
                    val activeLatest = liveMqttPrefix.firstOrNull() ?: latest
                    current.copy(
                        hasData = true,
                        currentPh = activeLatest.ph,
                        currentTds = activeLatest.tds,
                        currentTemp = activeLatest.temperature,
                        currentTurbidity = activeLatest.turbidity,
                        currentDo = activeLatest.doLevel,
                        currentCod = activeLatest.cod,
                        currentNh4 = activeLatest.nh4,
                        currentNo3 = activeLatest.no3,
                        batteryVoltage = activeLatest.batteryVoltage,
                        batteryCurrentA = activeLatest.batteryCurrentA,
                        status = activeLatest.status,
                        networkError = if (current.isMqttConnected) null else netErr,
                        logs = mergedLogs,
                        trendPoints = mergedLogs.mapNotNull { it.ph }.reversed()
                    )
                }
            } else {
                val liveObj = SupabaseApi.getLiveDeviceTelemetry(deviceId)
                if (liveObj != null) {
                    val phVal = liveObj.optNullableDoubleAny("ph_level", "live_ph", "ph")
                    val tdsVal = liveObj.optNullableDoubleAny("tds_ppm", "live_tds", "tds")
                    val tempVal = liveObj.optNullableDoubleAny("temperature_c", "live_temp", "temp")
                    val turbVal = liveObj.optNullableDoubleAny("turbidity_ntu", "live_turb", "turb")
                    val doVal = liveObj.optNullableDoubleAny("do_level", "do_mg_l", "dissolved_oxygen", "do", "live_do")
                    val evaluatedStatus = WaterQualityRules.evaluateStatusLabel(
                        ph = phVal,
                        tds = tdsVal,
                        temp = tempVal,
                        doLevel = doVal,
                        turbidity = turbVal,
                        fallback = liveObj.optNullableStringAny("status") ?: currentStatusOrDefault()
                    )
                    _uiState.update { current ->
                        if (current.isMqttConnected && current.logs.any { it.isLiveMqtt }) {
                            current.copy(networkError = null)
                        } else {
                            current.copy(
                                hasData = true,
                                currentPh = phVal,
                                currentTds = tdsVal,
                                currentTemp = tempVal,
                                currentTurbidity = turbVal,
                                currentDo = doVal,
                                batteryVoltage = liveObj.optNullableDoubleAny("battery_voltage", "bat_v"),
                                batteryCurrentA = liveObj.optNullableDoubleAny("battery_current_a", "bat_i"),
                                status = evaluatedStatus,
                                networkError = SupabaseApi.lastNetworkError
                            )
                        }
                    }
                } else if (netErr != null) {
                    _uiState.update { current ->
                        current.copy(networkError = if (current.isMqttConnected) null else netErr)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            _uiState.update { current ->
                current.copy(networkError = SupabaseApi.lastNetworkError ?: "Koneksi ke server lambat / terputus")
            }
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

    fun updateTurbConfig(slope: String, offset: String) {
        _uiState.update { it.copy(turbConfig = SensorParamConfig(slope, offset)) }
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
            val turbSlope = parse(state.turbConfig.slope)
            val turbOffset = parse(state.turbConfig.offset)

            val payload = org.json.JSONObject().apply {
                put("command", "calibrate")
                put("ph", org.json.JSONObject().put("slope", phSlope).put("offset", phOffset))
                put("do", org.json.JSONObject().put("slope", doSlope).put("offset", doOffset))
                put("tds", org.json.JSONObject().put("slope", tdsSlope).put("offset", tdsOffset))
                put("turbidity", org.json.JSONObject().put("slope", turbSlope).put("offset", turbOffset))
            }

            val ok = if (deviceId.isNotBlank()) SupabaseApi.updateDeviceCalibration(deviceId, payload) else false
            withContext(Dispatchers.Main) {
                onComplete(ok)
            }
        }
    }

    private fun currentStatusOrDefault(): String = _uiState.value.status.ifBlank { "NORMAL" }

    override fun onCleared() {
        super.onCleared()
        pollingJob?.cancel()
        mqttClient.stop()
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