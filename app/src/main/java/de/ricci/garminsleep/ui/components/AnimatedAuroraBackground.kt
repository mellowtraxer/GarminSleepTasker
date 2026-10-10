package de.ricci.garminsleep.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.asImageBitmap
import android.graphics.BitmapFactory
import java.io.File
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import kotlin.math.PI
import kotlin.math.sin

/** Shared visual inputs; no health information is sent off-device. */
object DreamscapeMotion {
    var scrollOffset by mutableFloatStateOf(0f)
    var lightShare by mutableFloatStateOf(.45f)
    var deepShare by mutableFloatStateOf(.25f)
    var remShare by mutableFloatStateOf(.30f)
    var healthEnergy by mutableFloatStateOf(.5f)
    var touchPulse by mutableIntStateOf(0)
    var wallpaperSource by mutableStateOf("aurora")
    var wallpaperEnabled by mutableStateOf(true)
    fun updateSleep(light: Long, deep: Long, rem: Long, hr: Double?) {
        val total = (light + deep + rem).coerceAtLeast(1L).toFloat()
        lightShare = (light / total).coerceIn(0f, 1f)
        deepShare = (deep / total).coerceIn(0f, 1f)
        remShare = (rem / total).coerceIn(0f, 1f)
        healthEnergy = (hr?.let { (it.toFloat() - 45f) / 55f } ?: .5f).coerceIn(.12f, .9f)
    }
    fun ripple() { touchPulse++ }
}

