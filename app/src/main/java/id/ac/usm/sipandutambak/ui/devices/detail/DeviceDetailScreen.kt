package id.ac.usm.sipandutambak.ui.devices.detail

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.ac.usm.sipandutambak.R
import id.ac.usm.sipandutambak.ui.components.AppSidebarMenu

@Composable
fun DeviceDetailScreen(
    viewModel: DeviceDetailViewModel,
    onBackClick: () -> Unit,
    onCalibrationClick: () -> Unit,
    onAboutSystemClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()
    var showSidebar by remember { mutableStateOf(false) }

    val statusBadgeBg = when (state.status.uppercase()) {
        "NORMAL" -> Color(0xFF86EFAC)
        "WASPADA" -> Color(0xFFFEF08A)
        "BAHAYA" -> Color(0xFFFECDD3)
        else -> Color(0xFFE2E8F0)
    }

    val statusBadgeText = when (state.status.uppercase()) {
        "NORMAL" -> Color(0xFF065F46)
        "WASPADA" -> Color(0xFF854D0E)
        "BAHAYA" -> Color(0xFF991B1B)
        else -> Color(0xFF475569)
    }

    val liveDotColor = when (state.status.uppercase()) {
        "NORMAL" -> Color(0xFF047857)
        "WASPADA" -> Color(0xFFD97706)
        "BAHAYA" -> Color(0xFFDC2626)
        else -> Color(0xFF94A3B8)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFDCEBFE))
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 150.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header SIPANDU TAMBAK
            item {
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
                                lineHeight = 22.sp
                            )
                        }
                    }

                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
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
            }

            // Judul Kolam & Komoditas
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFF93C5FD), thickness = 1.5.dp)
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = state.pondName.uppercase() + " - " + state.deviceName.uppercase(),
                            fontSize = 17.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF0F172A),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = state.commodity.uppercase(),
                            fontSize = 16.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF0F172A),
                            textAlign = TextAlign.Center
                        )
                    }
                    HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFF93C5FD), thickness = 1.5.dp)
                }
            }

            // Panduan Kalibrasi
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFDCEBFE)),
                    border = BorderStroke(1.dp, Color(0xFF93C5FD)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Surface(
                            color = Color(0xFFE0EDFE),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    FlaskMiniIcon(modifier = Modifier.size(18.dp))
                                    Text(
                                        text = "PANDUAN KALIBRASI",
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF0F172A)
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFF86EFAC),
                                    modifier = Modifier.height(20.dp)
                                ) {
                                    Box(
                                        modifier = Modifier.padding(horizontal = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "SOP VALID",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFF14532D)
                                        )
                                    }
                                }
                            }
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFEBF3FE))
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Lakukan kalibrasi berkala menggunakan larutan buffer standar yang telah terakreditasi ISO/IEC 17025.",
                                fontSize = 11.5.sp,
                                color = Color(0xFF334155),
                                lineHeight = 16.sp
                            )
                        }

                        Surface(
                            onClick = { onCalibrationClick() },
                            color = Color(0xFFBAE6FD),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = null,
                                    tint = Color(0xFF0284C7),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Kalibrasi Sensor",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0284C7)
                                )
                            }
                        }
                    }
                }
            }

            // Kartu Grafik Telemetri Reaktif
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Grafik Telemetri Realtime",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(0.8.dp, Color(0xFF93C5FD)),
                                color = Color.White
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .background(liveDotColor, CircleShape)
                                    )
                                    Text(
                                        text = if (state.hasData) "LIVE POINT | pH: ${state.currentPh} | TDS: ${state.currentTds}" else "MENUNGGU SENSOR",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0369A1)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        TelemetryChartCanvas(
                            currentPh = state.currentPh,
                            currentDo = state.currentDo,
                            currentTds = state.currentTds,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                        )
                    }
                }
            }

            // Pencatatan Telemetri Terpadu
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Pencatatan Telemetri Terpadu",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )

                    Surface(
                        onClick = { viewModel.exportCsvLog(context) },
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF059669),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            ExportMiniIcon(modifier = Modifier.size(12.dp))
                            Text(text = "Export CSV", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }

            // Tabel Log Riil Supabase
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Surface(
                            color = Color(0xFFF1F5F9),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("WAKTU", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155), modifier = Modifier.weight(1.1f), textAlign = TextAlign.Center)
                                Text("PH", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155), modifier = Modifier.weight(0.8f), textAlign = TextAlign.Center)
                                Text("TDS", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155), modifier = Modifier.weight(0.9f), textAlign = TextAlign.Center)
                                Text("COD", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155), modifier = Modifier.weight(0.9f), textAlign = TextAlign.Center)
                                Text("NH4+", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155), modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                                Text("NO3-", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155), modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                                Text("DO", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155), modifier = Modifier.weight(0.9f), textAlign = TextAlign.Center)
                                Text("STATUS", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155), modifier = Modifier.weight(1.2f), textAlign = TextAlign.Center)
                            }
                        }

                        HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 1.dp)

                        if (state.logs.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Belum ada rekaman data sensor.\nData akan muncul otomatis saat alat mengirim telemetri.",
                                    fontSize = 11.5.sp,
                                    color = Color(0xFF64748B),
                                    textAlign = TextAlign.Center,
                                    lineHeight = 16.sp
                                )
                            }
                        } else {
                            state.logs.forEachIndexed { index, log ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(log.timeLabel, fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1D4ED8), modifier = Modifier.weight(1.1f), textAlign = TextAlign.Center)
                                    Text(log.ph.toString(), fontSize = 8.5.sp, color = Color(0xFF0F172A), modifier = Modifier.weight(0.8f), textAlign = TextAlign.Center)
                                    Text(log.tds.toString(), fontSize = 8.5.sp, color = Color(0xFF0F172A), modifier = Modifier.weight(0.9f), textAlign = TextAlign.Center)
                                    Text(log.cod.toString(), fontSize = 8.5.sp, color = Color(0xFF0F172A), modifier = Modifier.weight(0.9f), textAlign = TextAlign.Center)
                                    Text(log.nh4.toString(), fontSize = 8.5.sp, color = Color(0xFF0F172A), modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                                    Text(log.no3.toString(), fontSize = 8.5.sp, color = Color(0xFF0F172A), modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                                    Text(log.doLevel.toString(), fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1D4ED8), modifier = Modifier.weight(0.9f), textAlign = TextAlign.Center)
                                    Box(modifier = Modifier.weight(1.2f), contentAlignment = Alignment.Center) {
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = if (log.status.equals("BAHAYA", true)) Color(0xFFFECDD3) else Color(0xFF86EFAC),
                                            modifier = Modifier.height(18.dp)
                                        ) {
                                            Box(modifier = Modifier.padding(horizontal = 6.dp), contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = log.status.uppercase(),
                                                    fontSize = 7.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (log.status.equals("BAHAYA", true)) Color(0xFF991B1B) else Color(0xFF065F46)
                                                )
                                            }
                                        }
                                    }
                                }
                                if (index < state.logs.size - 1) {
                                    HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 0.8.dp)
                                }
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
fun TelemetryChartCanvas(
    currentPh: Double,
    currentDo: Double,
    currentTds: Int,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val leftMargin = 32.dp.toPx()
        val bottomMargin = 22.dp.toPx()
        val graphW = w - leftMargin
        val graphH = h - bottomMargin

        val yLevels = listOf(500, 400, 300, 200, 100, 0)
        yLevels.forEachIndexed { index, _ ->
            val y = (graphH / 5f) * index
            drawLine(
                color = Color(0xFFE2E8F0),
                start = Offset(leftMargin, y),
                end = Offset(w, y),
                strokeWidth = 1.dp.toPx()
            )
        }

        if (currentPh > 0.0 || currentTds > 0) {
            fun getX(ratio: Float) = leftMargin + graphW * ratio
            fun getY(valRatio: Float) = graphH - (graphH * valRatio.coerceIn(0f, 1f))

            val tdsRatio = (currentTds / 500f).coerceIn(0.1f, 0.98f)
            val tdsPath = Path().apply {
                moveTo(getX(0f), getY(tdsRatio * 0.9f))
                lineTo(getX(1f), getY(tdsRatio))
            }
            drawPath(tdsPath, color = Color(0xFF1D4ED8), style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round))
            drawCircle(color = Color(0xFF1D4ED8), radius = 4.dp.toPx(), center = Offset(getX(1f), getY(tdsRatio)))

            val phRatio = ((currentPh * 10) / 140.0).toFloat().coerceIn(0.05f, 0.95f)
            val phPath = Path().apply {
                moveTo(getX(0f), getY(phRatio * 0.95f))
                lineTo(getX(1f), getY(phRatio))
            }
            drawPath(
                phPath,
                color = Color(0xFF047857),
                style = Stroke(width = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f))
            )
            drawCircle(color = Color(0xFF047857), radius = 4.dp.toPx(), center = Offset(getX(1f), getY(phRatio)))
        }
    }
}

