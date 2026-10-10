package de.ricci.garminsleep.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.*

/** Two draggable neon handles, independent from the glass-card color system. */
@Composable
fun NeonWallpaperColorWheel(onChange: (Int, Int) -> Unit) {
    var duo by remember { mutableStateOf(WallpaperNeonTuning.secondary != WallpaperNeonTuning.primary) }
    var active by remember { mutableIntStateOf(0) }
    val primary = WallpaperNeonTuning.primary.takeIf { it != 0 } ?: 0xFF00F0FF.toInt()
    val secondary = WallpaperNeonTuning.secondary.takeIf { it != 0 } ?: 0xFFFF00B8.toInt()
    fun hue(color: Int): Float {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(color, hsv)
        return hsv[0]
    }
    fun setHue(pos: Offset, width: Float, height: Float) {
        val dx = pos.x - width / 2f
        val dy = pos.y - height / 2f
        val h = ((Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat() + 90f + 360f) % 360f)
        val chosen = android.graphics.Color.HSVToColor(floatArrayOf(h, 1f, 1f))
        if (duo && active == 1) {
            WallpaperNeonTuning.secondary = chosen
            onChange(WallpaperNeonTuning.primary, chosen)
        } else {
            WallpaperNeonTuning.primary = chosen
            val second = if (duo) WallpaperNeonTuning.secondary else chosen
            WallpaperNeonTuning.secondary = second
            onChange(chosen, second)
        }
    }
    FrostedGlassCard {
        Text("✦  NEON COLOR LAB", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Spacer(Modifier.height(5.dp))
        Text("Wähle deine eigene Lichtwelt · unabhängig von den Karten",
            color = Color(0xFFB5C5E0), fontSize = 10.sp)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("● EINZELFARBE", color = if (!duo) Color(0xFF5DEBFF) else Color.LightGray,
                fontSize = 11.sp, modifier = Modifier.clickable {
                    duo = false
                    WallpaperNeonTuning.secondary = WallpaperNeonTuning.primary
                    onChange(WallpaperNeonTuning.primary, WallpaperNeonTuning.secondary)
                })
            Text("● DUO-FARBEN", color = if (duo) Color(0xFF5DEBFF) else Color.LightGray,
                fontSize = 11.sp, modifier = Modifier.clickable {
                    duo = true
                    active = 1
                    WallpaperNeonTuning.secondary = secondary
                    onChange(WallpaperNeonTuning.primary, secondary)
                })
        }
        Spacer(Modifier.height(14.dp))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(236.dp)
                .pointerInput(duo, active) {
                    detectDragGestures(
                        onDragStart = { pos ->
                            val c = Offset(size.width / 2f, size.height / 2f)
                            val touch = atan2((pos.y-c.y).toDouble(),(pos.x-c.x).toDouble())
                            fun handleDistance(color: Int): Double {
                                val a = Math.toRadians((hue(color)-90f).toDouble())
                                val hx = c.x + cos(a)*size.width*.36
                                val hy = c.y + sin(a)*size.height*.36
                                return hypot(pos.x-hx,pos.y-hy)
                            }
                            active = if (duo && handleDistance(secondary) < handleDistance(primary)) 1 else 0
                            setHue(pos,size.width.toFloat(),size.height.toFloat())
                        },
                        onDrag = { change, _ ->
                            setHue(change.position,size.width.toFloat(),size.height.toFloat())
                            change.consume()
                        }
                    )
                }
                .pointerInput(duo) {
                    detectTapGestures { pos ->
                        setHue(pos,size.width.toFloat(),size.height.toFloat())
                    }
                }) {
                val c = center
                val r = size.minDimension*.36f
                val stroke = 29.dp.toPx()
                val colors = listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan,
                    Color.Blue, Color.Magenta, Color.Red)
                drawCircle(Brush.sweepGradient(colors, c),r,c,style=Stroke(stroke))
                drawCircle(Color(0xFF080A17),r-stroke*.65f,c)
                drawCircle(Brush.radialGradient(listOf(Color(primary).copy(alpha=.20f),
                    Color(secondary).copy(alpha=.10f),Color.Transparent),center=c,radius=r*.85f),
                    radius=r*.85f,center=c)
                listOf(primary,secondary).take(if(duo) 2 else 1).forEachIndexed { i, color ->
                    val angle = Math.toRadians((hue(color)-90f).toDouble())
                    val p = Offset(c.x+r*cos(angle).toFloat(),c.y+r*sin(angle).toFloat())
                    drawCircle(Color(color).copy(alpha=.24f),24.dp.toPx(),p)
                    drawCircle(Color.White.copy(alpha=.85f),13.dp.toPx(),p)
                    drawCircle(Color(color),9.dp.toPx(),p)
                    if (i==active) drawCircle(Color.White,15.dp.toPx(),p,style=Stroke(2.dp.toPx()))
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("LIVE MIX",color=Color.White,fontWeight=FontWeight.Bold,fontSize=12.sp)
                Spacer(Modifier.height(7.dp))
                Box(Modifier.width(80.dp).height(7.dp).background(
                    Brush.horizontalGradient(listOf(Color(primary),Color(secondary))),CircleShape))
            }
        }
        Text("Finger über den Ring bewegen · im Duo-Modus beide Punkte verschieben",
            color=Color(0xFFB5C5E0),fontSize=10.sp)
    }
}
