package fi.heatcall.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Flame = Color(0xFFFF6D1F)
val FlameDeep = Color(0xFFD84315)
val Ember = Color(0xFFFFB74D)
val Frost = Color(0xFF4FC3F7)
val Night = Color(0xFF0E1A2B)
val NightSurface = Color(0xFF16263B)

private val DarkColors = darkColorScheme(
    primary = Flame,
    onPrimary = Color.White,
    primaryContainer = FlameDeep,
    onPrimaryContainer = Color.White,
    secondary = Frost,
    onSecondary = Night,
    tertiary = Ember,
    background = Night,
    onBackground = Color(0xFFE8EEF5),
    surface = Night,
    onSurface = Color(0xFFE8EEF5),
    surfaceVariant = NightSurface,
    onSurfaceVariant = Color(0xFFB8C4D3),
    surfaceContainer = NightSurface,
    error = Color(0xFFFF8A80),
)

@Composable
fun HeatCallTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        // Alkuperäisen sovelluksen tapaan aina tumma ulkoasu.
        colorScheme = DarkColors,
        content = content,
    )
}
