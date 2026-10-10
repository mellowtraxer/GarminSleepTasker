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
import android.graphics.BlurMaskFilter
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object StudioGlassTuning {
    var blur by mutableIntStateOf(20)
    var glass by mutableIntStateOf(34)
    var neon by mutableIntStateOf(35)
    var glow by mutableIntStateOf(35)
    var selectedNeon by mutableIntStateOf(0)
    var secondaryNeon by mutableIntStateOf(0)
    fun palette(default: List<Color> = listOf(Color(0xFF00F0FF), Color(0xFFAE48FF), Color(0xFFFF00B8))): List<Color> {
        val primary = selectedNeon
        if (primary == 0) return default
        val secondary = secondaryNeon.takeIf { it != 0 } ?: primary
        return listOf(Color(primary), Color(secondary), Color(primary))
    }
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
    // Blur dissolves the crisp neon stroke into the soft halo. At 100% only light remains.
    val sharpEdgeOpacity = (1f - blurPower).coerceIn(0f, 1f)
    val haloIntensity = (glowPower * (.35f + .65f * blurPower) + neonPower * blurPower * .35f).coerceIn(0f, 1f)
    val selected = StudioGlassTuning.selectedNeon
    val activeColors = StudioGlassTuning.palette(glowColors)
    val shape = RoundedCornerShape(cornerRadius)
    val glowBrush = Brush.linearGradient(glowColors)

    // Subtiler Lichteinfall auf dem Milchglas
    val glassFill = Brush.verticalGradient(
        listOf(
            Color.White.copy(alpha = .035f + glassPower * .82f), // Glasreflex: 30% ~ previous 100%
            Color.White.copy(alpha = .008f + glassPower * .25f)  // Glasboden: stronger depth
        )
    )

    // Reserve space for the glow OUTSIDE the glass surface. This prevents
    // clipping at the composable's rectangular edges.
    val haloSpace = 17.dp
    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
                clip = false
            }
            .then(if (onClick != null) Modifier.clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = { DreamscapeMotion.ripple(); onClick() }
            ) else Modifier)
            .drawBehind {
                val inset = haloSpace.toPx()
                val radius = cornerRadius.toPx()
                val left = inset
                val top = inset
                val right = size.width - inset
                val bottom = size.height - inset
                if (right > left && bottom > top) {
                    val gradient = LinearGradient(left, top, right, bottom,
                        activeColors.map { it.toArgb() }.toIntArray(), null, Shader.TileMode.CLAMP)
                    drawIntoCanvas { canvas ->
                        fun tube(widthDp: Float, blurDp: Float, opacity: Float) {
                            if (opacity <= 0f) return
                            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                style = Paint.Style.STROKE
                                strokeWidth = widthDp.dp.toPx()
                                shader = gradient
                                alpha = (opacity.coerceIn(0f, 1f) * 255f).toInt()
                                if (blurDp > 0f) maskFilter = BlurMaskFilter(
                                    blurDp.dp.toPx(), BlurMaskFilter.Blur.NORMAL)
                            }
                            canvas.nativeCanvas.drawRoundRect(left, top, right, bottom, radius, radius, paint)
                        }
                        tube(7f + glowPower * 9f, 11f + glowPower * 15f, haloIntensity * .68f)
                        tube(3.5f + neonPower * 2.5f, 3f + blurPower * 8f, haloIntensity * .92f)
                        tube(1.2f + neonPower * 1.5f, blurPower * 9f, neonPower * sharpEdgeOpacity)
                    }
                }
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(haloSpace)
                .clip(shape)
                .background(glassFill)

        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(18.dp)
            ) {
                content()
            }
        }
    }

}
