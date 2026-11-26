package id.ac.usm.sipandutambak.ui.devices.detail

import android.app.TimePickerDialog
import android.content.Context
import android.net.Uri
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.ac.usm.sipandutambak.R
import id.ac.usm.sipandutambak.util.CsvExportHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private const val CAMERA_PREFS_NAME = "sipandu_camera_prefs"
private const val KEY_STREAM_URL = "camera_stream_url"
private const val KEY_RESOLUTION = "camera_resolution"
private const val KEY_TARGET_FPS = "camera_target_fps"
private const val KEY_SCALE_COVER = "camera_scale_cover"
private const val KEY_NON_STOP_MODE = "camera_non_stop_mode"
private const val KEY_SHOW_DETECTION_OVERLAY = "camera_show_detection_overlay"
private const val KEY_AUTO_CAMERA_ENABLED = "camera_auto_enabled"
private const val KEY_AUTO_SCHEDULES = "camera_auto_schedules"
private const val DEFAULT_STREAM_URL = "http://192.168.100.41:5000/video_feed"
private const val DEFAULT_SCHEDULES = "10:00:00,11:00:00,13:00:00,14:00:00,17:00:00"

enum class CameraResolutionOption(
    val id: String,
    val label: String,
    val badgeLabel: String,
    val description: String
) {
    SD_480P("480p", "480p (640x480)", "SD 480p", "Direkomendasikan untuk inferensi YOLO paling lancar"),
    HD_720P("720p", "720p (1280x720)", "HD 720p", "Kualitas tajam standar"),
    FHD_1080P("1080p", "1080p (1920x1080)", "FHD 1080p", "Resolusi maksimal kamera");

    companion object {
        fun fromId(id: String?): CameraResolutionOption =
            entries.firstOrNull { it.id == id } ?: HD_720P
    }
}

enum class CameraFpsOption(
    val id: String,
    val label: String,
    val badgeLabel: String
) {
    FPS_5("5", "5 FPS (Stabil AI)", "5 FPS"),
    FPS_10("10", "10 FPS", "10 FPS"),
    FPS_15("15", "15 FPS", "15 FPS"),
    AUTO("auto", "Auto Jetson", "AUTO");

    companion object {
        fun fromId(id: String?): CameraFpsOption =
            entries.firstOrNull { it.id == id } ?: FPS_5
    }
}

private data class JetsonServerStatus(
    val isChecking: Boolean = true,
    val isOnline: Boolean = false,
    val httpCode: Int? = null,
    val message: String = "Memeriksa koneksi...",
    val liveFps: Double? = null,
    val fishCount: Int? = null
)

private fun extractHostPort(streamUrl: String): String {
    return try {
        val uri = Uri.parse(streamUrl.trim())
        val host = uri.host
        val port = uri.port
        when {
            !host.isNullOrBlank() && port > 0 -> "$host:$port"
            !host.isNullOrBlank() -> host
            else -> streamUrl.trim()
        }
    } catch (_: Exception) {
        streamUrl.trim()
    }
}

private fun extractBaseUrl(streamUrl: String): String? {
    return try {
        val uri = Uri.parse(streamUrl.trim())
        val scheme = uri.scheme ?: return null
        val host = uri.host ?: return null
        if (uri.port > 0) "$scheme://$host:${uri.port}" else "$scheme://$host"
    } catch (_: Exception) {
        null
    }
}

private suspend fun checkJetsonConnection(streamUrl: String): JetsonServerStatus = withContext(Dispatchers.IO) {
    val target = streamUrl.trim()
    if (target.isBlank()) {
        return@withContext JetsonServerStatus(
            isChecking = false,
            isOnline = false,
            message = "URL stream kosong"
        )
    }

    var code: Int? = null
    var errorMsg: String? = null
    var conn: HttpURLConnection? = null
    try {
        conn = (URL(target).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 6000
            readTimeout = 6000
            instanceFollowRedirects = true
        }
        code = conn.responseCode
    } catch (e: Exception) {
        errorMsg = e.localizedMessage ?: "Timeout / sinyal lemah"
    } finally {
        try {
            conn?.disconnect()
        } catch (_: Exception) {
        }
    }

    val isOnline = code != null && code in 200..399
    var liveFps: Double? = null
    var fishCount: Int? = null

    val baseUrl = extractBaseUrl(target)
    if (isOnline && baseUrl != null) {
        for (path in listOf("/api/status", "/status")) {
            var statusConn: HttpURLConnection? = null
            try {
                statusConn = (URL("$baseUrl$path").openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 3000
                    readTimeout = 3000
                }
                if (statusConn.responseCode in 200..299) {
                    val body = statusConn.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(body)
                    val streamObj = json.optJSONObject("stream")
                    if (streamObj != null && streamObj.has("fps")) {
                        liveFps = streamObj.optDouble("fps")
                    } else if (json.has("fps")) {
                        liveFps = json.optDouble("fps")
                    }
                    if (json.has("fish_count")) {
                        fishCount = json.optInt("fish_count")
                    } else if (json.has("total_ikan")) {
                        fishCount = json.optInt("total_ikan")
                    }
                    break
                }
            } catch (_: Exception) {
            } finally {
                try {
                    statusConn?.disconnect()
                } catch (_: Exception) {
                }
            }
        }
    }

    JetsonServerStatus(
        isChecking = false,
        isOnline = isOnline,
        httpCode = code,
        message = when {
            isOnline -> "Terhubung (HTTP $code)"
            code != null -> "Server merespons HTTP $code"
            else -> "Gagal terhubung (${errorMsg ?: "Offline"})"
        },
        liveFps = liveFps?.takeIf { !it.isNaN() },
        fishCount = fishCount
    )
}

