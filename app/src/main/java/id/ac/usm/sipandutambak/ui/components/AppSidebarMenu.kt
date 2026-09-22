package id.ac.usm.sipandutambak.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@Composable
fun AppSidebarMenu(
    showMenu: Boolean,
    onDismiss: () -> Unit,
    onInfoAlatClick: () -> Unit,
    onPusatPanggilanClick: () -> Unit
) {
    if (!showMenu) return

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF53C5FE)),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp)
            ) {
                // Tombol Close Silang di Kanan Atas
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Tutup Menu",
                        tint = Color.Black,
                        modifier = Modifier
                            .size(26.dp)
                            .clickable { onDismiss() }
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Item 1: Informasi Alat
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onInfoAlatClick() }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    InfoCircleIcon(modifier = Modifier.size(30.dp))
                    Text(
                        text = "Informasi Alat",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }

                HorizontalDivider(
                    color = Color(0x33000000),
                    thickness = 1.dp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                // Item 2: Pusat Panggilan
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPusatPanggilanClick() }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    HeadsetCallIcon(modifier = Modifier.size(30.dp))
                    Text(
                        text = "Pusat Panggilan",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }

                Spacer(modifier = Modifier.height(26.dp))

                // Versi Aplikasi
                Text(
                    text = "Versi Aplikasi : 0.0.1 Beta",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF1E293B),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun InfoCircleIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        // Lingkaran luar putih
        drawCircle(
            color = Color.White,
            radius = w * 0.46f,
            style = Stroke(width = 2.4.dp.toPx())
        )
        // Titik 'i'
        drawCircle(
            color = Color.White,
            radius = 2.2.dp.toPx(),
            center = Offset(w * 0.5f, h * 0.33f)
        )
        // Batang 'i'
        drawLine(
            color = Color.White,
            start = Offset(w * 0.5f, h * 0.46f),
            end = Offset(w * 0.5f, h * 0.72f),
            strokeWidth = 2.4.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun HeadsetCallIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        // Headband melingkar
        drawArc(
            color = Color.White,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(w * 0.15f, h * 0.15f),
            size = Size(w * 0.7f, h * 0.6f),
            style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round)
        )
        // Earpad kiri
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(w * 0.1f, h * 0.45f),
            size = Size(w * 0.18f, h * 0.32f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
        )
        // Earpad kanan
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(w * 0.72f, h * 0.45f),
            size = Size(w * 0.18f, h * 0.32f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
        )
        // Mic arm
        drawLine(
            color = Color.White,
            start = Offset(w * 0.8f, h * 0.72f),
            end = Offset(w * 0.6f, h * 0.88f),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawCircle(
            color = Color.White,
            radius = 2.5.dp.toPx(),
            center = Offset(w * 0.58f, h * 0.88f)
        )
    }
}