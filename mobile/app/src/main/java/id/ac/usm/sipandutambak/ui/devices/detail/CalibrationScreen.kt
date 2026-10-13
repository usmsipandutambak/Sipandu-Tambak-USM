package id.ac.usm.sipandutambak.ui.devices.detail

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.ac.usm.sipandutambak.R

@Composable
fun CalibrationScreen(
    viewModel: DeviceDetailViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFDCEBFE))
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 150.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header SIPANDU TAMBAK
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
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

            // Judul Terpusat: PARAMETER SENSOR
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = Color(0xFF93C5FD),
                        thickness = 1.5.dp
                    )
                    Text(
                        text = "PARAMETER SENSOR",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF0F172A),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 14.dp)
                    )
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = Color(0xFF93C5FD),
                        thickness = 1.5.dp
                    )
                }
            }

            // Sensor TDS
            item {
                SensorCalibrationBlock(
                    title = "Kalibrasi Sensor - TDS",
                    slopeLabel = "Slope  TDS (ppm/V)",
                    slopeValue = state.tdsConfig.slope,
                    onSlopeChange = { viewModel.updateTdsConfig(it, state.tdsConfig.offset) },
                    offsetLabel = "Offset TDS",
                    offsetValue = state.tdsConfig.offset,
                    onOffsetChange = { viewModel.updateTdsConfig(state.tdsConfig.slope, it) },
                    iconType = "GRID"
                )
            }

            // Sensor pH
            item {
                SensorCalibrationBlock(
                    title = "Kalibrasi Sensor - pH",
                    slopeLabel = "Slope  pH (/V)",
                    slopeValue = state.phConfig.slope,
                    onSlopeChange = { viewModel.updatePhConfig(it, state.phConfig.offset) },
                    offsetLabel = "Offset pH",
                    offsetValue = state.phConfig.offset,
                    onOffsetChange = { viewModel.updatePhConfig(state.phConfig.slope, it) },
                    iconType = "PH"
                )
            }

            // Sensor COD
            item {
                SensorCalibrationBlock(
                    title = "Kalibrasi Sensor - COD",
                    slopeLabel = "Slope  COD (mg/L)",
                    slopeValue = state.codConfig.slope,
                    onSlopeChange = { viewModel.updateCodConfig(it, state.codConfig.offset) },
                    offsetLabel = "Offset COD",
                    offsetValue = state.codConfig.offset,
                    onOffsetChange = { viewModel.updateCodConfig(state.codConfig.slope, it) },
                    iconType = "GRID"
                )
            }

            // Sensor NH4+
            item {
                SensorCalibrationBlock(
                    title = "Kalibrasi Sensor - NH4+",
                    slopeLabel = "Slope  NH4+ (mg/L)",
                    slopeValue = state.nh4Config.slope,
                    onSlopeChange = { viewModel.updateNh4Config(it, state.nh4Config.offset) },
                    offsetLabel = "Offset NH4+",
                    offsetValue = state.nh4Config.offset,
                    onOffsetChange = { viewModel.updateNh4Config(state.nh4Config.slope, it) },
                    iconType = "FLASK"
                )
            }

            // Sensor NO3-
            item {
                SensorCalibrationBlock(
                    title = "Kalibrasi Sensor - NO3-",
                    slopeLabel = "Slope  NO3- (mg/L)",
                    slopeValue = state.no3Config.slope,
                    onSlopeChange = { viewModel.updateNo3Config(it, state.no3Config.offset) },
                    offsetLabel = "Offset NO3-",
                    offsetValue = state.no3Config.offset,
                    onOffsetChange = { viewModel.updateNo3Config(state.no3Config.slope, it) },
                    iconType = "DROP"
                )
            }

            // Sensor DO
            item {
                SensorCalibrationBlock(
                    title = "Kalibrasi Sensor - DO",
                    slopeLabel = "Slope  DO (mg/L)",
                    slopeValue = state.doConfig.slope,
                    onSlopeChange = { viewModel.updateDoConfig(it, state.doConfig.offset) },
                    offsetLabel = "Offset mg/L",
                    offsetValue = state.doConfig.offset,
                    onOffsetChange = { viewModel.updateDoConfig(state.doConfig.slope, it) },
                    iconType = "MOLECULE"
                )
            }

            // Tombol Simpan Kalibrasi Sensor
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    onClick = {
                        Toast.makeText(context, "Kalibrasi berhasil disimpan! Status: " + state.status, Toast.LENGTH_SHORT).show()
                        onBackClick()
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFD6E7FC),
                    border = BorderStroke(1.dp, Color(0xFF93C5FD)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SaveTrayMiniIcon(modifier = Modifier.size(17.dp), color = Color(0xFF0284C7))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Simpan Kalibrasi Sensor",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0284C7)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SensorCalibrationBlock(
    title: String,
    slopeLabel: String,
    slopeValue: String,
    onSlopeChange: (String) -> Unit,
    offsetLabel: String,
    offsetValue: String,
    onOffsetChange: (String) -> Unit,
    iconType: String
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFBFDBFE),
                modifier = Modifier.size(30.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    ParamIconCanvas(type = iconType, modifier = Modifier.size(16.dp))
                }
            }
        }

        Text(
            text = slopeLabel,
            fontSize = 11.5.sp,
            color = Color(0xFF0F172A),
            fontWeight = FontWeight.Medium
        )

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFFD4E7FE),
            border = BorderStroke(1.dp, Color(0xFF0F172A)),
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                BasicTextField(
                    value = slopeValue,
                    onValueChange = onSlopeChange,
                    singleLine = true,
                    textStyle = TextStyle(
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Text(
            text = offsetLabel,
            fontSize = 11.5.sp,
            color = Color(0xFF0F172A),
            fontWeight = FontWeight.Medium
        )

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFFD4E7FE),
            border = BorderStroke(1.dp, Color(0xFF0F172A)),
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                BasicTextField(
                    value = offsetValue,
                    onValueChange = onOffsetChange,
                    singleLine = true,
                    textStyle = TextStyle(
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun SaveTrayMiniIcon(modifier: Modifier = Modifier, color: Color) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)

        val arrow = Path().apply {
            moveTo(w * 0.5f, h * 0.15f)
            lineTo(w * 0.5f, h * 0.65f)
            moveTo(w * 0.3f, h * 0.45f)
            lineTo(w * 0.5f, h * 0.65f)
            lineTo(w * 0.7f, h * 0.45f)
        }
        drawPath(arrow, color = color, style = stroke)

        val tray = Path().apply {
            moveTo(w * 0.2f, h * 0.65f)
            lineTo(w * 0.2f, h * 0.85f)
            lineTo(w * 0.8f, h * 0.85f)
            lineTo(w * 0.8f, h * 0.65f)
        }
        drawPath(tray, color = color, style = stroke)
    }
}

