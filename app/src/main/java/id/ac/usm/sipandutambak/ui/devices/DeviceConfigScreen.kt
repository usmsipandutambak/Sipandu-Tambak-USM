package id.ac.usm.sipandutambak.ui.devices

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
fun DeviceConfigScreen(
    viewModel: DeviceListViewModel,
    onSaveSuccess: () -> Unit = {},
    onRescanClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()
    var pondDropdownExpanded by remember { mutableStateOf(false) }

    val displayCardTitle = if (state.pendingDeviceName.isNotBlank()) {
        state.pendingDeviceName.uppercase()
    } else {
        "SIPANDU A"
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFDCEBFE))
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header SIPANDU TAMBAK
            item {
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
                            modifier = Modifier.size(width = 34.dp, height = 22.dp)
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
            }

            // Judul Layar
            item {
                Text(
                    text = "Konfigurasi Alat",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
            }

            // =========================================================================
            // KONDISI 1: QR TIDAK RESMI / DITOLAK (TAMPILAN ERROR)
            // =========================================================================
            if (!state.isScanValid) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE2D6DC)),
                        border = BorderStroke(1.2.dp, Color(0xFFDC2626)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFEF4444),
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "!",
                                        color = Color.White,
                                        fontSize = 26.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "QR Alat gagal Dipindai!",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Perangkat sensor gagal terhubungkan ke titik tambak.",
                                    fontSize = 11.5.sp,
                                    color = Color(0xFFEA580C),
                                    lineHeight = 15.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Surface(
                        onClick = {
                            viewModel.resetScanError()
                            onRescanClick()
                        },
                        shape = RoundedCornerShape(50.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                        shadowElevation = 1.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "Pindai Ulang QR",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF475569)
                            )
                        }
                    }
                }
            } else {
                // =========================================================================
                // KONDISI 2: QR BERHASIL (TAMPILAN PERSIS DESAIN ASLI FIGMA)
                // =========================================================================
                
                // 1. Banner Sukses
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFD1FAE5)),
                        border = BorderStroke(1.dp, Color(0xFFA7F3D0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF10B981),
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("✓", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "QR Alat Berhasil Dipindai!",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF065F46)
                                )
                                Text(
                                    text = "Perangkat sensor siap dihubungkan ke titik tambak.",
                                    fontSize = 11.sp,
                                    color = Color(0xFF047857)
                                )
                            }
                        }
                    }
                }

                // 2. Kartu Utama Konfigurasi Alat
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFD6E6FA)),
                        border = BorderStroke(1.2.dp, Color(0xFF93C5FD)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Header Kartu (ALAT - A)
                            Surface(
                                color = Color(0xFFCCE0F8),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = displayCardTitle,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF0F172A),
                                    textAlign = TextAlign.Center,
                                    letterSpacing = 1.sp,
                                    modifier = Modifier.padding(vertical = 12.dp)
                                )
                            }

                            HorizontalDivider(color = Color(0xFF93C5FD), thickness = 1.dp)

                            // Isi Form Konfigurasi
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                // Pemilihan Kolam
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Pilih Kolam (1 s/d 28 Kolam):",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF1E293B)
                                        )
                                        Text(
                                            text = "Denah 28 Kolam",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF0284C7)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Box(modifier = Modifier.fillMaxWidth()) {
                                        Surface(
                                            onClick = { pondDropdownExpanded = true },
                                            shape = RoundedCornerShape(22.dp),
                                            color = Color.White,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(46.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(horizontal = 18.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = state.selectedPond.ifBlank { "-- Pilih nomor kolam penempatan --" },
                                                    fontSize = 12.5.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Color(0xFF0F172A)
                                                )
                                                Icon(
                                                    imageVector = Icons.Default.KeyboardArrowDown,
                                                    contentDescription = null,
                                                    tint = Color(0xFF0284C7),
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                        }

                                        DropdownMenu(
                                            expanded = pondDropdownExpanded,
                                            onDismissRequest = { pondDropdownExpanded = false },
                                            modifier = Modifier
                                                .fillMaxWidth(0.85f)
                                                .background(Color.White)
                                        ) {
                                            (1..28).forEach { num ->
                                                DropdownMenuItem(
                                                    text = {
                                                        Text(
                                                            text = "Kolam $num",
                                                            fontSize = 13.sp,
                                                            fontWeight = FontWeight.Medium,
                                                            color = Color(0xFF0F172A)
                                                        )
                                                    },
                                                    onClick = {
                                                        viewModel.updatePond("Kolam $num")
                                                        pondDropdownExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }

                                // Nama Ikan / Komoditas
                                Column {
                                    Text(
                                        text = "Nama Ikan / Komoditas:",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1E293B)
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Surface(
                                        shape = RoundedCornerShape(22.dp),
                                        color = Color.White,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(46.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(horizontal = 18.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            BasicTextField(
                                                value = state.commodityName,
                                                onValueChange = { viewModel.updateCommodity(it) },
                                                singleLine = true,
                                                textStyle = TextStyle(
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Color(0xFF0F172A)
                                                ),
                                                modifier = Modifier.weight(1f)
                                            )
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = null,
                                                tint = Color(0xFF94A3B8),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Tombol Bawah Kartu: Simpan & Aktifkan Pemantauan
                            Surface(
                                onClick = {
                                    if (state.selectedPond.isBlank() || state.selectedPond.startsWith("--")) {
                                        viewModel.updatePond("Kolam 1")
                                    }
                                    viewModel.saveDeviceConfiguration()
                                    Toast.makeText(context, "Perangkat berhasil disimpan!", Toast.LENGTH_SHORT).show()
                                    onSaveSuccess()
                                },
                                color = Color(0xFFBAE6FD),
                                shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    SaveDownloadTrayIcon(
                                        modifier = Modifier.size(18.dp),
                                        tint = Color(0xFF059669)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Simpan & Aktifkan Pemantauan",
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF059669)
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. Tombol Luar: Pindai Ulang QR Alat Lain
                item {
                    Surface(
                        onClick = {
                            viewModel.resetScanError()
                            onRescanClick()
                        },
                        shape = RoundedCornerShape(50.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                        shadowElevation = 1.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "Pindai Ulang QR Alat Lain",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF475569)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SaveDownloadTrayIcon(modifier: Modifier = Modifier, tint: Color = Color(0xFF059669)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)

        // Tray
        val tray = Path().apply {
            moveTo(w * 0.15f, h * 0.52f)
            lineTo(w * 0.15f, h * 0.85f)
            lineTo(w * 0.85f, h * 0.85f)
            lineTo(w * 0.85f, h * 0.52f)
        }
        drawPath(tray, color = tint, style = stroke)

        // Arrow down
        val arrow = Path().apply {
            moveTo(w * 0.5f, h * 0.15f)
            lineTo(w * 0.5f, h * 0.65f)
            moveTo(w * 0.32f, h * 0.45f)
            lineTo(w * 0.5f, h * 0.65f)
            lineTo(w * 0.68f, h * 0.45f)
        }
        drawPath(arrow, color = tint, style = stroke)
    }
}