package id.ac.usm.sipandutambak.data.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object SupabaseApi {
    private const val BASE_URL = "https://tidmdjnjhqptszyubvue.supabase.co"
    private const val ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InRpZG1kam5qaHFwdHN6eXVidnVlIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODk1NDMwMzEsImV4cCI6MjEwNTExOTAzMX0.G2VeWnkN5nexmaYKdA1jI9AjwM-sw1yPnF7uHlnkT20"

    // 1. Ambil Semua Alat dari Supabase
    suspend fun getDevices(): JSONArray = withContext(Dispatchers.IO) {
        try {
            val endpoint = URL("$BASE_URL/rest/v1/devices?select=*&order=pond_number.asc")
            val conn = (endpoint.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("apikey", ANON_KEY)
                setRequestProperty("Authorization", "Bearer $ANON_KEY")
                connectTimeout = 8000
                readTimeout = 8000
            }

            if (conn.responseCode == 200) {
                val response = conn.inputStream.bufferedReader().use(BufferedReader::readText)
                JSONArray(response)
            } else {
                JSONArray()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            JSONArray()
        }
    }

    // 2. Simpan / Daftarkan Alat Baru (Upsert ke Supabase)
    suspend fun upsertDevice(
        id: String,
        name: String,
        pondNumber: Int,
        commodity: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val endpoint = URL("$BASE_URL/rest/v1/devices")
            val conn = (endpoint.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("apikey", ANON_KEY)
                setRequestProperty("Authorization", "Bearer $ANON_KEY")
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

    // 3. Hapus Alat Secara Nyata dari Supabase
    suspend fun deleteDevice(deviceId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val endpoint = URL("$BASE_URL/rest/v1/devices?id=eq.$deviceId")
            val conn = (endpoint.openConnection() as HttpURLConnection).apply {
                requestMethod = "DELETE"
                setRequestProperty("apikey", ANON_KEY)
                setRequestProperty("Authorization", "Bearer $ANON_KEY")
                connectTimeout = 8000
                readTimeout = 8000
            }
            conn.responseCode in 200..204
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // 4. Ambil Log Terakhir Hasil Kiriman ESP32
    suspend fun getLatestSensorLog(deviceId: String): JSONObject? = withContext(Dispatchers.IO) {
        try {
            val endpoint = URL("$BASE_URL/rest/v1/sensor_logs?device_id=eq.$deviceId&order=created_at.desc&limit=1")
            val conn = (endpoint.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("apikey", ANON_KEY)
                setRequestProperty("Authorization", "Bearer $ANON_KEY")
                connectTimeout = 8000
                readTimeout = 8000
            }

            if (conn.responseCode == 200) {
                val response = conn.inputStream.bufferedReader().use(BufferedReader::readText)
                val array = JSONArray(response)
                if (array.length() > 0) array.getJSONObject(0) else null
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // 5. Ambil Seluruh Riwayat Log untuk Tabel Detail
    suspend fun getSensorLogs(deviceId: String, limit: Int = 10): JSONArray = withContext(Dispatchers.IO) {
        try {
            val endpoint = URL("$BASE_URL/rest/v1/sensor_logs?device_id=eq.$deviceId&order=created_at.desc&limit=$limit")
            val conn = (endpoint.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("apikey", ANON_KEY)
                setRequestProperty("Authorization", "Bearer $ANON_KEY")
                connectTimeout = 8000
                readTimeout = 8000
            }

            if (conn.responseCode == 200) {
                val response = conn.inputStream.bufferedReader().use(BufferedReader::readText)
                JSONArray(response)
            } else {
                JSONArray()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            JSONArray()
        }
    }
}