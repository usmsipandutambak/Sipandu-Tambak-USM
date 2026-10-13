package id.ac.usm.sipandutambak

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import id.ac.usm.sipandutambak.di.ServiceLocator
import id.ac.usm.sipandutambak.ui.util.NotificationHelper
import id.ac.usm.sipandutambak.worker.SensorAlertWorker
import java.util.concurrent.TimeUnit

class SipanduApp : Application() {

    override fun onCreate() {
        super.onCreate()
        ServiceLocator.init(this)
        NotificationHelper.createNotificationChannel(this)
        setupBackgroundMonitoring()
    }

    private fun setupBackgroundMonitoring() {
        val monitoringWorkRequest = PeriodicWorkRequestBuilder<SensorAlertWorker>(
            15, TimeUnit.MINUTES
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "TambakSensorMonitoring",
            ExistingPeriodicWorkPolicy.KEEP,
            monitoringWorkRequest
        )
    }
}