@Composable
fun ParamIconCanvas(type: String, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val iconColor = Color(0xFF1D4ED8)
        val stroke = Stroke(width = 1.4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)

        when (type) {
            "PH" -> {
                val drop = Path().apply {
                    moveTo(w * 0.5f, 0f)
                    cubicTo(w * 0.1f, h * 0.6f, w * 0.1f, h * 0.9f, w * 0.5f, h)
                    cubicTo(w * 0.9f, h * 0.9f, w * 0.9f, h * 0.6f, w * 0.5f, 0f)
                }
                drawPath(drop, color = iconColor, style = stroke)
            }
            "FLASK" -> {
                val flask = Path().apply {
                    moveTo(w * 0.35f, 0f); lineTo(w * 0.65f, 0f)
                    moveTo(w * 0.42f, 0f); lineTo(w * 0.42f, h * 0.35f)
                    lineTo(w * 0.1f, h); lineTo(w * 0.9f, h)
                    lineTo(w * 0.58f, h * 0.35f); lineTo(w * 0.58f, 0f)
                }
                drawPath(flask, color = iconColor, style = stroke)
            }
            "DROP" -> {
                val drop = Path().apply {
                    moveTo(w * 0.5f, 0.1f * h)
                    cubicTo(w * 0.2f, h * 0.55f, w * 0.2f, h * 0.88f, w * 0.5f, h * 0.95f)
                    cubicTo(w * 0.8f, h * 0.88f, w * 0.8f, h * 0.55f, w * 0.5f, 0.1f * h)
                }
                drawPath(drop, color = iconColor, style = stroke)
            }
            "MOLECULE" -> {
                drawCircle(color = iconColor, radius = 2.dp.toPx(), center = Offset(w * 0.5f, h * 0.3f))
                drawCircle(color = iconColor, radius = 2.dp.toPx(), center = Offset(w * 0.3f, h * 0.7f))
                drawCircle(color = iconColor, radius = 2.dp.toPx(), center = Offset(w * 0.7f, h * 0.7f))
                drawLine(color = iconColor, start = Offset(w * 0.5f, h * 0.3f), end = Offset(w * 0.3f, h * 0.7f), strokeWidth = 1.2.dp.toPx())
                drawLine(color = iconColor, start = Offset(w * 0.5f, h * 0.3f), end = Offset(w * 0.7f, h * 0.7f), strokeWidth = 1.2.dp.toPx())
            }
            else -> {
                for (r in 0..2) {
                    for (c in 0..2) {
                        drawRect(
                            color = iconColor,
                            topLeft = Offset(c * (w / 3f) + 1.dp.toPx(), r * (h / 3f) + 1.dp.toPx()),
                            size = Size(2.dp.toPx(), 2.dp.toPx())
                        )
                    }
                }
            }
        }
    }
}