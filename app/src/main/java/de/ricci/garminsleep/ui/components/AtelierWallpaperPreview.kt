package de.ricci.garminsleep.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
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
    onChange: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bitmap = remember(imagePath) {
        imagePath?.let { path -> runCatching { BitmapFactory.decodeFile(path)?.asImageBitmap() }.getOrNull() }
    }
    FrostedGlassCard(modifier = modifier.fillMaxWidth().clickable(onClick = onChange)) {
        Box(Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(15.dp))) {
            if (bitmap != null) {
                Image(bitmap = bitmap, contentDescription = title, contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize())
            } else {
                Image(painterResource(R.drawable.cosmic_crescent), contentDescription = title,
                    contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            }
            Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.SpaceBetween) {
                Text("✦  AKTUELLES WALLPAPER", color = Color(0xFF7EEBFF), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Column {
                    Text(title, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    Text("Dein Schlafkosmos. In deinem Licht.", color = Color.White, fontSize = 12.sp)
                    Spacer(Modifier.height(10.dp))
                    Text("✦  WALLPAPER WECHSELN  ↗", color = Color(0xFF7EEBFF), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
