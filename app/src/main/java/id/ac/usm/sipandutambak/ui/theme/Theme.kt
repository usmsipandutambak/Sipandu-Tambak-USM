package id.ac.usm.sipandutambak.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = NavyDark,
    secondary = AccentTeal,
    tertiary = ChartBlue,
    background = BgGradientEnd,
    surface = SurfaceCard,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = NavyDark,
    onSurface = NavyDark
)

@Composable
fun SipanduTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        content = content
    )
}