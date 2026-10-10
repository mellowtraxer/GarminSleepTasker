package de.ricci.garminsleep.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

data class DnaPhase(val startMs: Long, val endMs: Long, val name: String)
private val dnaTones = mapOf(
    "leicht" to Color(0xFF49C8FF),
    "tief" to Color(0xFF8264FF),
    "rem" to Color(0xFFE466FF),
    "wach" to Color(0xFFFFB35B)
)

@Composable
fun SleepDnaHologram(
    startMs: Long,
    endMs: Long,
    durationMinutes: Long,
    phases: List<DnaPhase>,
    onExplore: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var focused by remember(startMs, endMs) { mutableStateOf<Long?>(null) }
    val motion = rememberInfiniteTransition(label = "sleepDnaHologram")
    val breath by motion.animateFloat(
        initialValue = .65f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "dnaBreath"
    )
    val rotation by motion.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(42000, easing = LinearEasing)),
        label = "dnaOrbit"
    )
    val span = (endMs - startMs).coerceAtLeast(1L)
    fun select(x: Float, y: Float, width: Float, height: Float) {
        val angle = ((Math.toDegrees(atan2((y - height / 2f).toDouble(), (x - width / 2f).toDouble())) + 450.0) % 360.0)
        val time = (startMs + (span * angle / 360.0).toLong()).coerceIn(startMs, endMs.coerceAtLeast(startMs + 1) - 1)
        focused = time
        onExplore(time)
    }
    FrostedGlassCard(modifier = modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().height(276.dp), contentAlignment = Alignment.Center) {
            Canvas(
                Modifier.fillMaxSize()
                    .pointerInput(startMs, endMs) {
                        detectTapGestures { select(it.x, it.y, size.width.toFloat(), size.height.toFloat()) }
                    }
                    .pointerInput(startMs, endMs) {
                        detectDragGestures(
                            onDragStart = { select(it.x, it.y, size.width.toFloat(), size.height.toFloat()) },
                            onDrag = { change, _ ->
                                change.consume()
                                select(change.position.x, change.position.y, size.width.toFloat(), size.height.toFloat())
                            }
                        )
                    }
            ) {
                val center = Offset(size.width / 2, size.height / 2)
                val outer = minOf(size.width, size.height) * .405f
                val inner = outer * .70f
                val neon = StudioGlassTuning.palette()
                val primary = neon[0]
                val secondary = neon[1]
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(primary.copy(alpha = .16f * breath),
                            secondary.copy(alpha = .07f * breath), Color.Transparent),
                        center = center, radius = outer * 1.5f
                    ), radius = outer * 1.5f, center = center
                )
                for (tick in 0 until 60) {
                    val angle = tick * Math.PI / 30.0 - Math.PI / 2.0
                    val major = tick % 5 == 0
                    val startR = outer * if (major) 1.10f else 1.13f
                    val endR = outer * if (major) 1.18f else 1.16f
                    drawLine(
                        (if (tick % 2 == 0) primary else secondary).copy(alpha = if (major) .55f else .20f),
                        Offset(center.x + cos(angle).toFloat() * startR, center.y + sin(angle).toFloat() * startR),
                        Offset(center.x + cos(angle).toFloat() * endR, center.y + sin(angle).toFloat() * endR),
                        strokeWidth = if (major) 1.7.dp.toPx() else .75.dp.toPx()
                    )
                }
                val track = Color(0xFF7289AF).copy(alpha = .22f)
                drawCircle(track, outer, center, style = Stroke(7.dp.toPx()))
                drawCircle(primary.copy(alpha = .16f * breath), outer + 10.dp.toPx(), center, style = Stroke(10.dp.toPx()))
                drawCircle(secondary.copy(alpha = .33f * breath), outer + 10.dp.toPx(), center, style = Stroke(1.2.dp.toPx()))
                // Innerer Ring: vier zusammenhängende Phasenanteile, nicht der Zeitverlauf.
                // Nur gültige Intervalle innerhalb der Nacht zählen.
                val shares = phases.groupBy { it.name.trim().lowercase() }
                    .mapValues { (_, intervals) ->
                        intervals.sumOf { phase ->
                            (minOf(phase.endMs, endMs) - maxOf(phase.startMs, startMs)).coerceAtLeast(0L)
                        }
                    }
                val total = shares.values.sum().coerceAtLeast(1L)
                var innerStart = -90f
                listOf("leicht", "tief", "rem", "wach").forEach { stage ->
                    val duration = shares[stage] ?: 0L
                    if (duration <= 0L) return@forEach
                    val sweep = duration.toFloat() / total * 360f
                    val color = dnaTones.getValue(stage)
                    drawArc(color.copy(alpha = .20f * breath), innerStart, sweep, false,
                        topLeft = Offset(center.x - inner, center.y - inner),
                        size = androidx.compose.ui.geometry.Size(inner * 2, inner * 2),
                        style = Stroke(13.dp.toPx(), cap = StrokeCap.Butt))
                    drawArc(color, innerStart, sweep, false,
                        topLeft = Offset(center.x - inner, center.y - inner),
                        size = androidx.compose.ui.geometry.Size(inner * 2, inner * 2),
                        style = Stroke(4.dp.toPx(), cap = StrokeCap.Butt))
                    innerStart += sweep
                }
                phases.forEach { phase ->
                    val start = ((phase.startMs - startMs).toFloat() / span * 360f).coerceIn(0f, 360f)
                    val sweep = ((phase.endMs - phase.startMs).toFloat() / span * 360f).coerceIn(0f, 360f - start)
                    if (sweep > 0f) {
                        val color = dnaTones[phase.name.trim().lowercase()] ?: Color(0xFF9AA9CA)
                        val top = Offset(center.x - outer, center.y - outer)
                        val arcSize = androidx.compose.ui.geometry.Size(outer * 2, outer * 2)
                        drawArc(color.copy(alpha = .20f * breath), start - 90f, sweep, false,
                            topLeft = top, size = arcSize, style = Stroke(16.dp.toPx(), cap = StrokeCap.Round))
                        drawArc(color, start - 90f, sweep, false,
                            topLeft = top, size = arcSize, style = Stroke(6.dp.toPx(), cap = StrokeCap.Round))
                    }
                }
                val orbitAngle = Math.toRadians(rotation.toDouble())
                val orbit = Offset(
                    center.x + cos(orbitAngle).toFloat() * (outer + 13.dp.toPx()),
                    center.y + sin(orbitAngle).toFloat() * (outer + 13.dp.toPx())
                )
                drawCircle(primary.copy(alpha = .20f * breath), 9.dp.toPx(), orbit)
                drawCircle(secondary.copy(alpha = .85f), 2.dp.toPx(), orbit)
                focused?.let { time ->
                    val angle = Math.toRadians(((time - startMs).toDouble() / span * 360.0) - 90.0)
                    val marker = Offset(center.x + cos(angle).toFloat() * outer, center.y + sin(angle).toFloat() * outer)
                    drawCircle(Color.White, 5.dp.toPx(), marker)
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val clock = remember { java.time.format.DateTimeFormatter.ofPattern("HH:mm").withZone(java.time.ZoneId.systemDefault()) }
                Text(focused?.let { clock.format(java.time.Instant.ofEpochMilli(it)) }
                    ?: "${durationMinutes / 60}:${(durationMinutes % 60).toString().padStart(2, '0')}",
                    color = Color.White, fontSize = 31.sp, fontWeight = FontWeight.Bold)
                Text(if (focused == null) "SCHLAFDAUER" else "AUSGEWÄHLTE UHRZEIT",
                    color = Color(0xFFBDD2ED), fontSize = 10.sp, letterSpacing = 1.sp)
            }
        }
    }
}
