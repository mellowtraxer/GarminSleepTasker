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
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.*

/**
 * Liquid Chrome: analytic flowing metal, specular Fresnel rims, neon environment
 * reflections and touch-driven ripples. Shared with Android WallpaperService.
 */
internal const val LIQUID_CHROME_SHADER = """
uniform float2 iResolution;
uniform float iTime;
uniform float2 iTouch;
uniform float3 colorA;
uniform float3 colorB;
uniform float intensity;
uniform float depth;
uniform float motion;

float field(float2 p, float t) {
    float2 q=p;
    for (int i=1;i<5;i++) {
        float f=float(i);
        q.x+=.19/f*sin(q.y*(2.3+f*.9)+t*(.5+.12*f)+f);
        q.y+=.22/f*cos(q.x*(2.0+f*.7)-t*(.38+.09*f)+f*1.4);
    }
    return q.y+.24*sin(q.x*2.8+t*.44)+.11*sin(q.x*6.3-t*.63);
}
half4 main(float2 xy) {
    float2 uv=(xy-.5*iResolution)/min(iResolution.x,iResolution.y);
    float t=iTime*motion;
    float2 touch=(iTouch-.5)*2.0;
    float2 delta=uv-touch*.40;
    float ripple=sin(length(delta)*18.0-t*4.0)*exp(-length(delta)*5.0)*.12;
    float2 p=uv*2.2+float2(ripple,ripple*.45);
    float f=field(p,t);
    float eps=.006;
    float2 gradient=float2(field(p+float2(eps,0.0),t)-field(p-float2(eps,0.0),t),
        field(p+float2(0.0,eps),t)-field(p-float2(0.0,eps),t))/(2.0*eps);
    float3 normal=normalize(float3(-gradient.x*.55,-gradient.y*.55,1.0));
    float3 lightDir=normalize(float3(-.52,.45,.72));
    float3 halfVector=normalize(lightDir+float3(0.0,0.0,1.0));
    float spec=pow(max(dot(normal,halfVector),0.0),36.0);
    float sharp=pow(max(dot(normal,normalize(float3(.65,-.24,.72))),0.0),120.0);
    float fresnel=pow(1.0-max(normal.z,0.0),2.5);
    float band=exp(-abs(f)*4.0);
    float ribbon=exp(-abs(f)*13.0);
    float flow=.5+.5*sin(p.x*3.0+f*5.0+t*.35);
    float3 environment=mix(colorA,colorB,flow);
    float3 metal=float3(.045,.052,.074)+environment*(.18+.52*band);
    metal+=float3(.82,.88,.99)*spec*(.28+.6*depth);
    metal+=float3(1.0,.95,1.0)*sharp*.95;
    metal+=environment*fresnel*.82;
    metal+=environment*ribbon*.22;
    float fade=1.0-smoothstep(.60,1.48,length(uv));
    float3 result=metal*fade*intensity;
    result=pow(max(result-.016,0.0),float3(1.12));
    return half4(half3(clamp(result,0.0,1.0)),1.0);
}
"""

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun LiquidChromeUniverse(modifier: Modifier = Modifier) {
    val shader = remember { RuntimeShader(LIQUID_CHROME_SHADER) }
    val transition = rememberInfiniteTransition(label = "LiquidChromeTime")
    val time by transition.animateFloat(0f,120f,
        infiniteRepeatable(tween(120000,easing=LinearEasing)),label="ChromePhase")
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
            shader.setFloatUniform("colorA",a.red,a.green,a.blue)
            shader.setFloatUniform("colorB",b.red,b.green,b.blue)
            shader.setFloatUniform("intensity",WallpaperNeonTuning.intensity/100f)
            shader.setFloatUniform("depth",WallpaperNeonTuning.depth/100f)
            shader.setFloatUniform("motion",.5f+WallpaperNeonTuning.animation/100f)
            drawRect(ShaderBrush(shader))
        }
    }
}
