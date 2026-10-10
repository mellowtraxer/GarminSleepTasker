package de.ricci.garminsleep.ui.components

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.toArgb

/** GPU-driven atmospheric rendering for Android 13+; fallback remains the Canvas engine. */
private const val CINEMATIC_SHADER = """
uniform float2 resolution;
uniform float time;
uniform float3 colorA;
uniform float3 colorB;
uniform float depth;
uniform float energy;
uniform float motion;
uniform float lightShare;
uniform float deepShare;
uniform float remShare;
uniform float scene;

float hash21(float2 p) {
    return fract(sin(dot(p,float2(127.1,311.7))) * 43758.5453);
}
float noise2(float2 p) {
    float2 i=floor(p), f=fract(p);
    f=f*f*(3.0-2.0*f);
    return mix(mix(hash21(i),hash21(i+float2(1.0,0.0)),f.x),
               mix(hash21(i+float2(0.0,1.0)),hash21(i+float2(1.0,1.0)),f.x),f.y);
}
half4 main(float2 xy) {
    float2 uv=(xy-resolution*.5)/max(resolution.y,1.0);
    float t=time*motion;
    float z=depth;
    float2 p=uv;
    float r=length(p);
    float a=atan(p.y,p.x);
    float3 col=float3(.002,.003,.014);
    float wave=sin(p.x*4.0+t*.8+noise2(p*2.1+t*.07)*2.0)*.17;
    wave+=sin(p.x*8.0-t*.47)*.055;
    float fog=noise2(p*3.0+float2(t*.12,-t*.08));
    fog=pow(fog,2.0);
    float light=0.0;
    if(scene<.5) {
        float d=abs(p.y-wave);
        light=.008/(d+.016)+.20*exp(-d*8.0);
    } else if(scene<1.5) {
        float2 q=float2(r, a+1.7*r-t*.25);
        light=pow(max(0.0,noise2(q*float2(5.0,2.7))),3.0)*1.7
             + .016/(abs(sin(q.y*2.4+q.x*5.0))+.065);
    } else if(scene<2.5) {
        float2 center=p-float2(.04*sin(t*.25),.03*cos(t*.3));
        float moon=1.0-smoothstep(.20,.215,length(center));
        float crater=noise2(center*32.0);
        light=moon*(.55+.38*crater)+.018/(abs(length(center)-.22)+.018);
    } else if(scene<3.5) {
        float d=abs(p.y-wave*.5);
        light=.012/(d+.02)+.35*exp(-d*6.0);
        light+=.10*noise2(p*6.0+float2(0.0,t*.1));
    } else if(scene<4.5) {
        float d=abs(p.y-sin(p.x*6.0+t*.65)*.20);
        light=.012/(d+.02)+.17*exp(-d*6.0);
    } else if(scene<5.5) {
        float2 q=p*5.0;
        float f=abs(fract(q.x+q.y*.45+t*.12)-.5);
        light=.01/(f+.04)*noise2(q*.7);
    } else if(scene<6.5) {
        float spiral=a+5.0*r-t*.22;
        light=.014/(abs(sin(spiral*3.0))+.035)*(1.0-smoothstep(.15,.8,r));
        light+=fog*.48;
    } else if(scene<7.5) {
        float2 grid=floor(p*float2(42.0,14.0));
        float streak=step(.91,hash21(float2(grid.x, floor(grid.y-t*5.0))));
        light=streak*.75*exp(-abs(fract(p.x*42.0)-.5)*15.0);
    } else if(scene<8.5) {
        float ring=abs(length(float2(p.x,p.y*2.2))-.35-.025*sin(t));
        light=.016/(ring+.018)+fog*.2;
    } else if(scene<9.5) {
        float d=abs(p.x-sin(p.y*4.0+t*.42)*.19);
        light=.01/(d+.02)+fog*.35;
    } else if(scene<10.5) {
        float d=abs(p.y-.15*sin(p.x*5.0+t*.4));
        light=.012/(d+.015)+.16*exp(-d*10.0);
    } else {
        float2 grid=floor(p*float2(60.0,60.0));
        float star=step(.973,hash21(grid));
        light=star*.9*(.7+.3*sin(t+grid.x));
    }
    float hue=.5+.5*sin(p.x*3.5+p.y*2.0+t*.16);
    float3 tint=mix(colorA,colorB,hue);
    float scattering=fog*(.12+.30*z)*exp(-r*1.4);
    col+=tint*(light*energy+scattering);
    col+=colorB*.035*exp(-r*r*4.0)*z;
    col*=1.0-.48*smoothstep(.18,1.15,r)*z;
    col*=.88+.12*lightShare+.10*remShare-.05*deepShare;
    return half4(half3(clamp(col,0.0,1.0)),1.0);
}
"""

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun AgslLiveWallpaper(style: String, modifier: Modifier = Modifier) {
    val shader = remember { RuntimeShader(CINEMATIC_SHADER) }
    val transition = rememberInfiniteTransition(label = "AgslTime")
    val phase by transition.animateFloat(
        0f, 120f, infiniteRepeatable(
            tween((120000 * 55 / WallpaperNeonTuning.animation.coerceAtLeast(1)).coerceIn(18000,120000),
                easing = LinearEasing)
        ), label = "AgslPhase"
    )
    val colors = WallpaperNeonTuning.palette()
    val a = colors[0]
    val b = colors[1]
    val scene = sleepSyncLiveStyles.indexOfFirst { it.id == style }.coerceAtLeast(0).toFloat()
    Canvas(modifier.fillMaxSize()) {
        shader.setFloatUniform("resolution", size.width, size.height)
        shader.setFloatUniform("time", phase)
        shader.setFloatUniform("colorA", a.red, a.green, a.blue)
        shader.setFloatUniform("colorB", b.red, b.green, b.blue)
        shader.setFloatUniform("depth", WallpaperNeonTuning.depth / 100f)
        shader.setFloatUniform("energy", WallpaperNeonTuning.intensity / 100f)
        shader.setFloatUniform("motion", .55f + WallpaperNeonTuning.animation / 100f)
        shader.setFloatUniform("lightShare", DreamscapeMotion.lightShare)
        shader.setFloatUniform("deepShare", DreamscapeMotion.deepShare)
        shader.setFloatUniform("remShare", DreamscapeMotion.remShare)
        shader.setFloatUniform("scene", scene)
        drawRect(ShaderBrush(shader))
    }
}
