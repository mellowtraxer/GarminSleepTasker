package de.ricci.garminsleep.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Sleep quality illustration, not an astronomical moon phase. */
@Composable
fun LivingSleepMoon(
    rating: Int,
    title: String,
    explanation: String,
    comparison: String,
    modifier: Modifier = Modifier
) {
    val safeRating = rating.coerceIn(0, 100)
    val quality = when {
        safeRating >= 80 -> 2
        safeRating >= 60 -> 1
        else -> 0
    }
    val moonColor = when (quality) {
        2 -> Color(0xFF8AF5DB)
        1 -> Color(0xFFD5ADFF)
        else -> Color(0xFFFFA47D)
    }
    val transition = rememberInfiniteTransition(label = "sleepMoonBreath")
    val glow by transition.animateFloat(
        initialValue = .30f, targetValue = .72f,
        animationSpec = infiniteRepeatable(
            animation = tween(4200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "moonGlow"
    )
    val drift by transition.animateFloat(
        initialValue = -3f, targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(6200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "moonDrift"
    )
    FrostedGlassCard(modifier = modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(112.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    val radius = size.minDimension * .30f
                    val center = Offset(size.width / 2f, size.height / 2f + drift.dp.toPx())
                    drawCircle(moonColor.copy(alpha = glow * .12f), radius * 1.6f, center)
                    drawCircle(moonColor.copy(alpha = glow * .23f), radius * 1.28f, center)
                    drawCircle(moonColor, radius, center)
                    // A dark crescent cutout: less illumination for lower sleep quality.
                    val cutout = when (quality) { 2 -> .72f; 1 -> .48f; else -> .22f }
                    drawCircle(
                        Color(0xFF090D1C),
                        radius * .95f,
                        Offset(center.x + radius * cutout, center.y - radius * .25f)
                    )
                    drawCircle(Color.White.copy(alpha = .8f * glow), 2.dp.toPx(),
                        Offset(center.x - radius * 1.4f, center.y - radius * 1.25f))
                    drawCircle(Color(0xFF75E9FF).copy(alpha = .7f * glow), 1.5.dp.toPx(),
                        Offset(center.x + radius * 1.45f, center.y + radius * .9f))
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("NACHT-INSIGHT", color = Color(0xFFBAA7FF),
                    fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Spacer(Modifier.height(5.dp))
                Text(title, color = moonColor, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Text("$safeRating / 100  ·  Schlafbewertung", color = Color(0xFFB9C9E2), fontSize = 11.sp)
                Spacer(Modifier.height(5.dp))
                Text(
                    when (quality) {
                        2 -> "Ruhige, klare Mondnacht"
                        1 -> "Sanfte, wechselhafte Mondnacht"
                        else -> "Unruhige Mondnacht"
                    },
                    color = Color(0xFFE2E8FF), fontSize = 11.sp
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(explanation, color = Color.White, fontSize = 13.sp, lineHeight = 19.sp)
        if (comparison.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(comparison, color = Color(0xFFB9C9E2), fontSize = 12.sp, lineHeight = 18.sp)
        }
    }
}
