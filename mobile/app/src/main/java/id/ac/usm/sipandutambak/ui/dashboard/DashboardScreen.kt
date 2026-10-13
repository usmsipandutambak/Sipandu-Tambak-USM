package id.ac.usm.sipandutambak.ui.dashboard

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import id.ac.usm.sipandutambak.R
import id.ac.usm.sipandutambak.data.api.SupabaseApi
import id.ac.usm.sipandutambak.ui.components.AppSidebarMenu
import id.ac.usm.sipandutambak.ui.devices.DeviceListViewModel
import id.ac.usm.sipandutambak.ui.util.AlertLevel
import id.ac.usm.sipandutambak.ui.util.ParamAlert
import id.ac.usm.sipandutambak.ui.util.WaterQualityRules
import id.ac.usm.sipandutambak.util.CsvExportHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class MapPondStatus {
    JERNIH, KERUH, STANDBY
}

// ---------------------------------------------------------------------------
// Data realtime kolam (dibaca langsung dari tabel devices di Supabase)
// ---------------------------------------------------------------------------

data class LivePond(
    val deviceId: String,
    val deviceName: String,
    val pondNumber: Int,
    val isConnected: Boolean,
    val ph: Double?,
    val tds: Double?,
    val temp: Double?,
    val doLevel: Double?,
    val alerts: List<ParamAlert>,
    val level: AlertLevel?
) {
    val pondName: String get() = "Kolam " + pondNumber

    val reasonText: String
        get() = alerts
            .filter { it.level != AlertLevel.NORMAL }
            .joinToString(", ") { a ->
                a.param + " " + fmtLive(a.value, 1) +
                        (if (a.unit.isNotEmpty()) " " + a.unit else "") +
                        " (" + a.level.label + ")"
            }
}

fun fmtLive(v: Double?, decimals: Int = 1): String =
    if (v == null) "-" else String.format(Locale.US, "%." + decimals + "f", v)

private fun JSONObject.nullableDouble(key: String): Double? {
    if (!has(key) || isNull(key)) return null
    val v = optDouble(key, Double.NaN)
    return if (v.isNaN()) null else v
}

suspend fun fetchLivePonds(): List<LivePond> {
    val arr = SupabaseApi.getDevices()
    val out = mutableListOf<LivePond>()
    for (i in 0 until arr.length()) {
        val o = arr.getJSONObject(i)
        val ph = o.nullableDouble("live_ph")
        val tds = o.nullableDouble("live_tds")
        val temp = o.nullableDouble("live_temp")
        val doLevel = o.nullableDouble("live_do")
        val alerts = WaterQualityRules.evaluateAll(ph, tds, temp, doLevel)

        out.add(
            LivePond(
                deviceId = o.optString("id", ""),
                deviceName = o.optString("name", "Sipandu"),
                pondNumber = o.optInt("pond_number", 1),
                isConnected = o.optBoolean("is_connected", false),
                ph = ph,
                tds = tds,
                temp = temp,
                doLevel = doLevel,
                alerts = alerts,
                level = if (alerts.isEmpty()) null else WaterQualityRules.overallStatus(alerts)
            )
        )
    }
    return out
}

