package id.ac.usm.sipandutambak.ui.devices.detail

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
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
import androidx.compose.ui.graphics.nativeCanvas
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
    var showCameraScreen by remember { mutableStateOf(false) }

    if (showCameraScreen) {
        CameraMonitoringScreen(
            deviceName = state.deviceName,
            pondName = state.pondName,
            commodity = state.commodity,
            onBackClick = { showCameraScreen = false }
        )
        return
    }

    val curTds: Double = state.currentTds ?: 0.0
    val curPh: Double = state.currentPh ?: 0.0
    val curTemp: Double = state.currentTemp ?: 0.0
    val curDo: Double = state.currentDo ?: 0.0
    val curTurb: Double = state.currentTurbidity ?: 0.0

    val tableScrollState = rememberScrollState()
    val displayTitle = "${state.deviceName.replace("Sipandu", "ALAT").uppercase()} - ${state.pondName.uppercase()}"

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFDCEBFE))
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 140.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
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
            }

            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = displayTitle,
                        fontSize = 17.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF0F172A),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = state.commodity.uppercase(),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF0F172A),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White,
                        border = BorderStroke(0.8.dp, Color(0xFFCBD5E1)),
                        modifier = Modifier.height(26.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = null,
                                tint = Color(0xFF0284C7),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            val dateLabel = state.currentDateFormatted.ifBlank { "22 Jan 2026" }
                            Text(
                                text = "$dateLabel  |  24 JAM",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0369A1)
                            )
                        }
                    }
                }
            }

            item {
                Surface(
                    onClick = { showCameraScreen = true },
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF1D4ED8),
                    shadowElevation = 6.dp,
                    tonalElevation = 2.dp,
                    border = BorderStroke(1.dp, Color(0xFF3B82F6)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CameraWhiteIcon(modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "KAMERA",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }

            item {
                Column {
                    Text(
                        text = "Data Realtime Sensor",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                SensorMetricTile(
                                    modifier = Modifier.weight(1f),
                                    title = "TDS",
                                    value = if (curTds > 0.0) curTds.toInt().toString() else "-",
                                    unit = "/ppm",
                                    iconType = "tds"
                                )
                                SensorMetricTile(
                                    modifier = Modifier.weight(1f),
                                    title = "PH",
                                    value = if (curPh > 0.0) curPh.toString() else "-",
                                    unit = "/ph",
                                    iconType = "ph"
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                SensorMetricTile(
                                    modifier = Modifier.weight(1f),
                                    title = "Temperature",
                                    value = if (curTemp > 0.0) curTemp.toInt().toString() else "-",
                                    unit = "°C",
                                    iconType = "temp"
                                )
                                SensorMetricTile(
                                    modifier = Modifier.weight(1f),
                                    title = "DO",
                                    value = if (curDo > 0.0) curDo.toString() else "-",
                                    unit = "mg/L",
                                    iconType = "do"
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                SensorMetricTile(
                                    modifier = Modifier.weight(1f),
                                    title = "Turbidity",
                                    value = if (curTurb > 0.0) curTurb.toInt().toString() else "-",
                                    unit = "NTU",
                                    iconType = "turb"
                                )
                                SensorMetricTile(
                                    modifier = Modifier.weight(1f),
                                    title = "NH4+",
                                    value = "-",
                                    unit = "mg/L",
                                    iconType = "flask"
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                SensorMetricTile(
                                    modifier = Modifier.weight(1f),
                                    title = "NO3-",
                                    value = "-",
                                    unit = "mg/L",
                                    iconType = "bucket"
                                )
                                SensorMetricTile(
                                    modifier = Modifier.weight(1f),
                                    title = "COD",
                                    value = "-",
                                    unit = "mg/L",
                                    iconType = "mesh"
                                )
                            }
                        }
                    }
                }
            }

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
                                .padding(14.dp)
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

            item {
                Column {
                    Text(
                        text = "ANALISIS DINAMIKA AIR",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B),
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Ringkasan Grafik Telemetri 24 Jam",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            TelemetryChartCanvasFull(
                                logs = state.logs,
                                currentPh = curPh,
                                currentTds = curTds,
                                currentTemp = curTemp,
                                currentTurbidity = curTurb,
                                currentDo = curDo,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    FigmaLegendPill(modifier = Modifier.weight(1f), color = Color(0xFF1D4ED8), text = "TDS (ppm)")
                                    FigmaLegendPill(modifier = Modifier.weight(1f), color = Color(0xFF047857), text = "pH (x10)")
                                    FigmaLegendPill(modifier = Modifier.weight(1f), color = Color(0xFFEA580C), text = "COD (mg/L)")
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    FigmaLegendPill(modifier = Modifier.weight(1f), color = Color(0xFF7C3AED), text = "NH4+ (mg/L)")
                                    FigmaLegendPill(modifier = Modifier.weight(1f), color = Color(0xFFEAB308), text = "DO (mg/L)")
                                    FigmaLegendPill(modifier = Modifier.weight(1f), color = Color(0xFF0D9488), text = "NO3- (mg/L)")
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    FigmaLegendPill(modifier = Modifier.weight(1.1f), color = Color(0xFFDC2626), text = "Turbidity (NTU/V)")
                                    FigmaLegendPill(modifier = Modifier.weight(1.3f), color = Color(0xFF06B6D4), text = "Temperature (°C/V)")
                                }
                            }
                        }
                    }
                }
            }

            item {
                Column {
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
                                text = "Data Keseluruhan Log Sensor",
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
                                .clickable { viewModel.exportCsvLog(context) }
                                .padding(horizontal = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                ExportMiniIcon(modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(text = "Export Excel", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(tableScrollState)
                        ) {
                            Surface(
                                color = Color(0xFFF1F5F9),
                                modifier = Modifier.width(740.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 10.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("WAKTU", fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155), modifier = Modifier.width(65.dp), textAlign = TextAlign.Center)
                                    Text("PH", fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155), modifier = Modifier.width(55.dp), textAlign = TextAlign.Center)
                                    Text("TDS", fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155), modifier = Modifier.width(60.dp), textAlign = TextAlign.Center)
                                    Text("TEMP", fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155), modifier = Modifier.width(65.dp), textAlign = TextAlign.Center)
                                    Text("TURB", fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155), modifier = Modifier.width(65.dp), textAlign = TextAlign.Center)
                                    Text("COD", fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155), modifier = Modifier.width(55.dp), textAlign = TextAlign.Center)
                                    Text("NH4+", fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155), modifier = Modifier.width(60.dp), textAlign = TextAlign.Center)
                                    Text("NO3-", fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155), modifier = Modifier.width(60.dp), textAlign = TextAlign.Center)
                                    Text("DO", fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155), modifier = Modifier.width(55.dp), textAlign = TextAlign.Center)
                                    Text("STATUS", fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155), modifier = Modifier.width(80.dp), textAlign = TextAlign.Center)
                                }
                            }

                            HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 1.dp)

                            if (state.logs.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .width(740.dp)
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Belum ada rekaman telemetri. Data akan muncul otomatis saat alat membaca sensor.",
                                        fontSize = 11.5.sp,
                                        color = Color(0xFF64748B),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            } else {
                                for (log in state.logs) {
                                    val isDanger = log.status.equals("BAHAYA", true)
                                    Row(
                                        modifier = Modifier
                                            .width(740.dp)
                                            .padding(vertical = 8.dp, horizontal = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(log.timeLabel, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1D4ED8), modifier = Modifier.width(65.dp), textAlign = TextAlign.Center)
                                        Text(log.ph.toString(), fontSize = 9.sp, color = Color(0xFF0F172A), modifier = Modifier.width(55.dp), textAlign = TextAlign.Center)
                                        Text((log.tds ?: 0.0).toInt().toString(), fontSize = 9.sp, color = Color(0xFF0F172A), modifier = Modifier.width(60.dp), textAlign = TextAlign.Center)
                                        Text("" + log.temperature + "°C", fontSize = 9.sp, color = Color(0xFF0F172A), modifier = Modifier.width(65.dp), textAlign = TextAlign.Center)
                                        Text("" + (log.turbidity ?: 0.0).toInt(), fontSize = 9.sp, color = Color(0xFF0F172A), modifier = Modifier.width(65.dp), textAlign = TextAlign.Center)
                                        Text(log.cod, fontSize = 9.sp, color = Color(0xFF94A3B8), modifier = Modifier.width(55.dp), textAlign = TextAlign.Center)
                                        Text(log.nh4, fontSize = 9.sp, color = Color(0xFF94A3B8), modifier = Modifier.width(60.dp), textAlign = TextAlign.Center)
                                        Text(log.no3, fontSize = 9.sp, color = Color(0xFF94A3B8), modifier = Modifier.width(60.dp), textAlign = TextAlign.Center)
                                        Text(log.doLevel.toString(), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0284C7), modifier = Modifier.width(55.dp), textAlign = TextAlign.Center)

                                        Box(modifier = Modifier.width(80.dp), contentAlignment = Alignment.Center) {
                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = if (isDanger) Color(0xFFFECDD3) else Color(0xFF86EFAC),
                                                modifier = Modifier.height(18.dp)
                                            ) {
                                                Box(modifier = Modifier.padding(horizontal = 6.dp), contentAlignment = Alignment.Center) {
                                                    Text(
                                                        text = log.status.uppercase(),
                                                        fontSize = 7.5.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isDanger) Color(0xFF991B1B) else Color(0xFF065F46)
                                                    )
                                                }
                                            }
                                        }
                                    }
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
fun CameraWhiteIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        val color = Color.White

        val cam = Path().apply {
            moveTo(w * 0.1f, h * 0.32f)
            lineTo(w * 0.3f, h * 0.32f)
            lineTo(w * 0.38f, h * 0.16f)
            lineTo(w * 0.62f, h * 0.16f)
            lineTo(w * 0.7f, h * 0.32f)
            lineTo(w * 0.9f, h * 0.32f)
            lineTo(w * 0.9f, h * 0.85f)
            lineTo(w * 0.1f, h * 0.85f)
            close()
        }
        drawPath(cam, color = color, style = stroke)
        drawCircle(color = color, radius = h * 0.19f, center = Offset(w * 0.5f, h * 0.58f), style = stroke)
        drawCircle(color = color, radius = h * 0.04f, center = Offset(w * 0.78f, h * 0.42f))
    }
}

