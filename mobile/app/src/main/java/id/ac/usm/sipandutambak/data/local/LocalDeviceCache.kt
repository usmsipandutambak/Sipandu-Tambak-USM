package id.ac.usm.sipandutambak.data.local

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import id.ac.usm.sipandutambak.domain.model.Device

data class ThresholdConfig(
    val minDo: Double = 4.0,       // Di bawah 4 mg/L = BAHAYA
    val minPh: Double = 7.0,       // pH minimum normal
    val maxPh: Double = 8.5,       // pH maksimum normal
    val minTemp: Double = 26.0,    // Suhu minimum normal (°C)
    val maxTemp: Double = 32.0     // Suhu maksimum normal (°C)
)

class LocalDeviceCache(context: Context) {
    private val prefs = context.getSharedPreferences("sipandu_offline_cache", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveDevices(devices: List<Device>) {
        val json = gson.toJson(devices)
        prefs.edit().putString("cached_devices", json).apply()
    }

    fun getDevices(): List<Device> {
        val json = prefs.getString("cached_devices", null) ?: return emptyList()
        val type = object : TypeToken<List<Device>>() {}.type
        return try {
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun updateDevicePond(deviceId: String, pondId: String, pondName: String) {
        val current = getDevices().map { device ->
            if (device.id == deviceId) {
                device.copy(pondId = pondId, pondName = pondName)
            } else {
                device
            }
        }
        saveDevices(current)
    }

    fun saveThresholds(config: ThresholdConfig) {
        val json = gson.toJson(config)
        prefs.edit().putString("water_thresholds", json).apply()
    }

    fun getThresholds(): ThresholdConfig {
        val json = prefs.getString("water_thresholds", null) ?: return ThresholdConfig()
        return try {
            gson.fromJson(json, ThresholdConfig::class.java) ?: ThresholdConfig()
        } catch (e: Exception) {
            ThresholdConfig()
        }
    }
}