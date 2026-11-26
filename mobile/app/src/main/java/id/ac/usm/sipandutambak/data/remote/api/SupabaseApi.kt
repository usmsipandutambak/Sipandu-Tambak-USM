package id.ac.usm.sipandutambak.data.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

fun JSONObject.optNullableDoubleAny(vararg keys: String): Double? {
    for (key in keys) {
        if (has(key) && !isNull(key)) {
            val v = optDouble(key, Double.NaN)
            if (!v.isNaN()) return v
        }
    }
    return null
}

fun JSONObject.optNullableIntAny(vararg keys: String): Int? {
    for (key in keys) {
        if (has(key) && !isNull(key)) {
            val v = optDouble(key, Double.NaN)
            if (!v.isNaN()) return v.toInt()
        }
    }
    return null
}

fun JSONObject.optNullableStringAny(vararg keys: String): String? {
    for (key in keys) {
        if (has(key) && !isNull(key)) {
            val raw = optString(key, "").trim()
            if (raw.isNotEmpty() && !raw.equals("null", ignoreCase = true)) {
                return raw
            }
        }
    }
    return null
}

fun formatSensorMetric(value: Double?, decimals: Int = 1, fallback: String = "N/A"): String {
    if (value == null || value.isNaN()) return fallback
    return if (decimals == 0) {
        value.toInt().toString()
    } else {
        String.format(Locale.US, "%.${decimals}f", value)
    }
}

object SupabaseApi {
    private const val BASE_URL = "https://tidmdjnjhqptszyubvue.supabase.co"
    private const val ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InRpZG1kam5qaHFwdHN6eXVidnVlIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODk1NDMwMzEsImV4cCI6MjEwNTExOTAzMX0.G2VeWnkN5nexmaYKdA1jI9AjwM-sw1yPnF7uHlnkT20"

    @Volatile
    var lastNetworkError: String? = null
        private set

    private fun friendlyNetworkMessage(e: Exception): String {
        val msg = e.localizedMessage.orEmpty()
        return when {
            e is java.net.SocketTimeoutException || msg.contains("timeout", ignoreCase = true) ->
                "Sinyal Wi-Fi / internet lemah (timeout ke Supabase)"
            e is java.net.UnknownHostException || e is java.net.ConnectException ->
                "Koneksi internet tidak stabil / server tidak terjangkau"
            else -> "Gangguan jaringan: ${msg.ifBlank { e.javaClass.simpleName }}"
        }
    }

    private fun executeGetJsonArray(pathWithQuery: String, timeoutMs: Int = 12000): JSONArray {
        var lastEx: Exception? = null
        for (attempt in 1..2) {
            var conn: HttpURLConnection? = null
            try {
                val endpoint = URL(BASE_URL + pathWithQuery)
                conn = (endpoint.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    setRequestProperty("apikey", ANON_KEY)
                    setRequestProperty("Authorization", "Bearer " + ANON_KEY)
                    connectTimeout = timeoutMs
                    readTimeout = maxOf(timeoutMs, 15000)
                }
                val code = conn.responseCode
                if (code == 200) {
                    val body = conn.inputStream.bufferedReader().use(BufferedReader::readText)
                    lastNetworkError = null
                    return JSONArray(body)
                } else {
                    lastNetworkError = "Server merespons HTTP $code"
                    return JSONArray()
                }
            } catch (e: Exception) {
                lastEx = e
                if (attempt < 2) {
                    try {
                        Thread.sleep(500)
                    } catch (_: InterruptedException) {
                    }
                }
            } finally {
                try {
                    conn?.disconnect()
                } catch (_: Exception) {
                }
            }
        }
        if (lastEx != null) {
            lastNetworkError = friendlyNetworkMessage(lastEx)
            throw lastEx
        }
        return JSONArray()
    }

    suspend fun getDevices(): JSONArray = withContext(Dispatchers.IO) {
        try {
            executeGetJsonArray("/rest/v1/devices?select=*&order=pond_number.asc")
        } catch (e: Exception) {
            e.printStackTrace()
            JSONArray()
        }
    }

