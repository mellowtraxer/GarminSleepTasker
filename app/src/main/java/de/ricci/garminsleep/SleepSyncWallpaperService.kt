package de.ricci.garminsleep

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RuntimeShader
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import de.ricci.garminsleep.ui.components.CINEMATIC_SHADER
import de.ricci.garminsleep.ui.components.HYPER_COSMIC_SHADER
import de.ricci.garminsleep.ui.components.LIQUID_CHROME_SHADER
import de.ricci.garminsleep.ui.components.sleepSyncLiveStyles
import kotlin.math.min

/** Actual Android live wallpaper. The system handles home/lock screen selection. */
class SleepSyncWallpaperService : WallpaperService() {
    override fun onCreateEngine(): Engine = LiveEngine()

    inner class LiveEngine : Engine() {
        private val handler = Handler(Looper.getMainLooper())
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private var visible = false
        private var width = 1
        private var height = 1
        private val startTime = android.os.SystemClock.uptimeMillis()
        private val shader = if (Build.VERSION.SDK_INT >= 33) RuntimeShader(CINEMATIC_SHADER) else null
        private val hyperShader = if (Build.VERSION.SDK_INT >= 33) RuntimeShader(HYPER_COSMIC_SHADER) else null
        private val chromeShader = if (Build.VERSION.SDK_INT >= 33) RuntimeShader(LIQUID_CHROME_SHADER) else null
        private var touchX = .5f
        private var touchY = .5f
        private val frame = object : Runnable {
            override fun run() {
                if (!visible) return
                render()
                handler.postDelayed(this, 33L)
            }
        }
        override fun onTouchEvent(event: android.view.MotionEvent) {
            if (event.actionMasked == android.view.MotionEvent.ACTION_DOWN || event.actionMasked == android.view.MotionEvent.ACTION_MOVE) {
                touchX = (event.x / width).coerceIn(0f,1f)
                touchY = (event.y / height).coerceIn(0f,1f)
            }
            super.onTouchEvent(event)
        }
        override fun onVisibilityChanged(visible: Boolean) {
            this.visible = visible
            handler.removeCallbacks(frame)
            if (visible) { setTouchEventsEnabled(true); handler.post(frame) }
        }
        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, w: Int, h: Int) {
            super.onSurfaceChanged(holder, format, w, h)
            width = w.coerceAtLeast(1)
            height = h.coerceAtLeast(1)
            if (visible) { handler.removeCallbacks(frame); handler.post(frame) }
        }
        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            visible = false
            handler.removeCallbacks(frame)
            super.onSurfaceDestroyed(holder)
        }
        override fun onDestroy() {
            visible = false
            handler.removeCallbacks(frame)
            super.onDestroy()
        }
        private fun render() {
            val holder = surfaceHolder
            var canvas: Canvas? = null
            try {
                canvas = if (Build.VERSION.SDK_INT >= 26) holder.lockHardwareCanvas() else holder.lockCanvas()
                val c = canvas ?: return
                c.drawColor(Color.rgb(2,3,12))
                if (Build.VERSION.SDK_INT >= 33 && shader != null) {
                    val prefs = getSharedPreferences("sleepsync_design", MODE_PRIVATE)
                    val primary = prefs.getInt("wallpaper_neon_primary",0)
                        .takeIf { it != 0 } ?: 0xFF00F0FF.toInt()
                    val secondary = prefs.getInt("wallpaper_neon_secondary",0)
                        .takeIf { it != 0 } ?: 0xFFAE48FF.toInt()
                    fun rgb(color: Int) = floatArrayOf(
                        Color.red(color)/255f, Color.green(color)/255f, Color.blue(color)/255f)
                    val a=rgb(primary)
                    val b=rgb(secondary)
                    val source=prefs.getString("wallpaper_source","live_aurora_dream")
                        ?.removePrefix("live_") ?: "aurora_dream"
                    val safeSource=WallpaperPremiumGate.safeStyle(this@SleepSyncWallpaperService, source)
                    val scene=sleepSyncLiveStyles.indexOfFirst { it.id==safeSource }.coerceAtLeast(0).toFloat()
                    val speed=prefs.getInt("wallpaper_cinema_animation",55)/100f
                    val elapsed=(android.os.SystemClock.uptimeMillis()-startTime)/1000f
                    shader.setFloatUniform("resolution",width.toFloat(),height.toFloat())
                    shader.setFloatUniform("time",elapsed*.6f)
                    shader.setFloatUniform("colorA",a)
                    shader.setFloatUniform("colorB",b)
                    shader.setFloatUniform("depth",prefs.getInt("wallpaper_cinema_depth",78)/100f)
                    shader.setFloatUniform("energy",prefs.getInt("wallpaper_cinema_intensity",72)/100f)
                    shader.setFloatUniform("motion",.55f+speed)
                    shader.setFloatUniform("lightShare",.45f)
                    shader.setFloatUniform("deepShare",.25f)
                    shader.setFloatUniform("remShare",.30f)
                    shader.setFloatUniform("scene",scene)
                    shader.setFloatUniform("particleDensity",prefs.getInt("wallpaper_cinema_particles",48)/100f)
                    shader.setFloatUniform("rayStrength",prefs.getInt("wallpaper_cinema_rays",62)/100f)
                    if (safeSource == "liquid_chrome" && chromeShader != null) {
                        chromeShader.setFloatUniform("iResolution",width.toFloat(),height.toFloat())
                        chromeShader.setFloatUniform("iTime",elapsed)
                        chromeShader.setFloatUniform("iTouch",touchX,touchY)
                        chromeShader.setFloatUniform("colorA",a)
                        chromeShader.setFloatUniform("colorB",b)
                        chromeShader.setFloatUniform("intensity",prefs.getInt("wallpaper_cinema_intensity",72)/100f)
                        chromeShader.setFloatUniform("depth",prefs.getInt("wallpaper_cinema_depth",78)/100f)
                        chromeShader.setFloatUniform("motion",.5f+speed)
                        paint.shader=chromeShader
                    } else if (safeSource == "hyper_cosmic" && hyperShader != null) {
                        hyperShader.setFloatUniform("iResolution",width.toFloat(),height.toFloat())
                        hyperShader.setFloatUniform("iTime",elapsed*.6f)
                        hyperShader.setFloatUniform("iTouch",touchX,touchY)
                        hyperShader.setFloatUniform("colorCyan",a)
                        hyperShader.setFloatUniform("colorMagenta",b)
                        hyperShader.setFloatUniform("colorCore",.65f,.2f,1f)
                        hyperShader.setFloatUniform("intensity",prefs.getInt("wallpaper_cinema_intensity",72)/100f)
                        hyperShader.setFloatUniform("depth",prefs.getInt("wallpaper_cinema_depth",78)/100f)
                        hyperShader.setFloatUniform("particleDensity",prefs.getInt("wallpaper_cinema_particles",48)/100f)
                        paint.shader=hyperShader
                    } else {
                        paint.shader=shader
                    }
                    c.drawRect(0f,0f,width.toFloat(),height.toFloat(),paint)
                    paint.shader=null
                }
            } catch (_: Exception) {
                // Surface can disappear during launcher transitions.
            } finally {
                if (canvas != null) runCatching { holder.unlockCanvasAndPost(canvas) }
            }
        }
    }
}
