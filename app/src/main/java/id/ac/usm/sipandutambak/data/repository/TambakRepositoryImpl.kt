package id.ac.usm.sipandutambak.data.repository

import id.ac.usm.sipandutambak.data.local.LocalDeviceCache
import id.ac.usm.sipandutambak.data.remote.api.TambakApiService
import id.ac.usm.sipandutambak.data.remote.dto.DeviceDto
import id.ac.usm.sipandutambak.data.remote.dto.UpdateCalibrationRequest
import id.ac.usm.sipandutambak.data.remote.dto.UpdatePondRequest
import id.ac.usm.sipandutambak.domain.model.*
import id.ac.usm.sipandutambak.domain.repository.TambakRepository

class TambakRepositoryImpl(
    private val api: TambakApiService,
    private val localCache: LocalDeviceCache
) : TambakRepository {

    override suspend fun getDevices(): Result<List<Device>> {
        return try {
            val response = api.getDevices()
            if (response.isSuccessful && response.body() != null) {
                val devices = response.body()!!.map { it.toDomain() }
                localCache.saveDevices(devices)
                Result.success(devices)
            } else {
                fetchFromLocalCache()
            }
        } catch (e: Exception) {
            fetchFromLocalCache()
        }
    }

    private fun fetchFromLocalCache(): Result<List<Device>> {
        val cached = localCache.getDevices()
        return if (cached.isNotEmpty()) {
            Result.success(cached)
        } else {
            Result.failure(Exception("Mode offline: Tidak ada data cache lokal."))
        }
    }

    override suspend fun getDeviceDetail(deviceId: String): Result<Device> {
        return try {
            val response = api.getDeviceDetail(deviceId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.toDomain())
            } else {
                fetchFromLocalDetail(deviceId)
            }
        } catch (e: Exception) {
            fetchFromLocalDetail(deviceId)
        }
    }

    private fun fetchFromLocalDetail(deviceId: String): Result<Device> {
        val found = localCache.getDevices().find { it.id == deviceId }
        return if (found != null) {
            Result.success(found)
        } else {
            Result.failure(Exception("Alat tidak ditemukan di penyimpanan offline."))
        }
    }

    override suspend fun getActiveAlerts(): Result<List<Alert>> {
        return try {
            val response = api.getActiveAlerts(active = true)
            if (response.isSuccessful && response.body() != null) {
                val alerts = response.body()!!.map { dto ->
                    Alert(
                        id = dto.id,
                        deviceId = dto.deviceId,
                        pondName = dto.pondName,
                        message = dto.message,
                        severity = mapSeverity(dto.severity),
                        timestamp = dto.timestamp,
                        snapshot = dto.snapshot ?: emptyMap()
                    )
                }
                Result.success(alerts)
            } else {
                Result.failure(Exception("Gagal memuat alert"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getPondSummary(): Result<PondSummary> {
        return try {
            val response = api.getPonds()
            if (response.isSuccessful && response.body() != null) {
                val ponds = response.body()!!
                val clearCount = ponds.count { it.clarity.equals("JERNIH", ignoreCase = true) }
                val turbidCount = ponds.size - clearCount
                Result.success(PondSummary(totalPonds = ponds.size, clearPonds = clearCount, turbidPonds = turbidCount))
            } else {
                Result.failure(Exception("Gagal memuat ringkasan kolam"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateDevicePond(deviceId: String, pondId: String): Result<Unit> {
        return try {
            val response = api.updateDevicePond(deviceId, UpdatePondRequest(pondId))
            if (response.isSuccessful) {
                val pondName = if (pondId == "p1") "Kolam Pembesaran 1" else "Kolam Pembesaran 2"
                localCache.updateDevicePond(deviceId, pondId, pondName)
                Result.success(Unit)
            } else {
                Result.failure(Exception("Gagal mengubah kolam"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun saveCalibration(
        deviceId: String,
        param: String,
        slope: Double,
        offset: Double
    ): Result<Unit> {
        return try {
            val response = api.updateCalibration(deviceId, UpdateCalibrationRequest(param, slope, offset))
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Gagal menyimpan kalibrasi"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun DeviceDto.toDomain(): Device {
        return Device(
            id = id,
            name = name,
            pondId = pondId,
            pondName = pondName,
            status = mapSeverity(status),
            lastSeen = lastSeen,
            parameters = parameters?.map { p ->
                SensorParameter(
                    id = p.id,
                    name = p.name,
                    value = p.value,
                    unit = p.unit,
                    status = mapSeverity(p.status),
                    sparklineData = p.sparkline ?: emptyList()
                )
            } ?: emptyList()
        )
    }

    private fun mapSeverity(statusStr: String): DeviceStatus {
        return try {
            DeviceStatus.valueOf(statusStr.uppercase())
        } catch (e: IllegalArgumentException) {
            DeviceStatus.NORMAL
        }
    }
}
