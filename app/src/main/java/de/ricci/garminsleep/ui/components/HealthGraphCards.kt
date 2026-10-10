package de.ricci.garminsleep.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class HealthGraphPoint(val timeMs: Long, val value: Double)
data class HealthGraphMetric(
    val title: String,
    val glyph: String,
    val value: String,
    val unit: String,
    val points: List<HealthGraphPoint>,
    val tone: Color
)

@Composable
fun HealthGraphCard(
    metric: HealthGraphMetric,
    startMs: Long,
    endMs: Long,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    FrostedGlassCard(modifier = modifier.then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)) {
        Text("${metric.glyph}  ${metric.title.uppercase()}", color = metric.tone,
            fontWeight = FontWeight.Bold, fontSize = 11.sp)
        Spacer(Modifier.height(5.dp))
        Text(metric.value, color = Color.White, fontWeight = FontWeight.Bold,
            fontSize = if (compact) 19.sp else 24.sp)
        if (!compact) {
            Text("• ${metric.points.size} Messpunkte · Garmin", color = Color(0xFFB4C9E2), fontSize = 10.sp)
            if (metric.points.isNotEmpty()) {
                val min = metric.points.minOf { it.value }
                val max = metric.points.maxOf { it.value }
                Text("MIN ${"%.1f".format(java.util.Locale.GERMANY, min)} ${metric.unit}   ·   MAX ${"%.1f".format(java.util.Locale.GERMANY, max)} ${metric.unit}",
                    color = Color(0xFFBAC9DB), fontSize = 10.sp)
            }
        }
        Spacer(Modifier.height(9.dp))
        Canvas(Modifier.fillMaxWidth().height(if (compact) 64.dp else 210.dp)) {
            val sorted = metric.points.sortedBy { it.timeMs }
            if (sorted.size < 2) return@Canvas
            val min = sorted.minOf { it.value }.toFloat()
            val max = sorted.maxOf { it.value }.toFloat()
            val spread = (max - min).coerceAtLeast(1f)
            val range = (endMs - startMs).coerceAtLeast(1L).toFloat()
            val path = Path()
            sorted.forEachIndexed { index, p ->
                val x = ((p.timeMs - startMs).toFloat() / range).coerceIn(0f, 1f) * size.width
                val y = size.height * (.88f - .76f * ((p.value.toFloat() - min) / spread))
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, metric.tone.copy(alpha = .16f), style = Stroke(7.dp.toPx()))
            drawPath(path, metric.tone, style = Stroke(if (compact) 2.dp.toPx() else 2.5.dp.toPx()))
        }
        if (!compact) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                val fmt = java.time.format.DateTimeFormatter.ofPattern("HH:mm").withZone(java.time.ZoneId.systemDefault())
                listOf(startMs, startMs + (endMs - startMs) / 2, endMs).forEach {
                    Text(fmt.format(java.time.Instant.ofEpochMilli(it)), color = Color(0xFF9FAFC7), fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
fun HealthOverviewGrid(metrics: List<HealthGraphMetric>, startMs: Long, endMs: Long, onClick: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        metrics.chunked(2).forEachIndexed { row, pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                pair.forEachIndexed { column, metric ->
                    HealthGraphCard(metric, startMs, endMs, Modifier.weight(1f), compact = true,
                        onClick = { onClick(row * 2 + column) })
                }
            }
        }
    }
}
