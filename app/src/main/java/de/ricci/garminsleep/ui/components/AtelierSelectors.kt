package de.ricci.garminsleep.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.ricci.garminsleep.R

@Composable
fun AtelierWallpaperGallery(selected: Int, onSelect: (Int) -> Unit) {
    val names = listOf("LIVE AURORA", "SleepSync", "DreamScape", "Eigenes", "OLED")
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(vertical = 8.dp)) {
        itemsIndexed(names) { index, name ->
            FrostedGlassCard(modifier = Modifier.width(138.dp), onClick = { onSelect(index) }) {
                Box(Modifier.fillMaxWidth().height(84.dp).clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF151A32)), contentAlignment = Alignment.Center) {
                    if (index == 1 || index == 2) Image(
                        painterResource(if (index == 2) R.drawable.cosmic_crescent else R.drawable.cosmic_planet),
                        contentDescription = name, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()
                    ) else Text(if (index == 2) "▧" else "●", color = Color.White, fontSize = 30.sp)
                    if (selected == index) Text("✓", color = Color.White, modifier = Modifier.align(Alignment.TopEnd)
                        .background(Color(0xFFB44AFF), CircleShape).padding(horizontal = 6.dp))
                }
                Spacer(Modifier.height(6.dp))
                Text(name, color = if (selected == index) Color(0xFF61E9FF) else Color.White,
                    fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun AtelierColorPalette(selectedColor: Int, onSelect: (Int, Int) -> Unit) {
    val palettes = listOf(
        Triple("Aurora", 0, 0),
        Triple("Cyan", 0xFF00F0FF.toInt(), 0xFF00F0FF.toInt()),
        Triple("Pink", 0xFFFF00B8.toInt(), 0xFFFF00B8.toInt()),
        Triple("Violett", 0xFFAE48FF.toInt(), 0xFFAE48FF.toInt()),
        Triple("Blau", 0xFF247BFF.toInt(), 0xFF247BFF.toInt()),
        Triple("Grün", 0xFF39FF14.toInt(), 0xFF39FF14.toInt()),
        Triple("Orange", 0xFFFF6A00.toInt(), 0xFFFF6A00.toInt()),
        Triple("Gelb", 0xFFFFFF00.toInt(), 0xFFFFFF00.toInt()),
        Triple("Rot", 0xFFFF1744.toInt(), 0xFFFF1744.toInt()),
        Triple("Cyberpunk", 0xFF00F0FF.toInt(), 0xFFFF00B8.toInt()),
        Triple("Ultraviolet", 0xFFAE48FF.toInt(), 0xFF00F0FF.toInt()),
        Triple("Laser", 0xFFFF1744.toInt(), 0xFF247BFF.toInt()),
        Triple("Toxic", 0xFF39FF14.toInt(), 0xFF00F0FF.toInt()),
        Triple("Sunset", 0xFFFF6A00.toInt(), 0xFFFF00B8.toInt()),
        Triple("Electric", 0xFFFFFF00.toInt(), 0xFFAE48FF.toInt()),
        Triple("Synthwave", 0xFFFF00B8.toInt(), 0xFF247BFF.toInt())

    )
    val liveSelection = StudioGlassTuning.selectedNeon
    val liveSecondary = StudioGlassTuning.secondaryNeon
    FrostedGlassCard {
        Text("✦  NEONFARBEN & DUOS", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            itemsIndexed(palettes) { _, (name, primary, secondary) ->
                Column(Modifier.width(64.dp).clickable { StudioGlassTuning.selectedNeon = primary; StudioGlassTuning.secondaryNeon = secondary; onSelect(primary, secondary) },
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(44.dp).clip(CircleShape)
                        .background(if (primary == 0) Brush.linearGradient(listOf(Color(0xFF00F0FF),Color(0xFFAE48FF),Color(0xFFFF00B8))) else Brush.linearGradient(listOf(Color(primary),Color(secondary))))
                        .then(if (liveSelection == primary && liveSecondary == secondary) Modifier.border(2.dp, Color.White, CircleShape) else Modifier))
                    Spacer(Modifier.height(5.dp))
                    Text(name, color = Color.White, fontSize = 10.sp)
                }
            }
        }
    }
}