// ---------------------------------------------------------------------------
// Layar Dashboard
// ---------------------------------------------------------------------------

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    deviceListViewModel: DeviceListViewModel = viewModel(),
    onPondClick: (Int) -> Unit = {},
    onAboutSystemClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val deviceListState by deviceListViewModel.uiState.collectAsState()
    val devices = deviceListState.devices

    val currentDate = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("id", "ID")).format(Date())
    var showSidebar by remember { mutableStateOf(false) }

    val anomalyScrollState = rememberScrollState()

    // Data sensor realtime dari Supabase (polling 5 detik)
    var livePonds by remember { mutableStateOf<List<LivePond>>(emptyList()) }
    LaunchedEffect(Unit) {
        while (true) {
            try {
                livePonds = withContext(Dispatchers.IO) { fetchLivePonds() }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            delay(5_000)
        }
    }

    // Status kolam dihitung dari nilai sensor (bukan dari kolom status)
    fun getPondStatus(num: Int): MapPondStatus {
        val p = livePonds.firstOrNull { it.pondNumber == num && it.isConnected }
        return when (p?.level) {
            null -> MapPondStatus.STANDBY
            AlertLevel.NORMAL -> MapPondStatus.JERNIH
            else -> MapPondStatus.KERUH
        }
    }

    val jernihCount = (1..28).count { getPondStatus(it) == MapPondStatus.JERNIH }
    val keruhCount = (1..28).count { getPondStatus(it) == MapPondStatus.KERUH }
    val totalAirKualitas = jernihCount + keruhCount

    val jernihPercent = if (totalAirKualitas > 0) (jernihCount * 100) / totalAirKualitas else 0
    val keruhPercent = if (totalAirKualitas > 0) 100 - jernihPercent else 0

    val keruhSweepAngle = if (totalAirKualitas > 0) (keruhCount.toFloat() / totalAirKualitas) * 360f else 0f
    val jernihSweepAngle = if (totalAirKualitas > 0) 360f - keruhSweepAngle else 0f

    val monitoredPonds = livePonds.filter { it.isConnected && it.level != null }
    val anomalyPonds = monitoredPonds
        .filter { it.level == AlertLevel.WASPADA || it.level == AlertLevel.BAHAYA }
        .sortedByDescending { it.level?.rank ?: 0 }

    val canScrollDown by remember {
        derivedStateOf {
            anomalyScrollState.value < anomalyScrollState.maxValue
        }
    }

    val activePonds = livePonds
        .filter { it.isConnected }
        .map { it.pondNumber }
        .filter { it in 1..28 }
        .toIntArray()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF1F6FE))
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 140.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header SIPANDU TAMBAK + Hamburger Menu
            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
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

                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.5.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { showSidebar = true }
                                .padding(4.dp)
                        ) {
                            repeat(3) {
                                Box(
                                    modifier = Modifier
                                        .width(26.dp)
                                        .height(3.dp)
                                        .background(Color(0xFF1E293B), RoundedCornerShape(2.dp))
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = currentDate,
                        fontSize = 12.sp,
                        color = Color(0xFF334155),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Banner Sambutan
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFDCEBFE)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Selamat Datang",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Pantau kondisi kualitas air tambak Anda secara real-time. Dapatkan informasi dan peringatan dini untuk menjaga ekosistem kolam agar tetap optimal.",
                            fontSize = 12.sp,
                            color = Color(0xFF334155),
                            lineHeight = 17.sp
                        )
                    }
                }
            }

            // 1. Monitoring Kualitas Kolam (Diagram Lingkaran)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Monitoring Kualitas Kolam",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0xFF0284C7), RoundedCornerShape(8.dp))
                            .background(Color(0xFFE0F2FE))
                            .clickable {
                                val headers = listOf(
                                    "ID ALAT", "NAMA ALAT", "KOLAM", "KOMODITAS",
                                    "STATUS", "KONEKSI", "BATERAI"
                                )
                                val rows = devices.map { dev ->
                                    val live = livePonds.firstOrNull { it.deviceId == dev.id }
                                    listOf(
                                        dev.id,
                                        dev.name,
                                        dev.pondName,
                                        dev.commodity,
                                        live?.level?.label ?: "-",
                                        if (dev.isConnected) "TERHUBUNG" else "TERPUTUS",
                                        "${dev.batteryPercent}%"
                                    )
                                }
                                CsvExportHelper.exportAndShareCsv(
                                    context = context,
                                    fileName = "SIPANDU_Ringkasan_Semua_Kolam",
                                    headers = headers,
                                    rows = rows
                                )
                            }
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Export Semua Data",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF0284C7)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier.size(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val diameter = size.minDimension
                                if (totalAirKualitas > 0) {
                                    drawArc(
                                        color = Color(0xFFF97316),
                                        startAngle = 45f,
                                        sweepAngle = keruhSweepAngle,
                                        useCenter = true,
                                        size = Size(diameter, diameter)
                                    )
                                    drawArc(
                                        color = Color(0xFF2563EB),
                                        startAngle = 45f + keruhSweepAngle,
                                        sweepAngle = jernihSweepAngle,
                                        useCenter = true,
                                        size = Size(diameter, diameter)
                                    )
                                } else {
                                    drawCircle(color = Color(0xFFCBD5E1), radius = diameter / 2f)
                                }
                            }

                            if (totalAirKualitas > 0) {
                                Column(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(top = 34.dp, end = 26.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("Jernih", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text(jernihCount.toString() + " kolam", color = Color.White, fontSize = 10.sp)
                                    Text(jernihPercent.toString() + "%", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                Column(
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .padding(top = 18.dp, end = 16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("Keruh", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    Text(keruhCount.toString() + " kolam", color = Color.White, fontSize = 11.sp)
                                    Text(keruhPercent.toString() + "%", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "SIAGA",
                                        color = Color(0xFF475569),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Text(
                                        text = "28 Kolam Standby",
                                        color = Color(0xFF64748B),
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        if (totalAirKualitas > 0) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(10.dp).background(Color(0xFF2563EB), CircleShape))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Jernih (" + jernihCount + " kolam) - " + jernihPercent + "%",
                                        fontSize = 10.5.sp,
                                        color = Color(0xFF334155),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(10.dp).background(Color(0xFFF97316), CircleShape))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Keruh (" + keruhCount + " kolam) - " + keruhPercent + "%",
                                        fontSize = 10.5.sp,
                                        color = Color(0xFF334155),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.size(8.dp).background(Color(0xFF94A3B8), CircleShape))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Belum ada data sensor terbaca",
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // 2. Peringatan Anomali Kualitas Air (dari nilai sensor realtime)
            item {
                Text(
                    text = "Peringatan Anomali Kualitas Air",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (anomalyPonds.isEmpty()) {
                    val hasReadings = monitoredPonds.isNotEmpty()
                    val cardBg = if (hasReadings) Color(0xFFECFDF5) else Color(0xFFF1F5F9)
                    val cardBorder = if (hasReadings) Color(0xFFA7F3D0) else Color(0xFFCBD5E1)
                    val iconBg = if (hasReadings) Color(0xFF10B981) else Color(0xFF94A3B8)
                    val titleColor = if (hasReadings) Color(0xFF065F46) else Color(0xFF475569)
                    val descColor = if (hasReadings) Color(0xFF047857) else Color(0xFF64748B)

                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = cardBg),
                        border = BorderStroke(1.dp, cardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = iconBg,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = if (hasReadings) "✓" else "-",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (hasReadings) "Kondisi Air Optimal" else "Belum Ada Data Sensor",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = titleColor
                                )
                                Text(
                                    text = if (hasReadings) {
                                        "Tidak ada anomali terdeteksi pada seluruh kolam yang terpantau."
                                    } else {
                                        "Belum ada alat aktif yang mengirim nilai sensor."
                                    },
                                    fontSize = 11.5.sp,
                                    color = descColor
                                )
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 225.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(anomalyScrollState),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            anomalyPonds.forEach { p ->
                                AnomalyPondCard(
                                    pondName = p.pondName,
                                    deviceName = p.deviceName,
                                    status = p.level?.label ?: "WASPADA",
                                    paramLine1 = "pH " + fmtLive(p.ph, 2) +
                                            " | DO " + fmtLive(p.doLevel, 1) +
                                            " | TDS " + fmtLive(p.tds, 0),
                                    paramLine2 = "Suhu " + fmtLive(p.temp, 1) +
                                            " | COD - | NH4+ - | NO3- -",
                                    reasonLine = p.reasonText
                                )
                            }
                        }

                        AnimatedVisibility(
                            visible = canScrollDown && anomalyPonds.size > 2,
                            enter = fadeIn(),
                            exit = fadeOut(),
                            modifier = Modifier.align(Alignment.BottomCenter)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(60.dp)
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color.Transparent,
                                                Color(0xDDF1F6FE),
                                                Color(0xFFF1F6FE)
                                            )
                                        )
                                    ),
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                ) {
                                    Text(
                                        text = "Bergulir ke bawah",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF64748B)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    DoubleChevronDownIcon()
                                }
                            }
                        }
                    }
                }
            }

            // 3. Denah Kolam
            item {
                Text(
                    text = "Denah Kolam",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
                Text(
                    text = "Tekan salah satu kolam untuk melihat detail kolam.",
                    fontSize = 11.sp,
                    color = Color(0xFF64748B),
                    modifier = Modifier.padding(top = 2.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 16.dp)
                    ) {
                        RealPondMapLayout(
                            activePondNumbers = activePonds,
                            statusResolver = { getPondStatus(it) },
                            onPondSelected = onPondClick
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        val activeCount = activePonds.size
                        val inactiveCount = 28 - activeCount
                        val standbyCount = 28 - totalAirKualitas

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(8.5.dp).background(Color(0xFF0284C7), RoundedCornerShape(2.dp)))
                                Spacer(modifier = Modifier.width(3.5.dp))
                                Text("Jernih (" + jernihCount + ")", fontSize = 8.5.sp, color = Color(0xFF334155), fontWeight = FontWeight.Medium)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(8.5.dp).background(Color(0xFFF97316), RoundedCornerShape(2.dp)))
                                Spacer(modifier = Modifier.width(3.5.dp))
                                Text("Keruh (" + keruhCount + ")", fontSize = 8.5.sp, color = Color(0xFF334155), fontWeight = FontWeight.Medium)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(8.5.dp).background(Color(0xFF94A3B8), RoundedCornerShape(2.dp)))
                                Spacer(modifier = Modifier.width(3.5.dp))
                                Text("Siaga (" + standbyCount + ")", fontSize = 8.5.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Medium)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(7.5.dp).clip(CircleShape).background(Color(0xFF31E924)))
                                Spacer(modifier = Modifier.width(3.5.dp))
                                Text("Hidup (" + activeCount + ")", fontSize = 8.5.sp, color = Color(0xFF334155), fontWeight = FontWeight.Bold)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(7.5.dp).clip(CircleShape).background(Color(0xFFD9D9D9)))
                                Spacer(modifier = Modifier.width(3.5.dp))
                                Text("Mati (" + inactiveCount + ")", fontSize = 8.5.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        AppSidebarMenu(
            showMenu = showSidebar,
            onDismiss = { showSidebar = false },
            onInfoAlatClick = {
                showSidebar = false
                onAboutSystemClick()
            },
            onPusatPanggilanClick = {
                Toast.makeText(context, "Menghubungi Pusat Bantuan: +62 812-3456-7890", Toast.LENGTH_SHORT).show()
                showSidebar = false
            }
        )
    }
}