private fun escapeHtmlAttribute(value: String): String = value
    .replace("&", "&amp;")
    .replace("\"", "&quot;")
    .replace("'", "&#39;")
    .replace("<", "&lt;")
    .replace(">", "&gt;")

private fun buildMjpegStreamHtml(streamUrl: String, useCoverScale: Boolean): String {
    val safeUrl = escapeHtmlAttribute(streamUrl.trim())
    val objectFit = if (useCoverScale) "cover" else "contain"
    return """
        <html>
        <body style='margin:0;padding:0;background:#0F172A;display:flex;justify-content:center;align-items:center;height:100vh;overflow:hidden;font-family:sans-serif;color:#E2E8F0;position:relative;'>
          <div id='status' style='position:absolute;inset:0;display:flex;flex-direction:column;justify-content:center;align-items:center;text-align:center;padding:16px;background:#0F172A;z-index:2;'>
            <div id='msg' style='font-size:13px;font-weight:600;color:#CBD5E1;'>Menghubungkan stream kamera Jetson...</div>
            <button id='retry' onclick='reloadStream()' style='display:none;margin-top:10px;padding:6px 14px;border:none;border-radius:8px;background:#2563EB;color:#fff;font-size:12px;font-weight:700;'>Muat Ulang Stream</button>
          </div>
          <img id='cam' src='$safeUrl' style='width:100%;height:100%;object-fit:$objectFit;display:block;' />
          <script>
            var loaded = false;
            var img = document.getElementById('cam');
            var st = document.getElementById('status');
            var msg = document.getElementById('msg');
            var btn = document.getElementById('retry');
            function showErr(txt) {
              if (loaded) return;
              st.style.display = 'flex';
              msg.innerText = txt;
              msg.style.color = '#FBBF24';
              btn.style.display = 'inline-block';
            }
            function reloadStream() {
              loaded = false;
              msg.innerText = 'Menghubungkan ulang stream...';
              msg.style.color = '#CBD5E1';
              btn.style.display = 'none';
              var sep = '$safeUrl'.indexOf('?') >= 0 ? '&' : '?';
              img.src = '$safeUrl' + sep + 'cb=' + Date.now();
            }
            img.onload = function() { loaded = true; st.style.display = 'none'; };
            img.onerror = function() { loaded = false; showErr('Sinyal lemah / stream Jetson tidak terjangkau'); };
            setTimeout(function() { if (!loaded) showErr('Koneksi stream lambat (timeout). Pastikan Jetson & Wi-Fi aktif.'); }, 10000);
          </script>
        </body>
        </html>
    """.trimIndent()
}

enum class CameraState {
    STANDBY,
    RECORDING,
    COOLDOWN
}