@Composable
fun AnimatedAuroraBackground(previewAurora: Boolean = false, previewDreamscape: Boolean = false, content: @Composable BoxScope.() -> Unit) {
    val context = LocalContext.current
    val source = when { previewAurora -> "aurora"; previewDreamscape -> "dreamscape"; else -> DreamscapeMotion.wallpaperSource }
    val enabled = previewAurora || previewDreamscape || DreamscapeMotion.wallpaperEnabled
    val imagePath = when(source) {
        "custom" -> File(context.filesDir,"sleepsync_custom_wallpaper").absolutePath
        "dreamscape" -> File(context.filesDir,"sleepsync_dreamscape.png").absolutePath
        else -> null
    }
    val bitmap = remember(imagePath) { imagePath?.let { path -> runCatching { BitmapFactory.decodeFile(path)?.asImageBitmap() }.getOrNull() } }
    val loop = rememberInfiniteTransition(label = "DreamscapeFlow")
    val phase by loop.animateFloat(
        initialValue = 0f, targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(26000, easing = LinearEasing)), label = "seamlessAurora"
    )
    val scroll by animateFloatAsState(
        targetValue = DreamscapeMotion.scrollOffset,
        animationSpec = tween(130, easing = LinearOutSlowInEasing), label = "auroraParallax"
    )
    var waveTarget by remember { mutableIntStateOf(0) }
    var waveActive by remember { mutableStateOf(false) }
    LaunchedEffect(DreamscapeMotion.touchPulse) {
        if (DreamscapeMotion.touchPulse > 0) {
            waveTarget = DreamscapeMotion.touchPulse
            waveActive = true
        }
    }
    val ripple by animateFloatAsState(
        targetValue = if (waveActive) 1f else 0f,
        animationSpec = tween(1100, easing = FastOutSlowInEasing),
        finishedListener = { if (waveActive) waveActive = false },
        label = "touchLightWave"
    )
    val dreamX = sin(phase) * 13f
    val dreamY = sin(phase + 1.5707963f) * 18f
    val dreamScale = 1.13f + .035f * sin(phase)
    Box(Modifier.fillMaxSize().clipToBounds().background(Color(0xFF030308))) {
        if (enabled && source.startsWith("live_")) {
            SleepSyncLiveWallpaper(source.removePrefix("live_"), Modifier.fillMaxSize())
        }
        if (enabled && source != "oled" && source != "aurora" && !source.startsWith("live_")) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap, contentDescription = null,
                    modifier = Modifier.fillMaxSize().graphicsLayer {
                        if (source == "dreamscape") {
                            scaleX = dreamScale
                            scaleY = dreamScale
                            translationX = dreamX * density - scroll * .012f
                            translationY = dreamY * density - scroll * .022f
                        }
                    },
                    contentScale = ContentScale.Crop
                )
                if (source == "dreamscape") {
                    Canvas(Modifier.fillMaxSize()) {
                        val center = Offset(size.width * (.48f + .06f * sin(phase)),
                            size.height * (.46f + .05f * sin(phase + 1f)))
                        drawCircle(
                            Brush.radialGradient(
                                listOf(StudioGlassTuning.palette()[0].copy(alpha = .09f + .035f * sin(phase)),
                                    StudioGlassTuning.palette()[1].copy(alpha = .045f), Color.Transparent),
                                center = center, radius = size.width * .95f
                            ),
                            radius = size.width * .95f, center = center
                        )
                    }
                }
            }
            else if (source == "builtin") Image(
                painter = androidx.compose.ui.res.painterResource(de.ricci.garminsleep.R.drawable.cosmic_planet),
                contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop
            )
        }
        if (enabled && source == "aurora") Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val light = DreamscapeMotion.lightShare
            val deep = DreamscapeMotion.deepShare
            val rem = DreamscapeMotion.remShare
            val energy = DreamscapeMotion.healthEnergy
            // Silk ribbons: transparent Bezier surfaces with luminous edges.
            // All oscillations use integer harmonics of the same 2π phase, so
            // the loop is mathematically seamless.
            val colors = StudioGlassTuning.palette(listOf(Color(0xFF00F0FF), Color(0xFFB452FF), Color(0xFFFF1AC6)))
            val shares = listOf(light, deep, rem)
            for (layer in 0..2) {
                val t = layer.toFloat()
                val parallax = scroll * (.025f + t * .018f)
                val shift = sin(phase + t * 2.0944f) * w * .065f
                val x0 = w * (.13f + t * .11f) + shift
                val x1 = w * (.85f - t * .09f) - shift
                val y0 = h * (.13f + t * .21f) - parallax
                val y1 = h * (.82f - t * .13f) - parallax
                val thickness = w * (.13f + .025f * sin(phase * 2f + t))
                val ribbon = Path().apply {
                    moveTo(x0, y0)
                    cubicTo(w * (.90f - t * .09f), h * (.13f + t * .12f) - parallax,
                        w * (.03f + t * .11f), h * (.59f + t * .07f) - parallax, x1, y1)
                    cubicTo(w * (.03f + t * .11f) + thickness, h * (.59f + t * .07f) - parallax,
                        w * (.90f - t * .09f) + thickness, h * (.13f + t * .12f) - parallax,
                        x0 + thickness, y0)
                    close()
                }
                val tint = colors[layer]
                drawPath(
                    ribbon,
                    brush = Brush.linearGradient(
                        listOf(tint.copy(alpha = .04f), tint.copy(alpha = .28f + shares[layer] * .24f),
                            colors[(layer + 1) % 3].copy(alpha = .15f), Color.Transparent),
                        start = Offset(x0, y0), end = Offset(x1, y1)
                    )
                )
                val edge = Path().apply {
                    moveTo(x0, y0)
                    cubicTo(w * (.90f - t * .09f), h * (.13f + t * .12f) - parallax,
                        w * (.03f + t * .11f), h * (.59f + t * .07f) - parallax, x1, y1)
                }
                drawPath(edge, tint.copy(alpha = .11f), style = Stroke(width = 13f, cap = StrokeCap.Round))
                drawPath(edge, tint.copy(alpha = .65f), style = Stroke(width = 2.4f, cap = StrokeCap.Round))
            }
            if (ripple > .001f) {
                val center = Offset(w * .5f, h * .52f)
                val radius = w * (.15f + .9f * ripple)
                drawCircle(colors[0].copy(alpha = (1f - ripple) * (.10f + energy * .12f)),
                    radius = radius, center = center, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f))
            }
        }
        content()
    }
}
