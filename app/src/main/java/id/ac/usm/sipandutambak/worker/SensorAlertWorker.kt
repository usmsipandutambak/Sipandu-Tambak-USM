package id.ac.usm.sipandutambak.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import id.ac.usm.sipandutambak.data.local.LocalDeviceCache
import id.ac.usm.sipandutambak.di.ServiceLocator
import id.ac.usm.sipandutambak.domain.model.DeviceStatus
import id.ac.usm.sipandutambak.ui.util.NotificationHelper

class SensorAlertWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val repository = ServiceLocator.repository
            val cache = LocalDeviceCache(context)
            val threshold = cache.getThresholds()

            // 1. Periksa alert langsung dari server/mock
            val alertsResult = repository.getActiveAlerts()
            if (alertsResult.isSuccess) {
                val alerts = alertsResult.getOrDefault(emptyList())
                val criticalAlert = alerts.firstOrNull { it.severity == DeviceStatus.BAHAYA }

                criticalAlert?.let { alert ->
                    NotificationHelper.showAlertNotification(
                        context = context,
                        title = "BAHAYA: ${alert.pondName}",
                        message = alert.message,
                        notificationId = alert.id.hashCode()
                    )
                    return Result.success()
                }
            }

            // 2. Evaluasi mandiri terhadap data sensor terkini berdasarkan ambang batas lokal
            val devicesResult = repository.getDevices()
            devicesResult.getOrNull()?.forEach { device ->
                device.parameters.forEach { param ->
                    val isCriticalDo = param.id == "do" && param.value < threshold.minDo
                    val isCriticalPh = param.id == "ph" && (param.value < threshold.minPh || param.value > threshold.maxPh)

                    if (isCriticalDo || isCriticalPh) {
                        NotificationHelper.showAlertNotification(
                            context = context,
                            title = "PERINGATAN: ${device.pondName}",
                            message = "${param.name} menyentuh angka kritis ${param.value} ${param.unit}!",
                            notificationId = (device.id + param.id).hashCode()
                        )
                        return Result.success()
                    }
                }
            }

            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}