data class CameraLogItem(
    val no: Int,
    val jam: String,
    val ikan: Int,
    val nonIkan: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraMonitoringScreen(
    deviceName: String = "Sipandu A",
    pondName: String = "Kolam 1",
    commodity: String = "LELE",
    onBackClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val cameraPrefs = remember(context) {
        context.getSharedPreferences(CAMERA_PREFS_NAME, Context.MODE_PRIVATE)
    }
    var cameraState by remember { mutableStateOf(CameraState.STANDBY) }
    var recordingSecondsLeft by remember { mutableStateOf(900) }
    var elapsedLiveSeconds by remember { mutableStateOf(0) }
    var cooldownSecondsLeft by remember { mutableStateOf(60) }
    var remainingQuota by remember { mutableStateOf(5) }
    var isAutoCameraEnabled by remember {
        mutableStateOf(cameraPrefs.getBoolean(KEY_AUTO_CAMERA_ENABLED, true))
    }
    var autoSchedules by remember {
        mutableStateOf(
            (cameraPrefs.getString(KEY_AUTO_SCHEDULES, DEFAULT_SCHEDULES) ?: DEFAULT_SCHEDULES)
                .split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .ifEmpty { DEFAULT_SCHEDULES.split(",") }
        )
    }
    var jetsonStreamUrl by remember {
        val rawSaved = cameraPrefs.getString(KEY_STREAM_URL, DEFAULT_STREAM_URL)?.trim().orEmpty()
        val migrated = if (rawSaved.isBlank() || rawSaved == "http://10.146.87.247:5000/video_feed") {
            if (rawSaved == "http://10.146.87.247:5000/video_feed") {
                cameraPrefs.edit().putString(KEY_STREAM_URL, DEFAULT_STREAM_URL).apply()
            }
            DEFAULT_STREAM_URL
        } else {
            rawSaved
        }
        mutableStateOf(migrated)
    }
    var selectedResolution by remember {
        mutableStateOf(
            CameraResolutionOption.fromId(
                cameraPrefs.getString(KEY_RESOLUTION, CameraResolutionOption.HD_720P.id)
            )
        )
    }
    var selectedFps by remember {
        mutableStateOf(
            CameraFpsOption.fromId(
                cameraPrefs.getString(KEY_TARGET_FPS, CameraFpsOption.FPS_5.id)
            )
        )
    }
    var useCoverScale by remember {
        mutableStateOf(cameraPrefs.getBoolean(KEY_SCALE_COVER, true))
    }
    var isNonStopMode by remember {
        mutableStateOf(cameraPrefs.getBoolean(KEY_NON_STOP_MODE, true))
    }
    var showDetectionOverlay by remember {
        mutableStateOf(cameraPrefs.getBoolean(KEY_SHOW_DETECTION_OVERLAY, true))
    }

    var serverStatus by remember { mutableStateOf(JetsonServerStatus()) }
    var statusCheckTrigger by remember { mutableStateOf(0) }
    var showConfigModal by remember { mutableStateOf(false) }

    BackHandler(enabled = !showConfigModal) {
        onBackClick()
    }

    LaunchedEffect(jetsonStreamUrl, statusCheckTrigger, cameraState) {
        serverStatus = serverStatus.copy(isChecking = true)
        serverStatus = checkJetsonConnection(jetsonStreamUrl)
        while (cameraState == CameraState.STANDBY) {
            delay(10000)
            serverStatus = checkJetsonConnection(jetsonStreamUrl)
        }
    }

    LaunchedEffect(cameraState, isNonStopMode) {
        if (cameraState == CameraState.RECORDING) {
            if (isNonStopMode) {
                elapsedLiveSeconds = 0
                while (cameraState == CameraState.RECORDING && isNonStopMode) {
                    delay(1000)
                    elapsedLiveSeconds++
                }
            } else {
                recordingSecondsLeft = 900
                while (recordingSecondsLeft > 0 && cameraState == CameraState.RECORDING && !isNonStopMode) {
                    delay(1000)
                    recordingSecondsLeft--
                }
                if (cameraState == CameraState.RECORDING && !isNonStopMode) {
                    cameraState = CameraState.COOLDOWN
                }
            }
        } else if (cameraState == CameraState.COOLDOWN) {
            if (isNonStopMode) {
                cameraState = CameraState.STANDBY
            } else {
                cooldownSecondsLeft = 60
                while (cooldownSecondsLeft > 0 && cameraState == CameraState.COOLDOWN) {
                    delay(1000)
                    cooldownSecondsLeft--
                }
                if (cameraState == CameraState.COOLDOWN) {
                    cameraState = CameraState.STANDBY
                }
            }
        }
    }

    val cameraLogs = remember {
        listOf(
            CameraLogItem(1, "10:00", 1, 1),
            CameraLogItem(2, "11:00", 1, 5),
            CameraLogItem(3, "12:00", 1, 7),
            CameraLogItem(4, "13:00", 1, 4)
        )
    }

    val totalIkan = cameraLogs.sumOf { it.ikan }
    val totalNonIkan = cameraLogs.sumOf { it.nonIkan }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFDCEBFE))
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "SIPANDU",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF453636),
                            letterSpacing = 0.5.sp,
                            lineHeight = 22.sp
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(id = R.drawable.famicons_fish),
                                contentDescription = "Logo Ikan",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.size(width = 36.dp, height = 24.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "TAMBAK",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF453636),
                                letterSpacing = 0.5.sp,
                                lineHeight = 22.sp
                            )
                        }
                    }
                }
            }

            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "MONITORING KAMERA",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B),
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${deviceName.replace("Sipandu", "ALAT").uppercase()} - ${pondName.uppercase()}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = commodity.uppercase(),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF0F172A)
                    )
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFF10B981), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Kondisi Kolam Normal",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "Kualitas air dalam batas aman",
                                fontSize = 11.5.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                }
            }

            item {
                val streamSignature = "$jetsonStreamUrl|$useCoverScale"
                val fpsBadgeText = serverStatus.liveFps?.let { "${it} FPS" } ?: selectedFps.badgeLabel

                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    border = BorderStroke(1.5.dp, Color(0xFF334155)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(230.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (jetsonStreamUrl.isNotBlank() && cameraState == CameraState.RECORDING) {
                            AndroidView(
                                factory = { ctx ->
                                    WebView(ctx).apply {
                                        layoutParams = ViewGroup.LayoutParams(
                                            ViewGroup.LayoutParams.MATCH_PARENT,
                                            ViewGroup.LayoutParams.MATCH_PARENT
                                        )
                                        setBackgroundColor(android.graphics.Color.parseColor("#0F172A"))
                                        settings.javaScriptEnabled = true
                                        settings.loadWithOverviewMode = true
                                        settings.useWideViewPort = true
                                        settings.domStorageEnabled = true
                                        settings.cacheMode = WebSettings.LOAD_NO_CACHE
                                        settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                        webViewClient = object : WebViewClient() {
                                            override fun onReceivedError(
                                                view: WebView?,
                                                request: android.webkit.WebResourceRequest?,
                                                error: android.webkit.WebResourceError?
                                            ) {
                                                if (request?.isForMainFrame == false) return
                                                val desc = error?.description?.toString() ?: "Koneksi terputus"
                                                view?.loadDataWithBaseURL(
                                                    null,
                                                    "<html><body style='margin:0;background:#0F172A;color:#FBBF24;display:flex;flex-direction:column;justify-content:center;align-items:center;height:100vh;font-family:sans-serif;text-align:center;padding:16px;'><div style='font-size:13px;font-weight:700;'>Sinyal lemah / gagal memuat kamera</div><div style='font-size:11px;color:#94A3B8;margin-top:4px;'>" + desc + "</div></body></html>",
                                                    "text/html",
                                                    "UTF-8",
                                                    null
                                                )
                                            }
                                        }
                                        webChromeClient = WebChromeClient()
                                        tag = streamSignature
                                        loadDataWithBaseURL(
                                            null,
                                            buildMjpegStreamHtml(jetsonStreamUrl, useCoverScale),
                                            "text/html",
                                            "UTF-8",
                                            null
                                        )
                                    }
                                },
                                update = { webView ->
                                    if (webView.tag != streamSignature) {
                                        webView.tag = streamSignature
                                        webView.loadDataWithBaseURL(
                                            null,
                                            buildMjpegStreamHtml(jetsonStreamUrl, useCoverScale),
                                            "text/html",
                                            "UTF-8",
                                            null
                                        )
                                    }
                                },
                                onRelease = { webView ->
                                    webView.stopLoading()
                                    webView.loadUrl("about:blank")
                                    webView.destroy()
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            StandbyCameraStatusView(
                                streamUrl = jetsonStreamUrl,
                                serverStatus = serverStatus,
                                resolution = selectedResolution,
                                targetFps = selectedFps,
                                isCooldown = cameraState == CameraState.COOLDOWN,
                                onRefreshStatus = { statusCheckTrigger++ },
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (cameraState == CameraState.RECORDING) Color(0xFFDC2626) else Color(0xCC000000)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .background(
                                                if (cameraState == CameraState.RECORDING) Color.White
                                                else if (serverStatus.isOnline) Color(0xFF22C55E)
                                                else Color(0xFFEF4444),
                                                CircleShape
                                            )
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = if (cameraState == CameraState.RECORDING) "LIVE REC" else "STANDBY",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xCCFFFFFF)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val statusDotColor = when {
                                        serverStatus.isChecking -> Color(0xFFF59E0B)
                                        serverStatus.isOnline -> Color(0xFF16A34A)
                                        else -> Color(0xFFDC2626)
                                    }
                                    val statusLabel = when {
                                        serverStatus.isChecking -> "CEK JETSON"
                                        serverStatus.isOnline -> "JETSON ONLINE"
                                        else -> "JETSON OFFLINE"
                                    }
                                    Box(modifier = Modifier.size(6.dp).background(statusDotColor, CircleShape))
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = "$statusLabel | ${selectedResolution.badgeLabel}",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A)
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xCC000000)
                                ) {
                                    Text(
                                        text = "${selectedResolution.badgeLabel} • $fpsBadgeText",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }

                                if (serverStatus.fishCount != null) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xCC0284C7)
                                    ) {
                                        Text(
                                            text = "Ikan: ${serverStatus.fishCount}",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                } else if (showDetectionOverlay) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xCC0F172A)
                                    ) {
                                        Text(
                                            text = "AI Overlay",
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF38BDF8),
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }

                            Surface(
                                onClick = { showConfigModal = true },
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xCC000000),
                                modifier = Modifier.size(30.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = "Setting",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                when (cameraState) {
                    CameraState.STANDBY -> {
                        Surface(
                            onClick = {
                                if (isNonStopMode || remainingQuota > 0) {
                                    if (!isNonStopMode) {
                                        remainingQuota--
                                    }
                                    cameraState = CameraState.RECORDING
                                } else {
                                    Toast.makeText(context, "Batas kuota harian kamera telah habis", Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF16A34A),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "Hidupkan Kamera Manual",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                    CameraState.RECORDING -> {
                        Surface(
                            onClick = {
                                cameraState = if (isNonStopMode) CameraState.STANDBY else CameraState.COOLDOWN
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFDC2626),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = if (isNonStopMode) "Matikan Live Kamera" else "Kamera Sedang Merekam",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                    CameraState.COOLDOWN -> {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFD1D5DB),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "Tunggu Jeda Selesai",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF4B5563)
                                )
                            }
                        }
                    }
                }
            }

            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (isNonStopMode) {
                        val statusLabel = when (cameraState) {
                            CameraState.RECORDING -> "Durasi live streaming aktif"
                            else -> "Mode Monitoring Non-Stop (Tanpa Batas)"
                        }
                        val totalSecs = if (cameraState == CameraState.RECORDING) elapsedLiveSeconds else 0
                        val hours = totalSecs / 3600
                        val minutes = (totalSecs % 3600) / 60
                        val seconds = totalSecs % 60

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = statusLabel,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF334155)
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                DigitBox(hours / 10)
                                DigitBox(hours % 10)
                                Text(":", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                DigitBox(minutes / 10)
                                DigitBox(minutes % 10)
                                Text(":", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                DigitBox(seconds / 10)
                                DigitBox(seconds % 10)
                            }
                        }
                    } else {
                        val statusLabel = when (cameraState) {
                            CameraState.RECORDING -> "Kamera akan mati dalam"
                            CameraState.COOLDOWN -> "Kamera bisa digunakan kembali dalam"
                            CameraState.STANDBY -> "Kamera siap dinyalakan"
                        }

                        val totalSecs = when (cameraState) {
                            CameraState.RECORDING -> recordingSecondsLeft
                            CameraState.COOLDOWN -> cooldownSecondsLeft
                            CameraState.STANDBY -> 0
                        }

                        val hours = totalSecs / 3600
                        val minutes = (totalSecs % 3600) / 60
                        val seconds = totalSecs % 60

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = statusLabel,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF334155)
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                DigitBox(hours / 10)
                                DigitBox(hours % 10)
                                Text(":", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                DigitBox(minutes / 10)
                                DigitBox(minutes % 10)
                                Text(":", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                DigitBox(seconds / 10)
                                DigitBox(seconds % 10)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Kesempatan menggunakan kamera",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF334155)
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                DigitBox(remainingQuota)
                                Text("/", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                DigitBox(5)
                            }
                        }
                    }
                }
            }

            item {
                Surface(
                    onClick = { showConfigModal = true },
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFDCEBFE),
                    border = BorderStroke(1.2.dp, Color(0xFF93C5FD)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = Color(0xFF0369A1),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Setting Kamera",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0369A1)
                        )
                    }
                }
            }

            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Settingan kamera hidup otomatis",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE0EDFE)),
                        border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Atur waktu kamera hidup:",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF334155),
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )

                            autoSchedules.forEachIndexed { index, time ->
                                Surface(
                                    onClick = {
                                        val parts = time.split(":")
                                        val initHour = parts.getOrNull(0)?.toIntOrNull() ?: 10
                                        val initMin = parts.getOrNull(1)?.toIntOrNull() ?: 0
                                        TimePickerDialog(
                                            context,
                                            { _, hourOfDay, minute ->
                                                val updated = String.format("%02d:%02d:00", hourOfDay, minute)
                                                val nextList = autoSchedules.toMutableList().apply {
                                                    this[index] = updated
                                                }
                                                autoSchedules = nextList
                                                cameraPrefs.edit()
                                                    .putString(KEY_AUTO_SCHEDULES, nextList.joinToString(","))
                                                    .apply()
                                            },
                                            initHour,
                                            initMin,
                                            true
                                        ).show()
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color.White,
                                    border = BorderStroke(0.8.dp, Color(0xFFCBD5E1)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(38.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = time,
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0F172A)
                                        )
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit Waktu",
                                            tint = Color(0xFF0284C7),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Setel kamera otomatis",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF334155)
                                )
                                Switch(
                                    checked = isAutoCameraEnabled,
                                    onCheckedChange = { enabled ->
                                        isAutoCameraEnabled = enabled
                                        cameraPrefs.edit()
                                            .putBoolean(KEY_AUTO_CAMERA_ENABLED, enabled)
                                            .apply()
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = Color(0xFF38BDF8)
                                    )
                                )
                            }
                        }
                    }
                }
            }

            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "PENCATATAN TELEMETRI TERPADU",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF64748B),
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Data Realtime Kamera",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .height(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF059669))
                                .clickable {
                                    val headers = listOf("NO", "JAM", "IKAN", "NON-IKAN")
                                    val rows = cameraLogs.map { listOf(it.no.toString(), it.jam, it.ikan.toString(), it.nonIkan.toString()) }
                                    CsvExportHelper.exportAndShareCsv(
                                        context = context,
                                        fileName = "SIPANDU_Data_Kamera_${pondName.replace(" ", "_")}",
                                        headers = headers,
                                        rows = rows
                                    )
                                }
                                .padding(horizontal = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Export Excel",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Surface(
                                color = Color(0xFFE2EDFE),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp, horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("NO", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155), modifier = Modifier.weight(0.6f), textAlign = TextAlign.Center)
                                    Text("JAM", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155), modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                                    Text("IKAN", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155), modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                                    Text("NON-IKAN", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155), modifier = Modifier.weight(1.2f), textAlign = TextAlign.Center)
                                }
                            }

                            cameraLogs.forEach { log ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp, horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(log.no.toString(), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0284C7), modifier = Modifier.weight(0.6f), textAlign = TextAlign.Center)
                                    Text(log.jam, fontSize = 9.sp, color = Color(0xFF0F172A), modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                                    Text(log.ikan.toString(), fontSize = 9.sp, color = Color(0xFF0F172A), modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                                    Text(log.nonIkan.toString(), fontSize = 9.sp, color = Color(0xFF0F172A), modifier = Modifier.weight(1.2f), textAlign = TextAlign.Center)
                                }
                                HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 0.8.dp)
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp, horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Spacer(modifier = Modifier.weight(0.6f))
                                Text("TOTAL", fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0F172A), modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                                Text(totalIkan.toString(), fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0F172A), modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                                Text(totalNonIkan.toString(), fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0F172A), modifier = Modifier.weight(1.2f), textAlign = TextAlign.Center)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "*Catatan Objek Non-Ikan: Rumput, Ranting, dan Daun",
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF475569)
                    )
                }
            }
        }

        if (showConfigModal) {
            CameraConfigBottomSheet(
                currentUrl = jetsonStreamUrl,
                currentResolution = selectedResolution,
                currentFps = selectedFps,
                currentUseCoverScale = useCoverScale,
                currentNonStopMode = isNonStopMode,
                currentShowOverlay = showDetectionOverlay,
                currentAutoCameraEnabled = isAutoCameraEnabled,
                currentSchedules = autoSchedules,
                onDismiss = { showConfigModal = false },
                onSave = { newUrl, res, fps, coverScale, nonStop, overlayEnabled, autoEnabled ->
                    val sanitizedUrl = newUrl.trim().ifBlank { DEFAULT_STREAM_URL }
                    cameraPrefs.edit()
                        .putString(KEY_STREAM_URL, sanitizedUrl)
                        .putString(KEY_RESOLUTION, res.id)
                        .putString(KEY_TARGET_FPS, fps.id)
                        .putBoolean(KEY_SCALE_COVER, coverScale)
                        .putBoolean(KEY_NON_STOP_MODE, nonStop)
                        .putBoolean(KEY_SHOW_DETECTION_OVERLAY, overlayEnabled)
                        .putBoolean(KEY_AUTO_CAMERA_ENABLED, autoEnabled)
                        .apply()
                    jetsonStreamUrl = sanitizedUrl
                    selectedResolution = res
                    selectedFps = fps
                    useCoverScale = coverScale
                    isNonStopMode = nonStop
                    showDetectionOverlay = overlayEnabled
                    isAutoCameraEnabled = autoEnabled
                    statusCheckTrigger++
                    showConfigModal = false
                    Toast.makeText(context, "Konfigurasi Kamera Berhasil Disimpan", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}

@Composable
private fun DigitBox(digit: Int) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(0.8.dp, Color(0xFF0F172A)),
        color = Color.White,
        modifier = Modifier.size(width = 16.dp, height = 20.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = digit.toString(),
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF0F172A)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CameraConfigBottomSheet(
    currentUrl: String = DEFAULT_STREAM_URL,
    currentResolution: CameraResolutionOption = CameraResolutionOption.HD_720P,
    currentFps: CameraFpsOption = CameraFpsOption.FPS_5,
    currentUseCoverScale: Boolean = true,
    currentNonStopMode: Boolean = true,
    currentShowOverlay: Boolean = true,
    currentAutoCameraEnabled: Boolean = true,
    currentSchedules: List<String> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (
        newUrl: String,
        resolution: CameraResolutionOption,
        fps: CameraFpsOption,
        useCoverScale: Boolean,
        nonStopMode: Boolean,
        showOverlay: Boolean,
        autoCameraEnabled: Boolean
    ) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var streamUrlInput by remember(currentUrl) { mutableStateOf(currentUrl) }
    var selectedResolution by remember(currentResolution) { mutableStateOf(currentResolution) }
    var selectedFps by remember(currentFps) { mutableStateOf(currentFps) }
    var coverScaleToggle by remember(currentUseCoverScale) { mutableStateOf(currentUseCoverScale) }
    var nonStopToggle by remember(currentNonStopMode) { mutableStateOf(currentNonStopMode) }
    var overlayToggle by remember(currentShowOverlay) { mutableStateOf(currentShowOverlay) }
    var autoCameraToggle by remember(currentAutoCameraEnabled) { mutableStateOf(currentAutoCameraEnabled) }

    var resolutionExpanded by remember { mutableStateOf(false) }
    var fpsExpanded by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<JetsonServerStatus?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 10.dp)
                .padding(bottom = 30.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Setting Kamera Jetson Nano",
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF0F172A)
            )

            // 1. Alamat Server Stream Jetson + Tes Koneksi
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9)),
                border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "1. Alamat Server Stream Jetson (URL / IP)",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0284C7)
                    )
                    Text(
                        text = "Default: $DEFAULT_STREAM_URL",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
                    )

                    OutlinedTextField(
                        value = streamUrlInput,
                        onValueChange = {
                            streamUrlInput = it
                            testResult = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text(DEFAULT_STREAM_URL, fontSize = 12.sp) }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                val targetUrl = streamUrlInput.trim().ifBlank { DEFAULT_STREAM_URL }
                                testResult = JetsonServerStatus(isChecking = true, message = "Menguji koneksi...")
                                coroutineScope.launch {
                                    testResult = checkJetsonConnection(targetUrl)
                                }
                            },
                            enabled = testResult?.isChecking != true,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0369A1)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (testResult?.isChecking == true) "Menguji..." else "Tes Koneksi",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        TextButton(
                            onClick = {
                                streamUrlInput = DEFAULT_STREAM_URL
                                testResult = null
                            }
                        ) {
                            Text("Reset Default", fontSize = 11.5.sp, color = Color(0xFF475569))
                        }
                    }

                    testResult?.let { result ->
                        val bg = when {
                            result.isChecking -> Color(0xFFFEF3C7)
                            result.isOnline -> Color(0xFFDCFCE7)
                            else -> Color(0xFFFEE2E2)
                        }
                        val fg = when {
                            result.isChecking -> Color(0xFF92400E)
                            result.isOnline -> Color(0xFF166534)
                            else -> Color(0xFF991B1B)
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = bg,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = result.message,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = fg,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                            )
                        }
                    }
                }
            }

            // 2. Pilihan Resolusi & Target FPS Rexus SW-10
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "2. Resolusi & Target FPS (Rexus SW-10)",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0284C7)
                    )

                    Column {
                        Text(
                            "Resolusi Stream Jetson",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Surface(
                                onClick = { resolutionExpanded = true },
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                color = Color(0xFFF8FAFC),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(42.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(selectedResolution.label, fontSize = 12.sp, color = Color(0xFF1E293B))
                                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = Color(0xFF0284C7))
                                }
                            }
                            DropdownMenu(
                                expanded = resolutionExpanded,
                                onDismissRequest = { resolutionExpanded = false }
                            ) {
                                CameraResolutionOption.entries.forEach { option ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(option.label, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                                                Text(option.description, fontSize = 10.5.sp, color = Color(0xFF64748B))
                                            }
                                        },
                                        onClick = {
                                            selectedResolution = option
                                            resolutionExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Column {
                        Text(
                            "Target Frame Rate (FPS)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Surface(
                                onClick = { fpsExpanded = true },
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                color = Color(0xFFF8FAFC),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(42.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(selectedFps.label, fontSize = 12.sp, color = Color(0xFF1E293B))
                                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = Color(0xFF0284C7))
                                }
                            }
                            DropdownMenu(
                                expanded = fpsExpanded,
                                onDismissRequest = { fpsExpanded = false }
                            ) {
                                CameraFpsOption.entries.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option.label, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold) },
                                        onClick = {
                                            selectedFps = option
                                            fpsExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    ConfigToggleItem(
                        title = "Skala Penuh Kotak Video (Object-Fit Cover)",
                        description = "Aktif: penuhi bingkai (cover). Nonaktif: tampilkan seluruh frame utuh (contain).",
                        checked = coverScaleToggle,
                        onCheckedChange = { coverScaleToggle = it }
                    )

                    ConfigToggleItem(
                        title = "Tampilkan Overlay Deteksi AI",
                        description = "Tampilkan indikator overlay deteksi objek ikan & metadata di layar video.",
                        checked = overlayToggle,
                        onCheckedChange = { overlayToggle = it }
                    )
                }
            }

            // 3. Mode Operasional & Jadwal Otomatis
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "3. Mode Operasional & Jadwal Otomatis",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0284C7)
                    )

                    ConfigToggleItem(
                        title = "Mode Monitoring Non-Stop (Tanpa Batas Waktu & Kuota)",
                        description = "Sembunyikan batas kuota 5/5 & timer mundur 15 menit agar kamera dapat menyala terus.",
                        checked = nonStopToggle,
                        onCheckedChange = { nonStopToggle = it }
                    )

                    ConfigToggleItem(
                        title = "Aktifkan Jadwal Kamera Otomatis",
                        description = if (currentSchedules.isNotEmpty()) {
                            "Jadwal aktif: ${currentSchedules.joinToString(", ") { it.take(5) }} (ubah jam di kartu jadwal)"
                        } else {
                            "Hidupkan kamera otomatis sesuai jadwal pemantauan kolam."
                        },
                        checked = autoCameraToggle,
                        onCheckedChange = { autoCameraToggle = it }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Batal", fontSize = 13.sp, color = Color(0xFF64748B))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    onClick = {
                        onSave(
                            streamUrlInput,
                            selectedResolution,
                            selectedFps,
                            coverScaleToggle,
                            nonStopToggle,
                            overlayToggle,
                            autoCameraToggle
                        )
                    },
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF0284C7),
                    modifier = Modifier
                        .width(130.dp)
                        .height(42.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Simpan", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun ConfigToggleItem(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(0.8.dp, Color(0xFFE2E8F0)),
        color = Color(0xFFF8FAFC),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                Text(description, fontSize = 10.sp, color = Color(0xFF64748B), lineHeight = 13.sp)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFF1D4ED8)
                )
            )
        }
    }
}