@Composable
fun SensorMetricTile(
    title: String,
    value: String,
    unit: String,
    iconType: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFFF0F6FE),
        border = BorderStroke(0.8.dp, Color(0xFFE2E8F0)),
        modifier = modifier.height(68.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 7.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFBFDBFE),
                    modifier = Modifier.size(24.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = when (iconType) {
                                "tds" -> "⁝⁝"
                                "ph" -> "💧"
                                "temp" -> "🌡"
                                "do" -> "☍"
                                "turb" -> "≋"
                                "flask" -> "🧪"
                                "bucket" -> "⏏"
                                else -> "⌗"
                            },
                            fontSize = 11.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = value,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF0F172A)
                )
                Text(
                    text = unit,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF64748B)
                )
            }
        }
    }
}

@Composable
fun TelemetryChartCanvasFull(
    logs: List<SensorLogRow>,
    currentPh: Double,
    currentTds: Double,
    currentTemp: Double,
    currentTurbidity: Double,
    currentDo: Double,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val leftMargin = 34.dp.toPx()
        val bottomMargin = 22.dp.toPx()
        val graphW = w - leftMargin
        val graphH = h - bottomMargin

        val yLevels = listOf(500, 400, 300, 200, 100, 0)
        yLevels.forEachIndexed { index, value ->
            val y = (graphH / 5f) * index
            drawLine(
                color = Color(0xFFE2E8F0),
                start = Offset(leftMargin, y),
                end = Offset(w, y),
                strokeWidth = 1.dp.toPx()
            )
            drawContext.canvas.nativeCanvas.drawText(
                value.toString(),
                4.dp.toPx(),
                y + 3.5.dp.toPx(),
                android.graphics.Paint().apply {
                    color = android.graphics.Color.parseColor("#94A3B8")
                    textSize = 9.sp.toPx()
                    isAntiAlias = true
                }
            )
        }

        val timeLabels = if (logs.isNotEmpty()) {
            logs.take(5).map { it.timeLabel }.reversed()
        } else {
            listOf("08:00", "09:00", "10:00 (AKTIF)", "11:00", "12:00")
        }

        timeLabels.forEachIndexed { index, label ->
            val ratio = if (timeLabels.size > 1) index.toFloat() / (timeLabels.size - 1) else 0.5f
            val x = leftMargin + graphW * ratio
            drawContext.canvas.nativeCanvas.drawText(
                label,
                x - 14.dp.toPx(),
                h - 4.dp.toPx(),
                android.graphics.Paint().apply {
                    color = if (label.contains("AKTIF")) android.graphics.Color.parseColor("#0284C7") else android.graphics.Color.parseColor("#64748B")
                    textSize = 8.sp.toPx()
                    isFakeBoldText = label.contains("AKTIF")
                    isAntiAlias = true
                }
            )
        }

        fun getX(ratio: Float) = leftMargin + graphW * ratio
        fun getY(valRatio: Float) = graphH - (graphH * valRatio.coerceIn(0.02f, 0.98f))

        val tdsRatio = (((currentTds ?: 0.0).toFloat()) / 500f).coerceIn(0.08f, 0.95f)
        val tdsPath = Path().apply {
            moveTo(getX(0f), getY(tdsRatio * 0.92f))
            cubicTo(
                getX(0.4f), getY(tdsRatio * 0.95f),
                getX(0.7f), getY(tdsRatio * 0.88f),
                getX(1f), getY(tdsRatio)
            )
        }
        val tdsArea = Path().apply {
            addPath(tdsPath)
            lineTo(getX(1f), graphH)
            lineTo(getX(0f), graphH)
            close()
        }
        drawPath(tdsArea, color = Color(0x222563EB))
        drawPath(tdsPath, color = Color(0xFF1D4ED8), style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round))
        drawCircle(color = Color(0xFF1D4ED8), radius = 4.dp.toPx(), center = Offset(getX(1f), getY(tdsRatio)))

        val turbRatio = (((currentTurbidity ?: 0.0).toFloat()) / 4000f).coerceIn(0.05f, 0.92f)
        val turbPath = Path().apply {
            moveTo(getX(0f), getY(turbRatio))
            lineTo(getX(1f), getY(turbRatio))
        }
        drawPath(turbPath, color = Color(0xFFDC2626), style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
        drawCircle(color = Color(0xFFDC2626), radius = 3.5.dp.toPx(), center = Offset(getX(1f), getY(turbRatio)))

        val phRatio = ((((currentPh ?: 0.0).toFloat()) * 10f) / 500f).coerceIn(0.04f, 0.85f)
        val phPath = Path().apply {
            moveTo(getX(0f), getY(phRatio * 0.98f))
            lineTo(getX(1f), getY(phRatio))
        }
        drawPath(phPath, color = Color(0xFF047857), style = Stroke(width = 1.8.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)))
        drawCircle(color = Color(0xFF047857), radius = 3.dp.toPx(), center = Offset(getX(1f), getY(phRatio)))
    }
}

@Composable
fun FigmaLegendPill(color: Color, text: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFFF1F5F9),
        border = BorderStroke(0.6.dp, Color(0xFFCBD5E1)),
        modifier = modifier.height(26.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(modifier = Modifier.size(6.5.dp).background(color, CircleShape))
            Spacer(modifier = Modifier.width(4.5.dp))
            Text(text = text, fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155), maxLines = 1)
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


