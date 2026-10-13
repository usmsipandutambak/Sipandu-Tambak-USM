package id.ac.usm.sipandutambak.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.ac.usm.sipandutambak.ui.devices.detail.TrendStats
import id.ac.usm.sipandutambak.ui.devices.detail.TrendTimeRange
import id.ac.usm.sipandutambak.ui.theme.*

@Composable
fun SensorTrendChart(
    title: String,
    unit: String,
    points: List<Double>,
    stats: TrendStats,
    selectedRange: TrendTimeRange,
    onRangeSelected: (TrendTimeRange) -> Unit,
    modifier: Modifier = Modifier
) {
    val ranges = listOf(TrendTimeRange.HOURS_24, TrendTimeRange.DAYS_7, TrendTimeRange.DAYS_30)

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tren Sensor: $title",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = NavyDark
                )
                Text(
                    text = "Satuan: $unit",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Pemilih Rentang Waktu
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ranges.forEach { range ->
                    FilterChip(
                        selected = selectedRange == range,
                        onClick = { onRangeSelected(range) },
                        label = { Text(range.label, fontSize = 11.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Kotak Ringkasan Statistik
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                StatColumn(label = "Terendah", value = "${stats.min}", color = StatusBahaya)
                StatColumn(label = "Rata-rata", value = "${stats.avg}", color = ChartBlue)
                StatColumn(label = "Tertinggi", value = "${stats.max}", color = StatusNormal)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Kanvas Grafik Kurva
            if (points.isEmpty()) {
                Text(text = "Belum ada riwayat data", fontSize = 12.sp, color = Color.Gray)
            } else {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                ) {
                    val width = size.width
                    val height = size.height
                    val maxVal = points.maxOrNull() ?: 1.0
                    val minVal = points.minOrNull() ?: 0.0
                    val range = if (maxVal == minVal) 1.0 else maxVal - minVal

                    val stepX = width / (points.size - 1).coerceAtLeast(1)
                    val strokePath = Path()
                    val fillPath = Path()

                    points.forEachIndexed { index, value ->
                        val x = index * stepX
                        val normalizedY = ((value - minVal) / range).toFloat()
                        val y = height - (normalizedY * (height - 20f)) - 10f

                        if (index == 0) {
                            strokePath.moveTo(x, y)
                            fillPath.moveTo(x, height)
                            fillPath.lineTo(x, y)
                        } else {
                            strokePath.lineTo(x, y)
                            fillPath.lineTo(x, y)
                        }

                        drawCircle(
                            color = ChartBlue,
                            radius = 4.dp.toPx(),
                            center = Offset(x, y)
                        )
                    }

                    fillPath.lineTo(width, height)
                    fillPath.close()

                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(ChartBlue.copy(alpha = 0.25f), Color.Transparent)
                        )
                    )

                    drawPath(
                        path = strokePath,
                        color = ChartBlue,
                        style = Stroke(width = 3.dp.toPx())
                    )
                }
            }
        }
    }
}

@Composable
private fun StatColumn(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, fontSize = 11.sp, color = Color.Gray)
        Text(text = value, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = color)
    }
}