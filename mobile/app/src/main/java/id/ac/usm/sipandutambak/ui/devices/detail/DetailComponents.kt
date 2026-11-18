package id.ac.usm.sipandutambak.ui.devices.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.ac.usm.sipandutambak.R
import id.ac.usm.sipandutambak.data.api.formatSensorMetric

@Composable
fun SipanduHeader(
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
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
                .clickable { onMenuClick() }
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

@Composable
fun TelemetryChartSection(
    logs: List<SensorLogRow>,
    currentPh: Double,
    currentTds: Double,
    currentTurbidity: Double,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
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
                    logs = logs,
                    currentPh = currentPh,
                    currentTds = currentTds,
                    currentTemp = 0.0,
                    currentTurbidity = currentTurbidity,
                    currentDo = 0.0,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FigmaLegendPill(modifier = Modifier.weight(1f), color = Color(0xFF1D4ED8), text = "TDS (ppm)")
                    FigmaLegendPill(modifier = Modifier.weight(1f), color = Color(0xFF047857), text = "pH (x10)")
                    FigmaLegendPill(modifier = Modifier.weight(1.3f), color = Color(0xFFDC2626), text = "Turbidity (NTU/V)")
                }
            }
        }
    }
}

@Composable
fun TelemetryLogSection(
    logs: List<SensorLogRow>,
    onExportClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tableScrollState = rememberScrollState()

    Column(modifier = modifier) {
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
                    .clickable { onExportClick() }
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
                val headerColor = Color(0xFF334155)
                Surface(color = Color(0xFFF1F5F9), modifier = Modifier.width(740.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf(
                            "WAKTU" to 65, "PH" to 55, "TDS" to 60, "TEMP" to 65, "TURB" to 65,
                            "COD" to 55, "NH4+" to 60, "NO3-" to 60, "DO" to 55, "STATUS" to 80
                        ).forEach { (label, w) ->
                            Text(
                                label, fontSize = 8.5.sp, fontWeight = FontWeight.Bold,
                                color = headerColor, modifier = Modifier.width(w.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 1.dp)

                if (logs.isEmpty()) {
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
                    for (log in logs) {
                        val isDanger = log.status.equals("BAHAYA", true)
                        Row(
                            modifier = Modifier
                                .width(740.dp)
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(log.timeLabel, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1D4ED8), modifier = Modifier.width(65.dp), textAlign = TextAlign.Center)
                            Text(formatSensorMetric(log.ph, 2), fontSize = 9.sp, color = Color(0xFF0F172A), modifier = Modifier.width(55.dp), textAlign = TextAlign.Center)
                            Text(formatSensorMetric(log.tds, 0), fontSize = 9.sp, color = Color(0xFF0F172A), modifier = Modifier.width(60.dp), textAlign = TextAlign.Center)
                            Text(
                                text = log.temperature?.let { "${formatSensorMetric(it, 1)}°C" } ?: "N/A",
                                fontSize = 9.sp,
                                color = Color(0xFF0F172A),
                                modifier = Modifier.width(65.dp),
                                textAlign = TextAlign.Center
                            )
                            Text(formatSensorMetric(log.turbidity, 1), fontSize = 9.sp, color = Color(0xFF0F172A), modifier = Modifier.width(65.dp), textAlign = TextAlign.Center)
                            Text(log.cod, fontSize = 9.sp, color = Color(0xFF94A3B8), modifier = Modifier.width(55.dp), textAlign = TextAlign.Center)
                            Text(log.nh4, fontSize = 9.sp, color = Color(0xFF94A3B8), modifier = Modifier.width(60.dp), textAlign = TextAlign.Center)
                            Text(log.no3, fontSize = 9.sp, color = Color(0xFF94A3B8), modifier = Modifier.width(60.dp), textAlign = TextAlign.Center)
                            Text(formatSensorMetric(log.doLevel, 1), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0284C7), modifier = Modifier.width(55.dp), textAlign = TextAlign.Center)

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