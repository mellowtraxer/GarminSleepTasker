package de.ricci.garminsleep.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class SleepTrendCardData(val days: Int, val averageMinutes: Long?, val nights: Int)

@Composable
fun SleepHistoryTrendCards(trends: List<SleepTrendCardData>) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("LANGZEIT-TREND  ·  Ø SCHLAFDAUER", color = Color(0xFFB763FF),
            fontSize = 11.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            trends.forEach { trend ->
                FrostedGlassCard(modifier = Modifier.weight(1f), cornerRadius = 15.dp) {
                    Text("${trend.days} TAGE", color = Color(0xFFB5C5E2), fontSize = 10.sp)
                    Spacer(Modifier.height(6.dp))
                    Text(trend.averageMinutes?.let { "${it / 60} h ${it % 60} min" } ?: "–",
                        color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text("${trend.nights} Nächte", color = Color(0xFFA0CDEB), fontSize = 10.sp)
                }
            }
        }
        Spacer(Modifier.height(18.dp))
    }
}
