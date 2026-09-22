package id.ac.usm.sipandutambak.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import id.ac.usm.sipandutambak.di.ServiceLocator
import id.ac.usm.sipandutambak.ui.about.AboutSystemScreen
import id.ac.usm.sipandutambak.ui.dashboard.DashboardScreen
import id.ac.usm.sipandutambak.ui.dashboard.DashboardViewModel
import id.ac.usm.sipandutambak.ui.devices.DeviceConfigScreen
import id.ac.usm.sipandutambak.ui.devices.DeviceListScreen
import id.ac.usm.sipandutambak.ui.devices.DeviceListViewModel
import id.ac.usm.sipandutambak.ui.devices.detail.CalibrationScreen
import id.ac.usm.sipandutambak.ui.devices.detail.DeviceDetailScreen
import id.ac.usm.sipandutambak.ui.devices.detail.DeviceDetailViewModel
import id.ac.usm.sipandutambak.ui.devices.detail.HistoryDetailScreen
import id.ac.usm.sipandutambak.ui.scan.ScanScreen
import id.ac.usm.sipandutambak.ui.splash.SplashScreen

@Composable
fun AppNavigation(
    dashboardViewModel: DashboardViewModel
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = currentRoute != null &&
            currentRoute != Screen.Splash.route &&
            currentRoute != Screen.Scan.route

    val deviceListViewModel: DeviceListViewModel = viewModel()
    val deviceListState by deviceListViewModel.uiState.collectAsState()

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                SipanduBottomBar(
                    currentRoute = currentRoute,
                    onNavigate = { targetRoute ->
                        if (currentRoute != targetRoute) {
                            navController.navigate(targetRoute) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Splash.route,
            modifier = Modifier.padding(top = innerPadding.calculateTopPadding())
        ) {
            composable(Screen.Splash.route) {
                SplashScreen(
                    onTimeout = {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    viewModel = dashboardViewModel,
                    deviceListViewModel = deviceListViewModel,
                    onPondClick = { pondNum ->
                        val targetName = "Kolam " + pondNum
                        val attachedDevice = deviceListState.devices.firstOrNull { dev ->
                            dev.pondName.equals(targetName, ignoreCase = true) ||
                                    dev.ponds.any { it.equals(targetName, ignoreCase = true) }
                        }

                        if (attachedDevice != null && attachedDevice.isConnected) {
                            navController.navigate(Screen.DeviceDetail.createRoute(attachedDevice.id))
                        } else {
                            navController.navigate("history_detail/" + pondNum)
                        }
                    },
                    onAboutSystemClick = {
                        navController.navigate(Screen.AboutSystem.route)
                    }
                )
            }

            composable(Screen.DeviceList.route) {
                DeviceListScreen(
                    viewModel = deviceListViewModel,
                    onDeviceClick = { deviceId ->
                        navController.navigate(Screen.DeviceDetail.createRoute(deviceId))
                    },
                    onConfigClick = { deviceId ->
                        deviceListViewModel.prepareEditDevice(deviceId)
                        navController.navigate(Screen.DeviceConfig.route)
                    },
                    onAboutSystemClick = {
                        navController.navigate(Screen.AboutSystem.route)
                    }
                )
            }

            composable(Screen.Scan.route) {
                ScanScreen(
                    onDeviceDetected = { rawQrString ->
                        deviceListViewModel.onScanSuccess(rawQrString)
                        navController.navigate(Screen.DeviceConfig.route) {
                            popUpTo(Screen.Scan.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.DeviceConfig.route) {
                DeviceConfigScreen(
                    viewModel = deviceListViewModel,
                    onSaveSuccess = {
                        navController.navigate(Screen.DeviceList.route) {
                            popUpTo(Screen.DeviceList.route) { inclusive = true }
                        }
                    },
                    onRescanClick = {
                        navController.navigate(Screen.Scan.route) {
                            popUpTo(Screen.DeviceConfig.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(
                route = Screen.DeviceDetail.route,
                arguments = listOf(navArgument("deviceId") { type = NavType.StringType })
            ) { backStackEntry ->
                val deviceId = backStackEntry.arguments?.getString("deviceId") ?: ""
                val detailViewModel: DeviceDetailViewModel = viewModel(
                    factory = DeviceDetailViewModel.Factory(deviceId, ServiceLocator.repository)
                )

                val currentDevice = deviceListState.devices.firstOrNull { it.id == deviceId }
                if (currentDevice != null) {
                    detailViewModel.setDeviceInfo(
                        name = currentDevice.name,
                        pond = currentDevice.pondName,
                        commodity = currentDevice.commodity
                    )
                }

                DeviceDetailScreen(
                    viewModel = detailViewModel,
                    onBackClick = { navController.popBackStack() },
                    onCalibrationClick = {
                        navController.navigate(Screen.Calibration.createRoute(deviceId))
                    },
                    onAboutSystemClick = {
                        navController.navigate(Screen.AboutSystem.route)
                    }
                )
            }

            composable(
                route = Screen.Calibration.route,
                arguments = listOf(navArgument("deviceId") { type = NavType.StringType })
            ) { backStackEntry ->
                val deviceId = backStackEntry.arguments?.getString("deviceId") ?: ""
                val detailViewModel: DeviceDetailViewModel = viewModel(
                    factory = DeviceDetailViewModel.Factory(deviceId, ServiceLocator.repository)
                )

                CalibrationScreen(
                    viewModel = detailViewModel,
                    onBackClick = {
                        deviceListViewModel.updateDeviceStatusFromCalibration(
                            deviceId = deviceId,
                            newStatus = detailViewModel.uiState.value.status
                        )
                        navController.popBackStack()
                    }
                )
            }

            composable(
                route = "history_detail/{kolamId}",
                arguments = listOf(navArgument("kolamId") { type = NavType.StringType })
            ) { backStackEntry ->
                val kolamId = backStackEntry.arguments?.getString("kolamId") ?: "11"
                HistoryDetailScreen(
                    kolamId = kolamId,
                    commodityName = "LELE",
                    onBackClick = { navController.popBackStack() },
                    onAboutSystemClick = {
                        navController.navigate(Screen.AboutSystem.route)
                    }
                )
            }

            composable(Screen.AboutSystem.route) {
                AboutSystemScreen()
            }
        }
    }
}