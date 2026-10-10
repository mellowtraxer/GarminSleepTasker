package de.ricci.garminsleep

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import de.ricci.garminsleep.ui.components.*

/** Immersive, distraction-free wallpaper cinema. Tap anywhere to reveal the controls. */
class WallpaperCinemaActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE)
        val prefs = getSharedPreferences("sleepsync_design", MODE_PRIVATE)
        WallpaperNeonTuning.primary = prefs.getInt("wallpaper_neon_primary",0)
        WallpaperNeonTuning.secondary = prefs.getInt("wallpaper_neon_secondary",0)
        WallpaperNeonTuning.intensity = prefs.getInt("wallpaper_cinema_intensity",72)
        WallpaperNeonTuning.animation = prefs.getInt("wallpaper_cinema_animation",55)
        WallpaperNeonTuning.depth = prefs.getInt("wallpaper_cinema_depth",78)
        WallpaperNeonTuning.particles = prefs.getInt("wallpaper_cinema_particles",48)
        WallpaperNeonTuning.rays = prefs.getInt("wallpaper_cinema_rays",62)
        val style = prefs.getString("wallpaper_source","live_aurora_dream")
            ?.removePrefix("live_") ?: "aurora_dream"
        setContent {
            var controls by remember { mutableStateOf(true) }
            Box(Modifier.fillMaxSize()) {
                SleepSyncLiveWallpaper(style,Modifier.fillMaxSize())
                Box(Modifier.fillMaxSize().clickable { controls = !controls })
                if (controls) {
                    Column(Modifier.align(Alignment.BottomCenter).padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("✦  PURE CINEMA  ·  Zum Ausblenden tippen",
                            color=Color.White)
                        Spacer(Modifier.height(16.dp))
                        Text("ALS ANDROID-WALLPAPER FESTLEGEN ↗",
                            color=Color(0xFF65EAFF),
                            modifier=Modifier.clickable { openSystemWallpaperPicker() }.padding(12.dp))
                        Text("ZURÜCK ZUM STUDIO",
                            color=Color.White,
                            modifier=Modifier.clickable { finish() }.padding(12.dp))
                    }
                }
            }
        }
    }
    private fun openSystemWallpaperPicker() {
        val component = ComponentName(this, SleepSyncWallpaperService::class.java)
        val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
            .putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, component)
        try { startActivity(intent) } catch (_: Exception) {
            startActivity(Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER))
        }
    }
}
