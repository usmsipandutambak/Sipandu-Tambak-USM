package id.ac.usm.sipandutambak.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import id.ac.usm.sipandutambak.data.api.SupabaseApi
import id.ac.usm.sipandutambak.data.api.optNullableDoubleAny
import id.ac.usm.sipandutambak.ui.util.AlertMonitor

class SensorAlertWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

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
                val latestLog = SupabaseApi.getLatestSensorLog(id)

                AlertMonitor.process(
                    context = context,
                    deviceId = id,
                    pondName = pondName,
                    ph = latestLog?.optNullableDoubleAny("ph_level", "ph") ?: dev.optNullableDoubleAny("live_ph", "ph_level"),
                    tds = latestLog?.optNullableDoubleAny("tds_ppm", "tds") ?: dev.optNullableDoubleAny("live_tds", "tds_ppm"),
                    temp = latestLog?.optNullableDoubleAny("temperature_c", "temp") ?: dev.optNullableDoubleAny("live_temp", "temperature_c"),
                    doLevel = latestLog?.optNullableDoubleAny("do_level", "do_mg_l", "dissolved_oxygen", "do") ?: dev.optNullableDoubleAny("live_do", "do_level", "dissolved_oxygen")
                )
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}