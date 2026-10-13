package id.ac.usm.sipandutambak.ui.devices.detail

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.ac.usm.sipandutambak.ui.components.AppSidebarMenu

@Composable
fun HistoryDetailScreen(
    kolamId: String,
    commodityName: String,
    onBackClick: () -> Unit,
    onAboutSystemClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showSidebar by remember { mutableStateOf(false) }

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
                SipanduHeader(onMenuClick = { showSidebar = true })
            }

            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "KOLAM $kolamId",
                        fontSize = 17.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF0F172A),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = commodityName.uppercase(),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF0F172A),
                        textAlign = TextAlign.Center
                    )
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE2EDFE)),
                    border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Belum ada alat yang terhubung ke kolam ini. Hubungkan alat melalui menu List untuk melihat data realtime.",
                        fontSize = 11.5.sp,
                        color = Color(0xFF334155),
                        lineHeight = 16.sp,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }

            item {
                TelemetryChartSection(
                    logs = emptyList(),
                    currentPh = 0.0,
                    currentTds = 0.0,
                    currentTurbidity = 0.0
                )
            }

            item {
                TelemetryLogSection(
                    logs = emptyList(),
                    onExportClick = {
                        Toast.makeText(context, "Belum ada data untuk diekspor", Toast.LENGTH_SHORT).show()
                    }
                )
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