package de.ricci.garminsleep.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Four native Compose sleep phase cards, all backed by recorded phase minutes. */
@Composable
fun SleepPhaseGrid(
    lightMinutes: Long,
    deepMinutes: Long,
    remMinutes: Long,
    awakeMinutes: Long,
    onPhaseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val phases = listOf(
        Triple("🌙  LEICHT", lightMinutes, Color(0xFF38D8FF)),
        Triple("🌑  TIEF", deepMinutes, Color(0xFF9370FF)),
        Triple("🧠  REM", remMinutes, Color(0xFFFF64CD)),
        Triple("👀  WACH", awakeMinutes, Color(0xFFFFBE73))
    )
    val total = phases.sumOf { it.second.coerceAtLeast(0L) }.coerceAtLeast(1L)
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        for (row in phases.chunked(2)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { (title, minutes, color) ->
                    FrostedGlassCard(modifier = Modifier.weight(1f), onClick = onPhaseClick) {
                        Text(title, color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text("${minutes / 60} h ${minutes % 60} min", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text("${minutes.coerceAtLeast(0L) * 100 / total} % der Nacht", color = Color(0xFFB5C5DC), fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
