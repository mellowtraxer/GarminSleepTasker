package de.ricci.garminsleep.ui.components

import androidx.compose.animation.core.*
import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import kotlin.math.*

/** Independent wallpaper palette. Card neon colors never change when these values change. */
object WallpaperNeonTuning {
    var primary by mutableIntStateOf(0)
    var secondary by mutableIntStateOf(0)
    var intensity by mutableIntStateOf(72)
    var animation by mutableIntStateOf(55)
    var depth by mutableIntStateOf(78)
    var particles by mutableIntStateOf(48)
    var rays by mutableIntStateOf(62)
    fun palette(): List<Color> {
        if (primary == 0) return listOf(Color(0xFF00F0FF), Color(0xFFAE48FF), Color(0xFFFF00B8))
        val second = secondary.takeIf { it != 0 } ?: primary
        return listOf(Color(primary), Color(second), Color(primary))
    }
}

/** Procedural OLED wallpapers. All motions use a common seamless 2π phase. */
data class LiveWallpaperStyle(val id: String, val title: String, val subtitle: String)
val sleepSyncLiveStyles = listOf(
    LiveWallpaperStyle("aurora_dream", "AURORA DREAM", "Fließende Schlaflichter"),
    LiveWallpaperStyle("nebula_flow", "NEBULA FLOW", "Schwerelose Lichtströme"),
    LiveWallpaperStyle("lunar_glow", "LUNAR GLOW", "Schwebende Mondlichter"),
    LiveWallpaperStyle("ocean_breeze", "OCEAN BREEZE", "Sanfte Meereswellen"),
    LiveWallpaperStyle("velvet_wave", "VELVET WAVE", "Samtige Neonwellen"),
    LiveWallpaperStyle("crystal_vibe", "CRYSTAL VIBE", "Facettierte Lichtkristalle"),
    LiveWallpaperStyle("galaxy_spin", "GALAXY SPIN", "Langsamer Galaxiewirbel"),
    LiveWallpaperStyle("neon_rain", "NEON RAIN", "Fallende Lichtspuren"),
    LiveWallpaperStyle("zen_glow", "ZEN GLOW", "Schwebende Lichtringe"),
    LiveWallpaperStyle("silk_dream", "SILK DREAM", "Seidige Lichtschleier"),
    LiveWallpaperStyle("horizon_line", "HORIZON LINE", "Leuchtender Horizont"),
    LiveWallpaperStyle("star_dust", "STAR DUST", "Treibende Sternenlichter"),
    LiveWallpaperStyle("hyper_cosmic", "HYPER-COSMIC SILK", "Interaktive Plasma-Seide & Stardust")
)

