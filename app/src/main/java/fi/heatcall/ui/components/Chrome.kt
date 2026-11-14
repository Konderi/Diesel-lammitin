package fi.heatcall.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Värit, joilla jäljitellään alkuperäisen sovelluksen sinistä hiilikuitu- ja kromiulkoasua. */
object ChromeColors {
    val Deep = Color(0xFF061423)
    val Mid = Color(0xFF0F3553)
    val Bright = Color(0xFF2C77A8)
    val Glow = Color(0xFF3FA9F5)
    val Panel = Color(0xCC1B2530)
    val PanelBorder = Color(0x66FFFFFF)
    val Label = Color(0xFFE6EDF3)
    val Drum = Color(0xFF0B0F14)
    val Bar = Color(0xE6101418)
}

/** Koko näytön tausta: sininen liukuväri, vaalea valojuova ja hienojakoinen kuviointi. */
@Composable
fun CarbonBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0f to ChromeColors.Bright,
                    0.35f to ChromeColors.Mid,
                    1f to ChromeColors.Deep,
                ),
            ),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            // Valojuova vasemmasta yläkulmasta.
            drawRect(
                Brush.radialGradient(
                    colors = listOf(Color(0x5591D7FF), Color.Transparent),
                    center = Offset(size.width * 0.2f, size.height * 0.05f),
                    radius = size.width * 0.9f,
                ),
            )
            drawRect(
                Brush.linearGradient(
                    0f to Color.Transparent,
                    0.45f to Color(0x14FFFFFF),
                    0.55f to Color(0x08FFFFFF),
                    1f to Color.Transparent,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, size.height),
                ),
            )
            // Hiilikuitukuvio: tiheä ristikko hyvin läpikuultavana.
            val step = 6.dp.toPx()
            val line = Color(0x0D000000)
            var x = -size.height
            while (x < size.width) {
                drawLine(line, Offset(x, 0f), Offset(x + size.height, size.height), strokeWidth = 1f)
                drawLine(line, Offset(x + size.height, 0f), Offset(x, size.height), strokeWidth = 1f)
                x += step
            }
            // Vaakasuora harjattu metalli -juovitus.
            var y = 0f
            val h = 3.dp.toPx()
            while (y < size.height) {
                drawLine(Color(0x08FFFFFF), Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
                y += h
            }
        }
        content()
    }
}

/** Läpikuultava, pyöristetty paneeli laitekorttia varten. */
fun Modifier.glassPanel(): Modifier = this
    .clip(RoundedCornerShape(10.dp))
    .background(
        Brush.verticalGradient(listOf(Color(0xCC3A4652), Color(0xCC151C24))),
    )
    .border(1.dp, ChromeColors.PanelBorder, RoundedCornerShape(10.dp))

/** Kromattu pyöreä painike sinisellä hehkurenkaalla. */
@Composable
fun ChromeStartButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 150.dp,
    enabled: Boolean = true,
    glow: Color = ChromeColors.Glow,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Box(
        modifier
            .size(size)
            .scale(if (pressed) 0.96f else 1f),
        contentAlignment = Alignment.Center,
    ) {
        // Hehkurengas
        Canvas(Modifier.fillMaxSize()) {
            val r = this.size.minDimension / 2
            drawCircle(
                Brush.radialGradient(
                    0.70f to Color.Transparent,
                    0.80f to glow.copy(alpha = if (enabled) 0.9f else 0.3f),
                    0.88f to glow.copy(alpha = 0.35f),
                    1f to Color.Transparent,
                    radius = r,
                ),
                radius = r,
            )
            // Tumma kehys
            drawCircle(Color(0xFF1A2633), radius = r * 0.80f)
            drawCircle(
                Brush.verticalGradient(listOf(Color(0xFF9FB3C6), Color(0xFF263544))),
                radius = r * 0.78f,
            )
        }
        // Kromipinta
        Box(
            Modifier
                .size(size * 0.70f)
                .shadow(8.dp, CircleShape)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFFFFFFFF), Color(0xFFD5DBE1), Color(0xFF8A949E), Color(0xFFC9D0D6)),
                        center = Offset.Unspecified,
                    ),
                )
                .background(
                    Brush.verticalGradient(
                        0f to Color(0x66FFFFFF),
                        0.5f to Color(0x00FFFFFF),
                        0.52f to Color(0x22000000),
                        1f to Color(0x11FFFFFF),
                    ),
                )
                .clickable(interactionSource = interaction, indication = null, enabled = enabled, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                label,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.padding(horizontal = 6.dp),
                style = TextStyle(
                    color = if (enabled) Color(0xFF3B4650) else Color(0xFF8A949E),
                    fontSize = when {
                        label.length > 8 -> 15.sp
                        label.length > 5 -> 18.sp
                        else -> 22.sp
                    },
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp,
                    shadow = Shadow(Color.White, Offset(0f, 2f), 1f),
                ),
            )
        }
    }
}