    suspend fun upsertDevice(id: String, name: String, pondNumber: Int, commodity: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val endpoint = URL(BASE_URL + "/rest/v1/devices")
            val conn = (endpoint.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("apikey", ANON_KEY)
                setRequestProperty("Authorization", "Bearer " + ANON_KEY)
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Prefer", "resolution=merge-duplicates")
                doOutput = true
                connectTimeout = 8000
                readTimeout = 8000
            }
            val payload = JSONObject().apply {
                put("id", id)
                put("name", name)
                put("pond_number", pondNumber)
                put("commodity", commodity)
                put("status", "NORMAL")
                put("is_connected", true)
                put("battery_percent", 100)
                put("signal_quality", "Kuat")
            }
            OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }
            conn.responseCode in 200..204
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun deleteDevice(deviceId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val endpoint = URL(BASE_URL + "/rest/v1/devices?id=eq." + deviceId)
            val conn = (endpoint.openConnection() as HttpURLConnection).apply {
                requestMethod = "DELETE"
                setRequestProperty("apikey", ANON_KEY)
                setRequestProperty("Authorization", "Bearer " + ANON_KEY)
                connectTimeout = 8000
                readTimeout = 8000
            }
            conn.responseCode in 200..204
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun getLiveDeviceTelemetry(deviceId: String): JSONObject? = withContext(Dispatchers.IO) {
        try {
            val logs = executeGetJsonArray("/rest/v1/sensor_logs?device_id=eq.$deviceId&select=*&order=created_at.desc&limit=1", 6000)
            if (logs.length() > 0) return@withContext logs.getJSONObject(0)

            val dev = executeGetJsonArray("/rest/v1/devices?id=eq.$deviceId&select=*", 6000)
            if (dev.length() > 0) dev.getJSONObject(0) else null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getLatestSensorLogs(limit: Int = 50): JSONArray = withContext(Dispatchers.IO) {
        try {
            executeGetJsonArray("/rest/v1/sensor_logs?select=*&order=created_at.desc&limit=$limit")
        } catch (e: Exception) {
            e.printStackTrace()
            JSONArray()
        }
    }

    suspend fun getLatestSensorLog(deviceId: String): JSONObject? = withContext(Dispatchers.IO) {
        try {
            val array = executeGetJsonArray("/rest/v1/sensor_logs?device_id=eq.$deviceId&select=*&order=created_at.desc&limit=1")
            if (array.length() > 0) array.getJSONObject(0) else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun getSensorLogs(deviceId: String, limit: Int = 20): JSONArray = withContext(Dispatchers.IO) {
        try {
            executeGetJsonArray("/rest/v1/sensor_logs?device_id=eq.$deviceId&select=*&order=created_at.desc&limit=$limit")
        } catch (e: Exception) {
            e.printStackTrace()
            JSONArray()
        }
    }

    suspend fun getSensorLogsByPond(pondNumber: Int, limit: Int = 30): JSONArray = withContext(Dispatchers.IO) {
        try {
            executeGetJsonArray("/rest/v1/sensor_logs?pond_number=eq.$pondNumber&select=*&order=created_at.desc&limit=$limit")
        } catch (e: Exception) {
            e.printStackTrace()
            JSONArray()
        }
    }

    suspend fun getDeviceCalibration(deviceId: String): JSONObject? = withContext(Dispatchers.IO) {
        try {
            val array = executeGetJsonArray("/rest/v1/devices?id=eq.$deviceId&select=*", 6000)
            if (array.length() > 0) array.getJSONObject(0) else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun updateDeviceCalibration(deviceId: String, calibrationJson: JSONObject): Boolean = withContext(Dispatchers.IO) {
        fun patchPayload(payload: JSONObject): Int {
            val endpoint = URL(BASE_URL + "/rest/v1/devices?id=eq." + deviceId)
            val conn = (endpoint.openConnection() as HttpURLConnection).apply {
                requestMethod = "PATCH"
                setRequestProperty("apikey", ANON_KEY)
                setRequestProperty("Authorization", "Bearer " + ANON_KEY)
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Prefer", "return=minimal")
                doOutput = true
                connectTimeout = 8000
                readTimeout = 8000
            }
            return try {
                OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }
                conn.responseCode
            } finally {
                conn.disconnect()
            }
        }

        try {
            val phObj = calibrationJson.optJSONObject("ph")
            val tdsObj = calibrationJson.optJSONObject("tds")
            val turbObj = calibrationJson.optJSONObject("turbidity")
            val doObj = calibrationJson.optJSONObject("do")

            val corePayload = JSONObject().apply {
                if (phObj != null) {
                    put("ph_slope", phObj.optDouble("slope", -5.70))
                    put("ph_offset", phObj.optDouble("offset", 21.34))
                }
                if (tdsObj != null) {
                    put("tds_slope", tdsObj.optDouble("slope", 500.0))
                    put("tds_offset", tdsObj.optDouble("offset", 0.0))
                }
                if (turbObj != null) {
                    put("turb_slope", turbObj.optDouble("slope", -1120.4))
                    put("turb_offset", turbObj.optDouble("offset", 4895.87))
                }
            }

            val extendedPayload = JSONObject(corePayload.toString()).apply {
                if (doObj != null) {
                    put("do_slope", doObj.optDouble("slope", 5.5))
                    put("do_offset", doObj.optDouble("offset", 0.0))
                }
            }

            var code = patchPayload(extendedPayload)
            if (code == 400 && corePayload.length() > 0) {
                code = patchPayload(corePayload)
            }
            code in 200..204
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