@Composable
fun RealPondMapLayout(
    activePondNumbers: IntArray,
    statusResolver: (Int) -> MapPondStatus,
    onPondSelected: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFFF1F5F9))
            .border(1.2.dp, Color(0xFFCBD5E1), RoundedCornerShape(10.dp))
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "DINDING BELAKANG / VENTILASI",
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF64748B),
            modifier = Modifier.padding(bottom = 6.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1.8f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                (10 downTo 1).forEach { num ->
                    PondTile(
                        number = num,
                        status = statusResolver(num),
                        hasActiveDevice = activePondNumbers.contains(num),
                        onClick = { onPondSelected(num) }
                    )
                }
            }

            Spacer(modifier = Modifier.width(4.dp))

            Box(
                modifier = Modifier
                    .width(28.dp)
                    .height(316.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFFCBD5E1)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "KORIDOR UTAMA",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF475569),
                    letterSpacing = 2.sp,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier
                        .requiredWidth(180.dp)
                        .rotate(-90f)
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            Column(
                modifier = Modifier.weight(4f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(27 to 28, 25 to 26, 23 to 24, 21 to 22).forEach { (l, r) ->
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.weight(1f)) {
                            PondTile(number = l, status = statusResolver(l), hasActiveDevice = activePondNumbers.contains(l), onClick = { onPondSelected(l) })
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            PondTile(number = r, status = statusResolver(r), hasActiveDevice = activePondNumbers.contains(r), onClick = { onPondSelected(r) })
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF475569)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "SEKAT PEMISAH",
                        fontSize = 8.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                listOf(19 to 20, 17 to 18, 15 to 16, 13 to 14, 11 to 12).forEach { (l, r) ->
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.weight(1f)) {
                            PondTile(number = l, status = statusResolver(l), hasActiveDevice = activePondNumbers.contains(l), onClick = { onPondSelected(l) })
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            PondTile(number = r, status = statusResolver(r), hasActiveDevice = activePondNumbers.contains(r), onClick = { onPondSelected(r) })
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(4.dp))

            Box(
                modifier = Modifier
                    .width(22.dp)
                    .height(316.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFFCBD5E1)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "AKSES TEPI",
                    fontSize = 7.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF475569),
                    letterSpacing = 2.sp,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier
                        .requiredWidth(140.dp)
                        .rotate(-90f)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(30.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF1E3A8A)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "PINTU MASUK",
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF38BDF8),
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
fun PondTile(
    number: Int,
    status: MapPondStatus,
    hasActiveDevice: Boolean,
    onClick: () -> Unit
) {
    val (bgColor, borderColor, textColor) = when (status) {
        MapPondStatus.KERUH -> Triple(Color(0xFFF97316), Color(0xFFEA580C), Color.White)
        MapPondStatus.JERNIH -> Triple(Color(0xFF0284C7), Color(0xFF0369A1), Color.White)
        MapPondStatus.STANDBY -> Triple(Color(0xFF94A3B8), Color(0xFF64748B), Color(0xFFF8FAFC))
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp)
            .border(1.2.dp, borderColor, RoundedCornerShape(4.dp))
            .background(bgColor, RoundedCornerShape(4.dp))
            .clickable { onClick() }
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Text(
            text = "" + number,
            color = textColor,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Center)
        )

        Box(
            modifier = Modifier
                .size(5.5.dp)
                .clip(CircleShape)
                .background(if (hasActiveDevice) Color(0xFF31E924) else Color(0xFFD9D9D9))
                .align(Alignment.TopEnd)
        )
    }
}

