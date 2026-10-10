package de.ricci.garminsleep.ui.components

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.*

/** Shared by Compose preview and Android's live WallpaperService. */
internal const val HYPER_COSMIC_SHADER = """
uniform float2 iResolution;
uniform float iTime;
uniform float2 iTouch;
uniform float3 colorCyan;
uniform float3 colorMagenta;
uniform float3 colorCore;
uniform float intensity;
uniform float depth;
uniform float particleDensity;
float hash21(float2 p) {
    return fract(sin(dot(p,float2(127.1,311.7)))*43758.5453);
}
float cosmicDust(float2 uv, float time) {
    float2 q=uv*float2(64.0,105.0);
    float2 id=floor(q);
    float2 cell=fract(q)-.5;
    float seed=hash21(id);
    float density=step(1.0-particleDensity*.065,seed);
    float radius=dot(cell,cell);
    float twinkle=.65+.35*sin(time*.8+seed*27.0);
    return density*exp(-radius*90.0)*twinkle;
}
half4 main(float2 fragCoord) {
    float2 uv=(fragCoord-.5*iResolution)/min(iResolution.x,iResolution.y);
    uv+=(iTouch-.5)*.15;
    float2 p=uv*2.2;
    float t=iTime*.25;
    for(int i=1;i<5;i++) {
        float f=float(i);
        p.x+=.35/f*sin(f*2.8*p.y+t+.4*f);
        p.y+=.35/f*cos(f*2.8*p.x-t+.4*f);
    }
    float ribbon=abs(p.x*sin(t*.3)+p.y*cos(t*.3));
    float glow=.035/(ribbon*ribbon+.025);
    float coreRibbon=length(p*.6+float2(sin(t*.5),cos(t*.5))*.3);
    float coreGlow=.02/(coreRibbon+.04);
    float mixFactor=sin(p.x+p.y+t)*.5+.5;
    float3 col=mix(colorCyan,colorMagenta,mixFactor)*glow;
    col+=colorCore*coreGlow*1.4;
    float filament=abs(sin(p.y*5.0+p.x*3.0+t*.8));
    col+=mix(colorMagenta,colorCyan,mixFactor)*(.004/(filament+.06))*depth;
    col+=mix(colorCyan,colorCore,.5)*cosmicDust(uv+float2(0.0,t*.015),t)*.68;
    float vignette=1.0-smoothstep(.35,1.5,length(uv))*.9;
    col=pow(max(col*intensity*vignette,0.0),float3(1.25));
    col=max(col-.012,0.0);
    return half4(half3(clamp(col,0.0,1.0)),1.0);
}
"""

/** Interactive shader and procedural stardust, without per-frame mutable particle lists. */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun HyperGlowUniverse(modifier: Modifier = Modifier, content: @Composable () -> Unit = {}) {
    val shader = remember { RuntimeShader(HYPER_COSMIC_SHADER) }
    val transition = rememberInfiniteTransition(label = "HyperCosmicTime")
    val time by transition.animateFloat(0f, 120f,
        infiniteRepeatable(tween(120000, easing = LinearEasing)), label = "CosmicPhase")
    var touch by remember { mutableStateOf(Offset(.5f,.5f)) }
    val palette = WallpaperNeonTuning.palette()
    Box(modifier.fillMaxSize()
        .pointerInput(Unit) {
            detectDragGestures { change, _ ->
                touch=Offset((change.position.x/size.width).coerceIn(0f,1f),
                    (change.position.y/size.height).coerceIn(0f,1f))
                change.consume()
            }
        }
        .pointerInput(Unit) {
            detectTapGestures { pos ->
                touch=Offset((pos.x/size.width).coerceIn(0f,1f),
                    (pos.y/size.height).coerceIn(0f,1f))
            }
        }) {
        Canvas(Modifier.fillMaxSize()) {
            val a=palette[0]
            val b=palette[1]
            shader.setFloatUniform("iResolution",size.width,size.height)
            shader.setFloatUniform("iTime",time)
            shader.setFloatUniform("iTouch",touch.x,touch.y)
            shader.setFloatUniform("colorCyan",a.red,a.green,a.blue)
            shader.setFloatUniform("colorMagenta",b.red,b.green,b.blue)
            shader.setFloatUniform("colorCore",.65f,.2f,1f)
            shader.setFloatUniform("intensity",WallpaperNeonTuning.intensity/100f)
            shader.setFloatUniform("depth",WallpaperNeonTuning.depth/100f)
            shader.setFloatUniform("particleDensity",WallpaperNeonTuning.particles/100f)
            drawRect(ShaderBrush(shader))
        }
        content()
    }
}
