package id.ac.usm.sipandutambak.data.remote.api

import id.ac.usm.sipandutambak.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

interface TambakApiService {

    @GET("devices")
    suspend fun getDevices(): Response<List<DeviceDto>>

    @GET("devices/{id}")
    suspend fun getDeviceDetail(
        @Path("id") deviceId: String
    ): Response<DeviceDto>

    @GET("ponds")
    suspend fun getPonds(): Response<List<PondDto>>

    @GET("alerts")
    suspend fun getActiveAlerts(
        @Query("active") active: Boolean = true
    ): Response<List<AlertDto>>

    @GET("devices/{id}/history")
    suspend fun getDeviceHistory(
        @Path("id") deviceId: String,
        @Query("param") param: String,
        @Query("range") range: String,             // "24h", "7d", "30d"
        @Query("resolution") resolution: String    // "1h", "3h", "1d"
    ): Response<HistoryDto>

    @GET("devices/{id}/readings")
    suspend fun getDeviceReadings(
        @Path("id") deviceId: String,
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0
    ): Response<List<ReadingDto>>

    @PUT("devices/{id}/pond")
    suspend fun updateDevicePond(
        @Path("id") deviceId: String,
        @Body body: UpdatePondRequest
    ): Response<Unit>

    @GET("devices/{id}/calibration")
    suspend fun getCalibration(
        @Path("id") deviceId: String,
        @Query("param") param: String
    ): Response<CalibrationDto>

    @PUT("devices/{id}/calibration")
    suspend fun updateCalibration(
        @Path("id") deviceId: String,
        @Body body: UpdateCalibrationRequest
    ): Response<Unit>

    @GET("health")
    suspend fun checkHealth(): Response<Map<String, Any>>
}