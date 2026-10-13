package id.ac.usm.sipandutambak.ui.util

import android.content.Context

enum class AlertLevel(val label: String, val rank: Int) {
    NORMAL("NORMAL", 0),
    WASPADA("WASPADA", 1),
    BAHAYA("BAHAYA", 2)
}

data class ParamAlert(
    val param: String,
    val level: AlertLevel,
    val value: Double,
    val unit: String
)

object WaterQualityRules {

    fun evaluateDo(v: Double?): AlertLevel? = when {
        v == null -> null
        v < 3.0 -> AlertLevel.BAHAYA
        v < 5.0 -> AlertLevel.WASPADA
        else -> AlertLevel.NORMAL
    }

    fun evaluatePh(v: Double?): AlertLevel? = when {
        v == null -> null
        v < 6.0 || v > 9.0 -> AlertLevel.BAHAYA
        v < 6.5 || v > 8.5 -> AlertLevel.WASPADA
        else -> AlertLevel.NORMAL
    }

    fun evaluateTemp(v: Double?): AlertLevel? = when {
        v == null -> null
        v < 22.0 || v > 34.0 -> AlertLevel.BAHAYA
        v < 25.0 || v > 32.0 -> AlertLevel.WASPADA
        else -> AlertLevel.NORMAL
    }

    fun evaluateTds(v: Double?): AlertLevel? = when {
        v == null -> null
        v > 1000.0 -> AlertLevel.BAHAYA
        v > 500.0 -> AlertLevel.WASPADA
        else -> AlertLevel.NORMAL
    }

    fun evaluateAll(ph: Double?, tds: Double?, temp: Double?, doLevel: Double?): List<ParamAlert> {
        val list = mutableListOf<ParamAlert>()
        evaluateDo(doLevel)?.let { list.add(ParamAlert("Oksigen Terlarut (DO)", it, doLevel!!, "mg/L")) }
        evaluatePh(ph)?.let { list.add(ParamAlert("pH", it, ph!!, "")) }
        evaluateTemp(temp)?.let { list.add(ParamAlert("Suhu", it, temp!!, "°C")) }
        evaluateTds(tds)?.let { list.add(ParamAlert("TDS", it, tds!!, "ppm")) }
        return list
    }

    fun overallStatus(alerts: List<ParamAlert>): AlertLevel =
        alerts.maxByOrNull { it.level.rank }?.level ?: AlertLevel.NORMAL
}

object AlertMonitor {
    private const val COOLDOWN_MS = 15 * 60 * 1000L
    private val lastLevel = mutableMapOf<String, AlertLevel>()
    private val lastSent = mutableMapOf<String, Long>()

    fun process(
        context: Context,
        deviceId: String,
        pondName: String,
        ph: Double?,
        tds: Double?,
        temp: Double?,
        doLevel: Double?
    ) {
        val now = System.currentTimeMillis()
        WaterQualityRules.evaluateAll(ph, tds, temp, doLevel).forEach { a ->
            val key = deviceId + ":" + a.param
            val prev = lastLevel[key] ?: AlertLevel.NORMAL
            lastLevel[key] = a.level

            if (a.level == AlertLevel.NORMAL) return@forEach

            val escalated = a.level.rank > prev.rank
            val cooledDown = now - (lastSent[key] ?: 0L) >= COOLDOWN_MS
            if (!escalated && !cooledDown) return@forEach

            lastSent[key] = now
            val valueText = String.format(java.util.Locale.US, "%.1f", a.value) +
                    (if (a.unit.isNotEmpty()) " " + a.unit else "")
            val title = a.level.label + ": " + pondName
            val message = a.param + " terbaca " + valueText +
                    (if (a.level == AlertLevel.BAHAYA) " (kritis, segera periksa kolam)" else " (di luar rentang normal)")

            NotificationHelper.showAlertNotification(
                context = context,
                title = title,
                message = message,
                notificationId = key.hashCode()
            )
        }
    }
}