package id.ac.usm.sipandutambak.ui.devices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.ac.usm.sipandutambak.data.model.DeviceData
import id.ac.usm.sipandutambak.di.ServiceLocator
import id.ac.usm.sipandutambak.di.SupabaseRealDeviceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DeviceListUiState(
    val devices: List<DeviceData> = emptyList(),
    val searchQuery: String = "",
    val selectedFilter: String = "Semua",
    val isRegistering: Boolean = false,
    val editingDeviceId: String? = null,
    val pendingDeviceName: String = "",
    val selectedPond: String = "",
    val commodityName: String = "LELE",
    val isScanValid: Boolean = true,
    val scannedRawId: String = ""
)

class DeviceListViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(DeviceListUiState())
    val uiState: StateFlow<DeviceListUiState> = _uiState.asStateFlow()

    // =========================================================================
    // KETENTUAN PERIZINAN ALAT (WHITELIST)
    // ID alat resmi yang diizinkan masuk ke sistem Supabase:
    // =========================================================================
    private val allowedDeviceIds = setOf(
        "SIPANDU_A"
        // Tambahkan ID alat baru di sini saat ada penambahan hardware
    )

    init {
        loadDevicesFromRepository()
    }

    fun loadDevicesFromRepository() {
        (ServiceLocator.deviceRepository as? SupabaseRealDeviceRepository)?.refreshData()

        viewModelScope.launch {
            ServiceLocator.deviceRepository.getDevices().collect { repoDevices ->
                _uiState.update { current ->
                    current.copy(devices = repoDevices)
                }
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun onFilterChange(filter: String) {
        _uiState.value = _uiState.value.copy(selectedFilter = filter)
    }

    fun onScanSuccess(rawQr: String) {
        val cleanedId = rawQr.trim().uppercase()

        val matchedAllowedId = allowedDeviceIds.firstOrNull { allowed ->
            cleanedId == allowed || cleanedId.contains(allowed)
        }

        if (matchedAllowedId != null) {
            val displayName = if (matchedAllowedId == "SIPANDU_A") "Sipandu A" else matchedAllowedId
            _uiState.value = _uiState.value.copy(
                isRegistering = true,
                isScanValid = true,
                editingDeviceId = matchedAllowedId,
                pendingDeviceName = displayName,
                selectedPond = "Kolam 1",
                commodityName = "LELE",
                scannedRawId = cleanedId
            )
        } else {
            _uiState.value = _uiState.value.copy(
                isRegistering = true,
                isScanValid = false,
                editingDeviceId = null,
                pendingDeviceName = "",
                scannedRawId = cleanedId
            )
        }
    }

    fun resetScanError() {
        _uiState.value = _uiState.value.copy(
            isRegistering = false,
            isScanValid = true,
            scannedRawId = ""
        )
    }

    fun prepareEditDevice(deviceId: String) {
        val device = _uiState.value.devices.firstOrNull { it.id == deviceId }
        if (device != null) {
            _uiState.value = _uiState.value.copy(
                isRegistering = false,
                isScanValid = true,
                editingDeviceId = device.id,
                pendingDeviceName = device.name,
                selectedPond = device.pondName,
                commodityName = device.commodity
            )
        }
    }

    fun updatePond(pond: String) {
        _uiState.value = _uiState.value.copy(selectedPond = pond)
    }

    fun updateCommodity(commodity: String) {
        _uiState.value = _uiState.value.copy(commodityName = commodity)
    }

    fun saveDeviceConfiguration() {
        val currentState = _uiState.value
        if (!currentState.isScanValid) return

        val currentList = currentState.devices.toMutableList()
        val targetPond = if (currentState.selectedPond.isNotBlank()) currentState.selectedPond else "Kolam 1"
        val targetCommodity = if (currentState.commodityName.isNotBlank()) currentState.commodityName else "LELE"
        val targetId = currentState.editingDeviceId ?: "SIPANDU_A"
        val devName = if (currentState.pendingDeviceName.isNotBlank()) currentState.pendingDeviceName else "Sipandu A"

        val existingIndex = currentList.indexOfFirst { it.id.equals(targetId, ignoreCase = true) }

        val targetDevice = if (existingIndex >= 0) {
            currentList[existingIndex].copy(
                name = devName,
                pondName = targetPond,
                commodity = targetCommodity,
                ponds = listOf(targetPond),
                isConnected = true
            )
        } else {
            DeviceData(
                id = targetId,
                name = devName,
                pondName = targetPond,
                commodity = targetCommodity,
                ipAddress = "-",
                batteryPercent = 100,
                signalQuality = "Kuat",
                status = "NORMAL",
                isConnected = true,
                ponds = listOf(targetPond),
                pondCommodities = mapOf(targetPond to targetCommodity)
            )
        }

        if (existingIndex >= 0) {
            currentList[existingIndex] = targetDevice
        } else {
            currentList.add(targetDevice)
        }

        viewModelScope.launch {
            ServiceLocator.deviceRepository.saveDevice(targetDevice)
        }

        _uiState.value = currentState.copy(
            devices = currentList,
            isRegistering = false,
            isScanValid = true,
            editingDeviceId = null,
            pendingDeviceName = ""
        )
    }

    // Fungsi yang dibutuhkan oleh AppNavigation.kt saat kalibrasi selesai
    fun updateDeviceStatusFromCalibration(deviceId: String, newStatus: String) {
        val currentList = _uiState.value.devices.toMutableList()
        val index = currentList.indexOfFirst { it.id == deviceId }
        if (index >= 0) {
            val updated = currentList[index].copy(
                status = newStatus,
                isConnected = true
            )
            currentList[index] = updated
            _uiState.value = _uiState.value.copy(devices = currentList)

            viewModelScope.launch {
                ServiceLocator.deviceRepository.updateDeviceStatus(deviceId, newStatus)
            }
        }
    }

    fun deleteDevice(deviceId: String) {
        val currentList = _uiState.value.devices.toMutableList()
        currentList.removeAll { it.id == deviceId }
        _uiState.value = _uiState.value.copy(devices = currentList)

        viewModelScope.launch {
            ServiceLocator.deviceRepository.deleteDevice(deviceId)
        }
    }
}