package de.ricci.garminsleep.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Native Compose dashboard hero. Values are supplied by the existing sleep calculation;
 * no Health Connect or Garmin business logic is duplicated here.
 */
@Composable
fun SleepDashboardHero(
    duration: String,
    score: Int,
    efficiency: Int,
    modifier: Modifier = Modifier
) {
    FrostedGlassCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("GESAMTSCHLAF", color = Color(0xFFB9D9ED), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                Spacer(Modifier.height(6.dp))
                Text(duration, color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Bold)
                Text("☾  Schlafzeit", color = Color(0xFF9CD5E5), fontSize = 12.sp)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(94.dp), contentAlignment = Alignment.Center) {
                    Canvas(Modifier.fillMaxSize()) {
                        val stroke = 6.dp.toPx()
                        drawArc(Color(0x557F9AB8), 0f, 360f, false, style = Stroke(stroke, cap = StrokeCap.Round))
                        drawArc(
                            when {
                                score >= 80 -> Color(0xFF4CEBBD)
                                score >= 60 -> Color(0xFFFFC856)
                                else -> Color(0xFFFF6887)
                            },
                            -90f, 360f * score.coerceIn(0, 100) / 100f, false,
                            style = Stroke(stroke, cap = StrokeCap.Round)
                        )
                    }
                    Text(score.toString(), color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                }
                Text("SLEEP SCORE", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text("$efficiency% EFFIZIENZ", color = Color(0xFFB9D9ED), fontSize = 10.sp)
            }
        }
    }
}
