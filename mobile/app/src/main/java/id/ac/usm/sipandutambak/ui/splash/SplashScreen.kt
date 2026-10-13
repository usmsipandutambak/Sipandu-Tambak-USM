package id.ac.usm.sipandutambak.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import id.ac.usm.sipandutambak.R

@Composable
fun SplashScreen(
    onTimeout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val alphaAnim = remember { Animatable(0f) }
    val scaleAnim = remember { Animatable(0.92f) }

    LaunchedEffect(Unit) {
        alphaAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing)
        )
        scaleAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing)
        )
        delay(1500L)
        onTimeout()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0E2338)), // Latar belakang navy gelap sesuai Figma
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .alpha(alphaAnim.value)
                .scale(scaleAnim.value)
        ) {
            // Memanggil langsung vektor ic_launch tanpa Box lingkaran tambahan
            Image(
                painter = painterResource(id = R.drawable.ic_launch),
                contentDescription = "Logo Sipandu Tambak",
                modifier = Modifier.size(92.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "SIPANDU TAMBAK",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "SISTEM PEMANTAUAN AIR TAMBAK",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF61CFFF),
                letterSpacing = 1.4.sp
            )
        }
    }
}