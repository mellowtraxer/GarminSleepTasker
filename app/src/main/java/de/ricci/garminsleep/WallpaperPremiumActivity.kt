package de.ricci.garminsleep

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.ricci.garminsleep.ui.components.SleepSyncLiveWallpaper

/** Honest, non-transactional premium teaser; never simulates a completed purchase. */
class WallpaperPremiumActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var devUnlocked by remember { mutableStateOf(WallpaperPremiumGate.isDeveloperPreview(this@WallpaperPremiumActivity)) }
            Box(Modifier.fillMaxSize().background(Color(0xFF02020A))) {
                SleepSyncLiveWallpaper("nebula_flow", Modifier.fillMaxSize())
                Box(Modifier.fillMaxSize().background(Color(0xB9000010)))
                Column(Modifier.fillMaxSize().padding(horizontal = 26.dp, vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                        Text("←  ZURÜCK", color = Color.White,
                            modifier = Modifier.clickable { finish() }.padding(8.dp), fontSize = 12.sp)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("✦", fontSize = 58.sp, color = Color(0xFF72EEFF))
                        Spacer(Modifier.height(12.dp))
                        Text("SLEEPSYNC", fontSize = 13.sp, color = Color(0xFF70E8FF),
                            fontWeight = FontWeight.Bold, letterSpacing = 4.sp)
                        Text("LIVE WORLDS", fontSize = 38.sp, color = Color.White,
                            fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(12.dp))
                        Text("DEIN UNIVERSUM. DEINE FARBEN.", fontSize = 12.sp,
                            color = Color(0xFFDDC4FF), letterSpacing = 1.2.sp)
                        Spacer(Modifier.height(25.dp))
                        Text("✧  12 animierte OLED-Lichtwelten\n✧  Neon Color Lab & Duo-Farben\n✧  Cinematic Depth Engine\n✧  PUR-Modus & Android-Live-Wallpaper",
                            color = Color.White, lineHeight = 28.sp, fontSize = 14.sp)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.fillMaxWidth()
                            .border(1.dp, Brush.horizontalGradient(listOf(Color(0xFF52E7FF),
                                Color(0xFFA75BFF),Color(0xFFFF49C5))), RoundedCornerShape(22.dp))
                            .background(Color(0xBB16112C), RoundedCornerShape(22.dp))
                            .padding(22.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("PREMIUM · EINMALIG",color=Color(0xFFB6C5F7),fontSize=12.sp)
                                Spacer(Modifier.height(5.dp))
                                Text(WallpaperPremiumGate.PREMIUM_PRICE_LABEL,
                                    color=Color.White,fontSize=36.sp,fontWeight=FontWeight.Bold)
                                Spacer(Modifier.height(10.dp))
                                Text("BALD VERFÜGBAR",color=Color(0xFF71E9FF),
                                    fontSize=15.sp,fontWeight=FontWeight.Bold)
                                Text("Dies ist eine Vorschau. Es wird keine Zahlung ausgelöst.",
                                    color=Color(0xFFD0C7DF),fontSize=11.sp,textAlign=TextAlign.Center)
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Text(if (devUnlocked) "✓ ENTWICKLERVORSCHAU AKTIV" else "✦ ENTWICKLERVORSCHAU FREISCHALTEN",
                            color=Color(0xFF6CF3FF),fontWeight=FontWeight.Bold,
                            modifier=Modifier.clickable {
                                WallpaperPremiumGate.setDeveloperPreview(this@WallpaperPremiumActivity, !devUnlocked)
                                devUnlocked = !devUnlocked
                                if (devUnlocked) finish()
                            }.padding(12.dp),fontSize=12.sp)
                        Text("Nur für interne Tests · kein Kauf · vor Veröffentlichung entfernen",
                            color=Color(0xFFB6C5F7),fontSize=10.sp,textAlign=TextAlign.Center)
                        Spacer(Modifier.height(8.dp))
                        Text("AURORA DREAM BLEIBT KOSTENLOS",color=Color.White,
                            modifier=Modifier.clickable { finish() }.padding(10.dp),fontSize=12.sp)
                    }
                }
            }
        }
    }
}