@Composable
fun DoubleChevronDownIcon() {
    Canvas(modifier = Modifier.size(22.dp, 13.dp)) {
        val strokeWidth = 2.dp.toPx()
        val iconColor = Color(0xFF94A3B8)

        val path1 = Path().apply {
            moveTo(size.width * 0.18f, size.height * 0.15f)
            lineTo(size.width * 0.5f, size.height * 0.52f)
            lineTo(size.width * 0.82f, size.height * 0.15f)
        }
        drawPath(
            path = path1,
            color = iconColor,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        val path2 = Path().apply {
            moveTo(size.width * 0.18f, size.height * 0.55f)
            lineTo(size.width * 0.5f, size.height * 0.92f)
            lineTo(size.width * 0.82f, size.height * 0.55f)
        }
        drawPath(
            path = path2,
            color = iconColor,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

@Composable
fun AnomalyPondCard(
    pondName: String,
    deviceName: String,
    status: String = "BAHAYA",
    paramLine1: String,
    paramLine2: String,
    reasonLine: String = ""
) {
    val isDanger = status.equals("BAHAYA", ignoreCase = true)
    val cardBg = if (isDanger) Color(0xFFFFEAEA) else Color(0xFFFFFBEB)
    val borderColor = if (isDanger) Color(0xFFFECACA) else Color(0xFFFEF3C7)
    val badgeBg = if (isDanger) Color(0xFFFFD2D2) else Color(0xFFFDE68A)
    val mainColor = if (isDanger) Color(0xFFDC2626) else Color(0xFFD97706)

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        modifier = Modifier
            .fillMaxWidth()
            .border(0.8.dp, borderColor, RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = pondName, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                    Text(text = deviceName, fontSize = 11.sp, color = Color(0xFF64748B))
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(badgeBg, RoundedCornerShape(50.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = status,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = mainColor
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = mainColor,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
                Text(text = paramLine1, fontSize = 10.sp, color = mainColor, fontWeight = FontWeight.Medium)
                Text(text = paramLine2, fontSize = 10.sp, color = mainColor, fontWeight = FontWeight.Medium)
                if (reasonLine.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Penyebab: " + reasonLine,
                        fontSize = 10.sp,
                        color = Color(0xFF475569),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}