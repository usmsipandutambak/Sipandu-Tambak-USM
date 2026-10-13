package id.ac.usm.sipandutambak.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import id.ac.usm.sipandutambak.di.ServiceLocator
import id.ac.usm.sipandutambak.ui.dashboard.DashboardViewModel
import id.ac.usm.sipandutambak.ui.navigation.AppNavigation
import id.ac.usm.sipandutambak.ui.theme.SipanduTheme

class MainActivity : ComponentActivity() {

    private val dashboardViewModel: DashboardViewModel by viewModels {
        DashboardViewModel.Factory(ServiceLocator.repository)
    }

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        askNotificationPermission()

        androidx.work.WorkManager.getInstance(this).enqueue(
            androidx.work.OneTimeWorkRequestBuilder<id.ac.usm.sipandutambak.worker.SensorAlertWorker>().build()
        )

        setContent {
            SipanduTheme {
                AppNavigation(dashboardViewModel = dashboardViewModel)
            }
        }
    }
    
    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionCheck = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            )
            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}