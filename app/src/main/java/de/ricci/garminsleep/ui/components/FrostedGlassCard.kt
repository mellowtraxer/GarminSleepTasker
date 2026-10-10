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
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import android.graphics.Paint
import android.graphics.LinearGradient
import android.graphics.Shader
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
    var glow by mutableIntStateOf(35)
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
    val glowPower = StudioGlassTuning.glow.coerceIn(0, 100) / 100f
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
        // Real, rounded soft-light shadow. Blur controls the shadow radius,
        // Glow controls its intensity, Neon controls the crisp border.
        // Draw INSIDE the allocated bounds to avoid clipped rectangular tiles.
        Box(Modifier.matchParentSize().drawBehind {
            if (glowPower > 0f && blurPower > 0f) {
                val blurRadius = (2f + 14f * blurPower).dp.toPx()
                val inset = blurRadius + 2.dp.toPx()
                val left = inset
                val top = inset
                val right = size.width - inset
                val bottom = size.height - inset
                if (right > left && bottom > top) {
                    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        style = Paint.Style.STROKE
                        strokeWidth = 1.5.dp.toPx()
                        shader = LinearGradient(left, top, right, bottom,
                            intArrayOf(0xFF00F0FF.toInt(), 0xFFA855F7.toInt(), 0xFFFF007F.toInt()),
                            null, Shader.TileMode.CLAMP)
                        setShadowLayer(blurRadius, 0f, 0f,
                            android.graphics.Color.argb(
                                (glowPower * 240f).toInt().coerceIn(0, 255), 168, 62, 245))
                    }
                    drawIntoCanvas { canvas ->
                        canvas.nativeCanvas.drawRoundRect(
                            left, top, right, bottom,
                            (cornerRadius.toPx() - inset).coerceAtLeast(3.dp.toPx()),
                            (cornerRadius.toPx() - inset).coerceAtLeast(3.dp.toPx()),
                            paint
                        )
                    }
                }
            }
        })

        // --- 3. SCHICHT: EIGENTLICHE GLASKARTE MIT FEINER KANTE ---
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(shape)
                .background(glassFill)
                .border(
                    width = (0.65f + neonPower * 1.15f).dp,
                    brush = Brush.linearGradient(glowColors.map { it.copy(alpha = (.06f + neonPower * .94f).coerceIn(0f, 1f)) }),
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