@Composable
fun SleepSyncLiveWallpaper(style: String, modifier: Modifier = Modifier) {
    if (style == "hyper_cosmic" && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        HyperGlowUniverse(modifier)
        return
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        AgslLiveWallpaper(style, modifier)
        return
    }
    val transition = rememberInfiniteTransition(label = "LiveWallpaper")
    val phase by transition.animateFloat(
        0f, (2f * PI).toFloat(),
        infiniteRepeatable(tween((60000 - WallpaperNeonTuning.animation * 480).coerceAtLeast(9000), easing = LinearEasing)), label = "SeamlessCycle"
    )
    val palette = WallpaperNeonTuning.palette()
    val c0 = palette[0]
    val c1 = palette[1]
    val c2 = palette[2]
    Canvas(modifier.background(Color(0xFF020208))) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas
        val motion = phase
        val scale = min(w, h)
        val intensity = WallpaperNeonTuning.intensity / 100f
        val depth = WallpaperNeonTuning.depth / 100f
        val particles = WallpaperNeonTuning.particles / 100f
        val rays = WallpaperNeonTuning.rays / 100f
        fun aura(x: Float, y: Float, radius: Float, color: Color, alpha: Float) {
            val center = Offset(x, y)
            drawCircle(
                Brush.radialGradient(listOf(color.copy(alpha = alpha), color.copy(alpha = 0f)),
                    center = center, radius = radius.coerceAtLeast(1f)),
                radius = radius.coerceAtLeast(1f), center = center
            )
        }
        fun silk(index: Int, amplitude: Float, width: Float, tint: Color, alpha: Float, drift: Float = 0f) {
            val shift = sin(motion + index * 1.7f) * w * drift
            val x = w * (.16f + index * .25f) + shift
            val p = Path().apply {
                moveTo(x - width, -h * .12f)
                cubicTo(x + amplitude, h * .24f, x - amplitude, h * .65f, x + width, h * 1.12f)
                cubicTo(x + width * 2f, h * 1.12f, x - amplitude + width, h * .65f,
                    x + amplitude + width, h * .24f)
                close()
            }
            drawPath(p, Brush.horizontalGradient(listOf(Color.Transparent,
                tint.copy(alpha = alpha), tint.copy(alpha = alpha * .38f), Color.Transparent)))
            val edge = Path().apply {
                moveTo(x - width, -h * .12f)
                cubicTo(x + amplitude, h * .24f, x - amplitude, h * .65f, x + width, h * 1.12f)
            }
            drawPath(edge, tint.copy(alpha = alpha * .65f), style = Stroke(2.2f, cap = StrokeCap.Round))
        }
        val vanish = Offset(w * (.5f + .10f * sin(motion)), h * (.46f + .035f * cos(motion)))
        aura(vanish.x, vanish.y, w * (1.15f + depth * .65f), c1, .09f * intensity)
        if (rays > .01f) {
            repeat(7) { i ->
                val angle = (i - 3) * .26f + sin(motion + i) * .035f
                val endpoint = Offset(vanish.x + sin(angle) * h * 1.35f,
                    vanish.y + cos(angle) * h * 1.35f)
                val rayPath = Path().apply {
                    moveTo(vanish.x, vanish.y)
                    lineTo(endpoint.x - w * .045f, endpoint.y)
                    lineTo(endpoint.x + w * .045f, endpoint.y)
                    close()
                }
                drawPath(rayPath, Brush.verticalGradient(
                    listOf(c0.copy(alpha = .10f * rays * intensity), Color.Transparent)))
            }
        }
        when (style) {
            "aurora_dream" -> {
                repeat(3) { silk(it, w * .72f, w * .14f, listOf(c0,c1,c2)[it], .36f, .07f) }
                aura(w * .42f, h * (.42f + .07f * sin(motion)), w * .70f, c0, .14f)
            }
            "nebula_flow" -> {
                repeat(4) { i -> silk(i, w * .94f, w * .21f,
                    listOf(c1,c0,c2,c1)[i], .27f, .12f) }
                aura(w * .5f, h * .53f, w, c1, .13f)
            }
            "lunar_glow" -> {
                val cx = w * (.52f + .035f * sin(motion))
                val cy = h * (.48f + .025f * cos(motion))
                aura(cx, cy, w * .88f, c1, .20f)
                repeat(3) { i ->
                    drawCircle(listOf(c0,c1,c2)[i].copy(alpha = .16f + i * .10f),
                        radius = w * (.23f + i * .115f), center = Offset(cx, cy),
                        style = Stroke(width = 2f + i * 1.4f))
                }
                drawCircle(Brush.radialGradient(listOf(c1.copy(alpha = .44f),
                    Color(0xFF080718)), center = Offset(cx,cy), radius = w * .28f),
                    radius = w * .28f, center = Offset(cx,cy))
            }
            "ocean_breeze", "velvet_wave", "silk_dream" -> {
                val tint = if (style == "velvet_wave") c2 else c0
                repeat(5) { i ->
                    val path = Path()
                    val baseline = h * (.19f + i * .16f)
                    path.moveTo(-w * .15f, baseline)
                    path.cubicTo(w * .32f, baseline - h * (.15f + .06f * sin(motion + i)),
                        w * .66f, baseline + h * (.15f + .05f * cos(motion + i)),
                        w * 1.15f, baseline)
                    drawPath(path, listOf(tint,c1,c2)[i%3].copy(alpha = .19f + i * .09f),
                        style = Stroke(width = w * (.014f + i * .006f), cap = StrokeCap.Round))
                    drawPath(path, listOf(tint,c1,c2)[i%3].copy(alpha = .70f),
                        style = Stroke(width = 1.6f, cap = StrokeCap.Round))
                }
                if (style == "silk_dream") repeat(2) { silk(it, w * .7f, w * .16f, c1, .23f, .08f) }
                aura(w * .5f, h * .52f, w * .85f, tint, .10f)
            }
            "crystal_vibe" -> {
                repeat(16) { i ->
                    val x = w * (.5f + .54f * sin(i * 2.399f + motion * .09f))
                    val y = h * (.5f + .55f * cos(i * 1.73f))
                    val p = Path().apply {
                        moveTo(x, y - h * .13f)
                        lineTo(x + w * .16f, y)
                        lineTo(x, y + h * .14f)
                        lineTo(x - w * .11f, y)
                        close()
                    }
                    drawPath(p, listOf(c0,c1,c2)[i%3].copy(alpha = .07f + (i%4)*.045f))
                    drawPath(p, listOf(c0,c1,c2)[i%3].copy(alpha = .34f),
                        style = Stroke(1.1f))
                }
            }
            "galaxy_spin" -> {
                val center = Offset(w * .5f,h * .5f)
                aura(center.x,center.y,w*.9f,c1,.18f)
                repeat(4) { arm ->
                    val p = Path()
                    repeat(100) { j ->
                        val t = j / 99f
                        val theta = t * 3.8f * PI.toFloat() + arm * PI.toFloat() / 2f + motion * .14f
                        val r = t * w * .67f
                        val x = center.x + cos(theta) * r
                        val y = center.y + sin(theta) * r * .9f
                        if (j==0) p.moveTo(x,y) else p.lineTo(x,y)
                    }
                    drawPath(p, listOf(c0,c1,c2)[arm%3].copy(alpha = .6f),
                        style = Stroke(w * .013f, cap = StrokeCap.Round))
                }
            }
            "neon_rain" -> {
                repeat(42) { i ->
                    val x = w * ((i * .6180339f) % 1f)
                    val y = h * ((i * .381966f + motion / (2f*PI).toFloat() * (.2f+i%4*.09f)) % 1f)
                    val len = h * (.045f + i%5*.017f)
                    val tint = listOf(c0,c1,c2)[i%3]
                    drawLine(tint.copy(alpha = .20f),Offset(x,y-len),Offset(x,y+len),
                        strokeWidth = 9f, cap = StrokeCap.Round)
                    drawLine(tint.copy(alpha = .70f),Offset(x,y-len),Offset(x,y+len),
                        strokeWidth = 1.7f, cap = StrokeCap.Round)
                }
            }
            "zen_glow" -> {
                repeat(3) { i ->
                    val y = h * (.30f + i * .22f + .025f * sin(motion+i))
                    val x = w * (.5f + .08f * sin(motion + i * 2f))
                    aura(x,y,w*.55f,listOf(c0,c1,c2)[i],.15f)
                    drawOval(listOf(c0,c1,c2)[i].copy(alpha=.65f),
                        topLeft=Offset(x-w*.31f,y-h*.06f),size=androidx.compose.ui.geometry.Size(w*.62f,h*.12f),
                        style=Stroke(2.8f))
                }
            }
            "horizon_line" -> {
                val horizon = h * (.59f + .03f * sin(motion))
                aura(w*.5f,horizon,w,c2,.22f)
                val p=Path().apply {
                    moveTo(0f,horizon)
                    cubicTo(w*.33f,horizon-h*.14f,w*.66f,horizon+h*.12f,w,horizon)
                }
                drawPath(p,c2.copy(alpha=.27f),style=Stroke(w*.06f,cap=StrokeCap.Round))
                drawPath(p,c0.copy(alpha=.94f),style=Stroke(2.4f,cap=StrokeCap.Round))
                repeat(9) { i ->
                    val y=horizon+h*.035f+i*h*.045f
                    drawLine(c1.copy(alpha=.26f*(1f-i/10f)),Offset(0f,y),Offset(w,y),
                        strokeWidth=1.2f)
                }
            }
            "star_dust" -> {
                repeat(85) { i ->
                    val x=w*((i*.75487766f+sin(motion+i)*.012f)%1f)
                    val y=h*((i*.5698403f+cos(motion+i*.7f)*.013f)%1f)
                    val r=scale*(.002f+(i%9)*.0013f)
                    val tint=listOf(c0,c1,c2)[i%3]
                    aura(x,y,r*5f,tint,.13f)
                    drawCircle(tint.copy(alpha=.42f+(i%5)*.10f),radius=r,center=Offset(x,y))
                }
            }
        }
        if (particles > .01f) {
            repeat((particles * 85).toInt()) { i ->
                val layer = (i % 4 + 1) / 4f
                val t = motion / (2f * PI).toFloat()
                val x = w * ((i * .6180339f + t * layer * .16f) % 1f)
                val y = h * ((i * .41421356f + sin(motion + i) * .013f * layer + 1f) % 1f)
                val radius = scale * (.0015f + .005f * layer * depth)
                val tint = listOf(c0,c1,c2)[i % 3]
                drawCircle(tint.copy(alpha = .22f * intensity * layer), radius * 3.5f, Offset(x,y))
                drawCircle(tint.copy(alpha = .58f * intensity * layer), radius, Offset(x,y))
            }
        }
        drawRect(Brush.radialGradient(
            listOf(Color.Transparent, Color.Black.copy(alpha = .36f * depth)),
            center = Offset(w * .5f, h * .48f), radius = max(w,h) * .78f))
    }
}
