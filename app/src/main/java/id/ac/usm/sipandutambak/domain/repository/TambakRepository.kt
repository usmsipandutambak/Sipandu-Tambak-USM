package id.ac.usm.sipandutambak.domain.repository

import id.ac.usm.sipandutambak.domain.model.Alert
import id.ac.usm.sipandutambak.domain.model.Device
import id.ac.usm.sipandutambak.domain.model.PondSummary

interface TambakRepository {
    suspend fun getDevices(): Result<List<Device>>
    suspend fun getDeviceDetail(deviceId: String): Result<Device>
    suspend fun getActiveAlerts(): Result<List<Alert>>
    suspend fun getPondSummary(): Result<PondSummary>
    suspend fun updateDevicePond(deviceId: String, pondId: String): Result<Unit>
    suspend fun saveCalibration(deviceId: String, param: String, slope: Double, offset: Double): Result<Unit>
}