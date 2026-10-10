package de.ricci.garminsleep.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Custom neon-light controls: no Material slider styling, direct GPU palette feedback. */
@Composable
fun WallpaperCinematicControls(onValueChanged: (String, Int) -> Unit) {
    val controls = listOf(
        Triple("INTENSITÄT", "intensity", WallpaperNeonTuning.intensity),
        Triple("ANIMATION", "animation", WallpaperNeonTuning.animation),
        Triple("3D-TIEFE", "depth", WallpaperNeonTuning.depth),
        Triple("PARTIKEL", "particles", WallpaperNeonTuning.particles),
        Triple("LICHTSTRAHLEN", "rays", WallpaperNeonTuning.rays)
    )
    val palette = WallpaperNeonTuning.palette()
    val a = palette[0]
    val b = palette[1]
    FrostedGlassCard {
        Text("✦  CINEMATIC DEPTH ENGINE", color = Color.White,
            fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("Atmosphäre · Parallaxe · Licht · Bewegung",
            color = Color(0xFFB9C9E3), fontSize = 11.sp)
        controls.forEach { (title, key, value) ->
            Spacer(Modifier.height(17.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(title, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text("$value %", color = b, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(6.dp))
            val update: (Float, Float) -> Unit = { x, width ->
                val v = ((x / width.coerceAtLeast(1f)).coerceIn(0f, 1f) * 100f).toInt()
                when (key) {
                    "intensity" -> WallpaperNeonTuning.intensity = v
                    "animation" -> WallpaperNeonTuning.animation = v
                    "depth" -> WallpaperNeonTuning.depth = v
                    "particles" -> WallpaperNeonTuning.particles = v
                    "rays" -> WallpaperNeonTuning.rays = v
                }
                onValueChanged(key, v)
            }
            Canvas(Modifier.fillMaxWidth().height(35.dp)
                .pointerInput(key) {
                    detectTapGestures { pos -> update(pos.x, size.width.toFloat()) }
                }
                .pointerInput(key) {
                    detectDragGestures(
                        onDragStart = { pos -> update(pos.x, size.width.toFloat()) },
                        onDrag = { change, _ ->
                            update(change.position.x, size.width.toFloat())
                            change.consume()
                        }
                    )
                }
            ) {
                val cy = size.height / 2f
                val start = 13.dp.toPx()
                val end = size.width - start
                val width = (end - start).coerceAtLeast(1f)
                val fraction = value / 100f
                val x = start + width * fraction
                val gradient = Brush.horizontalGradient(listOf(a, b, a))
                drawLine(Color(0xFF283147), Offset(start, cy), Offset(end, cy),
                    strokeWidth = 6.dp.toPx(), cap = StrokeCap.Round)
                if (fraction > 0f) {
                    drawLine(brush = gradient, start = Offset(start, cy),
                        end = Offset(x, cy), strokeWidth = 10.dp.toPx(),
                        alpha = .17f, cap = StrokeCap.Round)
                    drawLine(brush = gradient, start = Offset(start, cy),
                        end = Offset(x, cy), strokeWidth = 4.dp.toPx(),
                        cap = StrokeCap.Round)
                }
                drawCircle(b.copy(alpha = .09f), 16.dp.toPx(), Offset(x, cy))
                drawCircle(b.copy(alpha = .24f), 11.dp.toPx(), Offset(x, cy))
                drawCircle(Color.White, 6.5.dp.toPx(), Offset(x, cy))
                drawCircle(a, 4.dp.toPx(), Offset(x, cy))
            }
        }
    }
}
