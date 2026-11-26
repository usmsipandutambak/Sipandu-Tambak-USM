package id.ac.usm.sipandutambak.di

import android.content.Context
import id.ac.usm.sipandutambak.data.api.SupabaseApi
import id.ac.usm.sipandutambak.data.api.optNullableIntAny
import id.ac.usm.sipandutambak.data.api.optNullableStringAny
import id.ac.usm.sipandutambak.data.local.LocalDeviceCache
import id.ac.usm.sipandutambak.data.model.DeviceData
import id.ac.usm.sipandutambak.data.remote.api.TambakApiService
import id.ac.usm.sipandutambak.data.repository.DeviceRepository
import id.ac.usm.sipandutambak.data.repository.TambakRepositoryImpl
import id.ac.usm.sipandutambak.domain.repository.TambakRepository
import id.ac.usm.sipandutambak.util.WibTimeUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

object ServiceLocator {
    private var applicationContext: Context? = null

    // Digunakan oleh SipanduApp.kt
    fun init(context: Context) {
        applicationContext = context.applicationContext
    }

    // Cache Lokal
    val localCache: LocalDeviceCache by lazy {
        val ctx = applicationContext ?: error("ServiceLocator belum diinisialisasi. Pastikan ServiceLocator.init(context) dipanggil di Application.")
        LocalDeviceCache(ctx)
    }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    // Retrofit API Service
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://tidmdjnjhqptszyubvue.supabase.co/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val apiService: TambakApiService by lazy {
        retrofit.create(TambakApiService::class.java)
    }

    // Repository utama untuk MainActivity, SensorAlertWorker, dan Dashboard
    val repository: TambakRepository by lazy {
        TambakRepositoryImpl(apiService, localCache)
    }

    // Repository untuk integrasi langsung Supabase
    val deviceRepository: DeviceRepository by lazy {
        SupabaseRealDeviceRepository()
    }
}

class SupabaseRealDeviceRepository : DeviceRepository {
    private val _devicesFlow = MutableStateFlow<List<DeviceData>>(emptyList())
    private val scope = CoroutineScope(Dispatchers.IO)

    // Interval polling disamakan dengan UPLOAD_INTERVAL_MS di ESP32IoT.ino (60 detik)
    private val POLLING_INTERVAL_MS = 30_000L  // dibuat 30 detik agar UI terasa lebih responsif

    init {
        startAutoRefresh()
    }

    // Loop polling otomatis selama repository ini hidup
    private fun startAutoRefresh() {
        scope.launch {
            while (isActive) {
                fetchAndEmitDevices()
                delay(POLLING_INTERVAL_MS)
            }
        }
    }

    // Dipanggil manual dari ViewModel (mis. pull-to-refresh), tetap dipertahankan agar tidak breaking change
    fun refreshData() {
        scope.launch {
            fetchAndEmitDevices()
        }
    }

    private suspend fun fetchAndEmitDevices() {
        try {
            val jsonArray = SupabaseApi.getDevices()
            if (jsonArray.length() == 0 && SupabaseApi.lastNetworkError != null && _devicesFlow.value.isNotEmpty()) {
                // Pertahankan daftar alat terakhir jika sedang timeout di sinyal lemah
                return
            }

            val logsArray = SupabaseApi.getLatestSensorLogs(limit = 20)
            val latestCreatedByDevice = mutableMapOf<String, String>()
            for (j in 0 until logsArray.length()) {
                val logObj = logsArray.getJSONObject(j)
                val logDevId = logObj.optNullableStringAny("device_id")?.uppercase(Locale.ROOT) ?: continue
                if (!latestCreatedByDevice.containsKey(logDevId)) {
                    logObj.optNullableStringAny("created_at")?.let { ts ->
                        latestCreatedByDevice[logDevId] = ts
                    }
                }
            }

            val list = mutableListOf<DeviceData>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val devId = obj.optNullableStringAny("id") ?: continue
                val pondNum = obj.optNullableIntAny("pond_number") ?: 1
                val pondStr = "Kolam $pondNum"
                val latestCreatedAt = latestCreatedByDevice[devId.uppercase(Locale.ROOT)]
                val lastSeen = obj.optNullableStringAny("last_seen")
                val online = WibTimeUtil.isDeviceOnline(
                    createdAt = latestCreatedAt,
                    lastSeen = lastSeen,
                    fallbackFlag = obj.optBoolean("is_connected", false)
                )

                list.add(
                    DeviceData(
                        id = devId,
                        name = obj.optNullableStringAny("name") ?: "Sipandu",
                        pondName = pondStr,
                        ponds = listOf(pondStr),
                        commodity = obj.optNullableStringAny("commodity") ?: "LELE",
                        status = obj.optNullableStringAny("status") ?: "NORMAL",
                        isConnected = online,
                        batteryPercent = obj.optNullableIntAny("battery_percent") ?: 100,
                        signalQuality = obj.optNullableStringAny("signal_quality") ?: "Kuat",
                        ipAddress = obj.optNullableStringAny("ip_address") ?: "192.168.100.213"
                    )
                )
            }
            _devicesFlow.value = list
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun getDevices(): Flow<List<DeviceData>> = _devicesFlow.asStateFlow()

    override suspend fun getDeviceById(id: String): DeviceData? {
        return _devicesFlow.value.firstOrNull { it.id == id }
    }

    // Sinkronisasi Simpan ke Database Supabase secara Nyata
    override suspend fun saveDevice(device: DeviceData) {
        _devicesFlow.value = _devicesFlow.value.toMutableList().apply {
            val index = indexOfFirst { it.id == device.id }
            if (index != -1) set(index, device) else add(device)
        }
        scope.launch {
            val pondNumber = device.pondName.filter { it.isDigit() }.toIntOrNull() ?: 1
            SupabaseApi.upsertDevice(
                id = device.id,
                name = device.name,
                pondNumber = pondNumber,
                commodity = device.commodity
            )
        }
    }

    // Sinkronisasi Hapus ke Database Supabase secara Nyata
    override suspend fun deleteDevice(id: String) {
        _devicesFlow.value = _devicesFlow.value.filterNot { it.id == id }
        scope.launch {
            SupabaseApi.deleteDevice(id)
        }
    }

    override suspend fun updateDeviceStatus(id: String, newStatus: String) {
        _devicesFlow.value = _devicesFlow.value.map {
            if (it.id == id) it.copy(status = newStatus) else it
        }
    }
}