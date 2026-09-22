package id.ac.usm.sipandutambak.ui.devices

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import id.ac.usm.sipandutambak.R
import id.ac.usm.sipandutambak.data.model.DeviceData
import id.ac.usm.sipandutambak.ui.components.AppSidebarMenu

@Composable
fun DeviceListScreen(
    viewModel: DeviceListViewModel,
    onDeviceClick: (String) -> Unit,
    onConfigClick: (String) -> Unit = {},
    onAboutSystemClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()
    var deviceToDelete by remember { mutableStateOf<DeviceData?>(null) }
    var showSidebar by remember { mutableStateOf(false) }

    val filteredDevices = state.devices.filter { dev ->
        val matchesSearch = dev.name.contains(state.searchQuery, ignoreCase = true) ||
                dev.pondName.contains(state.searchQuery, ignoreCase = true) ||
                dev.commodity.contains(state.searchQuery, ignoreCase = true)
        val matchesFilter = when (state.selectedFilter) {
            "Normal" -> dev.status.equals("NORMAL", ignoreCase = true) && dev.isConnected
            "Waspada" -> dev.status.equals("WASPADA", ignoreCase = true) && dev.isConnected
            "Bahaya" -> dev.status.equals("BAHAYA", ignoreCase = true) && dev.isConnected
            else -> true
        }
        matchesSearch && matchesFilter
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFDCEBFE))
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 120.dp),
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

                    // Hamburger Menu (3 Garis)
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

            // List Alat + Input Pencarian
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "List Alat",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFF1F5F9),
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (state.searchQuery.isEmpty()) {
                                    Text(
                                        text = "Cari ID Alat, Sensor, Kolam...",
                                        fontSize = 11.5.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                                BasicTextField(
                                    value = state.searchQuery,
                                    onValueChange = { viewModel.onSearchQueryChange(it) },
                                    singleLine = true,
                                    textStyle = TextStyle(fontSize = 12.5.sp, color = Color(0xFF0F172A)),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }

            // Filter Tabs
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Semua", "Normal", "Waspada", "Bahaya").forEach { filter ->
                        val isSelected = state.selectedFilter == filter
                        Surface(
                            onClick = { viewModel.onFilterChange(filter) },
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) Color.White else Color(0xFFE2E8F0),
                            shadowElevation = if (isSelected) 2.dp else 0.dp,
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = filter,
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color(0xFF0F172A) else Color(0xFF64748B)
                                )
                            }
                        }
                    }
                }
            }

            // List Kartu Alat
            items(filteredDevices, key = { it.id }) { device ->
                DeviceCardItem(
                    device = device,
                    onClick = { onDeviceClick(device.id) },
                    onGearClick = {
                        viewModel.prepareEditDevice(device.id)
                        onConfigClick(device.id)
                    },
                    onDeleteClick = { deviceToDelete = device }
                )
            }
        }

        // Pop-up Konfirmasi Hapus
        deviceToDelete?.let { target ->
            Dialog(onDismissRequest = { deviceToDelete = null }) {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFD6E7FC)),
                    border = BorderStroke(1.dp, Color(0xFF93C5FD)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Apakah Anda yakin ingin\nmenghapus " + target.name + " ?",
                            fontSize = 18.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A),
                            textAlign = TextAlign.Center,
                            lineHeight = 25.sp
                        )

                        Spacer(modifier = Modifier.height(28.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Button(
                                onClick = {
                                    viewModel.deleteDevice(target.id)
                                    deviceToDelete = null
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBAE6FD)),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                            ) {
                                Text(
                                    text = "Ya",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                            }

                            Button(
                                onClick = { deviceToDelete = null },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF7D1DB)),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                            ) {
                                Text(
                                    text = "Tidak",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Sidebar Menu Pop-up Terhubung ke Tentang Sistem
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
fun DeviceCardItem(
    device: DeviceData,
    onClick: () -> Unit,
    onGearClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val gradientColors = when {
        !device.isConnected -> listOf(Color.White, Color.White)
        device.status.equals("BAHAYA", ignoreCase = true) -> listOf(Color.White, Color(0xFFFFD1D7))
        device.status.equals("WASPADA", ignoreCase = true) -> listOf(Color.White, Color(0xFFFEF08A))
        else -> listOf(Color.White, Color(0xFFBAF7D0))
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier
            .fillMaxWidth()
            .shadow(3.dp, RoundedCornerShape(18.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .background(Brush.horizontalGradient(gradientColors))
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color.White,
                shadowElevation = 2.dp,
                modifier = Modifier
                    .size(54.dp)
                    .clickable { onGearClick() }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Konfigurasi Alat",
                        tint = Color(0xFF93C5FD),
                        modifier = Modifier.size(30.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = device.name,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF0F172A)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "|",
                            fontSize = 13.sp,
                            color = Color(0xFFCBD5E1),
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = device.commodity,
                            fontSize = 11.5.sp,
                            color = Color(0xFF0284C7),
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Hapus Alat",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier
                            .size(19.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { onDeleteClick() }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(5.dp),
                        color = Color.Transparent,
                        border = BorderStroke(1.dp, Color(0xFF0D9488)),
                        modifier = Modifier.height(19.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 5.dp)
                        ) {
                            Text(
                                text = device.pondName,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "🔋 " + device.batteryPercent + "%",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF334155)
                        )
                        Text(
                            text = "📶 " + device.signalQuality,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF334155)
                        )

                        if (device.isConnected) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF059669),
                                modifier = Modifier.height(18.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(horizontal = 5.dp)
                                ) {
                                    Text(
                                        text = "TERHUBUNG",
                                        fontSize = 7.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}