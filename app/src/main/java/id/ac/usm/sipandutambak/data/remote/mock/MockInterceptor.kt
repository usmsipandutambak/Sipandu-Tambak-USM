package id.ac.usm.sipandutambak.data.remote.mock

import android.content.Context
import android.util.Log
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import java.io.IOException

class MockInterceptor(private val context: Context) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val path = request.url.encodedPath
        val method = request.method

        Log.d("MockInterceptor", "Intercepting: $method $path")

        // Simulasi respon berhasil untuk request mutasi (PUT / POST)
        if (method == "PUT" || method == "POST") {
            return Response.Builder()
                .code(200)
                .message("OK (Mock Write Success)")
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .body("{\"message\":\"Berhasil disimpan (Mock)\"}".toResponseBody("application/json".toMediaTypeOrNull()))
                .addHeader("content-type", "application/json")
                .build()
        }

        val assetPath = when {
            path.endsWith("/devices") -> "mock/devices.json"
            path.contains("/devices/") && path.endsWith("/history") -> "mock/devices/alt-a/history.json"
            path.contains("/devices/") && path.endsWith("/readings") -> "mock/devices/alt-a/history.json"
            path.matches(Regex(".*/devices/[^/]+$")) -> "mock/device_detail.json"
            path.endsWith("/ponds") -> "mock/ponds.json"
            path.endsWith("/alerts") -> "mock/alerts.json"
            else -> null
        }

        return if (assetPath != null) {
            loadAssetResponse(chain, assetPath)
        } else {
            chain.proceed(request)
        }
    }

    private fun loadAssetResponse(chain: Interceptor.Chain, assetPath: String): Response {
        return try {
            val jsonString = context.assets.open(assetPath).bufferedReader().use { it.readText() }
            Response.Builder()
                .code(200)
                .message("OK")
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .body(jsonString.toResponseBody("application/json".toMediaTypeOrNull()))
                .addHeader("content-type", "application/json")
                .build()
        } catch (e: IOException) {
            Log.e("MockInterceptor", "File mock tidak ditemukan: $assetPath", e)
            Response.Builder()
                .code(404)
                .message("Mock Asset Not Found: $assetPath")
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .body("{\"code\":404,\"message\":\"Mock file not found: $assetPath\"}".toResponseBody("application/json".toMediaTypeOrNull()))
                .addHeader("content-type", "application/json")
                .build()
        }
    }
}
