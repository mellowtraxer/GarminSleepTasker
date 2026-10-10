package de.ricci.garminsleep.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun FrostedGlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    borderColors: List<Color> = listOf(
        Color(0xFF00F0FF).copy(alpha = 0.9f), // Leuchtendes Cyan oben links
        Color(0xFFA855F7).copy(alpha = 0.7f), // Übergang zu Violett
        Color(0xFFEC4899).copy(alpha = 0.4f), // Neon-Pink
        Color(0x15FFFFFF)                     // Sanftes Ausklingen
    ),
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)

    // Subtiler Milchglas-Reflexions-Verlauf von oben nach unten
    val glassFill = Brush.verticalGradient(
        listOf(
            Color(0x22FFFFFF), // Hellerer Lichteinfall oben
            Color(0x0CFFFFFF)  // Dunklerer, matterer Schimmer unten
        )
    )

    Box(
        modifier = modifier
            .clip(shape)
            .background(glassFill)
            .border(
                width = 1.2.dp,
                brush = Brush.linearGradient(borderColors),
                shape = shape
            )
            .padding(18.dp)
    ) {
        Column {
            content()
        }
    }
}
