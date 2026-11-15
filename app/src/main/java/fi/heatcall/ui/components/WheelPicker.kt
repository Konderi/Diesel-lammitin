package fi.heatcall.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlin.math.abs

/**
 * Pystysuuntainen rullavalitsin: kolme riviä näkyvissä, ja keskimmäinen on valittuna.
 */
@Composable
fun <T> WheelPicker(
    items: List<T>,
    selected: T,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    label: (T) -> String = { it.toString() },
) {
    val itemHeight = 34.dp
    val visible = 3
    val density = LocalDensity.current
    val itemPx = with(density) { itemHeight.toPx() }
    val startIndex = items.indexOf(selected).coerceAtLeast(0)
    val state = rememberLazyListState(initialFirstVisibleItemIndex = startIndex)
    val fling = rememberSnapFlingBehavior(lazyListState = state)
    val currentOnSelected by rememberUpdatedState(onSelected)

    val centerIndex by remember {
        derivedStateOf {
            val extra = if (state.firstVisibleItemScrollOffset > itemPx / 2) 1 else 0
            (state.firstVisibleItemIndex + extra).coerceIn(0, items.lastIndex)
        }
    }

    // Ulkoinen muutos (esim. toinen laite valittu) → rullaa oikeaan kohtaan.
    LaunchedEffect(selected, items) {
        val target = items.indexOf(selected)
        if (target >= 0 && target != centerIndex && !state.isScrollInProgress) state.scrollToItem(target)
    }

    // Kun rullaus pysähtyy, ilmoitetaan valinta.
    LaunchedEffect(state, items) {
        snapshotFlow { state.isScrollInProgress }
            .distinctUntilChanged()
            .filter { !it }
            .collect {
                val item = items.getOrNull(centerIndex) ?: return@collect
                currentOnSelected(item)
            }
    }

    Box(
        modifier
            .height(itemHeight * visible)
            .clip(RoundedCornerShape(6.dp))
            .background(
                Brush.verticalGradient(
                    0f to Color(0xFF000000),
                    0.30f to Color(0xFF2A3038),
                    0.50f to Color(0xFF3A424C),
                    0.70f to Color(0xFF2A3038),
                    1f to Color(0xFF000000),
                ),
            )
            .border(1.dp, Color(0x55FFFFFF), RoundedCornerShape(6.dp)),
        contentAlignment = Alignment.Center,
    ) {
        LazyColumn(
            state = state,
            flingBehavior = fling,
            contentPadding = PaddingValues(vertical = itemHeight * ((visible - 1) / 2)),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
        ) {
            itemsIndexed(items) { index, item ->
                val distance = abs(index - centerIndex)
                Box(
                    Modifier
                        .height(itemHeight)
                        .fillMaxWidth()
                        .graphicsLayer {
                            alpha = if (distance == 0) 1f else 0.40f
                            scaleY = if (distance == 0) 1f else 0.80f
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        label(item),
                        color = Color(0xFFE8EDF2),
                        fontSize = if (distance == 0) 19.sp else 15.sp,
                        fontWeight = if (distance == 0) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1,
                    )
                }
            }
        }
        // Valintapalkin kiiltävä kehys keskellä.
        Box(
            Modifier
                .fillMaxWidth()
                .height(itemHeight)
                .border(1.dp, Color(0x33FFFFFF))
                .background(
                    Brush.verticalGradient(listOf(Color(0x22FFFFFF), Color(0x05FFFFFF), Color(0x18FFFFFF))),
                ),
        )
    }
}