@Composable
private fun StandbyCameraStatusView(
    streamUrl: String,
    serverStatus: JetsonServerStatus,
    resolution: CameraResolutionOption,
    targetFps: CameraFpsOption,
    isCooldown: Boolean,
    onRefreshStatus: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hostPort = remember(streamUrl) { extractHostPort(streamUrl) }
    val statusTitle = if (isCooldown) "Kamera Jeda (Cooldown)" else "Kamera Standby"
    val statusBadgeColor = when {
        serverStatus.isChecking -> Color(0xFFF59E0B)
        serverStatus.isOnline -> Color(0xFF22C55E)
        else -> Color(0xFFEF4444)
    }
    val statusText = when {
        serverStatus.isChecking -> "Memeriksa server Jetson..."
        serverStatus.isOnline -> "Server Jetson Online"
        else -> "Server Jetson Offline"
    }

    Box(
        modifier = modifier.background(Color(0xFF0F172A)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = statusTitle,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )

            Surface(
                onClick = onRefreshStatus,
                shape = RoundedCornerShape(999.dp),
                color = Color(0xFF1E293B),
                border = BorderStroke(1.dp, statusBadgeColor.copy(alpha = 0.7f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(statusBadgeColor, CircleShape)
                    )
                    Text(
                        text = statusText,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Cek Ulang Koneksi",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(13.dp)
                    )
                }
            }

            Text(
                text = "IP Server Aktif: $hostPort",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF38BDF8)
            )

            Text(
                text = "Profil: ${resolution.label} • ${targetFps.label}",
                fontSize = 10.5.sp,
                color = Color(0xFFCBD5E1),
                textAlign = TextAlign.Center
            )
        }
    }
}
