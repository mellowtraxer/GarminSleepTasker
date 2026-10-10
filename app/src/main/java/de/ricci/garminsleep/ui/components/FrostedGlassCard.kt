package de.ricci.garminsleep.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object StudioGlassTuning {
    var blur by mutableIntStateOf(20)
    var glass by mutableIntStateOf(34)
    var neon by mutableIntStateOf(35)
}

@Composable
fun FrostedGlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    glowColors: List<Color> = listOf(
        Color(0xFF00F0FF), // Neon Cyan
        Color(0xFFA855F7), // Neon Purple
        Color(0xFFFF007F)  // Neon Pink
    ),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed && onClick != null) 0.985f else 1f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 550f),
        label = "glassPressScale"
    )
    val pressGlow by animateFloatAsState(
        targetValue = if (pressed && onClick != null) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 420f),
        label = "glassPressGlow"
    )
    val neonPower = StudioGlassTuning.neon.coerceIn(0, 100) / 100f
    val blurPower = StudioGlassTuning.blur.coerceIn(0, 100) / 100f
    val glassPower = StudioGlassTuning.glass.coerceIn(0, 100) / 100f
    val shape = RoundedCornerShape(cornerRadius)
    val glowBrush = Brush.linearGradient(glowColors)

    // Subtiler Lichteinfall auf dem Milchglas
    val glassFill = Brush.verticalGradient(
        listOf(
            Color.White.copy(alpha = .035f + glassPower * .25f), // Glasreflex
            Color.White.copy(alpha = .008f + glassPower * .075f)  // Glasboden
        )
    )

    Box(modifier = modifier
        .graphicsLayer {
            scaleX = pressScale
            scaleY = pressScale
        }
        .then(if (onClick != null) Modifier.clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = { DreamscapeMotion.ripple(); onClick() }
        ) else Modifier)
    ) {
        // One continuous gradient, with two very low-opacity soft contours.
        // Keep every contour rounded and INSIDE the card bounds, avoiding
        // both rectangular blur tiles and thick concentric neon tubing.
        Box(Modifier.matchParentSize().drawBehind {
            if (neonPower > 0f) {
                val glow = Brush.linearGradient(glowColors.map {
                    it.copy(alpha = (neonPower * (.14f + pressGlow * .08f)).coerceIn(0f, 1f))
                })
                val soft = Brush.linearGradient(glowColors.map {
                    it.copy(alpha = (neonPower * (.055f + pressGlow * .035f)).coerceIn(0f, 1f))
                })
                val inner = (1.8f + blurPower * 2f).dp.toPx()
                val outer = (3f + blurPower * 3.5f).dp.toPx()
                drawRoundRect(
                    brush = soft,
                    topLeft = Offset(outer / 2f, outer / 2f),
                    size = Size((size.width - outer).coerceAtLeast(0f),
                        (size.height - outer).coerceAtLeast(0f)),
                    cornerRadius = CornerRadius((cornerRadius.toPx() - outer / 2f).coerceAtLeast(0f)),
                    style = Stroke(width = outer)
                )
                drawRoundRect(
                    brush = glow,
                    topLeft = Offset(inner / 2f, inner / 2f),
                    size = Size((size.width - inner).coerceAtLeast(0f),
                        (size.height - inner).coerceAtLeast(0f)),
                    cornerRadius = CornerRadius((cornerRadius.toPx() - inner / 2f).coerceAtLeast(0f)),
                    style = Stroke(width = inner)
                )
            }
        })

        // --- 3. SCHICHT: EIGENTLICHE GLASKARTE MIT FEINER KANTE ---
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(shape)
                .background(glassFill)
                .border(
                    width = (0.65f + neonPower * 0.9f).dp,
                    brush = Brush.linearGradient(glowColors.map { it.copy(alpha = (.22f + neonPower * .78f).coerceIn(0f, 1f)) }),
                    shape = shape
                )
        )

        // Inhalt der Karte mit Innenabstand
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            content()
        }
    }
}
