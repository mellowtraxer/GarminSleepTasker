package de.ricci.garminsleep.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.geometry.Offset

/** Four independent, touch-friendly controls without default Material slider thumbs. */
@Composable
fun DesignStudioEffects(
    blur: Int, glass: Int, neon: Int, glow: Int,
    onValueChanged: (String, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val settings = listOf(
        Triple("WEICHZEICHNUNG", "blur_strength", blur),
        Triple("GLASTIEFE", "glass_strength", glass),
        Triple("NEONKANTE", "neon_strength", neon),
        Triple("LICHTSCHEIN", "glow_strength", glow)
    )
    val colors = listOf(Color(0xFF24DFFF), Color(0xFF9862FF), Color(0xFFFF37BC), Color(0xFFB66BFF))
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        settings.chunked(2).forEachIndexed { row, pair ->
            FrostedGlassCard {
                Text(if (row == 0) "✦  MATERIAL & TIEFE" else "✦  LICHT & AURA",
                    color = Color(0xFFB8DAF4), fontSize = 11.sp,
                    fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Spacer(Modifier.height(16.dp))
                pair.forEachIndexed { col, (title, key, initial) ->
                    var value by remember(key) { mutableFloatStateOf(initial.coerceIn(0,100).toFloat()) }
                    val color = colors[row * 2 + col]
                    fun change(next: Float) {
                        value = next.coerceIn(0f,100f)
                        when (key) {
                            "blur_strength" -> StudioGlassTuning.blur = value.toInt()
                            "glass_strength" -> StudioGlassTuning.glass = value.toInt()
                            "neon_strength" -> StudioGlassTuning.neon = value.toInt()
                            "glow_strength" -> StudioGlassTuning.glow = value.toInt()
                        }
                        onValueChanged(key, value.toInt())
                    }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(title, Modifier.weight(1f), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        Text("${value.toInt()} %", color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(10.dp))
                    var trackWidth by remember { mutableIntStateOf(1) }
                    Box(Modifier.fillMaxWidth().height(26.dp)
                        .onSizeChanged { trackWidth = it.width.coerceAtLeast(1) }
                        .pointerInput(key, trackWidth) {
                            detectTapGestures { offset -> change(offset.x / trackWidth * 100f) }
                        }
                        .pointerInput(key, trackWidth) {
                            detectDragGestures { event, _ -> change(event.position.x / trackWidth * 100f) }
                        }, contentAlignment = Alignment.CenterStart) {
                        Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50))
                            .background(Color(0xFF19243A)))
                        Box(Modifier.fillMaxWidth(value / 100f).height(6.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Brush.horizontalGradient(listOf(color.copy(alpha=.45f), color))))
                        Box(Modifier.fillMaxWidth().height(6.dp).border(.4.dp,Color.White.copy(alpha=.12f),RoundedCornerShape(50)))
                        Box(Modifier.offset(x = 0.dp).fillMaxWidth(value / 100f).wrapContentWidth(Alignment.End)) {
                            Box(Modifier.size(15.dp).background(color, RoundedCornerShape(50))
                                .border(2.dp, Color.White.copy(alpha=.9f),RoundedCornerShape(50)))
                        }
                    }
                    if (col == 0) Spacer(Modifier.height(18.dp))
                }
            }
        }
    }
}
