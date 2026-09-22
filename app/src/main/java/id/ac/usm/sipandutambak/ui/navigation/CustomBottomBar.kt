package id.ac.usm.sipandutambak.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SipanduBottomBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // 3 Status Figma:
    // Status 2: Dashboard aktif
    val isHomeSelected = currentRoute == Screen.Dashboard.route
    // Status 3: List aktif
    val isListSelected = currentRoute == Screen.DeviceList.route
    // Status 1: Parameter Sensor / Netral (keduanya false)

    val homeGradientAlpha by animateFloatAsState(
        targetValue = if (isHomeSelected) 1f else 0f,
        animationSpec = tween(280),
        label = "homeGradient"
    )
    val listGradientAlpha by animateFloatAsState(
        targetValue = if (isListSelected) 1f else 0f,
        animationSpec = tween(280),
        label = "listGradient"
    )

    val density = LocalDensity.current
    // Cekungan Halus ke Bawah (Cradle Notch) Sesuai Variant4.png
    val cradleNotchShape = remember(density) {
        val cornerR = with(density) { 20.dp.toPx() }
        val barTop = with(density) { 20.dp.toPx() }      // Garis datar bilah bar
        val cradleW = with(density) { 38.dp.toPx() }     // Setengah lebar cekungan
        val cradleDepth = with(density) { 32.dp.toPx() } // Kedalaman cekungan ke bawah

        GenericShape { size, _ ->
            val w = size.width
            val h = size.height
            val cx = w / 2f

            moveTo(0f, h)
            lineTo(0f, barTop + cornerR)
            quadraticBezierTo(0f, barTop, cornerR, barTop)
            lineTo(cx - cradleW, barTop)

            // Lengkungan cekungan turun ke bawah (Notch)
            cubicTo(
                cx - cradleW * 0.5f, barTop,
                cx - cradleW * 0.5f, barTop + cradleDepth,
                cx, barTop + cradleDepth
            )
            // Lengkungan cekungan naik kembali ke garis datar
            cubicTo(
                cx + cradleW * 0.5f, barTop + cradleDepth,
                cx + cradleW * 0.5f, barTop,
                cx + cradleW, barTop
            )

            lineTo(w - cornerR, barTop)
            quadraticBezierTo(w, barTop, w, barTop + cornerR)
            lineTo(w, h)
            close()
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .height(86.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        // 1. Bilah Utama dengan Cekungan Variant4.png
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(86.dp)
                .shadow(elevation = 6.dp, shape = cradleNotchShape)
                .clip(cradleNotchShape)
                .background(Color(0xFFCEE3FF)) // Status 1: Warna dasar solid #CEE3FF
        ) {
            // Status 2: Gradasi Dashboard (Kiri #CEE3FF, Kanan memudar ke #FFFFFF)
            if (homeGradientAlpha > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(homeGradientAlpha)
                        .background(
                            Brush.horizontalGradient(
                                0.0f to Color.Transparent,
                                0.60f to Color.Transparent,
                                1.0f to Color(0xFFFFFFFF)
                            )
                        )
                )
            }

            // Status 3: Gradasi List (Kiri #FFFFFF, Kanan #CEE3FF)
            if (listGradientAlpha > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(listGradientAlpha)
                        .background(
                            Brush.horizontalGradient(
                                0.0f to Color(0xFFFFFFFF),
                                0.60f to Color.Transparent,
                                1.0f to Color.Transparent
                            )
                        )
                )
            }

            // Deretan Menu Samping: Beranda & List (Sejajar di Bilah Datar)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(66.dp)
                    .align(Alignment.BottomCenter),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BottomNavItem(
                    label = "Beranda",
                    isSelected = isHomeSelected,
                    onClick = { onNavigate(Screen.Dashboard.route) },
                    modifier = Modifier.weight(1f)
                ) { selected, color ->
                    HomeNavIcon(isSelected = selected, tint = color)
                }

                // Ruang kosong untuk cekungan tombol PINDAI
                Spacer(modifier = Modifier.width(88.dp))

                BottomNavItem(
                    label = "List",
                    isSelected = isListSelected,
                    onClick = { onNavigate(Screen.DeviceList.route) },
                    modifier = Modifier.weight(1f)
                ) { _, color ->
                    ListNavIcon(tint = color)
                }
            }
        }

        // 2. Tombol Bulat Melayang PINDAI (Menjorok ke Atas Menembus Garis Bar)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = 0.dp) // Puncak tombol tepat di posisi teratas
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onNavigate(Screen.Scan.route) }
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .shadow(elevation = 8.dp, shape = CircleShape)
                    .border(BorderStroke(2.8.dp, Color(0xFFFFFFFF)), CircleShape)
                    .clip(CircleShape)
                    .background(
                        Brush.verticalGradient(
                            0.07f to Color(0xFF52BCF4),
                            0.62f to Color(0xFFCAE5F8),
                            1.00f to Color(0xFFCAE5F8)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                CameraScanIcon(modifier = Modifier.size(26.dp))
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "PINDAI",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF000000), // Hitam pekat Figma #000000
                letterSpacing = 0.5.sp
            )
        }
    }
}

