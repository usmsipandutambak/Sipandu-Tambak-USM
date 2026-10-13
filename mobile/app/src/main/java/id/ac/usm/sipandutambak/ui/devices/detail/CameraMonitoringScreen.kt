package id.ac.usm.sipandutambak.ui.devices.detail

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.ac.usm.sipandutambak.R
import id.ac.usm.sipandutambak.util.CsvExportHelper
import kotlinx.coroutines.delay

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
    var cameraState by remember { mutableStateOf(CameraState.STANDBY) }
    var recordingSecondsLeft by remember { mutableStateOf(900) }
    var cooldownSecondsLeft by remember { mutableStateOf(60) }
    var remainingQuota by remember { mutableStateOf(5) }
    var isAutoCameraEnabled by remember { mutableStateOf(true) }

    var showConfigModal by remember { mutableStateOf(false) }

    LaunchedEffect(cameraState) {
        if (cameraState == CameraState.RECORDING) {
            recordingSecondsLeft = 900
            while (recordingSecondsLeft > 0 && cameraState == CameraState.RECORDING) {
                delay(1000)
                recordingSecondsLeft--
            }
            if (cameraState == CameraState.RECORDING) {
                cameraState = CameraState.COOLDOWN
            }
        } else if (cameraState == CameraState.COOLDOWN) {
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
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            onClick = onBackClick,
                            shape = CircleShape,
                            color = Color.White,
                            shadowElevation = 2.dp,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = "Kembali",
                                    tint = Color(0xFF0F172A),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Column {
                            Text(
                                text = "SIPANDU",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF453636),
                                lineHeight = 20.sp
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Image(
                                    painter = painterResource(id = R.drawable.famicons_fish),
                                    contentDescription = "Logo Ikan",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.size(width = 30.dp, height = 20.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "TAMBAK",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF453636),
                                    lineHeight = 20.sp
                                )
                            }
                        }
                    }

                    Surface(
                        onClick = onBackClick,
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White,
                        border = BorderStroke(0.8.dp, Color(0xFFCBD5E1))
                    ) {
                        Text(
                            text = "Tutup",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0284C7),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
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
                        LiveCameraWaterCanvas(modifier = Modifier.fillMaxSize())

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
                                            .background(Color(0xFFEF4444), CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = if (cameraState == CameraState.RECORDING) "LIVE REC" else "LIVE",
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
                                    Box(modifier = Modifier.size(5.dp).background(Color(0xFFDC2626), CircleShape))
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = "LIVE KAMERA | 1080p FHD",
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
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xCC000000)
                            ) {
                                Text(
                                    text = "HD 1080p",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
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
                                if (remainingQuota > 0) {
                                    remainingQuota--
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
                            onClick = { cameraState = CameraState.COOLDOWN },
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFDC2626),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "Kamera Sedang Merekam",
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

                            val schedules = listOf("10:00:00", "11:00:00", "13:00:00", "14:00:00", "17:00:00")
                            schedules.forEach { time ->
                                Surface(
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
                                    onCheckedChange = { isAutoCameraEnabled = it },
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
                onDismiss = { showConfigModal = false },
                onSave = {
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
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    var autoExposure by remember { mutableStateOf(false) }
    var lockAe by remember { mutableStateOf(true) }
    var wdr by remember { mutableStateOf(true) }

    var selectedResolution by remember { mutableStateOf("640 × 480 (VGA)") }
    var selectedFps by remember { mutableStateOf("30 FPS") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp)
                .padding(bottom = 30.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Edit Konfigurasi Kamera",
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF0F172A)
            )

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
                        text = "☼ 1. Pencahayaan",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0284C7)
                    )

                    ConfigToggleItem(
                        title = "Auto-Exposure (AE)",
                        description = "Biarkan menyala jika lingkungan cahaya berubah-ubah, misalnya di luar ruangan atau dekat jendela.",
                        checked = autoExposure,
                        onCheckedChange = { autoExposure = it }
                    )

                    ConfigToggleItem(
                        title = "Kunci AE",
                        description = "Gunakan jika pencahayaan lingkungan stabil (ruangan/indoor).",
                        checked = lockAe,
                        onCheckedChange = { lockAe = it }
                    )

                    ConfigToggleItem(
                        title = "WDR (Wide Dynamic Range)",
                        description = "Aktifkan jika objek sering membelakangi sumber cahaya (silau/backlight)",
                        checked = wdr,
                        onCheckedChange = { wdr = it }
                    )
                }
            }

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
                        text = "# 2. Resolusi dan Frame Rate",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0284C7)
                    )

                    Column {
                        Text("Resolusi Output Kamera", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                            color = Color(0xFFF8FAFC),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(selectedResolution, fontSize = 12.sp, color = Color(0xFF1E293B))
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = Color(0xFF0284C7))
                            }
                        }
                    }

                    Column {
                        Text("Frame Rate (FPS)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                            color = Color(0xFFF8FAFC),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(selectedFps, fontSize = 12.sp, color = Color(0xFF1E293B))
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = Color(0xFF0284C7))
                            }
                        }
                    }
                }
            }

            Surface(
                onClick = onSave,
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF0284C7),
                modifier = Modifier
                    .align(Alignment.End)
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
private fun LiveCameraWaterCanvas(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF0E4B5B), Color(0xFF134E4A), Color(0xFF064E3B))
            )
        )

        for (i in 1..4) {
            val yOffset = h * (i * 0.22f)
            val path = Path().apply {
                moveTo(0f, yOffset)
                cubicTo(w * 0.25f, yOffset - 12f, w * 0.75f, yOffset + 12f, w, yOffset)
            }
            drawPath(
                path = path,
                color = Color(0x33A7F3D0),
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        val fishCenters = listOf(
            Offset(w * 0.35f, h * 0.45f),
            Offset(w * 0.6f, h * 0.38f),
            Offset(w * 0.75f, h * 0.55f),
            Offset(w * 0.25f, h * 0.7f),
            Offset(w * 0.5f, h * 0.65f)
        )

        fishCenters.forEach { center ->
            drawOval(
                color = Color(0xFFF97316),
                topLeft = Offset(center.x - 14.dp.toPx(), center.y - 6.dp.toPx()),
                size = androidx.compose.ui.geometry.Size(28.dp.toPx(), 12.dp.toPx())
            )
            drawCircle(
                color = Color(0xFFEA580C),
                radius = 3.dp.toPx(),
                center = Offset(center.x + 8.dp.toPx(), center.y)
            )
        }
    }
}
