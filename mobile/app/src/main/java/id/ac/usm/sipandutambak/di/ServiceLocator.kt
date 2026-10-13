package id.ac.usm.sipandutambak.di

import android.content.Context
import id.ac.usm.sipandutambak.data.api.SupabaseApi
import id.ac.usm.sipandutambak.data.local.LocalDeviceCache
import id.ac.usm.sipandutambak.data.model.DeviceData
import id.ac.usm.sipandutambak.data.remote.api.TambakApiService
import id.ac.usm.sipandutambak.data.repository.DeviceRepository
import id.ac.usm.sipandutambak.data.repository.TambakRepositoryImpl
import id.ac.usm.sipandutambak.domain.repository.TambakRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
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

    // Retrofit API Service
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://tidmdjnjhqptszyubvue.supabase.co/")
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
            val list = mutableListOf<DeviceData>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val devId = obj.optString("id", "")
                val pondNum = obj.optInt("pond_number", 1)
                val pondStr = "Kolam $pondNum"

                list.add(
                    DeviceData(
                        id = devId,
                        name = obj.optString("name", "Sipandu"),
                        pondName = pondStr,
                        ponds = listOf(pondStr),
                        commodity = obj.optString("commodity", "LELE"),
                        status = obj.optString("status", "NORMAL"),
                        isConnected = obj.optBoolean("is_connected", false),
                        batteryPercent = obj.optInt("battery_percent", 100),
                        signalQuality = obj.optString("signal_quality", "Kuat"),
                        ipAddress = obj.optString("ip_address", "192.168.1.10")
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