@Composable
fun FlaskMiniIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val color = Color(0xFF1D4ED8)
        val stroke = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)

        val flask = Path().apply {
            moveTo(w * 0.38f, 0f)
            lineTo(w * 0.62f, 0f)
            moveTo(w * 0.42f, 0f)
            lineTo(w * 0.42f, h * 0.35f)
            lineTo(w * 0.1f, h)
            lineTo(w * 0.9f, h)
            lineTo(w * 0.58f, h * 0.35f)
            lineTo(w * 0.58f, 0f)
        }
        drawPath(flask, color = color, style = stroke)
    }
}

@Composable
fun ExportMiniIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)

        val arrow = Path().apply {
            moveTo(w * 0.5f, h * 0.15f)
            lineTo(w * 0.5f, h * 0.65f)
            moveTo(w * 0.28f, h * 0.35f)
            lineTo(w * 0.5f, h * 0.15f)
            lineTo(w * 0.72f, h * 0.35f)
        }
        drawPath(arrow, color = Color.White, style = stroke)

        val tray = Path().apply {
            moveTo(w * 0.2f, h * 0.65f)
            lineTo(w * 0.2f, h * 0.85f)
            lineTo(w * 0.8f, h * 0.85f)
            lineTo(w * 0.8f, h * 0.65f)
        }
        drawPath(tray, color = Color.White, style = stroke)
    }
}