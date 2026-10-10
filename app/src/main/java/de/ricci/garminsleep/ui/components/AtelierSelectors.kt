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
    val names = listOf("Empfohlen", "SleepSync", "Eigenes", "OLED")
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(vertical = 8.dp)) {
        itemsIndexed(names) { index, name ->
            FrostedGlassCard(modifier = Modifier.width(130.dp).clickable { onSelect(index) }) {
                Box(Modifier.fillMaxWidth().height(84.dp).clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF151A32)), contentAlignment = Alignment.Center) {
                    if (index < 2) Image(
                        painterResource(if (index == 0) R.drawable.cosmic_crescent else R.drawable.cosmic_planet),
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
        Triple("Blau", 0xFF4B7EFF.toInt(), 0xFF23DCFF.toInt()),
        Triple("Lila", 0xFFA149F2.toInt(), 0xFF6949FF.toInt()),
        Triple("Cyan", 0xFF0AD4EA.toInt(), 0xFF217DFF.toInt()),
        Triple("Pink", 0xFFF53E9C.toInt(), 0xFFA145F9.toInt()),
        Triple("Orange", 0xFFFF9B31.toInt(), 0xFFFF5575.toInt()),
        Triple("Grün", 0xFF21D897.toInt(), 0xFF18A8CF.toInt()),
        Triple("Gold", 0xFFFFC541.toInt(), 0xFFFF7836.toInt()),
        Triple("Dynamisch", 0xFF8C52FA.toInt(), 0xFF1BD9F3.toInt())
    )
    FrostedGlassCard {
        Text("◉  FAR BSCHEMA".replace("FAR B", "FARB"), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            itemsIndexed(palettes) { _, (name, primary, secondary) ->
                Column(Modifier.width(64.dp).clickable { onSelect(primary, secondary) },
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(44.dp).clip(CircleShape)
                        .background(Brush.linearGradient(listOf(Color(primary), Color(secondary))))
                        .then(if (selectedColor == primary) Modifier.border(2.dp, Color.White, CircleShape) else Modifier))
                    Spacer(Modifier.height(5.dp))
                    Text(name, color = Color.White, fontSize = 10.sp)
                }
            }
        }
    }
}
