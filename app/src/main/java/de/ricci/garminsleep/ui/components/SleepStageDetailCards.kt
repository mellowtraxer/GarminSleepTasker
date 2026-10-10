package de.ricci.garminsleep.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class SleepPhaseInterval(val startMs: Long, val endMs: Long)

@Composable
fun SleepStageDetailCards(
    startMs: Long,
    endMs: Long,
    lightMin: Long,
    deepMin: Long,
    remMin: Long,
    awakeMin: Long,
    light: List<SleepPhaseInterval>,
    deep: List<SleepPhaseInterval>,
    rem: List<SleepPhaseInterval>,
    awake: List<SleepPhaseInterval>,
    modifier: Modifier = Modifier
) {
    val fmt = remember { DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault()) }
    fun duration(m: Long) = if (m >= 60) "${m / 60} h ${m % 60} min" else "$m min"
    val total = (lightMin + deepMin + remMin + awakeMin).coerceAtLeast(1L)
    val stages = listOf(
        Triple("LEICHT", lightMin, light),
        Triple("TIEF", deepMin, deep),
        Triple("REM", remMin, rem),
        Triple("WACH", awakeMin, awake)
    )
    val tones = listOf(Color(0xFF54C9FF), Color(0xFF7860F5), Color(0xFFD267FA), Color(0xFFFFA85B))
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        FrostedGlassCard {
            Text("NACHT-ZUSAMMENFASSUNG", color = Color(0xFFB4D8FF), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(duration(lightMin + deepMin + remMin), color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Text("${fmt.format(Instant.ofEpochMilli(startMs))} – ${fmt.format(Instant.ofEpochMilli(endMs))} · Schlafdauer", color = Color(0xFFB9C7DB), fontSize = 11.sp)
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth().height(9.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                stages.forEachIndexed { index, (_, minutes, _) ->
                    if (minutes > 0L) Canvas(Modifier.weight(minutes.toFloat()).fillMaxHeight()) {
                        drawRoundRect(tones[index], cornerRadius = androidx.compose.ui.geometry.CornerRadius(5.dp.toPx()))
                    }
                }
            }
            Spacer(Modifier.height(7.dp))
            Text(stages.joinToString("  ·  ") { "${it.first.lowercase().replaceFirstChar { c -> c.uppercase() }} ${duration(it.second)}" }, color = Color(0xFFC8D4E9), fontSize = 10.sp)
        }
        val transition = rememberInfiniteTransition(label = "sleepPhaseGlow")
        val pulse by transition.animateFloat(
            initialValue = 0.62f, targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(2400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "phasePulse"
        )
        stages.forEachIndexed { index, (label, minutes, intervals) ->
            FrostedGlassCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(label, color = tones[index], fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("${minutes * 100 / total} %", color = tones[index], fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }
                Text(duration(minutes), color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Text("${intervals.size} ${if (intervals.size == 1) "Abschnitt" else "Abschnitte"}", color = Color(0xFFB9C7DB), fontSize = 11.sp)
                Spacer(Modifier.height(10.dp))
                Canvas(Modifier.fillMaxWidth().height(52.dp)) {
                    val span = (endMs - startMs).coerceAtLeast(1L).toFloat()
                    intervals.forEach { interval ->
                        val left = ((interval.startMs - startMs) / span).coerceIn(0f, 1f) * size.width
                        val right = ((interval.endMs - startMs) / span).coerceIn(0f, 1f) * size.width
                        if (right > left) {
                            drawRoundRect(
                                color = tones[index].copy(alpha = pulse),
                                topLeft = Offset(left, 1.dp.toPx()),
                                size = androidx.compose.ui.geometry.Size((right-left).coerceAtLeast(1.dp.toPx()), size.height - 2.dp.toPx()),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
                            )
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(fmt.format(Instant.ofEpochMilli(startMs)), color = Color(0xFFB9C7DB), fontSize = 10.sp)
                    Text(fmt.format(Instant.ofEpochMilli(startMs + (endMs - startMs) / 2)), color = Color(0xFFB9C7DB), fontSize = 10.sp)
                    Text(fmt.format(Instant.ofEpochMilli(endMs)), color = Color(0xFFB9C7DB), fontSize = 10.sp)
                }
            }
        }
    }
}
