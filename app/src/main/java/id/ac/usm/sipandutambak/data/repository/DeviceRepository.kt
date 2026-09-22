package id.ac.usm.sipandutambak.data.repository

import id.ac.usm.sipandutambak.data.model.DeviceData
import kotlinx.coroutines.flow.Flow

interface DeviceRepository {
    fun getDevices(): Flow<List<DeviceData>>
    suspend fun getDeviceById(id: String): DeviceData?
    suspend fun saveDevice(device: DeviceData)
    suspend fun deleteDevice(id: String)
    suspend fun updateDeviceStatus(id: String, newStatus: String)
}