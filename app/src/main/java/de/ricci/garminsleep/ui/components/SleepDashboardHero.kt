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
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val compact = maxWidth < 350.dp
            val ringSize = if (compact) 76.dp else 92.dp
            val scoreWidth = if (compact) 94.dp else 112.dp
            Row(
                modifier = Modifier.fillMaxWidth().heightIn(min = if (compact) 124.dp else 142.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        "GESAMTSCHLAF", color = Color(0xFFB9D9ED),
                        fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp
                    )
                    Spacer(Modifier.height(7.dp))
                    Text(
                        duration, color = Color.White,
                        fontSize = if (compact) 27.sp else 34.sp,
                        lineHeight = if (compact) 32.sp else 40.sp,
                        fontWeight = FontWeight.Bold, maxLines = 1,
                        softWrap = false
                    )
                    Spacer(Modifier.height(4.dp))
                    Text("☾  Schlafzeit", color = Color(0xFF9CD5E5), fontSize = 12.sp)
                }
                Column(
                    modifier = Modifier.width(scoreWidth),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(Modifier.size(ringSize), contentAlignment = Alignment.Center) {
                        Canvas(Modifier.fillMaxSize().padding(5.dp)) {
                            val stroke = 6.dp.toPx()
                            drawArc(
                                Color(0x557F9AB8), 0f, 360f, false,
                                style = Stroke(stroke, cap = StrokeCap.Round)
                            )
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
                        Text(
                            score.toString(), color = Color.White,
                            fontSize = if (compact) 24.sp else 28.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.height(9.dp))
                    Text(
                        "SLEEP SCORE", color = Color.White,
                        fontSize = 10.sp, fontWeight = FontWeight.Bold,
                        lineHeight = 13.sp, maxLines = 1, softWrap = false
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "$efficiency% EFFIZIENZ", color = Color(0xFFB9D9ED),
                        fontSize = 10.sp, lineHeight = 13.sp,
                        maxLines = 1, softWrap = false
                    )
                }
            }
        }
    }
}
