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
fun AtelierWallpaperGallery(selected: Int, onSelect: (Int) -> Unit, onPremium: () -> Unit, developerUnlocked: Boolean = false) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(vertical = 8.dp)) {
        itemsIndexed(sleepSyncLiveStyles) { index, style ->
            FrostedGlassCard(modifier = Modifier.width(164.dp), onClick = { if (index == 0 || developerUnlocked) onSelect(index) else onPremium() }) {
                Box(Modifier.fillMaxWidth().height(124.dp).clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF03030B))) {
                    SleepSyncLiveWallpaper(style.id, Modifier.fillMaxSize())
                    if (index != 0 && !developerUnlocked) Text("🔒 PREMIUM", color = Color.White,
                        fontSize = 10.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.TopStart)
                            .background(Color(0xC9000012), RoundedCornerShape(8.dp)).padding(5.dp))
                    if (selected == index && (index == 0 || developerUnlocked)) Text("✓", color = Color.White,
                        modifier = Modifier.align(Alignment.TopEnd)
                            .background(Color(0xFFB44AFF), CircleShape).padding(horizontal = 6.dp))
                    Text("▶", color = Color.White.copy(alpha = .85f),
                        modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp), fontSize = 13.sp)
                }
                Spacer(Modifier.height(5.dp))
                Text(style.title, color = if (selected == index) Color(0xFF61E9FF) else Color.White,
                    fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text(if (index == 0) "FREE · " + style.subtitle else style.subtitle,
                    color = Color(0xFFB8C7E0), fontSize = 9.sp)
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

@Composable
fun AtelierWallpaperColorPalette(onSelect: (Int, Int) -> Unit) {
    val palettes = listOf(
        Triple("Aurora", 0, 0),
        Triple("Ice Blue", 0xFF00F0FF.toInt(), 0xFF247BFF.toInt()),
        Triple("Pink Velvet", 0xFFFF00B8.toInt(), 0xFFAE48FF.toInt()),
        Triple("Cyberwave", 0xFF00F0FF.toInt(), 0xFFFF00B8.toInt()),
        Triple("Ultraviolet", 0xFFAE48FF.toInt(), 0xFF247BFF.toInt()),
        Triple("Toxic Bloom", 0xFF39FF14.toInt(), 0xFF00F0FF.toInt()),
        Triple("Solar Kiss", 0xFFFF6A00.toInt(), 0xFFFF00B8.toInt()),
        Triple("Electric Sun", 0xFFFFFF00.toInt(), 0xFFAE48FF.toInt()),
        Triple("Synthwave", 0xFFFF00B8.toInt(), 0xFF247BFF.toInt()),
        Triple("Neon Cyan", 0xFF00F0FF.toInt(), 0xFF00F0FF.toInt()),
        Triple("Neon Pink", 0xFFFF00B8.toInt(), 0xFFFF00B8.toInt()),
        Triple("Neon Violet", 0xFFAE48FF.toInt(), 0xFFAE48FF.toInt()),
        Triple("Neon Green", 0xFF39FF14.toInt(), 0xFF39FF14.toInt())
    )
    FrostedGlassCard {
        Text("✦  LIVE-WALLPAPER · FARBWELTEN", color = Color.White,
            fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(5.dp))
        Text("Nur für den Hintergrund · Kartenfarben bleiben unverändert",
            color = Color(0xFFB5C5E0), fontSize = 10.sp)
        Spacer(Modifier.height(12.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            itemsIndexed(palettes) { _, (name, primary, secondary) ->
                val chosen = WallpaperNeonTuning.primary == primary && WallpaperNeonTuning.secondary == secondary
                Column(Modifier.width(77.dp).clickable {
                    WallpaperNeonTuning.primary = primary
                    WallpaperNeonTuning.secondary = secondary
                    onSelect(primary, secondary)
                }, horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(48.dp).clip(CircleShape)
                        .background(Brush.linearGradient(if (primary == 0)
                            listOf(Color(0xFF00F0FF), Color(0xFFAE48FF), Color(0xFFFF00B8))
                            else listOf(Color(primary), Color(secondary))))
                        .then(if (chosen) Modifier.border(2.dp, Color.White, CircleShape) else Modifier)) {
                        if (chosen) Text("✓", color = Color.White,
                            modifier = Modifier.align(Alignment.Center), fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(name, color = Color.White, fontSize = 10.sp,
                        maxLines = 2, lineHeight = 12.sp)
                }
            }
        }
    }
}
