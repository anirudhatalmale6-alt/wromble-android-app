package dk.wromble.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Brand colours (mirror iOS: wrombleRed #E20F1E, dark red #8f0712, accent #FFE08A)
val WrombleRed = Color(0xFFE20F1E)
val WrombleDarkRed = Color(0xFF8F0712)
val WrombleAccent = Color(0xFFFFE08A)
val WrombleBg = Color(0xFFF6F6F8)
val WrombleCardDark = Color(0xFF141519)

private val LightColors = lightColorScheme(
    primary = WrombleRed,
    onPrimary = Color.White,
    secondary = WrombleDarkRed,
    background = WrombleBg,
    surface = Color.White,
    onSurface = Color(0xFF1A1A1E)
)

private val DarkColors = darkColorScheme(
    primary = WrombleRed,
    onPrimary = Color.White,
    secondary = WrombleAccent,
    background = Color(0xFF0F0F12),
    surface = Color(0xFF1A1B20),
    onSurface = Color.White
)

@Composable
fun WrombleTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        content = content
    )
}
