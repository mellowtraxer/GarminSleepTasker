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
fun AnimatedAuroraBackground(previewAurora: Boolean = false, content: @Composable BoxScope.() -> Unit) {
    val context = LocalContext.current
    val source = if (previewAurora) "aurora" else DreamscapeMotion.wallpaperSource
    val enabled = previewAurora || DreamscapeMotion.wallpaperEnabled
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
    Box(Modifier.fillMaxSize().background(Color(0xFF030308))) {
        if (enabled && source != "oled" && source != "aurora") {
            if (bitmap != null) Image(bitmap, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
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
            val cyan = Offset(w * (.30f + .17f * sin(phase)), h * (.22f + .05f * sin(phase + 1f)) - scroll * .035f)
            val violet = Offset(w * (.66f + .16f * sin(phase + 2f)), h * (.53f + .06f * sin(phase + 2.5f)) - scroll * .075f)
            val pink = Offset(w * (.42f + .18f * sin(phase + 4f)), h * (.82f + .05f * sin(phase + 3f)) - scroll * .12f)
            drawCircle(
                Brush.radialGradient(listOf(Color(0xFF00F0FF).copy(alpha = .12f + light * .22f), Color.Transparent), center = cyan, radius = w * 1.05f),
                radius = w * 1.05f, center = cyan
            )
            drawCircle(
                Brush.radialGradient(listOf(Color(0xFFA855F7).copy(alpha = .15f + deep * .32f), Color.Transparent), center = violet, radius = w),
                radius = w, center = violet
            )
            drawCircle(
                Brush.radialGradient(listOf(Color(0xFFFF007F).copy(alpha = .10f + rem * .28f), Color.Transparent), center = pink, radius = w * .95f),
                radius = w * .95f, center = pink
            )
            if (ripple > .001f) {
                val center = Offset(w * .5f, h * .52f)
                val radius = w * (.15f + .9f * ripple)
                drawCircle(Color(0xFF75E9FF).copy(alpha = (1f - ripple) * (.10f + energy * .12f)),
                    radius = radius, center = center, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f))
            }
        }
        content()
    }
}
