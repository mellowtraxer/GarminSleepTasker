package de.ricci.garminsleep.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.ricci.garminsleep.R

@Composable
fun AtelierWallpaperPreview(
    title: String,
    imagePath: String?,
    liveAurora: Boolean = false,
    liveDreamscape: Boolean = false,
    onChange: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bitmap = remember(imagePath) {
        imagePath?.let { path -> runCatching { BitmapFactory.decodeFile(path)?.asImageBitmap() }.getOrNull() }
    }
    FrostedGlassCard(modifier = modifier.fillMaxWidth(), onClick = onChange) {
        Box(Modifier.fillMaxWidth().height(260.dp).clip(RoundedCornerShape(15.dp))) {
            if (liveDreamscape) {
                AnimatedAuroraBackground(previewDreamscape = true) { }
            } else if (liveAurora) {
                AnimatedAuroraBackground(previewAurora = true) { }
            } else if (bitmap != null) {
                Image(bitmap = bitmap, contentDescription = title, contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize())
            } else {
                Image(painterResource(R.drawable.cosmic_crescent), contentDescription = title,
                    contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            }
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
                listOf(Color.Black.copy(alpha = .15f), Color.Transparent, Color(0xFF030308).copy(alpha = .92f))
            )))
            Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("✦  DREAM STUDIO  /  LIVE PREVIEW", color = Color(0xFF7EEBFF), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    Text(if (liveAurora || liveDreamscape) "● LIVE" else "● FOTO", color = Color(0xFF75FFD9), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                Column {
                    Text(title, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    Text(if (liveAurora) "Fließende Neonseide · Reagiert auf deine Nacht" else if (liveDreamscape) "Schwebender Kosmos · Sanfter Endlos-Loop" else "Dein Schlafkosmos. In deinem Licht.", color = Color(0xFFD7E9FF), fontSize = 12.sp)
                    Spacer(Modifier.height(10.dp))
                    Text("✦  LOOK WECHSELN  ↗", color = Color(0xFF7EEBFF), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
