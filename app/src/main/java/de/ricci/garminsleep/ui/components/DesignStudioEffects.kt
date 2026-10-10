package de.ricci.garminsleep.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Design Studio effects: native Compose controls, preserving the existing preference keys. */
@Composable
fun DesignStudioEffects(
    blur: Int,
    glass: Int,
    neon: Int,
    glow: Int,
    onValueChanged: (String, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val settings = listOf(
        Triple("✦ Blur", "blur_strength", blur),
        Triple("◉ Glas", "glass_strength", glass),
        Triple("✧ Neon", "neon_strength", neon),
        Triple("✺ Glow", "glow_strength", glow)
    )
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        settings.chunked(2).forEachIndexed { rowIndex, pair ->
          Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
           pair.forEachIndexed { columnIndex, (title, key, initial) ->
            val index = rowIndex * 2 + columnIndex
            var value by remember(key, initial) { mutableFloatStateOf(initial.coerceIn(0, 100).toFloat()) }
            FrostedGlassCard(modifier = Modifier.weight(1f)) {
                Text(title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                Spacer(Modifier.height(10.dp))
                Box(
                    Modifier.fillMaxWidth().height(46.dp)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    listOf(Color(0xFF22D9F5), Color(0xFF9B5AFF), Color(0xFFE14DFF), Color(0xFFFF4DDA))[index].copy(alpha = .25f + value / 140f),
                                    Color(0xFF10152C)
                                )
                            ), RoundedCornerShape(12.dp)
                        )
                )
                Spacer(Modifier.height(8.dp))
                Slider(
                    value = value,
                    onValueChange = {
                        value = it
                        when (key) {
                            "blur_strength" -> StudioGlassTuning.blur = it.toInt()
                            "glass_strength" -> StudioGlassTuning.glass = it.toInt()
                            "neon_strength" -> StudioGlassTuning.neon = it.toInt()
                            "glow_strength" -> StudioGlassTuning.glow = it.toInt()
                        }
                        onValueChanged(key, it.toInt())
                    },
                    onValueChangeFinished = { onValueChanged(key, value.toInt()) },
                    valueRange = 0f..100f
                )
                Text("${value.toInt()} %", color = Color(0xFFBDCAE0), fontSize = 10.sp)
            }
           }
          }
        }
    }
}
