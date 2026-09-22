package id.ac.usm.sipandutambak.ui.about

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.ac.usm.sipandutambak.R

@Composable
fun AboutSystemScreen(
    modifier: Modifier = Modifier
) {
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

                    Spacer(modifier = Modifier.height(14.dp))

                    // Garis Pembatas dengan Judul TENTANG SISTEM di Tengah
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            thickness = 1.5.dp,
                            color = Color(0xFF7DD3FC)
                        )
                        Text(
                            text = "TENTANG SISTEM",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF0F172A),
                            letterSpacing = 0.5.sp,
                            modifier = Modifier.padding(horizontal = 10.dp)
                        )
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            thickness = 1.5.dp,
                            color = Color(0xFF7DD3FC)
                        )
                    }
                }
            }

            // Kartu 1: Sensor Kualitas Air
            item {
                AboutSystemCard(title = "Sensor Kualitas Air") {
                    Text(
                        text = "6 sensor utama:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF334155),
                        modifier = Modifier.padding(bottom = 10.dp)
                    )
                    Text(
                        text = "-TDS : (kekeruhan partikel, 0-1000 ppm),measurement accuracy: ±10% F.S. (25°C),",
                        fontSize = 12.sp,
                        color = Color(0xFF334155),
                        lineHeight = 17.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "- pH : (keasaman air, 0-14), Kisaran Suhu deteksi 0-80 °C, dan  Kelembaban: 95% RH (kelembaban nominal 65% RH).",
                        fontSize = 12.sp,
                        color = Color(0xFF334155),
                        lineHeight = 17.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "-DO : Rentang Ukur 0 - 20 mg/L (atau 0 - 200% Saturasi)  Akurasi: ±3% FS",
                        fontSize = 12.sp,
                        color = Color(0xFF334155),
                        lineHeight = 17.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "-COD :",
                        fontSize = 12.sp,
                        color = Color(0xFF334155),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "-NO3- :",
                        fontSize = 12.sp,
                        color = Color(0xFF334155),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "-NH4+ :",
                        fontSize = 12.sp,
                        color = Color(0xFF334155)
                    )
                }
            }

            // Kartu 2: Sensor Kualitas Lingkungan
            item {
                AboutSystemCard(title = "Sensor Kualitas Lingkungan") {
                    Text(
                        text = "JSN-SR04T ultrasonik waterproof untuk level air (cm), dan sensor hujan dengan output analog & digital. Semua pembacaan bersifat non-blocking.",
                        fontSize = 12.sp,
                        color = Color(0xFF334155),
                        lineHeight = 18.sp,
                        textAlign = TextAlign.Start
                    )
                }
            }

            // Kartu 3: Huawei Cloud
            item {
                AboutSystemCard(title = "Huawei Cloud") {
                    Text(
                        text = "Huawei Cloud adalah layanan cloud dari Huawei yang digunakan untuk menyimpan dan mengelola data. IoTDA (IoT Device Access) digunakan untuk menghubungkan alat dan sensor kualitas air kolam ke cloud, sehingga data seperti pH, suhu, dan kekeruhan air dapat dikirim dan dipantau secara online.",
                        fontSize = 12.sp,
                        color = Color(0xFF334155),
                        lineHeight = 18.sp,
                        textAlign = TextAlign.Start
                    )
                }
            }
        }
    }
}

@Composable
fun AboutSystemCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.5.dp, Color(0xFF38BDF8)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            )
            content()
        }
    }
}