package de.ricci.garminsleep.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class HistoryNightUi(
    val id: Long,
    val date: String,
    val time: String,
    val minutes: Long,
    val light: Long,
    val deep: Long,
    val rem: Long,
    val awake: Long
)
data class HistoryWeekUi(
    val key: Int,
    val week: Int,
    val year: Int,
    val average: Long,
    val delta: Long?,
    val nights: List<HistoryNightUi>
)

private val muted = Color(0xFFB8C7E3)
private val cyan = Color(0xFF00F0FF)

@Composable
fun SleepHistoryWeekCards(
    weeks: List<HistoryWeekUi>,
    onNightClick: (Long) -> Unit,
    onWeekDetails: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
        weeks.forEach { week ->
            var expanded by remember(week.key) { mutableStateOf(false) }
            FrostedGlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("KW ${week.week} · ${week.year}", fontSize = 18.sp,
                            fontWeight = FontWeight.Bold, color = Color.White)
                        Spacer(Modifier.height(6.dp))
                        Text("Ø ${week.average / 60} h ${week.average % 60} min  ·  ${week.nights.size} Nächte",
                            fontSize = 12.sp, color = muted)
                        week.delta?.let { delta ->
                            val abs = kotlin.math.abs(delta)
                            Text("${if (delta >= 0) "▲ +" else "▼ −"}${abs / 60} h ${abs % 60} min zur Vorwoche",
                                fontSize = 11.sp,
                                color = if (delta >= 0) Color(0xFF57F4B9) else Color(0xFFFFAE83))
                        }
                    }
                    Text(if (expanded) "⌃" else "⌄", fontSize = 26.sp, color = cyan)
                }
                if (expanded) {
                    Spacer(Modifier.height(16.dp))
                    week.nights.forEach { night ->
                        FrostedGlassCard(
                            modifier = Modifier.fillMaxWidth()
                                .padding(vertical = 5.dp)
                                .clickable { onNightClick(night.id) },
                            cornerRadius = 17.dp
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("☾  ${night.date}", fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold, color = Color.White)
                                    Text(night.time, fontSize = 11.sp, color = muted)
                                }
                                Text("${night.minutes / 60} h ${night.minutes % 60} min  ›",
                                    fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            Spacer(Modifier.height(10.dp))
                            val segments = listOf(
                                night.light to Color(0xFF63BEFF),
                                night.deep to Color(0xFF654BDD),
                                night.rem to Color(0xFFB763FF),
                                night.awake to Color(0xFFFFA45B)
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(3.dp),
                                modifier = Modifier.fillMaxWidth().height(8.dp)) {
                                val total = segments.sumOf { it.first }.coerceAtLeast(1)
                                segments.forEach { (minutes, color) ->
                                    if (minutes > 0) {
                                        androidx.compose.foundation.layout.Box(
                                            Modifier.weight(minutes.toFloat() / total)
                                                .fillMaxHeight()
                                                .background(color, RoundedCornerShape(5.dp))
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("● LEICHT     ● TIEF     ● REM     ● WACH",
                        color = muted, fontSize = 10.sp)
                    Spacer(Modifier.height(10.dp))
                    FrostedGlassCard(modifier = Modifier.fillMaxWidth()
                        .clickable { onWeekDetails(week.key) }, cornerRadius = 16.dp) {
                        Text("▥  Wochen-Details  ›", color = cyan,
                            fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
