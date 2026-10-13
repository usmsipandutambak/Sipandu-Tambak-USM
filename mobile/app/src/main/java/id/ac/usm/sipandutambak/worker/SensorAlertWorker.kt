package id.ac.usm.sipandutambak.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import id.ac.usm.sipandutambak.data.api.SupabaseApi
import id.ac.usm.sipandutambak.ui.util.AlertMonitor
import org.json.JSONObject

class SensorAlertWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private fun JSONObject.nullableDouble(key: String): Double? {
        if (!has(key) || isNull(key)) return null
        val v = optDouble(key, Double.NaN)
        return if (v.isNaN()) null else v
    }

    override suspend fun doWork(): Result {
        return try {
            val devices = SupabaseApi.getDevices()

            for (i in 0 until devices.length()) {
                val dev = devices.getJSONObject(i)
                if (dev.has("is_connected") && !dev.optBoolean("is_connected", true)) continue

                val id = dev.optString("id", "")
                if (id.isBlank()) continue

                val pondNumber = dev.optInt("pond_number", 0)
                val pondName = if (pondNumber > 0) "Kolam " + pondNumber else dev.optString("name", id)

                AlertMonitor.process(
                    context = context,
                    deviceId = id,
                    pondName = pondName,
                    ph = dev.nullableDouble("live_ph"),
                    tds = dev.nullableDouble("live_tds"),
                    temp = dev.nullableDouble("live_temp"),
                    doLevel = dev.nullableDouble("live_do")
                )
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}