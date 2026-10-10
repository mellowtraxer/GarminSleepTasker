package de.ricci.garminsleep.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun WallpaperCinematicControls(onValueChanged: (String, Int) -> Unit) {
    val controls = listOf(
        Triple("INTENSITÄT", "intensity", WallpaperNeonTuning.intensity),
        Triple("ANIMATION", "animation", WallpaperNeonTuning.animation),
        Triple("3D-TIEFE", "depth", WallpaperNeonTuning.depth),
        Triple("PARTIKEL", "particles", WallpaperNeonTuning.particles),
        Triple("LICHTSTRAHLEN", "rays", WallpaperNeonTuning.rays)
    )
    FrostedGlassCard {
        Text("✦  CINEMATIC DEPTH ENGINE", color = Color.White,
            fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("Atmosphäre · Parallaxe · Licht · Bewegung",
            color = Color(0xFFB9C9E3), fontSize = 11.sp)
        controls.forEach { (title, key, value) ->
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(title, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text("$value %", color = Color(0xFF5DEBFF), fontSize = 11.sp)
            }
            Slider(
                value = value.toFloat(),
                onValueChange = { newValue ->
                    val v = newValue.toInt().coerceIn(0, 100)
                    when (key) {
                        "intensity" -> WallpaperNeonTuning.intensity = v
                        "animation" -> WallpaperNeonTuning.animation = v
                        "depth" -> WallpaperNeonTuning.depth = v
                        "particles" -> WallpaperNeonTuning.particles = v
                        "rays" -> WallpaperNeonTuning.rays = v
                    }
                    onValueChanged(key, v)
                },
                valueRange = 0f..100f,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF5DEBFF),
                    activeTrackColor = Color(0xFFB04AFF),
                    inactiveTrackColor = Color(0xFF29324B)
                )
            )
        }
    }
}