@Composable
private fun BottomNavItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable (isSelected: Boolean, color: Color) -> Unit
) {
    val activeColor = Color(0xFF0088CC)   // Biru Aktif Figma #0088CC
    val inactiveColor = Color(0xFF000000) // Hitam Tidak Aktif Figma #000000

    val itemColor by animateColorAsState(
        targetValue = if (isSelected) activeColor else inactiveColor,
        animationSpec = tween(220),
        label = "itemColor"
    )

    Column(
        modifier = modifier
            .fillMaxHeight()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
            icon(isSelected, itemColor)
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            fontSize = 11.5.sp,
            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
            color = itemColor
        )
    }
}

@Composable
fun HomeNavIcon(isSelected: Boolean, tint: Color) {
    Canvas(modifier = Modifier.size(22.dp)) {
        val w = size.width
        val h = size.height

        val roofPath = Path().apply {
            moveTo(w * 0.5f, h * 0.08f)
            lineTo(w * 0.92f, h * 0.44f)
            lineTo(w * 0.82f, h * 0.44f)
            lineTo(w * 0.82f, h * 0.90f)
            lineTo(w * 0.18f, h * 0.90f)
            lineTo(w * 0.18f, h * 0.44f)
            lineTo(w * 0.08f, h * 0.44f)
            close()
        }

        if (isSelected) {
            drawPath(roofPath, color = tint)
            drawRoundRect(
                color = Color(0xFFFFFFFF),
                topLeft = Offset(w * 0.38f, h * 0.52f),
                size = Size(w * 0.24f, h * 0.38f),
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
            )
        } else {
            drawPath(
                path = roofPath,
                color = tint,
                style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
            val doorPath = Path().apply {
                moveTo(w * 0.38f, h * 0.90f)
                lineTo(w * 0.38f, h * 0.56f)
                cubicTo(w * 0.38f, h * 0.50f, w * 0.62f, h * 0.50f, w * 0.62f, h * 0.56f)
                lineTo(w * 0.62f, h * 0.90f)
            }
            drawPath(
                path = doorPath,
                color = tint,
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
    }
}

@Composable
fun ListNavIcon(tint: Color) {
    Canvas(modifier = Modifier.size(22.dp)) {
        val w = size.width
        val h = size.height
        val strokeW = 2.2.dp.toPx()

        listOf(0.24f, 0.50f, 0.76f).forEach { yRatio ->
            val y = h * yRatio
            drawCircle(color = tint, radius = 1.6.dp.toPx(), center = Offset(w * 0.15f, y))
            drawLine(
                color = tint,
                start = Offset(w * 0.34f, y),
                end = Offset(w * 0.90f, y),
                strokeWidth = strokeW,
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
fun CameraScanIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        val black = Color(0xFF000000)
        val bracketLen = w * 0.22f

        val tl = Path().apply {
            moveTo(w * 0.08f, h * 0.30f); lineTo(w * 0.08f, h * 0.12f); lineTo(w * 0.08f + bracketLen, h * 0.12f)
        }
        drawPath(tl, color = black, style = stroke)

        val tr = Path().apply {
            moveTo(w * 0.92f - bracketLen, h * 0.12f); lineTo(w * 0.92f, h * 0.12f); lineTo(w * 0.92f, h * 0.30f)
        }
        drawPath(tr, color = black, style = stroke)

        val bl = Path().apply {
            moveTo(w * 0.08f, h * 0.70f); lineTo(w * 0.08f, h * 0.88f); lineTo(w * 0.08f + bracketLen, h * 0.88f)
        }
        drawPath(bl, color = black, style = stroke)

        val br = Path().apply {
            moveTo(w * 0.92f - bracketLen, h * 0.88f); lineTo(w * 0.92f, h * 0.88f); lineTo(w * 0.92f, h * 0.70f)
        }
        drawPath(br, color = black, style = stroke)

        val camPath = Path().apply {
            moveTo(w * 0.28f, h * 0.36f)
            lineTo(w * 0.36f, h * 0.36f)
            lineTo(w * 0.40f, h * 0.30f)
            lineTo(w * 0.60f, h * 0.30f)
            lineTo(w * 0.64f, h * 0.36f)
            lineTo(w * 0.72f, h * 0.36f)
            cubicTo(w * 0.76f, h * 0.36f, w * 0.76f, h * 0.40f, w * 0.76f, h * 0.40f)
            lineTo(w * 0.76f, h * 0.68f)
            lineTo(w * 0.24f, h * 0.68f)
            lineTo(w * 0.24f, h * 0.40f)
            close()
        }
        drawPath(camPath, color = black, style = stroke)

        drawCircle(
            color = black,
            radius = w * 0.11f,
            center = Offset(w * 0.5f, h * 0.51f),
            style = stroke
        )
    }
}