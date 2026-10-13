package id.ac.usm.sipandutambak.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import id.ac.usm.sipandutambak.domain.model.Alert
import id.ac.usm.sipandutambak.domain.model.Device
import id.ac.usm.sipandutambak.domain.model.PondSummary
import id.ac.usm.sipandutambak.domain.repository.TambakRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DashboardUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isAutoRefreshActive: Boolean = true,
    val isOfflineMode: Boolean = false,
    val lastSyncTime: String = "-",
    val pondSummary: PondSummary? = null,
    val alerts: List<Alert> = emptyList(),
    val devices: List<Device> = emptyList(),
    val errorMessage: String? = null
)

class DashboardViewModel(
    private val repository: TambakRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState(isLoading = true))
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private var pollingJob: Job? = null

    init {
        loadDashboardData(isInitial = true)
        startAutoRefresh()
    }

    fun loadDashboardData(isInitial: Boolean = false) {
        viewModelScope.launch {
            if (isInitial) {
                _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            } else {
                _uiState.value = _uiState.value.copy(isRefreshing = true)
            }

            val summaryResult = repository.getPondSummary()
            val alertsResult = repository.getActiveAlerts()
            val devicesResult = repository.getDevices()

            val isOffline = summaryResult.isFailure && devicesResult.isSuccess
            val currentTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())

            if (devicesResult.isSuccess || summaryResult.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isRefreshing = false,
                    isOfflineMode = isOffline,
                    lastSyncTime = currentTime,
                    pondSummary = summaryResult.getOrNull() ?: PondSummary(devicesResult.getOrDefault(emptyList()).size, 1, 0),
                    alerts = alertsResult.getOrDefault(emptyList()),
                    devices = devicesResult.getOrDefault(emptyList())
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isRefreshing = false,
                    isOfflineMode = true,
                    errorMessage = "Gagal menyinkronkan data telemetri."
                )
            }
        }
    }

    fun toggleAutoRefresh() {
        val newState = !_uiState.value.isAutoRefreshActive
        _uiState.value = _uiState.value.copy(isAutoRefreshActive = newState)

        if (newState) {
            startAutoRefresh()
        } else {
            pollingJob?.cancel()
            pollingJob = null
        }
    }

    private fun startAutoRefresh() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (isActive) {
                delay(10_000)
                if (_uiState.value.isAutoRefreshActive) {
                    loadDashboardData(isInitial = false)
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        pollingJob?.cancel()
    }

    class Factory(private val repository: TambakRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DashboardViewModel(repository) as T
        }
    }
}
