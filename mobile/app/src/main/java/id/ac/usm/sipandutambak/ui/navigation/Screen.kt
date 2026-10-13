package id.ac.usm.sipandutambak.ui.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Dashboard : Screen("dashboard")
    object DeviceList : Screen("device_list")
    object Scan : Screen("scan")
    object DeviceConfig : Screen("device_config")
    object DeviceDetail : Screen("device_detail/{deviceId}") {
        fun createRoute(deviceId: String): String = "device_detail/$deviceId"
    }
    object Calibration : Screen("calibration/{deviceId}") {
        fun createRoute(deviceId: String): String = "calibration/$deviceId"
    }
    object AboutSystem : Screen("about_system")
}