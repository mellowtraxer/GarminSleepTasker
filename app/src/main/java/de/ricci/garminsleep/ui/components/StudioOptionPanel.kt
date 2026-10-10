package de.ricci.garminsleep.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun StudioOptionPanel(
    title: String,
    options: List<Triple<String, String, String>>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    FrostedGlassCard(modifier = modifier) {
        Text(title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            options.forEach { (key, label, symbol) ->
                val active = key == selected
                val shape = RoundedCornerShape(12.dp)
                Column(
                    Modifier.weight(1f).height(86.dp)
                        .background(
                            if (active) Brush.verticalGradient(listOf(Color(0xFF39205D), Color(0xFF101B39)))
                            else Brush.verticalGradient(listOf(Color(0xFF182341), Color(0xFF10162B))),
                            shape
                        )
                        .border(
                            if (active) 1.5.dp else .7.dp,
                            if (active) Brush.linearGradient(listOf(Color(0xFF00E5FF), Color(0xFFA855F7), Color(0xFFFF1AB6)))
                            else Brush.linearGradient(listOf(Color(0xFF425277), Color(0xFF344163))),
                            shape
                        )
                        .clickable { DreamscapeMotion.ripple(); onSelect(key) }
                        .padding(horizontal = 2.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(symbol, color = if (active) Color(0xFFD9ADFF) else Color.White, fontSize = 23.sp)
                    Spacer(Modifier.height(5.dp))
                    Text(label, color = Color.White, fontSize = 9.sp, textAlign = TextAlign.Center, maxLines = 1)
                }
            }
        }
    }
}
