package de.ricci.garminsleep.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
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
    var chromaticV3 by mutableStateOf(false)
    var chromaticStrength by mutableIntStateOf(45)
    var chromaticGlow by mutableIntStateOf(65)
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
    chromaticKey: String = "",
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
    val glowPower = (if (StudioGlassTuning.chromaticV3) StudioGlassTuning.chromaticGlow else StudioGlassTuning.glow).coerceIn(0, 100) / 100f
    // Blur dissolves the crisp neon stroke into the soft halo. At 100% only light remains.
    val sharpEdgeOpacity = (1f - blurPower).coerceIn(0f, 1f)
    val haloIntensity = (glowPower * (.35f + .65f * blurPower) + neonPower * blurPower * .35f).coerceIn(0f, 1f)
    val chromatic = StudioGlassTuning.chromaticV3
    // Stable semantic palettes: never depend on composition order or scrolling.
    val chromaticColors = when {
        chromaticKey.contains("GESAMTSCHLAF", true) || chromaticKey.contains("SCHLAFDAUER", true) ->
            listOf(Color(0xFF00D6F5), Color(0xFF8D40EF))
        chromaticKey.contains("LEICHT", true) || chromaticKey.contains("ATMUNG", true) ->
            listOf(Color(0xFF00CFF5), Color(0xFF3979FF))
        chromaticKey.contains("TIEF", true) || chromaticKey.contains("HRV", true) ->
            listOf(Color(0xFF7455FF), Color(0xFFBF4DFF))
        chromaticKey.contains("REM", true) || chromaticKey.contains("PULS", true) ->
            listOf(Color(0xFFFF399C), Color(0xFFE951B9))
        chromaticKey.contains("WACH", true) ->
            listOf(Color(0xFFFFA23B), Color(0xFFFF5D83))
        chromaticKey.contains("SPO", true) || chromaticKey.contains("SAUERSTOFF", true) ->
            listOf(Color(0xFF00D6C2), Color(0xFF00A9F3))
        chromaticKey.contains("INSIGHT", true) ->
            listOf(Color(0xFF3DD4E7), Color(0xFFB344ED))
        chromaticKey.contains("FINGERPRINT", true) ->
            listOf(Color(0xFFFF9B48), Color(0xFFEB439A))
        chromaticKey.contains("DNA", true) ->
            listOf(Color(0xFF00D6F5), Color(0xFFAA44ED))
        else -> listOf(glowColors.firstOrNull() ?: Color(0xFF00F0FF),
            glowColors.lastOrNull() ?: Color(0xFFA855F7))
    }
    val selected = StudioGlassTuning.selectedNeon
    val activeColors = if (chromatic) chromaticColors else StudioGlassTuning.palette(glowColors)
    val shape = RoundedCornerShape(cornerRadius)
    val glowBrush = Brush.linearGradient(glowColors)

    // Subtiler Lichteinfall auf dem Milchglas
    // Opaque OLED base first, low-alpha chromatic tint second:
    // wallpaper motion remains atmospheric rather than competing with data.
    val chromaticTint = Brush.linearGradient(
        listOf(
            chromaticColors[0].copy(alpha = StudioGlassTuning.chromaticStrength.coerceIn(0,100) / 100f * .24f),
            chromaticColors[1].copy(alpha = StudioGlassTuning.chromaticStrength.coerceIn(0,100) / 100f * .18f)
        )
    )
    val glassFill = Brush.verticalGradient(
        listOf(
            Color.White.copy(alpha = .035f + glassPower * .82f),
            Color.White.copy(alpha = .008f + glassPower * .25f)
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
                .background(if (chromatic) Color(0xF2050711) else Color.Transparent)
                .background(if (chromatic) chromaticTint else glassFill)

        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(18.dp)
            ) {
                content()
            }
        }
    }

}
