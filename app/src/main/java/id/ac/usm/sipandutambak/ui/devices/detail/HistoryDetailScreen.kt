package id.ac.usm.sipandutambak.ui.devices.detail

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
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
import id.ac.usm.sipandutambak.data.api.SupabaseApi
import id.ac.usm.sipandutambak.ui.components.AppSidebarMenu
import id.ac.usm.sipandutambak.util.CsvExportHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

@Composable
fun HistoryDetailScreen(
    kolamId: String,
    commodityName: String = "LELE",
    onBackClick: () -> Unit = {},
    onAboutSystemClick: () -> Unit = {}
) {
    val context = LocalContext.current
    var showSidebar by remember { mutableStateOf(false) }

    var logsList by remember { mutableStateOf<List<SensorLogRow>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(kolamId) {
        withContext(Dispatchers.IO) {
            try {
                val jsonLogs = SupabaseApi.getSensorLogs("SIPANDU_A", limit = 30)
                val list = mutableListOf<SensorLogRow>()
                val inFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                val outFormat = SimpleDateFormat("HH:mm", Locale("id", "ID"))

                for (i in 0 until jsonLogs.length()) {
                    val obj = jsonLogs.getJSONObject(i)
                    val rawDate = obj.optString("created_at", "")
                    val formattedTime = try {
                        val parsed = inFormat.parse(rawDate.take(19))
                        if (parsed != null) outFormat.format(parsed) else rawDate.takeLast(8).take(5)
                    } catch (e: Exception) {
                        "Waktu"
                    }

                    list.add(
                        SensorLogRow(
                            timeLabel = formattedTime,
                            ph = obj.optDouble("ph_level", 0.0),
                            tds = obj.optDouble("tds_ppm", 0.0).toInt(),
                            status = obj.optString("status", "NORMAL")
                        )
                    )
                }
                logsList = list
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFDCEBFE))
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 140.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
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

                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.5.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { showSidebar = true }
                            .padding(6.dp)
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
                        text = "HISTORY TERAKHIR",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF475569),
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "KOLAM " + kolamId + " - " + commodityName.uppercase(),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF0F172A)
                    )
                }
            }

            // Kartu Tabel Log Sensor Terpadu Riil
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Data Riwayat Log Sensor",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )

                            // Tombol Export CSV menggunakan Box (Bebas Error Overload)
                            Box(
                                modifier = Modifier
                                    .height(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF059669))
                                    .clickable {
                                        val headers = listOf("WAKTU", "KOLAM", "KOMODITAS", "PH", "TDS (PPM)", "DO (MG/L)", "STATUS")
                                        val rows = logsList.map { log ->
                                            listOf(
                                                log.timeLabel,
                                                "Kolam $kolamId",
                                                commodityName,
                                                log.ph.toString(),
                                                log.tds.toString(),
                                                log.doLevel.toString(),
                                                log.status
                                            )
                                        }
                                        CsvExportHelper.exportAndShareCsv(
                                            context = context,
                                            fileName = "SIPANDU_History_Kolam_$kolamId",
                                            headers = headers,
                                            rows = rows
                                        )
                                    }
                                    .padding(horizontal = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "↑ Export CSV",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Surface(
                            shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
                            color = Color(0xFFF1F5F9),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 7.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TableCellHeader("WAKTU")
                                TableCellHeader("PH")
                                TableCellHeader("TDS")
                                TableCellHeader("COD")
                                TableCellHeader("NH4+")
                                TableCellHeader("NO3-")
                                TableCellHeader("DO")
                                TableCellHeader("STATUS")
                            }
                        }

                        if (isLoading) {
                            Box(modifier = Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = Color(0xFF0284C7), modifier = Modifier.size(24.dp))
                            }
                        } else if (logsList.isEmpty()) {
                            Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                Text("Tidak ada catatan riwayat untuk kolam ini.", fontSize = 12.sp, color = Color(0xFF64748B))
                            }
                        } else {
                            logsList.forEach { log ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(log.timeLabel, fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0284C7))
                                    Text(log.ph.toString(), fontSize = 9.5.sp, color = Color(0xFF334155))
                                    Text(log.tds.toString(), fontSize = 9.5.sp, color = Color(0xFF334155))
                                    Text(log.cod.toString(), fontSize = 9.5.sp, color = Color(0xFF334155))
                                    Text(log.nh4.toString(), fontSize = 9.5.sp, color = Color(0xFF334155))
                                    Text(log.no3.toString(), fontSize = 9.5.sp, color = Color(0xFF334155))
                                    Text(log.doLevel.toString(), fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0284C7))

                                    Surface(
                                        color = if (log.status.equals("BAHAYA", true)) Color(0xFFFECDD3) else Color(0xFF86EFAC),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.height(18.dp)
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier.padding(horizontal = 6.dp)
                                        ) {
                                            Text(
                                                text = log.status.uppercase(),
                                                fontSize = 7.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (log.status.equals("BAHAYA", true)) Color(0xFF991B1B) else Color(0xFF065F46)
                                            )
                                        }
                                    }
                                }
                                HorizontalDivider(color = Color(0xFFF8FAFC))
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
fun TableCellHeader(text: String) {
    Text(
        text = text,
        fontSize = 8.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF64748B),
        textAlign = TextAlign.Center
    )
}