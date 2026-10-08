package de.ricci.garminsleep

import androidx.activity.ComponentActivity
import android.os.Bundle
import android.content.pm.PackageManager
import android.Manifest
import android.provider.CalendarContract
import androidx.activity.result.contract.ActivityResultContracts
import java.security.MessageDigest
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import android.graphics.Typeface
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.DynamicColors
import android.widget.LinearLayout
import android.widget.EditText
import android.text.InputType
import android.app.AlertDialog
import android.widget.ScrollView
import android.widget.TextView
import android.widget.GridLayout
import android.widget.ProgressBar
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.ColorDrawable
import android.animation.ValueAnimator
import android.view.animation.LinearInterpolator
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.LinearGradient
import android.graphics.Shader
import android.content.res.ColorStateList
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.*
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

private class MotionGlassDrawable(private val context:android.content.Context, private val radius:Float) : android.graphics.drawable.Drawable(), SensorEventListener {
    private val sm=context.getSystemService(android.content.Context.SENSOR_SERVICE) as SensorManager
    private val sensor=sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val p=Paint(Paint.ANTI_ALIAS_FLAG)
    private var x=0f; private var y=0f
    init { sensor?.let { sm.registerListener(this,it,SensorManager.SENSOR_DELAY_GAME) } }
    override fun onAccuracyChanged(sensor:Sensor?,accuracy:Int){}
    override fun onSensorChanged(e:SensorEvent){ if(e.sensor.type!=Sensor.TYPE_ROTATION_VECTOR)return; val r=FloatArray(9); val o=FloatArray(3); SensorManager.getRotationMatrixFromVector(r,e.values); SensorManager.getOrientation(r,o); val nx=(o[2]/.34f).coerceIn(-1f,1f); val ny=(-o[1]/.34f).coerceIn(-1f,1f); x+=(nx-x)*.055f; y+=(ny-y)*.055f; invalidateSelf() }
    override fun draw(c:Canvas){
        val b=bounds; if(b.isEmpty)return
        val d=context.resources.displayMetrics.density; val l=b.left.toFloat(); val t=b.top.toFloat(); val r=b.right.toFloat(); val bot=b.bottom.toFloat(); val w=b.width().toFloat(); val h=b.height().toFloat()
        fun rr(inset:Float,rad:Float)=Path().apply{addRoundRect(l+inset,t+inset,r-inset,bot-inset,rad,rad,Path.Direction.CW)}
        c.save(); c.clipPath(rr(0f,radius)); p.style=Paint.Style.FILL
        p.shader=LinearGradient(l,t,r,bot,intArrayOf(Color.argb(80,6,18,38),Color.argb(35,12,36,60),Color.argb(58,35,14,52),Color.argb(74,5,25,43)),null,Shader.TileMode.CLAMP); c.drawRect(b,p)
        val glowX=l+w*(.5f+x*.48f); val glowY=t+h*(.5f+y*.34f)
        p.shader=android.graphics.RadialGradient(glowX,glowY,maxOf(w*.52f,h*1.45f),intArrayOf(Color.argb(70,255,255,255),Color.argb(18,210,240,255),Color.TRANSPARENT),null,Shader.TileMode.CLAMP); c.drawRect(b,p)
        val left=(70+120*((-x+1)/2)).toInt(); val right=(70+120*((x+1)/2)).toInt(); val top=(60+125*((-y+1)/2)).toInt(); val bottom=(60+125*((y+1)/2)).toInt()
        p.style=Paint.Style.STROKE
        // Rounded glass bead: mostly clear, with nested bevels rather than a neon outline.
        p.strokeWidth=16*d
        p.shader=LinearGradient(0f,t,0f,bot,intArrayOf(Color.argb(105,255,255,255),Color.argb(20,220,240,255),Color.argb(12,120,165,210),Color.argb(82,255,255,255)),floatArrayOf(0f,.30f,.72f,1f),Shader.TileMode.CLAMP)
        c.drawPath(rr(8*d,radius-7*d),p)
        p.strokeWidth=9*d
        p.shader=LinearGradient(0f,t,0f,bot,intArrayOf(Color.argb(175,255,255,255),Color.argb(30,255,255,255),Color.argb(24,80,115,160),Color.argb(120,215,240,255)),floatArrayOf(0f,.22f,.70f,1f),Shader.TileMode.CLAMP)
        c.drawPath(rr(4.8f*d,radius-4*d),p)
        p.strokeWidth=3.2f*d
        p.shader=LinearGradient(0f,t,0f,bot,intArrayOf(Color.argb(220,255,255,255),Color.argb(58,255,255,255),Color.argb(30,130,165,205),Color.argb(155,245,252,255)),null,Shader.TileMode.CLAMP)
        c.drawPath(rr(1.8f*d,radius-1.5f*d),p)
        // Almost invisible inner depth: no black drawn frame.
        p.shader=null; p.strokeWidth=1.4f*d; p.color=Color.argb(25,0,12,28)
        c.drawPath(rr(13*d,(radius-13*d).coerceAtLeast(5*d)),p)
        // Local colour refraction only where the virtual light reaches the bead.
        val cyanX=l+w*(.18f+x*.18f); val magentaX=l+w*(.82f+x*.14f)
        p.strokeWidth=6.5f*d
        p.shader=android.graphics.RadialGradient(cyanX,t+h*(.55f+y*.18f),w*.28f,intArrayOf(Color.argb(205,45,225,255),Color.argb(70,70,210,255),Color.TRANSPARENT),null,Shader.TileMode.CLAMP)
        c.drawPath(rr(5*d,radius-4*d),p)
        p.shader=android.graphics.RadialGradient(magentaX,t+h*(.45f+y*.16f),w*.26f,intArrayOf(Color.argb(190,255,80,220),Color.argb(62,255,135,225),Color.TRANSPARENT),null,Shader.TileMode.CLAMP)
        c.drawPath(rr(5*d,radius-4*d),p)
        // Tight specular hot spot follows the gyro along the curved rim.
        val specX=l+w*(.5f+x*.48f); val specY=t+h*(.5f+y*.43f)
        p.strokeWidth=4.0f*d
        p.shader=android.graphics.RadialGradient(specX,specY,maxOf(w*.18f,h*.62f),intArrayOf(Color.WHITE,Color.argb(215,255,255,255),Color.argb(55,220,245,255),Color.TRANSPARENT),floatArrayOf(0f,.18f,.48f,1f),Shader.TileMode.CLAMP)
        c.drawPath(rr(2.6f*d,radius-2*d),p)
        p.strokeWidth=1.25f*d
        p.shader=android.graphics.RadialGradient(specX,specY,maxOf(w*.12f,h*.42f),intArrayOf(Color.WHITE,Color.argb(210,255,255,255),Color.TRANSPARENT),floatArrayOf(0f,.22f,1f),Shader.TileMode.CLAMP)
        c.drawPath(rr(.8f*d,radius-.8f*d),p)
        p.style=Paint.Style.FILL
        val shineX=l+w*(.5f+x*.42f); val half=w*.20f
        p.shader=LinearGradient(shineX-half,0f,shineX+half,0f,intArrayOf(Color.TRANSPARENT,Color.argb(22,255,255,255),Color.argb(105,255,255,255),Color.argb(18,255,255,255),Color.TRANSPARENT),floatArrayOf(0f,.32f,.5f,.68f,1f),Shader.TileMode.CLAMP); c.drawRect(b,p)
        p.shader=LinearGradient(0f,t,0f,t+22*d,intArrayOf(Color.argb(top.coerceAtMost(180),255,255,255),Color.argb(30,255,255,255),Color.TRANSPARENT),null,Shader.TileMode.CLAMP); c.drawRect(l,t,r,t+22*d,p)
        p.shader=LinearGradient(0f,bot-24*d,0f,bot,intArrayOf(Color.TRANSPARENT,Color.argb(34,255,100,220),Color.argb(bottom.coerceAtMost(180),90,220,255)),null,Shader.TileMode.CLAMP); c.drawRect(l,bot-24*d,r,bot,p)
        p.shader=null; c.restore()
    }
    override fun setAlpha(alpha:Int){}; override fun setColorFilter(cf:android.graphics.ColorFilter?){}; @Suppress("DEPRECATION") override fun getOpacity()=android.graphics.PixelFormat.TRANSLUCENT
}

private class NightLandscapeView(context: android.content.Context) : View(context) {
    private val p=Paint(Paint.ANTI_ALIAS_FLAG)
    override fun onDraw(c:Canvas){
        super.onDraw(c); val w=width.toFloat(); val h=height.toFloat(); val d=resources.displayMetrics.density; val sh=minOf(h,900*d)
        p.shader=android.graphics.LinearGradient(0f,0f,0f,sh,intArrayOf(Color.rgb(0,5,28),Color.rgb(13,18,91),Color.rgb(76,25,135),Color.rgb(13,32,78),Color.rgb(0,12,28)),floatArrayOf(0f,.20f,.36f,.61f,1f),android.graphics.Shader.TileMode.CLAMP); c.drawRect(0f,0f,w,h,p); p.shader=null
        p.shader=android.graphics.RadialGradient(w*.52f,sh*.16f,w*.43f,intArrayOf(Color.argb(220,163,62,255),Color.argb(120,72,42,220),Color.TRANSPARENT),null,android.graphics.Shader.TileMode.CLAMP); c.drawOval(w*.08f,-sh*.03f,w*.96f,sh*.40f,p); p.shader=null
        for(i in 0 until 260){ val x=((i*83)%257)/257f*w; val y=(.015f+((i*59)%173)/173f*.33f)*sh; val big=i%23==0; p.color=Color.argb(if(big)255 else 185,215+(i%3)*13,220+(i%2)*25,255); c.drawCircle(x,y,(if(big)1.75f else .62f+(i%5)*.12f)*d,p) }
        p.style=Paint.Style.STROKE; p.strokeWidth=1.1f*d; p.color=Color.argb(170,182,126,255); c.drawLine(w*.12f,sh*.10f,w*.26f,sh*.15f,p); c.drawLine(w*.26f,sh*.15f,w*.28f,sh*.165f,p); p.style=Paint.Style.FILL
        p.setShadowLayer(42*d,0f,0f,Color.argb(235,202,78,255)); setLayerType(LAYER_TYPE_SOFTWARE,p); p.color=Color.rgb(235,221,255); c.drawCircle(w*.78f,sh*.16f,50*d,p); p.clearShadowLayer(); p.color=Color.rgb(10,16,69); c.drawCircle(w*.815f,sh*.132f,47*d,p)
        fun ridge(color:Int,base:Float,pts:FloatArray){ val q=Path(); q.moveTo(0f,sh*base); pts.forEachIndexed{i,v->q.lineTo(w*i/(pts.size-1),sh*v)}; q.lineTo(w,sh*base); q.close(); p.color=color; c.drawPath(q,p) }
        ridge(Color.rgb(74,59,145),.48f,floatArrayOf(.46f,.40f,.43f,.31f,.42f,.24f,.38f,.20f,.40f,.27f,.43f,.34f,.47f))
        p.style=Paint.Style.STROKE; p.strokeWidth=1.8f*d; p.color=Color.argb(235,219,196,255); c.drawLine(w*.49f,sh*.34f,w*.58f,sh*.20f,p); c.drawLine(w*.58f,sh*.20f,w*.67f,sh*.38f,p); c.drawLine(w*.30f,sh*.41f,w*.38f,sh*.31f,p); c.drawLine(w*.38f,sh*.31f,w*.45f,sh*.42f,p); p.style=Paint.Style.FILL
        ridge(Color.rgb(6,17,48),.55f,floatArrayOf(.54f,.47f,.52f,.41f,.54f,.44f,.55f,.43f,.54f,.46f,.55f))
        for(i in 0 until 42){ val x=i*w/41f; val ht=(18+(i*19)%58)*d; val q=Path(); q.moveTo(x-4*d,sh*.58f); q.lineTo(x+4*d,sh*.58f); q.lineTo(x,sh*.58f-ht); q.close(); p.color=Color.rgb(1,10,28); c.drawPath(q,p) }
        p.shader=android.graphics.LinearGradient(0f,sh*.55f,0f,sh*.83f,intArrayOf(Color.rgb(11,38,91),Color.rgb(20,34,91),Color.rgb(4,20,49)),null,android.graphics.Shader.TileMode.CLAMP); c.drawRect(0f,sh*.55f,w,h,p); p.shader=null
        p.shader=android.graphics.RadialGradient(w*.72f,sh*.61f,w*.38f,intArrayOf(Color.argb(230,255,108,220),Color.argb(130,106,68,255),Color.TRANSPARENT),null,android.graphics.Shader.TileMode.CLAMP); c.drawOval(w*.30f,sh*.55f,w*1.08f,sh*.84f,p); p.shader=null
        for(i in 0 until 20){ val yy=sh*(.585f+i*.012f); val spread=w*(.025f+i*.018f); p.color=Color.argb(170-i*6,255,128,226); c.drawRoundRect(w*.72f-spread,yy,w*.72f+spread,yy+1.8f*d,2*d,2*d,p) }
        p.shader=android.graphics.LinearGradient(0f,sh*.69f,0f,h,intArrayOf(Color.TRANSPARENT,Color.argb(125,0,12,29),Color.rgb(0,11,24)),null,android.graphics.Shader.TileMode.CLAMP); c.drawRect(0f,sh*.68f,w,h,p); p.shader=null
    }
}

private class MetricSparklineView(context:android.content.Context, private val points:List<MetricPoint>, private val tone:Int, private val label:String, private val valueText:TextView):View(context){
    private val p=Paint(Paint.ANTI_ALIAS_FLAG); private var touchX=-1f; private val originalText=valueText.text
    init{setLayerType(LAYER_TYPE_SOFTWARE,null);isClickable=true
        setOnTouchListener{v,e->when(e.actionMasked){
            android.view.MotionEvent.ACTION_DOWN->{parent?.requestDisallowInterceptTouchEvent(true);touchX=e.x.coerceIn(0f,width.toFloat());updateReadout();invalidate();true}
            android.view.MotionEvent.ACTION_MOVE->{touchX=e.x.coerceIn(0f,width.toFloat());updateReadout();invalidate();true}
            android.view.MotionEvent.ACTION_UP->{touchX=e.x.coerceIn(0f,width.toFloat());updateReadout();invalidate();v.performClick();parent?.requestDisallowInterceptTouchEvent(false);true}
            android.view.MotionEvent.ACTION_CANCEL->{touchX=-1f;valueText.text=originalText;invalidate();parent?.requestDisallowInterceptTouchEvent(false);true}
            else->false}}
    }
    override fun performClick():Boolean{super.performClick();return true}
    private fun fmt(v:Double)=when(label){"Puls"->String.format(java.util.Locale.GERMANY,"%.0f bpm",v);"SpO₂"->String.format(java.util.Locale.GERMANY,"%.1f %%",v);"Atmung"->String.format(java.util.Locale.GERMANY,"%.1f /min",v);"HRV"->String.format(java.util.Locale.GERMANY,"%.0f ms",v);else->String.format(java.util.Locale.GERMANY,"%.1f",v)}
    private fun selectedIndex():Int{if(points.size<2||width<=0)return 0;return ((touchX/width)*(points.size-1)).toInt().coerceIn(0,points.lastIndex)}
    private fun updateReadout(){if(points.isEmpty()||width<=0)return;val q=points[selectedIndex()];val tm=java.time.Instant.ofEpochMilli(q.timeMs).atZone(java.time.ZoneId.systemDefault()).format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));valueText.text=fmt(q.value)+"  ·  "+tm}
    override fun onDraw(c:Canvas){super.onDraw(c);if(points.size<2)return;val w=width.toFloat();val h=height.toFloat();val min=points.minOf{it.value};val max=points.maxOf{it.value};val span=(max-min).coerceAtLeast(.01);fun x(i:Int):Float{return i*w/(points.size-1)};fun y(v:Double):Float{return h*.82f-((v-min)/span).toFloat()*h*.58f}
        val path=Path();points.forEachIndexed{i,q->if(i==0)path.moveTo(x(i),y(q.value))else path.lineTo(x(i),y(q.value))}
        val lastX=x(points.lastIndex);val firstX=x(0)
        p.style=Paint.Style.FILL;p.color=Color.argb(42,Color.red(tone),Color.green(tone),Color.blue(tone));val area=Path(path);area.lineTo(lastX,h);area.lineTo(firstX,h);area.close();c.drawPath(area,p)
        p.style=Paint.Style.STROKE;p.strokeWidth=resources.displayMetrics.density*2.2f;p.strokeCap=Paint.Cap.ROUND;p.strokeJoin=Paint.Join.ROUND;p.color=tone;p.setShadowLayer(resources.displayMetrics.density*7f,0f,0f,tone);c.drawPath(path,p);p.clearShadowLayer()
        if(touchX>=0){val idx=selectedIndex();val xx=x(idx);val yy=y(points[idx].value);p.color=Color.argb(150,255,255,255);p.strokeWidth=resources.displayMetrics.density;p.style=Paint.Style.STROKE;c.drawLine(xx,0f,xx,h,p);p.style=Paint.Style.FILL;p.color=Color.WHITE;c.drawCircle(xx,yy,resources.displayMetrics.density*5f,p);p.color=tone;c.drawCircle(xx,yy,resources.displayMetrics.density*2.6f,p)}
    }
}


private class StageNeonView(context:android.content.Context, private val stages:List<StagePoint>, private val target:String, private val tone:Int, private val startMs:Long, private val endMs:Long):View(context){
    private val p=Paint(Paint.ANTI_ALIAS_FLAG)
    init{setLayerType(LAYER_TYPE_SOFTWARE,null)}
    override fun onDraw(c:Canvas){
        super.onDraw(c); if(stages.isEmpty()||endMs<=startMs)return
        val w=width.toFloat(); val h=height.toFloat(); val span=(endMs-startMs).toFloat()
        fun x(t:Long)=((t-startMs)/span*w).coerceIn(0f,w)
        val y=h*.52f; val path=Path(); var drawing=false
        stages.sortedBy{it.startMs}.forEach { s ->
            if(s.stageLabel.equals(target,true)){
                val a=x(s.startMs); val b=x(s.endMs)
                if(!drawing){path.moveTo(a,y);drawing=true}else path.moveTo(a,y)
                path.lineTo(b,y)
            }
        }
        p.style=Paint.Style.STROKE;p.strokeWidth=resources.displayMetrics.density*2.6f;p.strokeCap=Paint.Cap.ROUND;p.color=tone
        p.setShadowLayer(resources.displayMetrics.density*8f,0f,0f,tone);c.drawPath(path,p);p.clearShadowLayer()
        p.style=Paint.Style.FILL;p.color=Color.argb(35,Color.red(tone),Color.green(tone),Color.blue(tone))
        stages.filter{it.stageLabel.equals(target,true)}.forEach{s->c.drawRoundRect(x(s.startMs),y+5f,x(s.endMs),h,5f,5f,p)}
    }
}

private class BottomNavIconView(context: android.content.Context, private val kind:Int) : View(context) {
    private val p=Paint(Paint.ANTI_ALIAS_FLAG).apply { style=Paint.Style.STROKE; strokeWidth=resources.displayMetrics.density*2.15f; strokeCap=Paint.Cap.ROUND; strokeJoin=Paint.Join.ROUND }
    var active=false; set(v){field=v; invalidate()}
    override fun onDraw(c:Canvas){ super.onDraw(c); val d=resources.displayMetrics.density; val cx=width/2f; val cy=height/2f; p.color=Color.WHITE; p.alpha=if(active) 255 else 205; p.style=Paint.Style.STROKE
        when(kind){
            0->{ val q=Path(); q.moveTo(cx-9*d,cy); q.lineTo(cx,cy-8*d); q.lineTo(cx+9*d,cy); q.moveTo(cx-6*d,cy-2*d); q.lineTo(cx-6*d,cy+8*d); q.lineTo(cx+6*d,cy+8*d); q.lineTo(cx+6*d,cy-2*d); c.drawPath(q,p) }
            1->{ c.drawRoundRect(cx-9*d,cy-7*d,cx+9*d,cy+7*d,2*d,2*d,p); c.drawLine(cx-5*d,cy-2*d,cx-1*d,cy-2*d,p); c.drawLine(cx-5*d,cy+3*d,cx+4*d,cy+3*d,p); c.drawLine(cx+4*d,cy-4*d,cx+6*d,cy-4*d,p) }
            2->{ c.drawRoundRect(cx-8*d,cy-7*d,cx+8*d,cy+8*d,2*d,2*d,p); c.drawLine(cx-8*d,cy-2*d,cx+8*d,cy-2*d,p); c.drawLine(cx-4*d,cy-9*d,cx-4*d,cy-5*d,p); c.drawLine(cx+4*d,cy-9*d,cx+4*d,cy-5*d,p); p.style=Paint.Style.FILL; c.drawCircle(cx-3*d,cy+2*d,1.2f*d,p); c.drawCircle(cx+3*d,cy+2*d,1.2f*d,p) }
            else->{ c.drawCircle(cx,cy,3.2f*d,p); for(i in 0 until 8){ val a=Math.PI*2*i/8; c.drawLine(cx+(Math.cos(a)*6*d).toFloat(),cy+(Math.sin(a)*6*d).toFloat(),cx+(Math.cos(a)*9*d).toFloat(),cy+(Math.sin(a)*9*d).toFloat(),p) } }
        }
    }
}

class MainActivity : ComponentActivity(), CoroutineScope by MainScope() {
    private var settingsBlurTarget: eightbitlab.com.blurview.BlurTarget? = null
    private lateinit var status: TextView
    private lateinit var sleepCard: LinearLayout
    private lateinit var pageTitle: TextView
    private lateinit var pageSubtitle: TextView
    private lateinit var contentHost: LinearLayout
    private lateinit var actionsTitle: TextView
    private lateinit var actionsBox: LinearLayout
    private lateinit var brandGlow: View
    private var brandGlowAnimator: ValueAnimator? = null
    private var lastSummary: SleepSummary? = null
    private var sleepHistory: List<SleepSummary> = emptyList()
    private var viewingHistoryNight = false
    private val nightBg = Color.rgb(5, 6, 14)
    private val cardBg = Color.argb(222, 10, 16, 36)
    private fun designPercent(key:String, fallback:Int):Int {
        val p=getSharedPreferences("sleepsync_design",MODE_PRIVATE)
        return if(p.getBoolean("custom_enabled",false)) p.getInt(key,fallback).coerceIn(0,100) else fallback
    }
    private fun designGlassAlpha(defaultAlpha:Int=168):Int {
        // UI value is TRANSPARENCY: 0% = fully opaque, 100% = fully transparent.
        // Keep one global source of truth so every glass surface behaves identically.
        val transparency=designPercent("glass_strength",34)
        return (255f*(1f-transparency/100f)).toInt().coerceIn(0,255)
    }
    private fun designGlassOverlayAlpha(maxAlpha:Int=110):Int =
        (maxAlpha*(designGlassAlpha()/255f)).toInt().coerceIn(0,maxAlpha)
    private fun designNeonAlpha(base:Int):Int=(base*(designPercent("neon_strength",100)/100f)).toInt().coerceIn(0,255)
    private fun designGlowAlpha(base:Int):Int=(base*(designPercent("glow_strength",100)/100f)).toInt().coerceIn(0,255)
    private fun designBlurRadius():Float=(25f*(designPercent("blur_strength",20)/100f)).coerceAtLeast(0f)
    private fun applySleepSyncStandard() {
        getSharedPreferences("sleepsync_design",MODE_PRIVATE).edit()
            .putBoolean("custom_enabled",true).putBoolean("wallpaper_enabled",true)
            .putInt("accent",Color.rgb(139,92,246)).putInt("accent2",Color.rgb(34,211,238))
            .putInt("stage_light",Color.rgb(99,190,255)).putInt("stage_deep",Color.rgb(95,75,220))
            .putInt("stage_rem",Color.rgb(183,99,255)).putInt("stage_awake",Color.rgb(255,164,91))
            .putInt("heart",Color.rgb(255,82,126)).putInt("spo2",Color.rgb(44,205,255))
            .putInt("resp",Color.rgb(80,225,184)).putInt("hrv",Color.rgb(213,96,255))
            .putInt("glass_strength",34).putInt("blur_strength",8).putInt("neon_strength",100).putInt("glow_strength",100).apply()
    }
    private fun designColor(key:String,fallback:Int):Int {
        val p=getSharedPreferences("sleepsync_design",MODE_PRIVATE)
        return if(p.getBoolean("custom_enabled",false)) p.getInt(key,fallback) else fallback
    }
    private val accent get() = designColor("accent",Color.rgb(139,92,246))
    private val accent2 get() = designColor("accent2",Color.rgb(34,211,238))
    private val stageLight get() = designColor("stage_light",Color.rgb(99,190,255))
    private val stageDeep get() = designColor("stage_deep",Color.rgb(95,75,220))
    private val stageRem get() = designColor("stage_rem",Color.rgb(183,99,255))
    private val stageAwake get() = designColor("stage_awake",Color.rgb(255,164,91))
    private val garminClient by lazy { GarminConnectClient(this) }
    private val permissions = setOf(
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(OxygenSaturationRecord::class),
        HealthPermission.getReadPermission(RespiratoryRateRecord::class)
    )
    private val calendarPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { showCalendarPlaceholder() }
    private fun calendarPermissionReady() = checkSelfPermission(Manifest.permission.READ_CALENDAR)==PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.WRITE_CALENDAR)==PackageManager.PERMISSION_GRANTED
    private fun requestCalendarPermission(){ calendarPermissionLauncher.launch(arrayOf(Manifest.permission.READ_CALENDAR,Manifest.permission.WRITE_CALENDAR)) }
    private fun availableCalendars():List<Triple<Long,String,String>> {
        if(!calendarPermissionReady()) return emptyList()
        val out=mutableListOf<Triple<Long,String,String>>()
        contentResolver.query(CalendarContract.Calendars.CONTENT_URI,arrayOf(CalendarContract.Calendars._ID,CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,CalendarContract.Calendars.ACCOUNT_NAME),CalendarContract.Calendars.VISIBLE+"=1",null,CalendarContract.Calendars.CALENDAR_DISPLAY_NAME+" COLLATE NOCASE")?.use{q->while(q.moveToNext())out+=Triple(q.getLong(0),q.getString(1)?:"Kalender",q.getString(2)?:"")}
        return out
    }
    private fun chooseCalendar() {
        if(!calendarPermissionReady()){requestCalendarPermission();return}
        val items=availableCalendars(); if(items.isEmpty()){AlertDialog.Builder(this).setMessage("Android stellt aktuell keinen beschreibbaren Kalender bereit.").setPositiveButton("OK",null).show();return}
        val d=resources.displayMetrics.density; fun dp(v:Int)=(v*d).toInt()
        val theme=getSharedPreferences("sleepsync_ui",MODE_PRIVATE).getString("theme","dark")?:"dark"; val sysDark=(resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK)==android.content.res.Configuration.UI_MODE_NIGHT_YES; val light=theme=="light" || (theme=="system" && !sysDark); val primary=Color.WHITE; val secondary=if(light) Color.rgb(220,228,246) else Color.rgb(165,175,205); val selectedId=calendarPrefs().getLong("calendar_id",-1L)
        val shell=(if(light) eightbitlab.com.blurview.BlurView(this) else android.widget.FrameLayout(this)).apply{
            background=LayerDrawable(arrayOf(
                GradientDrawable().apply{cornerRadius=dp(24).toFloat();setColor(if(light) Color.argb(designGlassAlpha(),72,88,112) else Color.rgb(12,15,35));setStroke(dp(4),Color.argb(42,Color.red(stageRem),Color.green(stageRem),Color.blue(stageRem)))},
                GradientDrawable().apply{cornerRadius=dp(24).toFloat();setColor(Color.TRANSPARENT);setStroke(dp(2),stageRem)}
            ))
            if(light && this is eightbitlab.com.blurview.BlurView){outlineProvider=android.view.ViewOutlineProvider.BACKGROUND;clipToOutline=true;settingsBlurTarget?.let{target->setupWith(target).setBlurRadius(designBlurRadius()).setOverlayColor(Color.argb(designGlassOverlayAlpha(),72,88,112))}}
        }
        val shellContent=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(18),dp(18),dp(18),dp(12));background=null}
        shell.addView(shellContent,android.widget.FrameLayout.LayoutParams(-1,-2))
        shellContent.addView(TextView(this).apply{text="📅  ZIELKALENDER";textSize=18f;setTextColor(stageRem);setTypeface(typeface,Typeface.BOLD);setPadding(0,0,0,dp(4))})
        shellContent.addView(TextView(this).apply{text="Wohin soll SleepSync deine Nächte schreiben?";textSize=12f;setTextColor(secondary);setPadding(0,0,0,dp(12))})
        val list=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}
        val dialog=AlertDialog.Builder(this).setView(ScrollView(this).apply{background=ColorDrawable(Color.TRANSPARENT);addView(shell)}).create()
        items.forEach{item->val selected=item.first==selectedId;list.addView(TextView(this).apply{text=(if(selected) "✓  " else "")+item.second+"\n"+item.third;textSize=14f;setTextColor(primary);setPadding(dp(14),dp(11),dp(14),dp(11));background=GradientDrawable().apply{cornerRadius=dp(13).toFloat();setColor(if(light) (if(selected) Color.argb(designGlassAlpha(),83,62,145) else Color.argb(designGlassAlpha(),72,88,112)) else Color.argb(120,40,29,70));setStroke(if(selected) dp(2) else dp(1),if(selected) stageRem else Color.argb(80,stageRem shr 16 and 255,stageRem shr 8 and 255,stageRem and 255))};layoutParams=LinearLayout.LayoutParams(-1,-2).apply{setMargins(0,0,0,dp(7))};setOnClickListener{getSharedPreferences("sleepsync_calendar",MODE_PRIVATE).edit().putLong("calendar_id",item.first).putString("calendar_name",item.second).putString("calendar_account",item.third).apply();dialog.dismiss();showCalendarPlaceholder()}})}
        shellContent.addView(list);dialog.setOnShowListener{dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT));dialog.window?.decorView?.setLayerType(View.LAYER_TYPE_SOFTWARE,null)};dialog.show()
    }
    private fun calendarPrefs()=getSharedPreferences("sleepsync_calendar",MODE_PRIVATE)
    private fun calendarAutoEnabled()=calendarPrefs().getBoolean("auto_enabled",true)
    private fun calendarEventExists(s:SleepSummary):Boolean {
        if(!calendarPermissionReady())return false
        val id=calendarPrefs().getLong("calendar_id",-1);if(id<0)return false
        val projection=arrayOf(CalendarContract.Events._ID)
        val selection=CalendarContract.Events.CALENDAR_ID+"=? AND "+CalendarContract.Events.DTSTART+"=? AND "+CalendarContract.Events.DTEND+"=? AND "+CalendarContract.Events.TITLE+"=?"
        val args=arrayOf(id.toString(),s.startMs.toString(),s.endMs.toString(),"💤 Garmin Schlaf")
        return contentResolver.query(CalendarContract.Events.CONTENT_URI,projection,selection,args,null)?.use{it.moveToFirst()}==true
    }
    private fun insertNightIntoCalendar(s:SleepSummary):Boolean {
        if(!calendarPermissionReady())return false
        val p=calendarPrefs();val id=p.getLong("calendar_id",-1);if(id<0)return false
        if(calendarEventExists(s))return true
        val zone=ZoneId.systemDefault();val values=android.content.ContentValues().apply{put(CalendarContract.Events.CALENDAR_ID,id);put(CalendarContract.Events.TITLE,"💤 Garmin Schlaf");put(CalendarContract.Events.DTSTART,s.startMs);put(CalendarContract.Events.DTEND,s.endMs);put(CalendarContract.Events.EVENT_TIMEZONE,zone.id);put(CalendarContract.Events.DESCRIPTION,s.calendarText+"\n\nSleepSync")}
        val ok=contentResolver.insert(CalendarContract.Events.CONTENT_URI,values)!=null
        if(ok)p.edit().putLong("last_inserted_end",s.endMs).apply()
        return ok
    }
    private fun syncLatestNightToCalendar(s:SleepSummary){
        if(!calendarAutoEnabled()||!calendarPermissionReady()||calendarPrefs().getLong("calendar_id",-1)<0)return
        runCatching{insertNightIntoCalendar(s)}
    }
    private val permissionLauncher = registerForActivityResult(PermissionController.createRequestPermissionResultContract()) { refresh() }

    private fun scheduleBackgroundSleepSync(){
        val minutes=calendarPrefs().getInt("check_interval_min",30).coerceIn(15,240)
        val request=PeriodicWorkRequestBuilder<SleepSyncWorker>(minutes.toLong(),TimeUnit.MINUTES).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork("sleepsync_background",ExistingPeriodicWorkPolicy.UPDATE,request)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        scheduleBackgroundSleepSync()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val d = resources.displayMetrics.density
        fun dp(v: Int) = (v * d).toInt()
        fun button(label: String, action: () -> Unit) = MaterialButton(this).apply {
            text = label; isAllCaps = false; textSize = 15f; minHeight = dp(56); setOnClickListener { action() }
        }
        status = TextView(this).apply { textSize = 14f; setPadding(dp(12),dp(7),dp(12),dp(7)); gravity=android.view.Gravity.CENTER_VERTICAL }
        sleepCard = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(18),dp(18),dp(18),dp(18)) }
        val bootTheme=getSharedPreferences("sleepsync_ui",MODE_PRIVATE).getString("theme","dark") ?: "dark"
        val bootSystemDark=(resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK)==android.content.res.Configuration.UI_MODE_NIGHT_YES
        val bootLight=bootTheme=="light" || (bootTheme=="system" && !bootSystemDark)
        pageTitle = TextView(this).apply { text = "SleepSync"; textSize = 30f; setTypeface(typeface, Typeface.BOLD); setTextColor(if(bootLight) Color.rgb(24,29,48) else Color.WHITE) }
        pageSubtitle = TextView(this).apply { text = "Dein Schlaf. Klar, automatisch, im Kalender."; textSize = 15f; setTextColor(if(bootLight) Color.rgb(85,94,122) else Color.rgb(184,194,224)); alpha = .82f; setPadding(0,dp(4),0,dp(16)) }
        val sleepShell = MaterialCardView(this).apply {
            radius=0f; cardElevation=0f; strokeWidth=0
            setCardBackgroundColor(Color.TRANSPARENT)
            addView(sleepCard,android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
            ))
            layoutParams=LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        var currentPageIndex=0
        lateinit var swipeOpenPage:(Int)->Unit
        val nav = LinearLayout(this).apply {
            orientation=LinearLayout.HORIZONTAL; gravity=android.view.Gravity.CENTER; setPadding(dp(6),dp(6),dp(6),dp(6))
            val tabs=mutableListOf<MaterialCardView>()
            fun activate(active:MaterialCardView)=tabs.forEachIndexed { index,card ->
                val on=card===active; val tone=Color.rgb(111,82,255)
                card.setCardBackgroundColor(if(on) Color.argb(205,38,65,190) else Color.TRANSPARENT)
                card.strokeWidth=if(on) dp(2) else 0; card.strokeColor=Color.rgb(82,118,255); card.cardElevation=0f
                val box=card.getChildAt(0) as LinearLayout; (box.getChildAt(0) as BottomNavIconView).active=on
                (box.getChildAt(1) as TextView).setTextColor(Color.WHITE)
                (box.getChildAt(1) as TextView).alpha=if(on) 1f else .80f
            }
            fun tab(kind:Int,label:String,action:()->Unit)=MaterialCardView(this@MainActivity).apply {
                radius=dp(12).toFloat(); setCardBackgroundColor(Color.TRANSPARENT)
                layoutParams=LinearLayout.LayoutParams(0,dp(56),1f).apply { setMargins(dp(3),0,dp(3),0) }
                addView(LinearLayout(this@MainActivity).apply { orientation=LinearLayout.VERTICAL; gravity=android.view.Gravity.CENTER
                    addView(BottomNavIconView(this@MainActivity,kind),LinearLayout.LayoutParams(dp(27),dp(27)))
                    addView(TextView(this@MainActivity).apply { text=label; textSize=9f; gravity=android.view.Gravity.CENTER },LinearLayout.LayoutParams(-1,dp(18)))
                }); setOnClickListener { activate(this); action() }; tabs.add(this)
            }
            fun openPage(index:Int, animate:Boolean=true) {
                if(index !in tabs.indices) return
                val oldIndex=tabs.indexOfFirst { it.strokeWidth>0 }
                activate(tabs[index])
                currentPageIndex=index
                val direction=if(oldIndex<0 || index>=oldIndex) 1f else -1f
                val action={
                    when(index) {
                        0 -> showOverview()
                        1 -> showHistoryPlaceholder()
                        2 -> showCalendarPlaceholder()
                        else -> showSettings()
                    }
                }
                if(!animate) { action(); return }
                sleepCard.animate().cancel()
                pageTitle.animate().cancel()
                pageSubtitle.animate().cancel()
                sleepCard.animate().alpha(0f).translationX(-direction*dp(22).toFloat()).setDuration(105).withEndAction {
                    action()
                    sleepCard.translationX=direction*dp(30).toFloat(); sleepCard.alpha=0f
                    sleepCard.animate().alpha(1f).translationX(0f).setDuration(190).setInterpolator(android.view.animation.DecelerateInterpolator()).start()
                }.start()
                pageTitle.alpha=.55f; pageSubtitle.alpha=.55f
                pageTitle.animate().alpha(1f).setDuration(220).start()
                pageSubtitle.animate().alpha(1f).setDuration(220).start()
            }
            val home=tab(0,"Übersicht"){}; addView(home)
            addView(tab(1,"Verlauf"){})
            addView(tab(2,"Kalender"){})
            addView(tab(3,"Einstellungen"){})
            tabs.forEachIndexed { index,card -> card.setOnClickListener { openPage(index) } }
            swipeOpenPage={ index -> openPage(index) }
            post { activate(home) }
        }
        actionsTitle = TextView(this).apply { text="Verbindungen & Automatik"; textSize=18f; setTypeface(typeface, Typeface.BOLD); setPadding(0,dp(22),0,dp(8)) }
        val grant = button("Health Connect · Berechtigungen") { permissionLauncher.launch(permissions) }
        val link = button("Garmin Connect · Verbinden") { showGarminLogin() }
        val unlink = button("Garmin Connect · Trennen") { garminClient.logout(); refresh() }
        val test = button("Schlafdaten neu laden") { testRead() }
        val sig = button("App-Signatur anzeigen") { showAppSignature() }
        val statusCard = MaterialCardView(this).apply {
            radius=dp(18).toFloat()
            cardElevation=0f
            strokeWidth=dp(2)
            strokeColor=Color.rgb(49,216,255)
            setCardBackgroundColor(Color.argb(designGlassAlpha(),72,88,112))
            elevation=dp(6).toFloat()
            outlineAmbientShadowColor=Color.rgb(49,216,255)
            outlineSpotShadowColor=Color.rgb(49,216,255)
            addView(status)
            layoutParams=LinearLayout.LayoutParams(-1,-2).apply { setMargins(0,dp(10),0,dp(12)) }
        }
        actionsBox = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; addView(grant); addView(link); addView(unlink); addView(test); addView(sig) }
        pageTitle.setTextColor(Color.WHITE); pageTitle.textSize=31f; pageTitle.setTypeface(pageTitle.typeface,Typeface.BOLD); pageTitle.letterSpacing=-.025f
        pageTitle.background=null
        pageTitle.setPadding(0,0,0,0)
        pageSubtitle.setTextColor(Color.rgb(235,240,255)); pageSubtitle.textSize=13f; pageSubtitle.alpha=.90f; pageSubtitle.setShadowLayer(0f,0f,0f,Color.TRANSPARENT)
        status.setTextColor(Color.WHITE); status.textSize=10f; status.letterSpacing=.08f; status.setTypeface(status.typeface,Typeface.BOLD)
        actionsTitle.setTextColor(Color.WHITE)
        brandGlow = View(this).apply {
            background=GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,intArrayOf(Color.rgb(78,118,255),Color.rgb(49,216,255),Color.rgb(190,91,255),Color.TRANSPARENT)).apply { cornerRadius=dp(2).toFloat() }
            elevation=0f
            layoutParams=LinearLayout.LayoutParams(dp(138),dp(3)).apply { setMargins(0,dp(7),0,dp(3)) }
        }
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(20),dp(26),dp(20),dp(24))
            addView(pageTitle); addView(pageSubtitle); addView(brandGlow); addView(statusCard); addView(sleepShell); addView(actionsTitle); addView(actionsBox)
        }
        val savedTheme=getSharedPreferences("sleepsync_ui",MODE_PRIVATE).getString("theme","dark") ?: "dark"
        val systemDark=(resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK)==android.content.res.Configuration.UI_MODE_NIGHT_YES
        val useLight=savedTheme=="light" || (savedTheme=="system" && !systemDark)
        val nightAtmosphere = if(useLight) LayerDrawable(arrayOf(
            GradientDrawable(GradientDrawable.Orientation.TL_BR,intArrayOf(Color.rgb(247,250,255),Color.rgb(232,244,255),Color.rgb(242,236,255),Color.rgb(226,247,255))),
            GradientDrawable(GradientDrawable.Orientation.TR_BL,intArrayOf(Color.argb(48,255,184,218),Color.TRANSPARENT,Color.argb(45,86,214,255))),
            GradientDrawable(GradientDrawable.Orientation.BL_TR,intArrayOf(Color.argb(35,56,224,214),Color.TRANSPARENT,Color.argb(34,176,105,255)))
        )) else LayerDrawable(arrayOf(
            GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(Color.rgb(20,10,58), Color.rgb(8,30,68), Color.rgb(5,6,14), Color.rgb(2,3,9))),
            GradientDrawable(GradientDrawable.Orientation.TR_BL, intArrayOf(Color.argb(105,121,64,255), Color.TRANSPARENT, Color.argb(70,0,214,255)))
        ))
        val blurTarget = eightbitlab.com.blurview.BlurTarget(this).apply {
            val designPrefs=getSharedPreferences("sleepsync_design",MODE_PRIVATE)
            val wallpaperOn=designPrefs.getBoolean("wallpaper_enabled",true)
            if(wallpaperOn) addView(android.widget.ImageView(this@MainActivity).apply {
                scaleType=android.widget.ImageView.ScaleType.CENTER_CROP
                adjustViewBounds=false
                setImageResource(if(useLight) R.drawable.sleepsync_day else R.drawable.sleepsync_night)
                alpha=1f
            }, android.widget.FrameLayout.LayoutParams(-1,-1))
            else addView(View(this@MainActivity).apply {
                background=ColorDrawable(if(useLight) Color.rgb(38,45,64) else Color.rgb(5,6,14))
            },android.widget.FrameLayout.LayoutParams(-1,-1))
            addView(View(this@MainActivity).apply {
                background=if(useLight) GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,intArrayOf(Color.argb(34,255,255,255),Color.argb(12,240,247,255),Color.argb(24,225,245,255))) else GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,intArrayOf(Color.TRANSPARENT,Color.argb(35,2,5,15),Color.argb(150,2,4,12)))
            }, android.widget.FrameLayout.LayoutParams(-1,-1))
        }
        settingsBlurTarget=blurTarget
        val scene = android.widget.FrameLayout(this).apply {
            // Wallpaper is owned by the full-screen root below. Keep this scene
            // transparent so the same image remains visible behind content.
            addView(box, android.widget.FrameLayout.LayoutParams(-1,-2))
        }
        val scroll = ScrollView(this).apply {
            isFillViewport=true
            clipToPadding=false
            background=ColorDrawable(Color.TRANSPARENT)
            addView(scene)
            layoutParams = android.widget.FrameLayout.LayoutParams(-1,-1)
        }
        var swipeDownX=0f
        var swipeDownY=0f
        var swipeLastX=0f
        var swipeDownTime=0L
        var swipeTracking=false
        var swipeHorizontal=false
        scroll.setOnTouchListener { _,event ->
            when(event.actionMasked) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    swipeDownX=event.x; swipeDownY=event.y; swipeLastX=event.x; swipeDownTime=event.eventTime
                    swipeTracking=true; swipeHorizontal=false
                    sleepCard.animate().cancel()
                    false
                }
                android.view.MotionEvent.ACTION_MOVE -> {
                    if(!swipeTracking) return@setOnTouchListener false
                    val dx=event.x-swipeDownX
                    val dy=event.y-swipeDownY
                    if(!swipeHorizontal && kotlin.math.abs(dx)>dp(10) && kotlin.math.abs(dx)>kotlin.math.abs(dy)*1.15f) {
                        swipeHorizontal=true
                        scroll.parent?.requestDisallowInterceptTouchEvent(true)
                    }
                    if(swipeHorizontal) {
                        val edgeResistance=(dx>0 && currentPageIndex==0)||(dx<0 && currentPageIndex==3)
                        val drag=if(edgeResistance) dx*.22f else dx*.72f
                        sleepCard.translationX=drag
                        sleepCard.alpha=(1f-(kotlin.math.abs(drag)/(scroll.width.coerceAtLeast(1)*1.8f))).coerceIn(.72f,1f)
                        swipeLastX=event.x
                        true
                    } else false
                }
                android.view.MotionEvent.ACTION_UP -> {
                    if(!swipeTracking) return@setOnTouchListener false
                    swipeTracking=false
                    val dx=event.x-swipeDownX
                    val dy=event.y-swipeDownY
                    val dt=(event.eventTime-swipeDownTime).coerceAtLeast(1L)
                    val velocity=dx*1000f/dt
                    val commit=swipeHorizontal && kotlin.math.abs(dy)<dp(150) &&
                        (kotlin.math.abs(dx)>scroll.width*.20f || kotlin.math.abs(velocity)>720f)
                    if(commit) {
                        val next=if(dx<0) currentPageIndex+1 else currentPageIndex-1
                        if(next in 0..3) {
                            sleepCard.animate().cancel()
                            sleepCard.animate().translationX(if(dx<0) -scroll.width*.16f else scroll.width*.16f).alpha(.55f)
                                .setDuration(90).setInterpolator(android.view.animation.AccelerateInterpolator()).withEndAction {
                                    sleepCard.translationX=0f; sleepCard.alpha=1f; swipeOpenPage(next)
                                }.start()
                        } else {
                            sleepCard.animate().translationX(0f).alpha(1f).setDuration(180).setInterpolator(android.view.animation.DecelerateInterpolator()).start()
                        }
                    } else if(swipeHorizontal) {
                        sleepCard.animate().translationX(0f).alpha(1f).setDuration(180).setInterpolator(android.view.animation.DecelerateInterpolator()).start()
                    }
                    scroll.parent?.requestDisallowInterceptTouchEvent(false)
                    swipeHorizontal
                }
                android.view.MotionEvent.ACTION_CANCEL -> {
                    swipeTracking=false
                    if(swipeHorizontal) sleepCard.animate().translationX(0f).alpha(1f).setDuration(160).setInterpolator(android.view.animation.DecelerateInterpolator()).start()
                    swipeHorizontal=false
                    scroll.parent?.requestDisallowInterceptTouchEvent(false)
                    false
                }
                else -> false
            }
        }
        val navShell = MaterialCardView(this).apply {
            radius=dp(18).toFloat()
            cardElevation=0f
            strokeWidth=dp(2)
            strokeColor=Color.rgb(70,205,225)
            setCardBackgroundColor(if(useLight) Color.argb(designGlassAlpha(190),52,67,94) else Color.argb(230,6,12,25))
            foreground=GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,intArrayOf(Color.argb(22,70,205,225),Color.TRANSPARENT,Color.argb(26,120,170,255))).apply { cornerRadius=dp(18).toFloat() }
            elevation=dp(8).toFloat()
            outlineAmbientShadowColor=Color.rgb(70,205,225)
            outlineSpotShadowColor=Color.rgb(70,205,225)
            addView(nav)
            layoutParams=LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,dp(68)).apply { setMargins(dp(18),dp(4),dp(18),dp(8)) }
        }
        val contentColumn=LinearLayout(this).apply {
            orientation=LinearLayout.VERTICAL
            background=ColorDrawable(Color.TRANSPARENT)
            addView(scroll,LinearLayout.LayoutParams(-1,0,1f))
            addView(navShell)
        }
        val root=android.widget.FrameLayout(this).apply {
            // One continuous wallpaper, including underneath the app nav and
            // Android's transparent system navigation area.
            addView(blurTarget,android.widget.FrameLayout.LayoutParams(-1,-1))
            addView(contentColumn,android.widget.FrameLayout.LayoutParams(-1,-1))
        }
        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            val bars=insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            contentColumn.setPadding(0,bars.top,0,0)
            (navShell.layoutParams as? LinearLayout.LayoutParams)?.let { lp ->
                lp.bottomMargin=bars.bottom+dp(8)
                navShell.layoutParams=lp
            }
            insets
        }
        // Edge-to-edge at the bottom: let SleepSync continue behind Android's
        // navigation controls instead of painting a separate dark system bar.
        window.statusBarColor=Color.TRANSPARENT
        window.navigationBarColor=Color.TRANSPARENT
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window,false)
        androidx.core.view.WindowInsetsControllerCompat(window,window.decorView).apply {
            isAppearanceLightStatusBars=false
            isAppearanceLightNavigationBars=false
        }
        setContentView(root)
        loadCachedHistory()
        refresh()
        testRead()
    }

    private fun refresh() = launch {
        val sdk = HealthConnectClient.getSdkStatus(this@MainActivity)
        if (sdk != HealthConnectClient.SDK_AVAILABLE) { status.text = "Health Connect ist auf diesem Gerät nicht verfügbar."; return@launch }
        val granted = HealthConnectClient.getOrCreate(this@MainActivity).permissionController.getGrantedPermissions()
        val garminReady = garminClient.isLinked()
        val healthReady = granted.containsAll(permissions)
        val gcDot = if (garminReady) "●" else "○"
        val hcDot = if (healthReady) "●" else "○"
        val gcColor = if (garminReady) Color.rgb(49,216,255) else Color.rgb(150,160,180)
        val hcColor = if (healthReady) Color.rgb(86,235,170) else Color.rgb(150,160,180)
        val text = android.text.SpannableString("GARMIN  $gcDot     HEALTH CONNECT  $hcDot")
        val gcStart = text.toString().indexOf(gcDot)
        val hcStart = text.toString().lastIndexOf(hcDot)
        text.setSpan(android.text.style.ForegroundColorSpan(gcColor),gcStart,gcStart+1,android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        text.setSpan(android.text.style.ForegroundColorSpan(hcColor),hcStart,hcStart+1,android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        status.text = text
    }


    private fun showAppSignature() {
        val info = packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
        val cert = info.signingInfo?.apkContentsSigners?.firstOrNull()?.toByteArray()
        val sha = cert?.let { MessageDigest.getInstance("SHA-256").digest(it).joinToString("") { b -> "%02x".format(b) } } ?: "unbekannt"
        val dialog=AlertDialog.Builder(this)
            .setTitle("Installierte App-Signatur")
            .setMessage("Paket: $packageName\nVersion: ${info.longVersionCode}\nSHA-256:\n$sha")
            .setPositiveButton("Schließen", null)
            .create()
        dialog.setOnShowListener { styleSleepSyncDialog(dialog) }
        dialog.show()
    }

    private fun showGarminLogin() {
        val email = EditText(this).apply { hint = "Garmin E-Mail"; inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS }
        val password = EditText(this).apply { hint = "Garmin Passwort"; inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD }
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(48,16,48,0); addView(email); addView(password) }
        AlertDialog.Builder(this)
            .setTitle("Garmin Connect verbinden")
            .setMessage("Das Passwort wird nur für die Anmeldung verwendet und nicht gespeichert. Gespeichert werden ausschließlich Garmin-OAuth-Tokens im privaten App-Speicher.")
            .setView(box)
            .setNegativeButton("Abbrechen", null)
            .setPositiveButton("Verbinden") { _, _ ->
                launch {
                    status.text = "Verbinde mit Garmin Connect…"
                    val result = withContext(Dispatchers.IO) { garminClient.login(email.text.toString().trim(), password.text.toString()) }
                    handleLoginResult(result)
                }
            }.show()
    }

    private fun handleLoginResult(result: GarminLoginResult) {
        when (result) {
            GarminLoginResult.Success -> { status.text = "✅ Garmin Connect verbunden."; refresh() }
            is GarminLoginResult.Error -> status.text = "❌ ${result.message}"
            is GarminLoginResult.MfaRequired -> showMfaDialog(result.method)
        }
    }

    private fun showMfaDialog(method: String) {
        val code = EditText(this).apply { hint = "Bestätigungscode"; inputType = InputType.TYPE_CLASS_NUMBER }
        AlertDialog.Builder(this)
            .setTitle("Garmin Bestätigung")
            .setMessage("Garmin verlangt einen MFA-Code ($method).")
            .setView(code)
            .setNegativeButton("Abbrechen", null)
            .setPositiveButton("Bestätigen") { _, _ ->
                launch {
                    status.text = "Prüfe Garmin-Code…"
                    val result = withContext(Dispatchers.IO) { garminClient.verifyMfa(code.text.toString()) }
                    handleLoginResult(result)
                }
            }.show()
    }

    private fun setLoadingGlow(loading:Boolean) {
        if(!::brandGlow.isInitialized)return
        brandGlowAnimator?.cancel(); brandGlowAnimator=null
        brandGlow.translationX=0f
        if(!loading) {
            brandGlow.background=GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,intArrayOf(Color.rgb(78,118,255),Color.rgb(49,216,255),Color.rgb(190,91,255),Color.TRANSPARENT)).apply { cornerRadius=resources.displayMetrics.density*2f }
            return
        }
        val spectrum=intArrayOf(
            Color.rgb(255,70,120),Color.rgb(255,184,72),Color.rgb(86,235,170),
            Color.rgb(48,211,255),Color.rgb(100,105,255),Color.rgb(205,83,255),
            Color.rgb(255,70,120),Color.rgb(255,184,72),Color.rgb(86,235,170),
            Color.rgb(48,211,255),Color.rgb(100,105,255),Color.rgb(205,83,255),
            Color.rgb(255,70,120)
        )
        val paint=Paint(Paint.ANTI_ALIAS_FLAG)
        brandGlow.background=null
        brandGlowAnimator=ValueAnimator.ofFloat(0f,1f).apply {
            duration=1800L; repeatCount=ValueAnimator.INFINITE; repeatMode=ValueAnimator.RESTART; interpolator=LinearInterpolator()
            addUpdateListener { a ->
                val phase=a.animatedValue as Float
                brandGlow.setTag(phase)
                brandGlow.invalidate()
            }
            start()
        }
        brandGlow.setWillNotDraw(false)
        brandGlow.setLayerType(View.LAYER_TYPE_SOFTWARE,null)
        brandGlow.background=object:android.graphics.drawable.Drawable(){
            override fun draw(canvas:Canvas){
                val w=bounds.width().toFloat().coerceAtLeast(1f); val h=bounds.height().toFloat()
                val phase=(brandGlow.tag as? Float) ?: 0f
                // One complete spectrum cycle exactly equals the translation distance.
                // Because the first and last spectrum colors are identical, phase 1.0
                // renders pixel-for-pixel like phase 0.0: no visible reset or jump.
                val period=w
                paint.shader=LinearGradient(
                    -period+phase*period,0f,
                    phase*period,0f,
                    spectrum,null,Shader.TileMode.REPEAT
                )
                canvas.drawRoundRect(0f,0f,w,h,h/2f,h/2f,paint)
                paint.shader=null
            }
            override fun setAlpha(alpha:Int){paint.alpha=alpha}
            override fun setColorFilter(cf:android.graphics.ColorFilter?){paint.colorFilter=cf}
            @Suppress("DEPRECATION") override fun getOpacity()=android.graphics.PixelFormat.TRANSLUCENT
        }
    }

    private fun testRead() = launch {
        status.text = "GARMIN  ●     HEALTH CONNECT  ●     0 % · Schlafdaten"
        setLoadingGlow(true)
        var shownProgress=0
        val progressJob=launch {
            val steps=listOf(
                8 to "Schlafsessions",
                22 to "Schlafphasen",
                38 to "Herzfrequenz",
                52 to "SpO₂",
                66 to "Atmung",
                78 to "HRV",
                90 to "Garmin-Nachtwerte"
            )
            for((target,label) in steps) {
                while(shownProgress<target) {
                    shownProgress++
                    status.text="GARMIN  ●     HEALTH CONNECT  ●     "+shownProgress+" % · "+label
                    kotlinx.coroutines.delay(32)
                }
                kotlinx.coroutines.delay(90)
            }
        }
        try {
            val history = withContext(Dispatchers.IO) { SleepReader(this@MainActivity).garminHistory() }
            sleepHistory = mergeHistory(sleepHistory, history)
            saveCachedHistory(sleepHistory)
            val s = history.maxByOrNull { it.endMs } ?: error("Keine Garmin-Schlafsession gefunden")
            renderDashboard(s)
            withContext(Dispatchers.IO) { syncLatestNightToCalendar(s) }
            progressJob.cancel()
            while(shownProgress<100) {
                shownProgress++
                status.text="GARMIN  ●     HEALTH CONNECT  ●     "+shownProgress+" % · "+(if(shownProgress<96) "Abschließen" else "Fertig")
                kotlinx.coroutines.delay(28)
            }
            kotlinx.coroutines.delay(350)
            refresh()
        } catch (t: Throwable) {
            sleepCard.removeAllViews()
            sleepCard.addView(TextView(this@MainActivity).apply { text = "⚠️ Schlafdaten konnten nicht geladen werden\n${t.message.orEmpty()}"; textSize = 16f })
        } finally {
            progressJob.cancel()
            setLoadingGlow(false)
        }
    }

    private fun showOverview() {
        actionsTitle.visibility = View.GONE
        actionsBox.visibility = View.GONE
        pageTitle.text = "SleepSync"
        pageSubtitle.text = "Guten Morgen  ·  Deine letzte Nacht"
        lastSummary?.let { renderDashboard(it) }
    }

    private fun historyLoadingView(): View = LinearLayout(this).apply {
        orientation=LinearLayout.HORIZONTAL; gravity=android.view.Gravity.CENTER_VERTICAL; setPadding(18,18,18,18)
        val spin=ProgressBar(this@MainActivity).apply { isIndeterminate=true }
        addView(spin,LinearLayout.LayoutParams(42,42).apply{marginEnd=18})
        addView(TextView(this@MainActivity).apply { text="Nächte werden synchronisiert …"; textSize=13f; setTextColor(Color.rgb(170,205,230)) })
    }

    private fun showHistoryPlaceholder() {
        val d=resources.displayMetrics.density; fun dp(v:Int)=(v*d).toInt()
        val theme=getSharedPreferences("sleepsync_ui",MODE_PRIVATE).getString("theme","dark")?:"dark"
        val sysDark=(resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK)==android.content.res.Configuration.UI_MODE_NIGHT_YES
        val light=theme=="light" || (theme=="system" && !sysDark)
        val primary=Color.WHITE
        val secondary=if(light) Color.rgb(225,232,248) else Color.rgb(160,205,235)
        val muted=if(light) Color.rgb(215,225,245) else Color.rgb(135,150,180)
        actionsTitle.visibility=View.GONE; actionsBox.visibility=View.GONE
        viewingHistoryNight=false
        pageTitle.text="Verlauf"; pageSubtitle.text="Deine Nächte · nach Kalenderwochen"
        sleepCard.removeAllViews(); sleepCard.background=null
        val tf=DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())
        val dateFmt=DateTimeFormatter.ofPattern("EEE, d. MMM",java.util.Locale.GERMAN).withZone(ZoneId.systemDefault())
        val weekFields=java.time.temporal.WeekFields.ISO
        val nights=(if(sleepHistory.isNotEmpty()) sleepHistory else listOfNotNull(lastSummary)).sortedByDescending { it.endMs }
        if(nights.isEmpty()){ sleepCard.addView(historyLoadingView()); return }
        val grouped=nights.groupBy { s -> val z=Instant.ofEpochMilli(s.endMs).atZone(ZoneId.systemDefault()).toLocalDate(); (z.get(weekFields.weekBasedYear())*100)+z.get(weekFields.weekOfWeekBasedYear()) }
        grouped.toSortedMap(compareByDescending<Int>{it}).forEach { (key,items) ->
            val year=key/100; val kw=key%100; val avg=items.map{it.totalMin}.average().toLong()
            val tone=if(kw%2==0) accent2 else stageRem
            val shell=MaterialCardView(this).apply{
                radius=dp(22).toFloat();strokeWidth=dp(2);strokeColor=tone;cardElevation=dp(2).toFloat()
                setCardBackgroundColor(if(light) Color.argb(designGlassAlpha(),72,88,112) else Color.argb(225,12,18,40))
                if(light){elevation=dp(6).toFloat();outlineAmbientShadowColor=tone;outlineSpotShadowColor=tone}
                layoutParams=LinearLayout.LayoutParams(-1,-2).apply{setMargins(dp(4),dp(9),dp(4),dp(9))}
            }
            val box=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; layoutParams=android.widget.FrameLayout.LayoutParams(-1,-2) }
            val rows=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; visibility=View.GONE }
            val head=LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; gravity=android.view.Gravity.CENTER_VERTICAL; setPadding(dp(15),dp(13),dp(15),dp(13))
                val title=TextView(this@MainActivity).apply { text="KW "+kw+" · "+year; textSize=16f; setTextColor(primary); setTypeface(typeface,Typeface.BOLD) }
                addView(title,LinearLayout.LayoutParams(0,-2,1f))
                addView(TextView(this@MainActivity).apply { text="Ø "+(avg/60)+" h "+(avg%60)+" min  ·  "+items.size+" Nächte"; textSize=11f; setTextColor(secondary) })
                addView(TextView(this@MainActivity).apply { text="  ▾"; textSize=18f; setTextColor(accent2) })
                setOnClickListener { rows.visibility=if(rows.visibility==View.VISIBLE) View.GONE else View.VISIBLE }
            }
            items.sortedByDescending{it.endMs}.forEach { s ->
                rows.addView(LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(dp(15),dp(9),dp(15),dp(11)); background=GradientDrawable().apply{setColor(if(light) Color.argb(designGlassAlpha(),38,49,72) else Color.argb(70,25,32,58));setStroke(dp(1),if(light) Color.argb(42,210,225,250) else Color.TRANSPARENT)}; isClickable=true; isFocusable=true; setOnClickListener { showHistoryNight(s) }
                    addView(LinearLayout(this@MainActivity).apply { orientation=LinearLayout.HORIZONTAL
                        addView(TextView(this@MainActivity).apply { text=dateFmt.format(Instant.ofEpochMilli(s.endMs)); textSize=13f; setTextColor(if(light) Color.rgb(242,246,255) else Color.rgb(220,225,245)); layoutParams=LinearLayout.LayoutParams(0,-2,1f) })
                        addView(TextView(this@MainActivity).apply { text=(s.totalMin/60).toString()+" h "+(s.totalMin%60).toString()+" min"; textSize=14f; setTextColor(primary); setTypeface(typeface,Typeface.BOLD) })
                    })
                    addView(TextView(this@MainActivity).apply { text=tf.format(Instant.ofEpochMilli(s.startMs))+" – "+tf.format(Instant.ofEpochMilli(s.endMs)); textSize=10f; setTextColor(muted); setPadding(0,dp(2),0,dp(7)) })
                    addView(LinearLayout(this@MainActivity).apply { orientation=LinearLayout.HORIZONTAL
                        listOf(s.lightMin to stageLight,s.deepMin to stageDeep,s.remMin to stageRem,s.awakeMin to stageAwake).filter{it.first>0}.forEach { pair -> val bar=View(this@MainActivity).apply { background=GradientDrawable().apply{cornerRadius=dp(4).toFloat();setColor(pair.second)} }; addView(bar,LinearLayout.LayoutParams(0,dp(8),pair.first.toFloat()).apply{setMargins(0,0,dp(2),0)}) }
                    })
                })
            }
            box.addView(head); box.addView(rows); shell.addView(box); sleepCard.addView(shell)
        }
    }
    private fun showHistoryNight(s: SleepSummary) {
        viewingHistoryNight=true
        pageTitle.text="←  Nacht"
        pageTitle.setOnClickListener { showHistoryPlaceholder() }
        pageSubtitle.text=java.time.format.DateTimeFormatter.ofPattern("EEEE, d. MMMM yyyy",java.util.Locale.GERMAN).withZone(java.time.ZoneId.systemDefault()).format(java.time.Instant.ofEpochMilli(s.endMs))
        renderDashboard(s)
    }

    override fun onBackPressed() {
        if(viewingHistoryNight) showHistoryPlaceholder() else super.onBackPressed()
    }

    private fun metricPointsToJson(points:List<MetricPoint>)=JSONArray().apply { points.forEach { put(JSONArray().put(it.timeMs).put(it.value)) } }
    private fun stagePointsToJson(points:List<StagePoint>)=JSONArray().apply { points.forEach { put(JSONArray().put(it.startMs).put(it.endMs).put(it.stageLabel)) } }
    private fun metricPointsFromJson(a:JSONArray?):List<MetricPoint> = if(a==null) emptyList() else (0 until a.length()).mapNotNull { i -> runCatching { val p=a.getJSONArray(i); MetricPoint(p.getLong(0),p.getDouble(1)) }.getOrNull() }
    private fun stagePointsFromJson(a:JSONArray?):List<StagePoint> = if(a==null) emptyList() else (0 until a.length()).mapNotNull { i -> runCatching { val p=a.getJSONArray(i); StagePoint(p.getLong(0),p.getLong(1),p.getString(2)) }.getOrNull() }

    private fun mergeHistory(existing:List<SleepSummary>, fresh:List<SleepSummary>):List<SleepSummary> =
        (existing + fresh).groupBy { it.startMs to it.endMs }.values.map { group ->
            group.maxByOrNull { it.heartRateSeries.size + it.spo2Series.size + it.respirationSeries.size + it.hrvSeries.size + it.stageSeries.size } ?: group.first()
        }.sortedByDescending { it.endMs }

    private fun saveCachedHistory(items:List<SleepSummary>) {
        val arr=JSONArray()
        items.forEach { s ->
            arr.put(JSONObject()
                .put("start",s.startMs).put("end",s.endMs).put("total",s.totalMin)
                .put("light",s.lightMin).put("deep",s.deepMin).put("rem",s.remMin).put("awake",s.awakeMin).put("sleeping",s.sleepingMin)
                .put("hr",s.avgHr).put("spo2",s.avgSpo2).put("resp",s.avgResp).put("minSpo2",s.minSpo2).put("minResp",s.minResp).put("hrv",s.avgHrv)
                .put("source",s.source).put("calendarText",s.calendarText)
                .put("heartSeries",metricPointsToJson(s.heartRateSeries))
                .put("spo2Series",metricPointsToJson(s.spo2Series))
                .put("respSeries",metricPointsToJson(s.respirationSeries))
                .put("hrvSeries",metricPointsToJson(s.hrvSeries))
                .put("stageSeries",stagePointsToJson(s.stageSeries)))
        }
        getSharedPreferences("sleepsync_history",MODE_PRIVATE).edit().putString("nights",arr.toString()).apply()
    }

    private fun loadCachedHistory() {
        val raw=getSharedPreferences("sleepsync_history",MODE_PRIVATE).getString("nights",null) ?: return
        sleepHistory=runCatching {
            val a=JSONArray(raw)
            (0 until a.length()).map { i ->
                val o=a.getJSONObject(i)
                SleepSummary(
                    o.getLong("start"),o.getLong("end"),o.getLong("total"),o.getLong("light"),o.getLong("deep"),o.getLong("rem"),o.getLong("awake"),o.getLong("sleeping"),
                    o.optDouble("hr").takeUnless{it.isNaN()},o.optDouble("spo2").takeUnless{it.isNaN()},o.optDouble("resp").takeUnless{it.isNaN()},
                    o.optDouble("minSpo2").takeUnless{it.isNaN()},o.optDouble("minResp").takeUnless{it.isNaN()},o.optDouble("hrv").takeUnless{it.isNaN()},
                    o.optString("source","cache"),o.optString("calendarText",""),
                    metricPointsFromJson(o.optJSONArray("heartSeries")),metricPointsFromJson(o.optJSONArray("spo2Series")),
                    metricPointsFromJson(o.optJSONArray("respSeries")),metricPointsFromJson(o.optJSONArray("hrvSeries")),
                    stagePointsFromJson(o.optJSONArray("stageSeries"))
                )
            }
        }.getOrDefault(emptyList())
        lastSummary=sleepHistory.maxByOrNull{it.endMs}
        lastSummary?.let { renderDashboard(it) }
    }

    private fun showCalendarPlaceholder() {
        val d=resources.displayMetrics.density; fun dp(v:Int)=(v*d).toInt()
        val theme=getSharedPreferences("sleepsync_ui",MODE_PRIVATE).getString("theme","dark")?:"dark"
        val sysDark=(resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK)==android.content.res.Configuration.UI_MODE_NIGHT_YES
        val light=theme=="light" || (theme=="system" && !sysDark)
        val primary=Color.WHITE
        val secondary=if(light) Color.rgb(225,232,248) else Color.rgb(150,165,195)
        val muted=if(light) Color.rgb(215,225,245) else Color.rgb(165,175,205)
        val glass=if(light) Color.argb(designGlassAlpha(),72,88,112) else Color.argb(225,12,18,40)
        actionsTitle.visibility=View.GONE; actionsBox.visibility=View.GONE
        pageTitle.text="Kalender"; pageSubtitle.text="Deine Nächte · automatisch dort, wo du sie willst"
        sleepCard.removeAllViews(); sleepCard.background=null
        fun card(title:String,sub:String,tone:Int,body:LinearLayout.()->Unit):View {
            val fill=if(light) Color.argb(designGlassAlpha(),72,88,112) else Color.argb(225,12,18,40)
            val host=android.widget.FrameLayout(this).apply {
                clipChildren=false;clipToPadding=false
                layoutParams=LinearLayout.LayoutParams(-1,-2).apply{setMargins(dp(4),dp(9),dp(4),dp(9))}
            }
            if(light) host.addView(object:View(this){
                private val p=Paint(Paint.ANTI_ALIAS_FLAG).apply{style=Paint.Style.STROKE;strokeJoin=Paint.Join.ROUND}
                init{setLayerType(View.LAYER_TYPE_SOFTWARE,null)}
                override fun onDraw(c:Canvas){
                    val q=dp(1).toFloat();p.strokeWidth=dp(3).toFloat();p.color=Color.argb(210,Color.red(tone),Color.green(tone),Color.blue(tone))
                    p.maskFilter=android.graphics.BlurMaskFilter(dp(14).toFloat(),android.graphics.BlurMaskFilter.Blur.OUTER)
                    c.drawRoundRect(q,q,width-q,height-q,dp(22).toFloat(),dp(22).toFloat(),p)
                    p.maskFilter=null;p.strokeWidth=dp(2).toFloat();p.color=tone
                    c.drawRoundRect(q,q,width-q,height-q,dp(22).toFloat(),dp(22).toFloat(),p)
                }
            },android.widget.FrameLayout.LayoutParams(-1,-1))
            val glassCard=(if(light) eightbitlab.com.blurview.BlurView(this) else android.widget.FrameLayout(this)).apply{
                background=if(light) LayerDrawable(arrayOf(
                    GradientDrawable().apply{cornerRadius=dp(22).toFloat();setColor(fill);setStroke(dp(4),Color.argb(42,Color.red(tone),Color.green(tone),Color.blue(tone)))},
                    GradientDrawable().apply{cornerRadius=dp(22).toFloat();setColor(Color.TRANSPARENT);setStroke(dp(2),tone)}
                )) else GradientDrawable().apply{cornerRadius=dp(22).toFloat();setColor(fill);setStroke(dp(1),tone)}
                layoutParams=android.widget.FrameLayout.LayoutParams(-1,-2)
                if(light && this is eightbitlab.com.blurview.BlurView){
                    outlineProvider=android.view.ViewOutlineProvider.BACKGROUND;clipToOutline=true
                    settingsBlurTarget?.let{target->setupWith(target).setBlurRadius(designBlurRadius()).setOverlayColor(Color.argb(designGlassOverlayAlpha(),72,88,112))}
                }
                addView(LinearLayout(this@MainActivity).apply{
                    orientation=LinearLayout.VERTICAL;setPadding(dp(18),dp(16),dp(18),dp(16));background=null
                    addView(TextView(this@MainActivity).apply{text=title;textSize=12f;letterSpacing=.08f;setTextColor(tone);setTypeface(typeface,Typeface.BOLD)})
                    addView(TextView(this@MainActivity).apply{text=sub;textSize=11f;setTextColor(if(light) Color.argb(215,245,248,255) else secondary);setPadding(0,dp(3),0,dp(12))})
                    body()
                })
            }
            host.addView(glassCard)
            return host
        }
        val s=lastSummary ?: sleepHistory.maxByOrNull{it.endMs}
        sleepCard.addView(card("✦  NÄCHSTER KALENDEREINTRAG","Vorschau deiner synchronisierten Nacht",accent2){
            if(s!=null){val tf=DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault());val fmt={m:Long->(m/60).toString()+" h "+(m%60).toString()+" min"}
                addView(TextView(this@MainActivity).apply{text="🌙  "+fmt(s.totalMin)+"     "+tf.format(Instant.ofEpochMilli(s.startMs))+" – "+tf.format(Instant.ofEpochMilli(s.endMs));textSize=22f;setTextColor(primary);setTypeface(typeface,Typeface.BOLD)})
                addView(LinearLayout(this@MainActivity).apply{orientation=LinearLayout.HORIZONTAL;setPadding(0,dp(13),0,dp(8));listOf(s.lightMin to stageLight,s.deepMin to stageDeep,s.remMin to stageRem,s.awakeMin to stageAwake).filter{it.first>0}.forEach{q->addView(View(this@MainActivity).apply{background=GradientDrawable().apply{cornerRadius=dp(5).toFloat();setColor(q.second)}},LinearLayout.LayoutParams(0,dp(9),q.first.toFloat()).apply{setMargins(0,0,dp(2),0)})}})
                addView(TextView(this@MainActivity).apply{text="Leicht "+fmt(s.lightMin)+"  ·  Tief "+fmt(s.deepMin)+"  ·  REM "+fmt(s.remMin)+"  ·  Wach "+fmt(s.awakeMin);textSize=11f;setTextColor(if(light) Color.rgb(235,240,252) else Color.rgb(190,200,225))})
            } else addView(TextView(this@MainActivity).apply{text="Noch keine Nacht synchronisiert";setTextColor(primary)})
        })
        sleepCard.addView(card("⚡  AUTOMATIK","Neue Nächte selbstständig eintragen",Color.rgb(74,224,181)){
            addView(LinearLayout(this@MainActivity).apply{gravity=android.view.Gravity.CENTER_VERTICAL
                addView(TextView(this@MainActivity).apply{text="Automatisch eintragen\n"+if(calendarAutoEnabled()) "Aktiv" else "Aus";textSize=14f;setTextColor(primary);setTypeface(typeface,Typeface.BOLD);layoutParams=LinearLayout.LayoutParams(0,-2,1f)})
                addView(android.widget.Switch(this@MainActivity).apply{isChecked=calendarAutoEnabled();setOnCheckedChangeListener{_,checked->calendarPrefs().edit().putBoolean("auto_enabled",checked).apply();showCalendarPlaceholder()}})
            })
        })
        sleepCard.addView(MaterialButton(this).apply{
            text="⚡  JETZT EINTRAGEN";isAllCaps=false;textSize=15f;setTypeface(typeface,Typeface.BOLD)
            setTextColor(Color.WHITE)
            backgroundTintList=ColorStateList.valueOf(if(light) Color.argb(designGlassAlpha(),46,62,150) else Color.rgb(64,63,205))
            strokeWidth=dp(2)
            strokeColor=ColorStateList.valueOf(if(light) Color.rgb(95,125,255) else Color.TRANSPARENT)
            cornerRadius=dp(18);layoutParams=LinearLayout.LayoutParams(-1,dp(58)).apply{setMargins(0,0,0,dp(12))}
            setOnClickListener{
                val latest=lastSummary ?: sleepHistory.maxByOrNull{it.endMs}
                when {
                    latest==null -> { text="⚠  Keine Nacht vorhanden" }
                    !calendarPermissionReady() -> requestCalendarPermission()
                    calendarPrefs().getLong("calendar_id",-1)<0 -> chooseCalendar()
                    calendarEventExists(latest) -> { text="✓  BEREITS EINGETRAGEN";setTextColor(Color.rgb(120,245,190)) }
                    insertNightIntoCalendar(latest) -> { text="✓  EINGETRAGEN";setTextColor(Color.rgb(120,245,190)) }
                    else -> text="⚠  Eintrag fehlgeschlagen"
                }
            }
        })
        sleepCard.addView(card("📅  ZIELKALENDER","Wähle einen Kalender auf diesem Gerät",stageRem){
            val cp=getSharedPreferences("sleepsync_calendar",MODE_PRIVATE); val selected=cp.getString("calendar_name",null); addView(TextView(this@MainActivity).apply{text=(selected ?: if(calendarPermissionReady()) "Kalender auswählen" else "Kalenderzugriff erlauben")+"  ›";textSize=16f;setTextColor(primary);setTypeface(typeface,Typeface.BOLD);setPadding(dp(12),dp(12),dp(12),dp(12));background=GradientDrawable().apply{cornerRadius=dp(15).toFloat();setColor(if(light) Color.argb(designGlassAlpha(),38,46,92) else Color.argb(150,45,29,73));setStroke(dp(1),if(light) stageRem else Color.TRANSPARENT)};isClickable=true;setOnClickListener{chooseCalendar()}})
            addView(TextView(this@MainActivity).apply{text="Google · Outlook · Exchange und weitere Android-Kalender können hier später ausgewählt werden.";textSize=11f;setTextColor(muted);setPadding(0,dp(10),0,0)})
        })
        sleepCard.addView(card("◷  LETZTE EINTRÄGE","Zuletzt synchronisierte Nächte",stageLight){
            val recent=sleepHistory.sortedByDescending{it.endMs}.take(4)
            if(recent.isEmpty()) addView(TextView(this@MainActivity).apply{text="Noch keine Einträge";setTextColor(if(light) Color.rgb(92,104,132) else Color.rgb(180,190,215))})
            recent.forEach{s0->val df=DateTimeFormatter.ofPattern("EEE, dd.MM.",java.util.Locale.GERMAN).withZone(ZoneId.systemDefault());addView(TextView(this@MainActivity).apply{text=df.format(Instant.ofEpochMilli(s0.endMs))+"     "+(s0.totalMin/60)+" h "+(s0.totalMin%60)+" min     ✓";textSize=14f;setTextColor(primary);setPadding(dp(4),dp(10),dp(4),dp(10))})}
        })
        val bgPrefs=calendarPrefs()
        val lastCheck=bgPrefs.getLong("last_background_check",0L)
        val lastAuto=bgPrefs.getLong("last_auto_insert_at",0L)
        val statusFmt=DateTimeFormatter.ofPattern("dd.MM. · HH:mm").withZone(ZoneId.systemDefault())
        val statusText=buildString{
            append(if(calendarAutoEnabled()) "●  Automatik aktiv" else "○  Automatik aus")
            append("\nLetzte Hintergrundprüfung: ")
            append(if(lastCheck>0) statusFmt.format(Instant.ofEpochMilli(lastCheck))+" Uhr" else "noch keine")
            append("\nLetzter automatischer Eintrag: ")
            append(if(lastAuto>0) statusFmt.format(Instant.ofEpochMilli(lastAuto))+" Uhr" else "noch keiner")
        }
        sleepCard.addView(card("●  STATUS","Kalender-Automatik",if(calendarAutoEnabled()) Color.rgb(74,224,181) else muted){
            addView(TextView(this@MainActivity).apply{text=statusText;textSize=11f;setTextColor(if(light) Color.rgb(235,240,252) else muted);setPadding(dp(2),0,dp(2),0)})
        })
    }
    private fun showSettings() {
        val d = resources.displayMetrics.density
        fun dp(v: Int) = (v * d).toInt()
        actionsTitle.visibility = View.VISIBLE
        actionsBox.visibility = View.VISIBLE
        pageTitle.text = "Einstellungen"
        pageSubtitle.text = "Verbindungen, Automatik & Darstellung"
        sleepCard.removeAllViews()
        sleepCard.addView(TextView(this).apply {
            text = "SYSTEM"; textSize = 12f; setTextColor(accent2); setTypeface(typeface, Typeface.BOLD); letterSpacing = .16f
        })
        val settingsGrid = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(0,dp(8),0,0); clipChildren=false; clipToPadding=false }
        sleepCard.clipChildren=false
        sleepCard.clipToPadding=false
        val prefs=getSharedPreferences("sleepsync_ui",MODE_PRIVATE)
        val selectedTheme=prefs.getString("theme","dark") ?: "dark"
        val selectedThemeLabel=when(selectedTheme) { "light"->"Neon Sunrise"; "system"->"Automatisch"; else->"OLED Night" }
        val sysDark=(resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK)==android.content.res.Configuration.UI_MODE_NIGHT_YES
        val settingsLight=selectedTheme=="light" || (selectedTheme=="system" && !sysDark)
        fun setting(icon:String, title:String, sub:String, color:Int, onClick:(() -> Unit)?=null) {
            val fill = if(settingsLight) Color.argb(designGlassAlpha(),72,88,112) else Color.rgb((Color.red(color)*0.14f).toInt()+8,(Color.green(color)*0.14f).toInt()+8,(Color.blue(color)*0.14f).toInt()+12)
            val glowSpace=if(settingsLight) dp(14) else 0
            val host=android.widget.FrameLayout(this).apply {
                clipChildren=false
                clipToPadding=false
                layoutParams=LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT).apply{
                    setMargins(dp(4),dp(9),dp(4),dp(9))
                }
            }
            if(settingsLight) {
                host.addView(object:View(this) {
                    private val glowPaint=Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        style=Paint.Style.STROKE
                        strokeWidth=dp(2).toFloat()
                        strokeJoin=Paint.Join.ROUND
                    }
                    init { setLayerType(View.LAYER_TYPE_SOFTWARE,null) }
                    override fun onDraw(canvas:Canvas) {
                        super.onDraw(canvas)
                        val inset=dp(1).toFloat()
                        // Real neon aura: blur the alpha mask OUTSIDE the card contour.
                        // This view is software-rendered so BlurMaskFilter is applied reliably.
                        glowPaint.clearShadowLayer()
                        glowPaint.style=Paint.Style.STROKE
                        glowPaint.strokeWidth=dp(3).toFloat()
                        glowPaint.color=Color.argb(210,Color.red(color),Color.green(color),Color.blue(color))
                        glowPaint.maskFilter=android.graphics.BlurMaskFilter(
                            dp(14).toFloat(),
                            android.graphics.BlurMaskFilter.Blur.OUTER
                        )
                        canvas.drawRoundRect(inset,inset,width-inset,height-inset,dp(22).toFloat(),dp(22).toFloat(),glowPaint)

                        // Crisp neon core over the diffuse outer aura.
                        glowPaint.maskFilter=null
                        glowPaint.strokeWidth=dp(2).toFloat()
                        glowPaint.color=color
                        canvas.drawRoundRect(inset,inset,width-inset,height-inset,dp(22).toFloat(),dp(22).toFloat(),glowPaint)
                    }
                },android.widget.FrameLayout.LayoutParams(-1,-1))
            }
            val card=(if(settingsLight) eightbitlab.com.blurview.BlurView(this) else android.widget.FrameLayout(this)).apply {
                background=if(settingsLight) LayerDrawable(arrayOf(
                    GradientDrawable().apply {
                        cornerRadius=dp(22).toFloat()
                        setColor(fill)
                        setStroke(dp(4),Color.argb(42,Color.red(color),Color.green(color),Color.blue(color)))
                    },
                    GradientDrawable().apply {
                        cornerRadius=dp(22).toFloat()
                        setColor(Color.TRANSPARENT)
                        setStroke(dp(2),Color.argb(255,Color.red(color),Color.green(color),Color.blue(color)))
                    }
                )) else GradientDrawable().apply {
                    cornerRadius=dp(22).toFloat()
                    setColor(fill)
                    setStroke(dp(1),color)
                }
                layoutParams=android.widget.FrameLayout.LayoutParams(-1,-2)
                isClickable=onClick!=null; isFocusable=onClick!=null; if(onClick!=null) setOnClickListener { onClick() }
                if(settingsLight && this is eightbitlab.com.blurview.BlurView) {
                    outlineProvider=android.view.ViewOutlineProvider.BACKGROUND
                    clipToOutline=true
                    settingsBlurTarget?.let { target -> setupWith(target).setBlurRadius(designBlurRadius()).setOverlayColor(Color.argb(designGlassOverlayAlpha(),72,88,112)) }
                }
                addView(LinearLayout(this@MainActivity).apply {
                    orientation=LinearLayout.HORIZONTAL; gravity=android.view.Gravity.CENTER_VERTICAL; setPadding(dp(16),dp(14),dp(16),dp(14))
                    if(settingsLight) background=null
                    addView(TextView(this@MainActivity).apply {
                        text=icon; textSize=25f; gravity=android.view.Gravity.CENTER; setTextColor(if(settingsLight) Color.WHITE else color); setPadding(dp(6),dp(6),dp(6),dp(6))
                        background=GradientDrawable().apply { cornerRadius=dp(15).toFloat(); setColor(Color.argb(205,Color.red(color),Color.green(color),Color.blue(color))); setStroke(dp(1),Color.argb(230,255,255,255)) }
                        layoutParams=LinearLayout.LayoutParams(dp(50),dp(50)).apply { setMargins(0,0,dp(12),0) }
                    })
                    addView(LinearLayout(this@MainActivity).apply {
                        orientation=LinearLayout.VERTICAL; layoutParams=LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,1f)
                        addView(TextView(this@MainActivity).apply { text=title; textSize=15f; setTextColor(Color.WHITE); setTypeface(typeface,Typeface.BOLD) })
                        addView(TextView(this@MainActivity).apply { text=sub; textSize=12f; setTextColor(if(settingsLight) Color.argb(210,245,248,255) else Color.rgb(166,172,202)); setPadding(0,dp(3),0,0) })
                    })
                    addView(TextView(this@MainActivity).apply { text="›"; textSize=28f; setTextColor(color) })
                })
            }
            host.addView(card)
            settingsGrid.addView(host)
        }
        val garminLabel=if(garminClient.isLinked()) "Verbunden · Schlafdaten synchronisieren" else "Nicht verbunden · Jetzt verbinden"
        setting("⌚","Garmin Connect",garminLabel,accent2) { showGarminSettings() }
        setting("♥","Health Connect","Berechtigungen & Gesundheitsdaten",stageRem) { showHealthSettings() }
        setting("⚡","Automatik","Hintergrund-Sync & Kalender",stageAwake) { showAutomationSettings() }
        setting("✦","Darstellung","$selectedThemeLabel · SleepSync",accent) { showAppearanceSettings() }
        setting("◈","Datenschutz","Lokale Daten & Diagnose",stageLight) { showPrivacySettings() }
        setting("↻","Updates","Nach neuer SleepSync-Version suchen",Color.rgb(70,205,225)) { checkForPreviewUpdate() }
        setting("ⓘ","Über SleepSync","Version, Build & Entwickler",Color.rgb(120,170,255)) { showAboutSettings() }
        sleepCard.addView(settingsGrid)
        // Collapsible diagnostics: keep the settings page clean until explicitly opened.
        var diagnosticsOpen=false
        actionsTitle.text="DIAGNOSE   ›"
        actionsTitle.setTextColor(if(settingsLight) Color.rgb(215,225,245) else stageAwake)
        actionsTitle.textSize=11f
        actionsTitle.letterSpacing=.14f
        actionsTitle.setPadding(dp(4),dp(14),dp(4),dp(10))
        actionsTitle.isClickable=true
        actionsTitle.isFocusable=true

        val diagnosticRed=Color.rgb(255,70,82)
        listOf(0,1,2,3,4).forEach { i ->
            val b=actionsBox.getChildAt(i) as? MaterialButton ?: return@forEach
            b.cornerRadius=dp(18)
            b.textSize=12f
            b.minHeight=dp(48)
            b.setTextColor(Color.WHITE)
            b.backgroundTintList=ColorStateList.valueOf(
                if(settingsLight) Color.argb(designGlassAlpha(),72,88,112) else Color.rgb(14,17,34)
            )
            b.strokeWidth=dp(2)
            b.strokeColor=ColorStateList.valueOf(diagnosticRed)
            // Keep the MaterialButton paint untouched so its label stays crisp.
            // Use Android's software shadow only for the red outer aura.
            b.setLayerType(View.LAYER_TYPE_SOFTWARE,null)
            b.paint.maskFilter=null
            b.setShadowLayer(
                dp(8).toFloat(),
                0f,
                0f,
                Color.argb(190,Color.red(diagnosticRed),Color.green(diagnosticRed),Color.blue(diagnosticRed))
            )
            b.layoutParams=(b.layoutParams ?: LinearLayout.LayoutParams(-1,-2)).apply {
                height=dp(48)
                if(this is LinearLayout.LayoutParams) setMargins(0,dp(5),0,dp(5))
            }
            b.visibility=View.GONE
        }
        actionsTitle.setOnClickListener {
            diagnosticsOpen=!diagnosticsOpen
            actionsTitle.text=if(diagnosticsOpen) "DIAGNOSE   ⌄" else "DIAGNOSE   ›"
            listOf(0,1,2,3,4).forEach { i ->
                actionsBox.getChildAt(i)?.visibility=if(diagnosticsOpen) View.VISIBLE else View.GONE
            }
        }
        // showSettings() can be opened repeatedly; keep exactly one footer.
        while(actionsBox.childCount>5) actionsBox.removeViewAt(actionsBox.childCount-1)
        actionsBox.addView(TextView(this).apply {
            text="😴\nSleep well."
            textSize=12f
            gravity=android.view.Gravity.CENTER
            setTextColor(if(settingsLight) Color.argb(135,58,72,105) else Color.argb(125,175,185,215))
            setPadding(0,dp(28),0,dp(24))
        })
    }

    private fun checkForPreviewUpdate() {
        val info=packageManager.getPackageInfo(packageName,0)
        val current=info.longVersionCode
        val currentName=info.versionName ?: "unbekannt"
        val loading=AlertDialog.Builder(this)
            .setTitle("SleepSync Updates")
            .setMessage("Suche nach einer neuen Preview-Version …")
            .setNegativeButton("Abbrechen",null)
            .create()
        loading.setOnShowListener { styleSleepSyncDialog(loading) }
        loading.show()

        launch(Dispatchers.IO) {
            val result=runCatching {
                val connection=java.net.URL("https://api.github.com/repos/mellowtraxer/SleepSync-Updates/contents/latest.json?ref=main&t=${System.currentTimeMillis()}").openConnection() as java.net.HttpURLConnection
                connection.connectTimeout=8000
                connection.readTimeout=8000
                connection.useCaches=false
                connection.setRequestProperty("Cache-Control","no-cache, no-store, max-age=0")
                connection.setRequestProperty("Pragma","no-cache")
                connection.setRequestProperty("Accept","application/vnd.github.raw+json")
                connection.setRequestProperty("User-Agent","SleepSync-TurboUpdater")
                connection.inputStream.bufferedReader().use { JSONObject(it.readText()) }
            }
            withContext(Dispatchers.Main) {
                if(!loading.isShowing) return@withContext
                loading.dismiss()
                result.fold(
                    onSuccess={ latest ->
                        val remote=latest.getLong("versionCode")
                        val remoteName=latest.optString("versionName","Build $remote")
                        val apkUrl=latest.optString("apk")
                        val newer=remote>current
                        val builder=AlertDialog.Builder(this@MainActivity)
                            .setTitle(if(newer) "Update verfügbar ✨" else "SleepSync ist aktuell ✓")
                            .setMessage(if(newer)
                                "Installiert: $currentName (Build $current)\nNeu: $remoteName (Build $remote)\n\nEine neue SleepSync-Version ist verfügbar."
                            else
                                "Installiert: $currentName (Build $current)\nNeuester Build: $remoteName (Build $remote)\n\nDu verwendest bereits die aktuelle Version."
                            )
                        if(newer && apkUrl.isNotBlank()) {
                            builder.setPositiveButton("HERUNTERLADEN & INSTALLIEREN") { _,_ -> downloadPreviewUpdate(apkUrl,remoteName) }
                            builder.setNegativeButton("SPÄTER",null)
                        } else builder.setPositiveButton("OK",null)
                        val dialog=builder.create()
                        dialog.setOnShowListener { styleSleepSyncDialog(dialog) }
                        dialog.show()
                    },
                    onFailure={
                        val dialog=AlertDialog.Builder(this@MainActivity)
                            .setTitle("Update-Suche fehlgeschlagen")
                            .setMessage("Die Preview-Updatequelle konnte gerade nicht erreicht werden. Bitte versuche es später noch einmal.")
                            .setPositiveButton("OK",null)
                            .create()
                        dialog.setOnShowListener { styleSleepSyncDialog(dialog) }
                        dialog.show()
                    }
                )
            }
        }
    }

    private fun downloadPreviewUpdate(apkUrl:String,versionName:String) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O && !packageManager.canRequestPackageInstalls()) {
            startActivity(android.content.Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,android.net.Uri.parse("package:$packageName")))
            android.widget.Toast.makeText(this,"Bitte „Aus dieser Quelle zulassen“ aktivieren und das Update danach erneut starten.",android.widget.Toast.LENGTH_LONG).show()
            return
        }
        val request=android.app.DownloadManager.Request(android.net.Uri.parse(apkUrl))
            .setTitle("SleepSync $versionName")
            .setDescription("Update wird heruntergeladen …")
            .setMimeType("application/vnd.android.package-archive")
            .setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(this,android.os.Environment.DIRECTORY_DOWNLOADS,"SleepSync-update.apk")
        val manager=getSystemService(android.content.Context.DOWNLOAD_SERVICE) as android.app.DownloadManager
        val id=manager.enqueue(request)
        val receiver=object:android.content.BroadcastReceiver(){
            override fun onReceive(context:android.content.Context,intent:android.content.Intent){
                if(intent.getLongExtra(android.app.DownloadManager.EXTRA_DOWNLOAD_ID,-1L)!=id)return
                unregisterReceiver(this)
                val status=manager.query(android.app.DownloadManager.Query().setFilterById(id)).use { cursor ->
                    if(cursor.moveToFirst()) cursor.getInt(cursor.getColumnIndexOrThrow(android.app.DownloadManager.COLUMN_STATUS)) else -1
                }
                if(status!=android.app.DownloadManager.STATUS_SUCCESSFUL){
                    android.widget.Toast.makeText(this@MainActivity,"Update-Download fehlgeschlagen.",android.widget.Toast.LENGTH_LONG).show()
                    return
                }
                val uri=manager.getUriForDownloadedFile(id) ?: run {
                    android.widget.Toast.makeText(this@MainActivity,"Update-Datei konnte nicht geöffnet werden.",android.widget.Toast.LENGTH_LONG).show()
                    return
                }
                val install=android.content.Intent(android.content.Intent.ACTION_INSTALL_PACKAGE).apply {
                    data=uri
                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    putExtra(android.content.Intent.EXTRA_NOT_UNKNOWN_SOURCE,true)
                    putExtra(android.content.Intent.EXTRA_RETURN_RESULT,false)
                }
                try {
                    startActivity(install)
                } catch(e:Exception) {
                    android.widget.Toast.makeText(this@MainActivity,"Android-Installer konnte nicht geöffnet werden.",android.widget.Toast.LENGTH_LONG).show()
                }
            }
        }
        androidx.core.content.ContextCompat.registerReceiver(this,receiver,android.content.IntentFilter(android.app.DownloadManager.ACTION_DOWNLOAD_COMPLETE),androidx.core.content.ContextCompat.RECEIVER_EXPORTED)
        android.widget.Toast.makeText(this,"SleepSync-Update wird geladen …",android.widget.Toast.LENGTH_LONG).show()
    }

    private fun showGarminSettings() {
        val linked=garminClient.isLinked()
        val dialog=AlertDialog.Builder(this)
            .setTitle("Garmin Connect")
            .setMessage(if(linked) "Garmin Connect ist verbunden. Du kannst Schlafdaten jetzt neu synchronisieren oder die Verbindung trennen." else "Verbinde SleepSync mit Garmin Connect, damit deine Schlafdaten synchronisiert werden können.")
            .setPositiveButton(if(linked) "Schlafdaten laden" else "Verbinden") { _,_ -> if(linked) testRead() else showGarminLogin() }
            .setNeutralButton(if(linked) "Trennen" else null) { _,_ -> garminClient.logout(); refresh(); showSettings() }
            .setNegativeButton("Schließen",null)
            .create()
        dialog.setOnShowListener { styleSleepSyncDialog(dialog) }
        dialog.show()
    }

    private fun styleSleepSyncDialog(dialog:AlertDialog) {
        val theme=getSharedPreferences("sleepsync_ui",MODE_PRIVATE).getString("theme","dark")?:"dark"
        val sysDark=(resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK)==android.content.res.Configuration.UI_MODE_NIGHT_YES
        val light=theme=="light" || (theme=="system" && !sysDark)
        if(!light)return
        val d=resources.displayMetrics.density
        fun dp(v:Int)=(v*d).toInt()
        dialog.window?.setBackgroundDrawable(GradientDrawable(GradientDrawable.Orientation.TL_BR,intArrayOf(
            Color.argb(250,250,252,255),Color.argb(248,239,247,255),Color.argb(248,244,240,255)
        )).apply { cornerRadius=dp(26).toFloat(); setStroke(dp(1),Color.argb(185,128,104,235)) })
        fun recolor(v:View?) {
            if(v==null)return
            if(v is TextView) {
                v.setTextColor(Color.rgb(35,42,62))
                if(v.textSize < 13f*resources.displayMetrics.scaledDensity) v.textSize=15f
            }
            if(v is android.view.ViewGroup) for(i in 0 until v.childCount) recolor(v.getChildAt(i))
        }
        // AlertDialog list rows are framework TextViews and are not covered by android.R.id.message.
        // Recolor the whole content tree first, then restore the stronger title/button styling below.
        recolor(dialog.window?.decorView)
        val titleId=resources.getIdentifier("alertTitle","id","android")
        if(titleId!=0) dialog.findViewById<TextView>(titleId)?.apply { setTextColor(Color.rgb(24,29,48)); setTypeface(typeface,Typeface.BOLD); textSize=20f }
        dialog.findViewById<TextView>(android.R.id.message)?.apply { setTextColor(Color.rgb(55,64,88)); textSize=16f }
        dialog.listView?.apply {
            divider=ColorDrawable(Color.argb(38,70,80,110))
            dividerHeight=dp(1)
            setBackgroundColor(Color.TRANSPARENT)
        }
        listOf(AlertDialog.BUTTON_POSITIVE,AlertDialog.BUTTON_NEGATIVE,AlertDialog.BUTTON_NEUTRAL).forEach { which ->
            dialog.getButton(which)?.apply { setTextColor(Color.rgb(118,82,205)); setTypeface(typeface,Typeface.BOLD) }
        }
    }

    private fun showHealthSettings() {
        launch {
            val sdk=HealthConnectClient.getSdkStatus(this@MainActivity)
            if(sdk!=HealthConnectClient.SDK_AVAILABLE) {
                AlertDialog.Builder(this@MainActivity).setTitle("Health Connect").setMessage("Health Connect ist auf diesem Gerät nicht verfügbar.").setPositiveButton("OK",null).show()
                return@launch
            }
            val granted=HealthConnectClient.getOrCreate(this@MainActivity).permissionController.getGrantedPermissions()
            val ok=granted.containsAll(permissions)
            val dialog=AlertDialog.Builder(this@MainActivity)
                .setTitle("Health Connect")
                .setMessage((if(ok) "✓ Alle benötigten Berechtigungen sind erteilt." else "SleepSync benötigt noch Berechtigungen.")+"\n\nGelesen werden Schlaf, Herzfrequenz, Sauerstoffsättigung und Atemfrequenz. Die Daten werden lokal verarbeitet.")
                .setPositiveButton("Berechtigungen") { _,_ -> permissionLauncher.launch(permissions) }
                .setNegativeButton("Schließen",null)
                .create()
            dialog.setOnShowListener { styleSleepSyncDialog(dialog) }
            dialog.show()
        }
    }

    private fun showAutomationSettings() {
        val p=calendarPrefs()
        val d=resources.displayMetrics.density
        fun dp(v:Int)=(v*d).toInt()
        val startMin=p.getInt("check_start_min",4*60)
        val endMin=p.getInt("check_end_min",10*60)
        val interval=p.getInt("check_interval_min",30)
        fun hm(m:Int)=String.format(java.util.Locale.GERMANY,"%02d:%02d",m/60,m%60)
        fun chooseTime(title:String,current:Int,onSave:(Int)->Unit){
            val dlg=android.app.TimePickerDialog(this,{_,h,m->onSave(h*60+m);scheduleBackgroundSleepSync();showAutomationSettings()},current/60,current%60,true)
            dlg.setTitle(title);dlg.show()
        }
        fun chooseInterval(){
            val values=intArrayOf(15,30,45,60,90,120)
            val labels=values.map{"$it Minuten"}.toTypedArray()
            val selected=values.indexOf(interval).coerceAtLeast(0)
            val dlg=AlertDialog.Builder(this).setTitle("Prüfintervall").setSingleChoiceItems(labels,selected){dialog,which->
                p.edit().putInt("check_interval_min",values[which]).apply();scheduleBackgroundSleepSync();dialog.dismiss();showAutomationSettings()
            }.setNegativeButton("Abbrechen",null).create()
            dlg.setOnShowListener{styleSleepSyncDialog(dlg)};dlg.show()
        }
        val lastCheck=p.getLong("last_background_check",0L)
        val lastAuto=p.getLong("last_auto_insert_at",0L)
        val fmt=DateTimeFormatter.ofPattern("dd.MM. · HH:mm").withZone(ZoneId.systemDefault())
        val selected=p.getString("calendar_name",null) ?: "Noch kein Zielkalender"
        val windowText=hm(startMin)+" – "+hm(endMin)+" Uhr"
        val crosses=if(endMin<=startMin) " · über Mitternacht" else ""
        val msg=buildString {
            append(if(calendarAutoEnabled()) "✓ Automatik ist aktiv" else "○ Automatik ist ausgeschaltet")
            append("\n\nSCHLAFERKENNUNG")
            append("\nPrüfzeitraum: ").append(windowText).append(crosses)
            append("\nPrüfintervall: alle ").append(interval).append(" Minuten")
            append("\nNach einem erfolgreichen Eintrag wird derselbe Schlaf nicht erneut eingetragen.")
            append("\n\nZielkalender: ").append(selected)
            append("\nLetzte Hintergrundprüfung: ").append(if(lastCheck>0) fmt.format(Instant.ofEpochMilli(lastCheck))+" Uhr" else "noch keine")
            append("\nLetzter automatischer Eintrag: ").append(if(lastAuto>0) fmt.format(Instant.ofEpochMilli(lastAuto))+" Uhr" else "noch keiner")
        }
        val dialog=AlertDialog.Builder(this)
            .setTitle("Automatik & Schlaferkennung")
            .setMessage(msg)
            .setPositiveButton("Zeiten konfigurieren",null)
            .setNeutralButton("Zielkalender"){_,_->chooseCalendar()}
            .setNegativeButton("Schließen",null)
            .create()
        dialog.setOnShowListener {
            styleSleepSyncDialog(dialog)
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val choices=arrayOf(
                    "Startzeit · "+hm(startMin),
                    "Endzeit · "+hm(endMin),
                    "Intervall · "+interval+" Min.",
                    if(calendarAutoEnabled()) "Automatik ausschalten" else "Automatik einschalten"
                )
                val sub=AlertDialog.Builder(this).setTitle("Schlaferkennung konfigurieren").setItems(choices){_,which->
                    when(which){
                        0->chooseTime("Prüfung starten",startMin){p.edit().putInt("check_start_min",it).apply()}
                        1->chooseTime("Prüfung beenden",endMin){p.edit().putInt("check_end_min",it).apply()}
                        2->chooseInterval()
                        3->{p.edit().putBoolean("auto_enabled",!calendarAutoEnabled()).apply();showAutomationSettings()}
                    }
                }.setNegativeButton("Zurück",null).create()
                sub.setOnShowListener{styleSleepSyncDialog(sub)};sub.show()
            }
        }
        dialog.show()
    }

    private fun showPrivacySettings() {
        val cached=sleepHistory.size
        val dialog=AlertDialog.Builder(this)
            .setTitle("Datenschutz & Diagnose")
            .setMessage("SleepSync verarbeitet deine Schlaf- und Gesundheitsdaten lokal auf diesem Gerät. Garmin-Anmeldedaten werden nicht gespeichert; gespeichert werden nur die für die Verbindung benötigten OAuth-Tokens.\n\nLokaler Verlauf: $cached Nächte\nPaket: $packageName\n\nUnter Diagnose findest du technische Informationen zur installierten App.")
            .setPositiveButton("Diagnose") { _,_ -> showAppSignature() }
            .setNeutralButton("Verlauf löschen") { _,_ ->
                val deleteDialog=AlertDialog.Builder(this)
                    .setTitle("Lokalen Verlauf löschen?")
                    .setMessage("Der lokal zwischengespeicherte SleepSync-Verlauf wird gelöscht. Daten bei Garmin, Health Connect und im Kalender bleiben erhalten.")
                    .setPositiveButton("Löschen") { _,_ ->
                        getSharedPreferences("sleepsync_history",MODE_PRIVATE).edit().clear().apply()
                        sleepHistory=emptyList()
                        showSettings()
                    }
                    .setNegativeButton("Abbrechen",null)
                    .create()
                deleteDialog.setOnShowListener {
                    styleSleepSyncDialog(deleteDialog)
                    deleteDialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(Color.rgb(220,70,85))
                }
                deleteDialog.show()
            }
            .setNegativeButton("Schließen",null)
            .create()
        dialog.setOnShowListener { styleSleepSyncDialog(dialog) }
        dialog.show()
    }

    private fun showAboutSettings() {
        val info=packageManager.getPackageInfo(packageName,0)
        val versionName=info.versionName ?: "–"
        val versionCode=info.longVersionCode
        val installed=runCatching { DateTimeFormatter.ofPattern("dd.MM.yyyy · HH:mm").withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(info.lastUpdateTime)) }.getOrDefault("–")
        val dialog=AlertDialog.Builder(this)
            .setTitle("Über SleepSync")
            .setMessage("SleepSync\nDein Schlaf. Klar, automatisch, im Kalender.\n\nEntwickelt von Riccardo Hoff\n© 2026\n\nVersion: $versionName\nBuild: $versionCode\nPaket: $packageName\nInstallierter Build: $installed\n\nGarmin → Health Connect → SleepSync → Kalender\n\nSleepSync ist ein unabhängiges Projekt und steht in keiner offiziellen Verbindung zu Garmin.")
            .setPositiveButton("Schließen",null)
            .setNeutralButton("App-Signatur") { _,_ -> showAppSignature() }
            .create()
        dialog.setOnShowListener { styleSleepSyncDialog(dialog) }
        dialog.show()
    }

    private fun showDesignStudio() {
        val d=resources.displayMetrics.density; fun dp(v:Int)=(v*d).toInt()
        val p=getSharedPreferences("sleepsync_design",MODE_PRIVATE)
        pageTitle.text="Design Studio";pageSubtitle.text="Dein SleepSync. Dein Look."
        actionsTitle.visibility=View.GONE;actionsBox.visibility=View.GONE;sleepCard.removeAllViews()
        val names=arrayOf("Hauptakzent","Sekundärakzent","Leichtschlaf","Tiefschlaf","REM","Wach","Puls","SpO₂","Atmung","HRV")
        val keys=arrayOf("accent","accent2","stage_light","stage_deep","stage_rem","stage_awake","heart","spo2","resp","hrv")
        val defs=intArrayOf(Color.rgb(139,92,246),Color.rgb(34,211,238),Color.rgb(99,190,255),Color.rgb(95,75,220),Color.rgb(183,99,255),Color.rgb(255,164,91),Color.rgb(255,82,126),Color.rgb(44,205,255),Color.rgb(80,225,184),Color.rgb(213,96,255))
        fun pickFullColor(index:Int) {
            val current=p.getInt(keys[index],defs[index])
            val hsv=FloatArray(3);Color.colorToHSV(current,hsv)
            var hue=hsv[0];var sat=hsv[1];var value=hsv[2]
            val wrap=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(22),dp(8),dp(22),dp(10))}
            val swatch=TextView(this).apply{gravity=android.view.Gravity.CENTER;textSize=16f;setTypeface(typeface,Typeface.BOLD);setTextColor(Color.WHITE);layoutParams=LinearLayout.LayoutParams(-1,dp(62)).apply{setMargins(0,0,0,dp(14))}}
            wrap.addView(swatch)
            fun update(){val col=Color.HSVToColor(floatArrayOf(hue,sat,value));swatch.text=String.format("#%06X",0xFFFFFF and col);swatch.background=GradientDrawable().apply{cornerRadius=dp(16).toFloat();setColor(col)}}
            val spectrum=View(this).apply{background=GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,intArrayOf(Color.RED,Color.YELLOW,Color.GREEN,Color.CYAN,Color.BLUE,Color.MAGENTA,Color.RED)).apply{cornerRadius=dp(9).toFloat()};layoutParams=LinearLayout.LayoutParams(-1,dp(18)).apply{setMargins(0,0,0,dp(3))}}
            wrap.addView(TextView(this).apply{text="FARBSPEKTRUM";textSize=11f;letterSpacing=.12f;setTextColor(Color.WHITE);setTypeface(typeface,Typeface.BOLD)})
            wrap.addView(spectrum)
            wrap.addView(android.widget.SeekBar(this).apply{max=360;progress=hue.toInt();setOnSeekBarChangeListener(object:android.widget.SeekBar.OnSeekBarChangeListener{override fun onProgressChanged(s:android.widget.SeekBar?,v:Int,u:Boolean){hue=v.toFloat();update()}override fun onStartTrackingTouch(s:android.widget.SeekBar?){};override fun onStopTrackingTouch(s:android.widget.SeekBar?){}})})
            fun hsvSlider(title:String,start:Int,onChange:(Int)->Unit){val label=TextView(this).apply{text="$title   $start%";textSize=12f;setTextColor(Color.WHITE)};wrap.addView(label);wrap.addView(android.widget.SeekBar(this).apply{max=100;progress=start;setOnSeekBarChangeListener(object:android.widget.SeekBar.OnSeekBarChangeListener{override fun onProgressChanged(s:android.widget.SeekBar?,v:Int,u:Boolean){onChange(v);label.text="$title   $v%";update()}override fun onStartTrackingTouch(s:android.widget.SeekBar?){};override fun onStopTrackingTouch(s:android.widget.SeekBar?){}})})}
            hsvSlider("Sättigung",(sat*100).toInt()){sat=it/100f};hsvSlider("Helligkeit",(value*100).toInt()){value=it/100f};update()
            val dlg=AlertDialog.Builder(this).setTitle(names[index]).setView(wrap).setPositiveButton("Übernehmen"){_,_->p.edit().putInt(keys[index],Color.HSVToColor(floatArrayOf(hue,sat,value))).putBoolean("custom_enabled",true).apply();recreate()}.setNegativeButton("Abbrechen",null).create()
            dlg.setOnShowListener{dlg.window?.setBackgroundDrawable(GradientDrawable().apply{cornerRadius=dp(24).toFloat();setColor(Color.rgb(28,35,52));setStroke(dp(2),Color.HSVToColor(floatArrayOf(hue,sat,value)))});val titleId=resources.getIdentifier("alertTitle","id","android");if(titleId!=0)dlg.findViewById<TextView>(titleId)?.setTextColor(Color.WHITE);dlg.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(Color.rgb(70,220,255));dlg.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(Color.rgb(190,150,255))}
            dlg.show()
        }
        fun section(t:String)=sleepCard.addView(TextView(this).apply{text=t;textSize=11f;letterSpacing=.14f;setTypeface(typeface,Typeface.BOLD);setTextColor(Color.WHITE);setPadding(dp(4),dp(18),0,dp(8))})
        fun row(title:String,sub:String,tone:Int,click:()->Unit)=sleepCard.addView(MaterialCardView(this).apply{
            radius=dp(18).toFloat();strokeWidth=dp(2);strokeColor=tone;setCardBackgroundColor(Color.argb(designGlassAlpha(),72,88,112));layoutParams=LinearLayout.LayoutParams(-1,-2).apply{setMargins(0,dp(4),0,dp(4))}
            addView(LinearLayout(this@MainActivity).apply{orientation=LinearLayout.HORIZONTAL;gravity=android.view.Gravity.CENTER_VERTICAL;setPadding(dp(15),dp(12),dp(15),dp(12))
                addView(View(this@MainActivity).apply{background=GradientDrawable().apply{shape=GradientDrawable.OVAL;setColor(tone);setStroke(dp(2),Color.WHITE)};layoutParams=LinearLayout.LayoutParams(dp(30),dp(30)).apply{marginEnd=dp(13)}})
                addView(LinearLayout(this@MainActivity).apply{orientation=LinearLayout.VERTICAL;addView(TextView(this@MainActivity).apply{text=title;textSize=15f;setTypeface(typeface,Typeface.BOLD);setTextColor(Color.WHITE)});addView(TextView(this@MainActivity).apply{text=sub;textSize=11f;setTextColor(Color.rgb(220,228,245))})},LinearLayout.LayoutParams(0,-2,1f))
                addView(TextView(this@MainActivity).apply{text="›";textSize=27f;setTextColor(tone)})
            });setOnClickListener{click()}
        })
        sleepCard.addView(TextView(this).apply{text="‹   Darstellung";textSize=12f;setTypeface(typeface,Typeface.BOLD);setTextColor(Color.rgb(115,210,255));setPadding(dp(4),dp(8),0,dp(12));setOnClickListener{showAppearanceSettings()}})
        // Live preview
        sleepCard.addView(MaterialCardView(this).apply{radius=dp(24).toFloat();strokeWidth=dp(2);strokeColor=designColor("accent2",defs[1]);setCardBackgroundColor(Color.argb(designGlassAlpha(),72,88,112));layoutParams=LinearLayout.LayoutParams(-1,dp(142)).apply{setMargins(0,0,0,dp(8))}
            addView(LinearLayout(this@MainActivity).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(18),dp(15),dp(18),dp(12));addView(TextView(this@MainActivity).apply{text="LIVE-VORSCHAU";textSize=10f;letterSpacing=.14f;setTextColor(designColor("accent2",defs[1]));setTypeface(typeface,Typeface.BOLD)});addView(TextView(this@MainActivity).apply{text="7 h 42 min";textSize=29f;setTextColor(Color.WHITE);setTypeface(typeface,Typeface.BOLD)});addView(LinearLayout(this@MainActivity).apply{orientation=LinearLayout.HORIZONTAL;listOf("Leicht" to 2,"Tief" to 3,"REM" to 4,"Wach" to 5).forEach{(n,i)->addView(TextView(this@MainActivity).apply{text=n;textSize=10f;gravity=android.view.Gravity.CENTER;setTextColor(defs[i]);background=GradientDrawable().apply{cornerRadius=dp(8).toFloat();setColor(Color.argb(42,Color.red(defs[i]),Color.green(defs[i]),Color.blue(defs[i])));setStroke(dp(1),defs[i])}},LinearLayout.LayoutParams(0,dp(30),1f).apply{setMargins(dp(2),0,dp(2),0)})}})})
        })
        section("FARBEN")
        names.indices.forEach{i->val tone=p.getInt(keys[i],defs[i]);row(names[i],String.format("#%06X",0xFFFFFF and tone),tone){pickFullColor(i)}}
        section("HINTERGRUND")
        row("Wallpaper",if(p.getBoolean("wallpaper_enabled",true)) "Aktiv · SleepSync Wallpaper" else "Aus · einfarbiger Hintergrund",Color.rgb(70,205,225)){p.edit().putBoolean("wallpaper_enabled",!p.getBoolean("wallpaper_enabled",true)).putBoolean("custom_enabled",true).apply();recreate()}
        section("GLAS & EFFEKTE")
        fun slider(title:String,key:String,value:Int,max:Int,tone:Int){
            val box=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(14),dp(10),dp(14),dp(8));background=GradientDrawable().apply{cornerRadius=dp(18).toFloat();setColor(Color.argb(designGlassAlpha(),72,88,112));setStroke(dp(1),tone)};layoutParams=LinearLayout.LayoutParams(-1,-2).apply{setMargins(0,dp(4),0,dp(4))}
                val label=TextView(this@MainActivity).apply{text="$title   $value%";textSize=13f;setTypeface(typeface,Typeface.BOLD);setTextColor(Color.WHITE)};addView(label)
                addView(android.widget.SeekBar(this@MainActivity).apply{this.max=max;progress=value;progressTintList=ColorStateList.valueOf(tone);thumbTintList=ColorStateList.valueOf(tone);setOnSeekBarChangeListener(object:android.widget.SeekBar.OnSeekBarChangeListener{override fun onProgressChanged(s:android.widget.SeekBar?,v:Int,u:Boolean){label.text="$title   $v%";if(u)p.edit().putInt(key,v).putBoolean("custom_enabled",true).apply()}override fun onStartTrackingTouch(s:android.widget.SeekBar?){};override fun onStopTrackingTouch(s:android.widget.SeekBar?){showDesignStudio()}})})
            };sleepCard.addView(box)
        }
        slider("Glas-Transparenz","glass_strength",p.getInt("glass_strength",34),100,Color.rgb(90,190,255))
        slider("Blur","blur_strength",p.getInt("blur_strength",20),100,Color.rgb(183,99,255))
        slider("Neon","neon_strength",p.getInt("neon_strength",100),100,Color.rgb(255,82,190))
        slider("Glow","glow_strength",p.getInt("glow_strength",100),100,Color.rgb(80,225,184))
        sleepCard.addView(MaterialButton(this).apply{text="✓  Übernehmen";isAllCaps=false;textSize=16f;setTypeface(typeface,Typeface.BOLD);setTextColor(Color.WHITE);backgroundTintList=ColorStateList.valueOf(designColor("accent2",Color.rgb(34,211,238)));layoutParams=LinearLayout.LayoutParams(-1,dp(54)).apply{setMargins(0,dp(18),0,dp(6))};setOnClickListener{p.edit().putBoolean("custom_enabled",true).apply();recreate()}})
        sleepCard.addView(MaterialButton(this).apply{text="↺  SleepSync Standard wiederherstellen";isAllCaps=false;setTextColor(Color.WHITE);backgroundTintList=ColorStateList.valueOf(Color.argb(190,55,48,82));layoutParams=LinearLayout.LayoutParams(-1,dp(52)).apply{setMargins(0,dp(16),0,dp(10))};setOnClickListener{applySleepSyncStandard();recreate()}})
    }

    private fun showAppearanceSettings() {
        val d=resources.displayMetrics.density
        fun dp(v:Int)=(v*d).toInt()
        pageTitle.text="Wähle dein Design"
        pageSubtitle.text="SleepSync so, wie du es magst"
        actionsTitle.visibility=View.GONE
        actionsBox.visibility=View.GONE
        sleepCard.removeAllViews()
        sleepCard.clipChildren=false
        sleepCard.clipToPadding=false

        sleepCard.addView(TextView(this).apply {
            text="‹   Einstellungen"
            textSize=12f
            setTypeface(typeface,Typeface.BOLD)
            setTextColor(Color.rgb(115,210,255))
            setPadding(dp(4),dp(10),0,dp(16))
            setOnClickListener { showSettings() }
        })

        val prefs=getSharedPreferences("sleepsync_ui",MODE_PRIVATE)
        val current=prefs.getString("theme","dark") ?: "dark"

        fun choice(key:String,title:String,sub:String,desc:String,icon:String,tone:Int) {
            val active=current==key
            val host=object:android.widget.FrameLayout(this) {
                private val glowPaint=Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style=Paint.Style.STROKE
                    strokeWidth=dp(if(active) 4 else 3).toFloat()
                }
                init {
                    clipChildren=false
                    clipToPadding=false
                    setWillNotDraw(false)
                    setLayerType(View.LAYER_TYPE_SOFTWARE,null)
                }
                override fun onDraw(c:Canvas) {
                    super.onDraw(c)
                    val inset=dp(3).toFloat()
                    glowPaint.color=Color.argb(if(active) 230 else 170,Color.red(tone),Color.green(tone),Color.blue(tone))
                    glowPaint.maskFilter=android.graphics.BlurMaskFilter(
                        dp(if(active) 14 else 9).toFloat(),
                        android.graphics.BlurMaskFilter.Blur.OUTER
                    )
                    c.drawRoundRect(inset,inset,width-inset,height-inset,dp(22).toFloat(),dp(22).toFloat(),glowPaint)
                    glowPaint.maskFilter=null
                }
            }.apply {
                layoutParams=LinearLayout.LayoutParams(-1,-2).apply { setMargins(dp(4),dp(7),dp(4),dp(7)) }
            }

            val card=android.widget.FrameLayout(this).apply {
                background=LayerDrawable(arrayOf(
                    GradientDrawable().apply {
                        cornerRadius=dp(22).toFloat()
                        setColor(Color.argb(if(active) 195 else 178,72,88,112))
                        setStroke(dp(if(active) 3 else 2),tone)
                    },
                    GradientDrawable().apply {
                        cornerRadius=dp(22).toFloat()
                        setColor(Color.TRANSPARENT)
                        setStroke(dp(1),Color.argb(170,255,255,255))
                    }
                ))
                isClickable=true
                isFocusable=true
                setOnClickListener { prefs.edit().putString("theme",key).apply(); recreate() }

                addView(LinearLayout(this@MainActivity).apply {
                    orientation=LinearLayout.HORIZONTAL
                    gravity=android.view.Gravity.CENTER_VERTICAL
                    setPadding(dp(16),dp(14),dp(16),dp(14))

                    addView(TextView(this@MainActivity).apply {
                        text=icon
                        textSize=31f
                        gravity=android.view.Gravity.CENTER
                        setTextColor(Color.WHITE)
                        background=GradientDrawable().apply {
                            cornerRadius=dp(17).toFloat()
                            setColor(Color.argb(205,Color.red(tone),Color.green(tone),Color.blue(tone)))
                            setStroke(dp(1),Color.argb(220,255,255,255))
                        }
                        layoutParams=LinearLayout.LayoutParams(dp(64),dp(64))
                    })

                    addView(LinearLayout(this@MainActivity).apply {
                        orientation=LinearLayout.VERTICAL
                        setPadding(dp(16),0,dp(8),0)
                        addView(TextView(this@MainActivity).apply {
                            text=title; textSize=17f; setTypeface(typeface,Typeface.BOLD); setTextColor(Color.WHITE)
                        })
                        addView(TextView(this@MainActivity).apply {
                            text=sub; textSize=12f; setTypeface(typeface,Typeface.BOLD); setTextColor(Color.rgb(220,226,242)); setPadding(0,dp(2),0,dp(4))
                        })
                        addView(TextView(this@MainActivity).apply {
                            text=desc; textSize=11f; setTextColor(Color.rgb(210,218,235)); maxLines=2
                        })
                    },LinearLayout.LayoutParams(0,-2,1f))

                    addView(LinearLayout(this@MainActivity).apply {
                        orientation=LinearLayout.VERTICAL
                        gravity=android.view.Gravity.CENTER
                        addView(TextView(this@MainActivity).apply {
                            text=if(active) "◉" else "○"
                            textSize=30f
                            gravity=android.view.Gravity.CENTER
                            setTextColor(if(active) tone else Color.rgb(190,205,230))
                        })
                        if(active) addView(TextView(this@MainActivity).apply {
                            text="✓ AKTIV"
                            textSize=10f
                            setTypeface(typeface,Typeface.BOLD)
                            setTextColor(tone)
                            gravity=android.view.Gravity.CENTER
                        })
                    },LinearLayout.LayoutParams(dp(72),-1))
                },android.widget.FrameLayout.LayoutParams(
                    android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                    android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
                ))
            }
            host.addView(card,android.widget.FrameLayout.LayoutParams(-1,-2))
            sleepCard.addView(host)
        }

        choice("dark","Dunkel","OLED Night","Dunkles Design für beste Lesbarkeit bei Nacht.","☾",Color.rgb(91,92,255))
        choice("light","Hell","Neon Sunrise","Helles Design mit freundlichen Farben für den Tag.","☀",Color.rgb(255,166,46))
        choice("system","Automatisch","System","Wechselt automatisch zwischen hell und dunkel – je nach Tageszeit.","◐",Color.rgb(49,216,255))

        val studio=MaterialCardView(this).apply {
            radius=dp(22).toFloat(); strokeWidth=dp(2); strokeColor=Color.rgb(255,92,205); setCardBackgroundColor(Color.argb(designGlassAlpha(),72,88,112))
            layoutParams=LinearLayout.LayoutParams(-1,-2).apply{setMargins(dp(4),dp(10),dp(4),dp(7))}
            addView(LinearLayout(this@MainActivity).apply {
                orientation=LinearLayout.HORIZONTAL;gravity=android.view.Gravity.CENTER_VERTICAL;setPadding(dp(16),dp(15),dp(16),dp(15))
                addView(TextView(this@MainActivity).apply{text="🎨";textSize=30f;gravity=android.view.Gravity.CENTER;layoutParams=LinearLayout.LayoutParams(dp(58),dp(58))})
                addView(LinearLayout(this@MainActivity).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(14),0,0,0);addView(TextView(this@MainActivity).apply{text="Design Studio";textSize=17f;setTypeface(typeface,Typeface.BOLD);setTextColor(Color.WHITE)});addView(TextView(this@MainActivity).apply{text="Farben frei personalisieren";textSize=12f;setTextColor(Color.rgb(220,226,242));setPadding(0,dp(3),0,0)})},LinearLayout.LayoutParams(0,-2,1f))
                addView(TextView(this@MainActivity).apply{text="›";textSize=30f;setTextColor(Color.rgb(255,92,205))})
            })
            setOnClickListener{showDesignStudio()}
        }
        sleepCard.addView(studio)

        sleepCard.addView(TextView(this).apply {
            text="ⓘ   Deine Auswahl gilt für die gesamte App."
            textSize=11f
            setTextColor(Color.WHITE)
            alpha=.88f
            setPadding(dp(8),dp(16),dp(8),dp(4))
        })
    }

    private fun sleepStageStrip(s: SleepSummary): LinearLayout {
        val d = resources.displayMetrics.density
        fun dp(v: Int) = (v * d).toInt()
        val values = listOf(
            s.lightMin to stageLight,
            s.deepMin to stageDeep,
            s.remMin to stageRem,
            s.awakeMin to stageAwake
        )
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            background = GradientDrawable().apply {
                cornerRadius = dp(10).toFloat()
                setColor(Color.rgb(43, 37, 72))
            }
            values.forEach { (minutes, color) ->
                addView(View(this@MainActivity).apply {
                    background = GradientDrawable().apply {
                        cornerRadius = dp(8).toFloat()
                        setColor(color)
                    }
                    layoutParams = LinearLayout.LayoutParams(
                        0, dp(16), minutes.coerceAtLeast(1).toFloat()
                    ).apply { setMargins(dp(1), 0, dp(1), 0) }
                })
            }
        }
    }

    private fun metricCard(icon: String, label: String, value: String, onClick: (() -> Unit)? = null, series: List<MetricPoint> = emptyList(), sleep: SleepSummary? = null): android.view.View {
        val d=resources.displayMetrics.density; fun dp(v:Int)=(v*d).toInt()
        val tone=when(label){"Leicht"->stageLight;"Tief"->stageDeep;"REM"->stageRem;"Wach"->stageAwake;"Puls"->designColor("heart",Color.rgb(255,82,126));"SpO₂"->designColor("spo2",Color.rgb(44,205,255));"Atmung"->designColor("resp",Color.rgb(80,225,184));"HRV"->designColor("hrv",Color.rgb(213,96,255));else->accent}
        val fill=when(label){"Leicht"->Color.rgb(10,32,48);"Tief"->Color.rgb(22,20,56);"REM"->Color.rgb(42,18,58);"Wach"->Color.rgb(54,31,16);"Puls"->Color.rgb(54,18,31);"SpO₂"->Color.rgb(9,37,49);"Atmung"->Color.rgb(10,42,34);"HRV"->Color.rgb(45,17,55);else->Color.rgb(15,18,38)}
        val theme=getSharedPreferences("sleepsync_ui",MODE_PRIVATE).getString("theme","dark")?:"dark"
        val sysDark=(resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK)==android.content.res.Configuration.UI_MODE_NIGHT_YES
        val light=theme=="light" || (theme=="system" && !sysDark)
        val pct=if(sleep!=null && label in listOf("Leicht","Tief","REM","Wach")) when(label){"Leicht"->sleep.lightMin;"Tief"->sleep.deepMin;"REM"->sleep.remMin;else->sleep.awakeMin}*100/sleep.totalMin.coerceAtLeast(1) else -1
        val valueText=TextView(this).apply{text=value;textSize=19f;setTextColor(Color.WHITE);setTypeface(typeface,Typeface.BOLD);setPadding(0,dp(7),0,dp(3))}
        val body=LinearLayout(this).apply{
            orientation=LinearLayout.VERTICAL;setPadding(dp(15),dp(14),dp(15),dp(12))
            addView(TextView(this@MainActivity).apply{text="$icon   ${label.uppercase()}";textSize=11f;letterSpacing=.08f;setTextColor(if(light) Color.WHITE else tone);setTypeface(typeface,Typeface.BOLD);if(light)setShadowLayer(dp(2).toFloat(),0f,0f,tone)})
            addView(LinearLayout(this@MainActivity).apply { orientation=LinearLayout.HORIZONTAL; gravity=android.view.Gravity.CENTER_VERTICAL; addView(valueText,LinearLayout.LayoutParams(0,-2,1f)); if(pct>=0)addView(TextView(this@MainActivity).apply{text="${pct}%";textSize=17f;setTextColor(tone);setTypeface(typeface,Typeface.BOLD)}) })
            if(series.size>=2) {
                addView(MetricSparklineView(this@MainActivity,series,tone,label,valueText).apply {
                    layoutParams=LinearLayout.LayoutParams(-1,dp(52)).apply { setMargins(0,dp(4),0,0) }
                })
            } else if(sleep!=null && label in listOf("Leicht","Tief","REM","Wach")) {
                addView(StageNeonView(this@MainActivity,sleep.stageSeries,label,tone,sleep.startMs,sleep.endMs).apply {
                    layoutParams=LinearLayout.LayoutParams(-1,dp(36)).apply { setMargins(0,dp(5),0,0) }
                })
            } else {
                addView(View(this@MainActivity).apply {
                    background=GradientDrawable().apply { cornerRadius=dp(3).toFloat(); setColor(tone) }
                    layoutParams=LinearLayout.LayoutParams(dp(38),dp(3)).apply { setMargins(0,dp(4),0,0) }
                })
            }
        }
        val card=MaterialCardView(this).apply{
            radius=dp(21).toFloat();cardElevation=if(light) dp(14).toFloat() else dp(2).toFloat();strokeWidth=dp(2);strokeColor=tone;setCardBackgroundColor(Color.TRANSPARENT);if(light){background=neonGlowGlass(21,tone,::dp);outlineAmbientShadowColor=tone;outlineSpotShadowColor=tone}else setCardBackgroundColor(fill)
            layoutParams=android.widget.FrameLayout.LayoutParams(-1,-1)
            addView(body);if(onClick!=null){isClickable=true;isFocusable=true;setOnClickListener{onClick()}}
        }
        return if(light) neonWrap(card,tone,21,::dp).apply { layoutParams=GridLayout.LayoutParams().apply{width=0;height=dp(if(label in listOf("Puls","SpO₂","Atmung","HRV")) 176 else 122);columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1f);setMargins(0,0,0,0)} } else card.apply { layoutParams=GridLayout.LayoutParams().apply{width=0;height=dp(if(label in listOf("Puls","SpO₂","Atmung","HRV")) 164 else 110);columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1f);setMargins(dp(4),dp(4),dp(4),dp(4))} }
    }
    private fun showStageTimeline(label:String, tone:Int, minutes:Long, s:SleepSummary) {
        showAllStageTimelines(s)
    }

    private fun showAllStageTimelines(s:SleepSummary) {
        val d=resources.displayMetrics.density; fun dp(v:Int)=(v*d).toInt()
        val theme=getSharedPreferences("sleepsync_ui",MODE_PRIVATE).getString("theme","dark")?:"dark"
        val sysDark=(resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK)==android.content.res.Configuration.UI_MODE_NIGHT_YES
        val light=theme=="light" || (theme=="system" && !sysDark)
        val primary=Color.WHITE
        val secondary=if(light) Color.rgb(225,232,248) else Color.rgb(165,175,205)
        val timeColor=if(light) Color.rgb(210,222,244) else Color.rgb(135,147,180)
        val tf=DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())
        fun fmtMin(m:Long)=if(m>=60) (m/60).toString()+" h "+(m%60).toString()+" min" else m.toString()+" min"
        pageTitle.text="Schlafphasen"; pageSubtitle.text="Die Architektur deiner Nacht"
        actionsTitle.visibility=View.GONE; actionsBox.visibility=View.GONE; sleepCard.removeAllViews()
        sleepCard.addView(TextView(this).apply { text="‹  Zurück zur Übersicht"; textSize=12f; setTextColor(accent2); setPadding(dp(2),dp(8),0,dp(14)); setOnClickListener { showOverview() } })

        // One-glance summary: sleep duration + full-night composition.
        sleepCard.addView(MaterialCardView(this).apply {
            radius=dp(24).toFloat();strokeWidth=0;setCardBackgroundColor(Color.TRANSPARENT);cardElevation=0f;elevation=0f
            background=GradientDrawable().apply{cornerRadius=dp(24).toFloat();setColor(if(light) Color.argb(designGlassAlpha(),72,88,112) else Color.argb(190,9,15,31));setStroke(dp(2),Color.argb(designNeonAlpha(255),Color.red(accent2),Color.green(accent2),Color.blue(accent2)))}
            layoutParams=LinearLayout.LayoutParams(-1,-2).apply{setMargins(0,0,0,dp(12))}
            addView(LinearLayout(this@MainActivity).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(18),dp(16),dp(18),dp(16))
                addView(TextView(this@MainActivity).apply{text="NACHT-ZUSAMMENFASSUNG";textSize=10f;letterSpacing=.12f;setTextColor(accent2);setTypeface(typeface,Typeface.BOLD)})
                addView(TextView(this@MainActivity).apply{text=fmtMin(s.totalMin);textSize=30f;setTextColor(primary);setTypeface(typeface,Typeface.BOLD);setPadding(0,dp(5),0,dp(2))})
                addView(TextView(this@MainActivity).apply{text=tf.format(Instant.ofEpochMilli(s.startMs))+" – "+tf.format(Instant.ofEpochMilli(s.endMs))+"  ·  Schlafdauer";textSize=11f;setTextColor(secondary)})
                addView(LinearLayout(this@MainActivity).apply{orientation=LinearLayout.HORIZONTAL;setPadding(0,dp(13),0,dp(8))
                    listOf(s.lightMin to stageLight,s.deepMin to stageDeep,s.remMin to stageRem,s.awakeMin to stageAwake).forEach{q->if(q.first>0)addView(View(this@MainActivity).apply{background=GradientDrawable().apply{cornerRadius=dp(5).toFloat();setColor(q.second)}},LinearLayout.LayoutParams(0,dp(10),q.first.toFloat()).apply{setMargins(0,0,dp(2),0)})}
                })
                addView(TextView(this@MainActivity).apply{text="Leicht "+fmtMin(s.lightMin)+"  ·  Tief "+fmtMin(s.deepMin)+"  ·  REM "+fmtMin(s.remMin)+"  ·  Wach "+fmtMin(s.awakeMin);textSize=10f;setTextColor(secondary)})
            })
        })

        fun stageCard(label:String, minutes:Long, stageTone:Int) {
            val intervals=s.stageSeries.filter{it.stageLabel==label}.sortedBy{it.startMs}
            val pct=((minutes*100f)/s.totalMin.coerceAtLeast(1)).toInt()
            sleepCard.addView(MaterialCardView(this).apply {
                radius=dp(24).toFloat();strokeWidth=0;setCardBackgroundColor(Color.TRANSPARENT);cardElevation=0f;elevation=0f
                background=GradientDrawable().apply{cornerRadius=dp(24).toFloat();setColor(if(light) Color.argb(designGlassAlpha(),72,88,112) else Color.argb(190,9,15,31));setStroke(dp(2),Color.argb(designNeonAlpha(255),Color.red(stageTone),Color.green(stageTone),Color.blue(stageTone)))}
                layoutParams=LinearLayout.LayoutParams(-1,-2).apply{setMargins(0,0,0,dp(12))}
                addView(LinearLayout(this@MainActivity).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(18),dp(15),dp(18),dp(15))
                    addView(LinearLayout(this@MainActivity).apply{orientation=LinearLayout.HORIZONTAL;gravity=android.view.Gravity.CENTER_VERTICAL
                        addView(TextView(this@MainActivity).apply{text=label.uppercase();textSize=11f;letterSpacing=.08f;setTextColor(stageTone);setTypeface(typeface,Typeface.BOLD)},LinearLayout.LayoutParams(0,-2,1f))
                        addView(TextView(this@MainActivity).apply{text=pct.toString()+" %";textSize=18f;setTextColor(stageTone);setTypeface(typeface,Typeface.BOLD)})
                    })
                    addView(TextView(this@MainActivity).apply{text=fmtMin(minutes);textSize=27f;setTextColor(primary);setTypeface(typeface,Typeface.BOLD);setPadding(0,dp(3),0,dp(2))})
                    addView(TextView(this@MainActivity).apply{text=intervals.size.toString()+" "+if(intervals.size==1)"Abschnitt" else "Abschnitte";textSize=10f;setTextColor(secondary);setPadding(0,0,0,dp(9))})
                    addView(LinearLayout(this@MainActivity).apply{orientation=LinearLayout.HORIZONTAL
                        val duration=(s.endMs-s.startMs).coerceAtLeast(1);var cursor=s.startMs
                        fun seg(ms:Long,active:Boolean)=View(this@MainActivity).apply{if(active)background=GradientDrawable().apply{cornerRadius=dp(5).toFloat();setColor(stageTone)};layoutParams=LinearLayout.LayoutParams(0,dp(if(active)44 else 1),(ms.toFloat()/duration).coerceAtLeast(.001f)).apply{gravity=android.view.Gravity.CENTER_VERTICAL;setMargins(if(active)dp(1) else 0,0,if(active)dp(1) else 0,0)}}
                        intervals.forEach{st->if(st.startMs>cursor)addView(seg(st.startMs-cursor,false));addView(seg(st.endMs-st.startMs,true));cursor=st.endMs};if(cursor<s.endMs)addView(seg(s.endMs-cursor,false))
                    })
                    addView(LinearLayout(this@MainActivity).apply{orientation=LinearLayout.HORIZONTAL;setPadding(0,dp(7),0,0)
                        addView(TextView(this@MainActivity).apply{text=tf.format(Instant.ofEpochMilli(s.startMs));textSize=9f;setTextColor(timeColor);layoutParams=LinearLayout.LayoutParams(0,-2,1f)})
                        addView(TextView(this@MainActivity).apply{text=tf.format(Instant.ofEpochMilli(s.startMs+(s.endMs-s.startMs)/2));textSize=9f;gravity=android.view.Gravity.CENTER;setTextColor(timeColor);layoutParams=LinearLayout.LayoutParams(0,-2,1f)})
                        addView(TextView(this@MainActivity).apply{text=tf.format(Instant.ofEpochMilli(s.endMs));textSize=9f;gravity=android.view.Gravity.END;setTextColor(timeColor);layoutParams=LinearLayout.LayoutParams(0,-2,1f)})
                    })
                })
            })
        }
        stageCard("Leicht",s.lightMin,stageLight)
        stageCard("Tief",s.deepMin,stageDeep)
        stageCard("REM",s.remMin,stageRem)
        stageCard("Wach",s.awakeMin,stageAwake)
    }

    private fun showMetricDetail(label: String, icon: String, tone: Int, s: SleepSummary) {
        pageTitle.text = "Gesundheitswerte"
        pageSubtitle.text = "Berühren & entlang der Kurven fahren"
        actionsTitle.visibility=View.GONE; actionsBox.visibility=View.GONE
        sleepCard.removeAllViews(); sleepCard.background=null
        val d=resources.displayMetrics.density; fun px(v:Int)=(v*d).toInt()
        val theme=getSharedPreferences("sleepsync_ui",MODE_PRIVATE).getString("theme","dark")?:"dark"
        val sysDark=(resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK)==android.content.res.Configuration.UI_MODE_NIGHT_YES
        val light=theme=="light" || (theme=="system" && !sysDark)
        sleepCard.addView(TextView(this).apply {
            text="‹  Zurück zur Übersicht"; textSize=12f; setTextColor(accent2); setPadding(px(2),px(8),0,px(12))
            setOnClickListener { showOverview() }
        })
        fun chartCard(name:String, glyph:String, color:Int, value:String, points:List<MetricPoint>):View {
            val tokenColor=when(name){"Puls"->designColor("heart",Color.rgb(255,82,126));"SpO₂"->designColor("spo2",Color.rgb(44,205,255));"Atmung"->designColor("resp",Color.rgb(80,225,184));"HRV"->designColor("hrv",Color.rgb(213,96,255));else->color}
            return MaterialCardView(this).apply {
                radius=px(24).toFloat(); strokeWidth=px(2); strokeColor=tokenColor
                // Important: CardView's own semi-transparent surface is rendered as a rectangular
                // compatibility layer on some devices. Keep the card surface transparent and put
                // the rounded glass drawable on the card itself instead.
                setCardBackgroundColor(Color.TRANSPARENT); cardElevation=0f; elevation=0f
                background=GradientDrawable().apply {
                    cornerRadius=px(24).toFloat()
                    setColor(if(light) Color.argb(designGlassAlpha(),72,88,112) else Color.argb(188,9,15,31))
                    setStroke(px(2),Color.argb(designNeonAlpha(255),Color.red(tokenColor),Color.green(tokenColor),Color.blue(tokenColor)))
                }
                layoutParams=LinearLayout.LayoutParams(-1,-2).apply { setMargins(0,0,0,px(14)) }
                addView(LinearLayout(this@MainActivity).apply {
                    orientation=LinearLayout.VERTICAL; setPadding(px(16),px(15),px(16),px(12))
                    addView(LinearLayout(this@MainActivity).apply {
                        orientation=LinearLayout.HORIZONTAL; gravity=android.view.Gravity.CENTER_VERTICAL
                        addView(TextView(this@MainActivity).apply { text=glyph+"  "+name.uppercase(); textSize=12f; letterSpacing=.08f; setTextColor(tokenColor); setTypeface(typeface,Typeface.BOLD); layoutParams=LinearLayout.LayoutParams(0,-2,1f) })
                        addView(TextView(this@MainActivity).apply { text=value; textSize=22f; setTextColor(Color.WHITE); setTypeface(typeface,Typeface.BOLD); setShadowLayer(px(7).toFloat(),0f,0f,Color.argb(75,Color.red(tokenColor),Color.green(tokenColor),Color.blue(tokenColor))) })
                    })
                    if(points.isNotEmpty()) {
                        addView(TextView(this@MainActivity).apply {
                            text="●  "+points.size.toString()+" Messpunkte  ·  Garmin"
                            textSize=9f; setTextColor(Color.argb(175,Color.red(tokenColor),Color.green(tokenColor),Color.blue(tokenColor))); setPadding(0,px(4),0,0)
                        })
                        val min=points.minOf { it.value }; val max=points.maxOf { it.value }
                        fun fv(v:Double)=if(kotlin.math.abs(v-kotlin.math.round(v))<0.05) kotlin.math.round(v).toInt().toString() else String.format(java.util.Locale.GERMANY,"%.1f",v)
                        val unit=when(name) { "Puls"->"bpm"; "SpO₂"->"%"; "Atmung"->"/min"; "HRV"->"ms"; else->"" }
                        addView(TextView(this@MainActivity).apply {
                            text="MIN  "+fv(min)+" "+unit+"     •     MAX  "+fv(max)+" "+unit
                            textSize=10f; letterSpacing=.05f; setTextColor(if(light) Color.rgb(220,228,245) else Color.rgb(154,164,191)); setPadding(0,px(7),0,px(2))
                        })
                    }
                    addView(SleepMetricChartView(this@MainActivity,tokenColor,name,s.startMs,s.endMs,points))
                })
            }
        }
        fun n(v:Double?,suffix:String)=v?.let { String.format(java.util.Locale.GERMANY,"%.1f %s",it,suffix) } ?: "–"
        val cards=listOf(
            chartCard("Puls","❤️",Color.rgb(255,82,126),n(s.avgHr,"bpm"),s.heartRateSeries),
            chartCard("SpO₂","🩸",Color.rgb(44,205,255),n(s.avgSpo2,"%"),s.spo2Series),
            chartCard("Atmung","🫁",Color.rgb(80,225,184),n(s.avgResp,"/min"),s.respirationSeries),
            chartCard("HRV","💓",Color.rgb(213,96,255),n(s.avgHrv,"ms"),s.hrvSeries)
        )
        sleepCard.addView(TextView(this).apply {
            text="NACHTVERLAUF  ·  "+java.time.format.DateTimeFormatter.ofPattern("HH:mm").withZone(java.time.ZoneId.systemDefault()).format(java.time.Instant.ofEpochMilli(s.startMs))+" – "+java.time.format.DateTimeFormatter.ofPattern("HH:mm").withZone(java.time.ZoneId.systemDefault()).format(java.time.Instant.ofEpochMilli(s.endMs))
            textSize=10f; letterSpacing=.08f; setTextColor(if(light) Color.rgb(210,222,244) else Color.rgb(112,122,153)); setPadding(px(2),0,0,px(10))
        })
        cards.forEach { sleepCard.addView(it) }
        cards[listOf("Puls","SpO₂","Atmung","HRV").indexOf(label).coerceAtLeast(0)].post { cards[listOf("Puls","SpO₂","Atmung","HRV").indexOf(label).coerceAtLeast(0)].requestFocus() }
    }

    private inner class NeonGlowFrame(context: android.content.Context, private val tone:Int, private val radiusPx:Float): android.widget.FrameLayout(context) {
        private val halo=android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            style=android.graphics.Paint.Style.STROKE; strokeWidth=7f*resources.displayMetrics.density
            color=android.graphics.Color.argb(designGlowAlpha(190),android.graphics.Color.red(tone),android.graphics.Color.green(tone),android.graphics.Color.blue(tone))
            maskFilter=android.graphics.BlurMaskFilter(15f*resources.displayMetrics.density,android.graphics.BlurMaskFilter.Blur.NORMAL)
        }
        private val core=android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            style=android.graphics.Paint.Style.STROKE; strokeWidth=2.0f*resources.displayMetrics.density; color=android.graphics.Color.argb(245,android.graphics.Color.red(tone),android.graphics.Color.green(tone),android.graphics.Color.blue(tone))
            maskFilter=android.graphics.BlurMaskFilter(2.2f*resources.displayMetrics.density,android.graphics.BlurMaskFilter.Blur.NORMAL)
        }
        init { setWillNotDraw(false); setLayerType(android.view.View.LAYER_TYPE_SOFTWARE,null); clipChildren=false; clipToPadding=false }
        override fun onDraw(c:android.graphics.Canvas) {
            val inset=9f*resources.displayMetrics.density
            c.drawRoundRect(inset,inset,width-inset,height-inset,radiusPx,radiusPx,halo)
            c.drawRoundRect(inset,inset,width-inset,height-inset,radiusPx,radiusPx,core)
            super.onDraw(c)
        }
    }

    private fun neonWrap(view:android.view.View,tone:Int,radius:Int,dp:(Int)->Int):android.view.View =
        NeonGlowFrame(this,tone,dp(radius).toFloat()).apply {
            setPadding(dp(9),dp(9),dp(9),dp(9))
            addView(view,android.widget.FrameLayout.LayoutParams(-1,-1))
        }

    private fun neonGlowGlass(radius:Int, tone:Int, dp:(Int)->Int)=LayerDrawable(arrayOf(
        // Light-theme overview cards: same slate-glass DNA as Verlauf/Kalender/Einstellungen.
        // Keep enough contrast for white text while letting the wallpaper breathe through.
        GradientDrawable(GradientDrawable.Orientation.TL_BR,intArrayOf(Color.argb(designGlassAlpha(),72,88,112),Color.argb((designGlassAlpha()*.92f).toInt(),66,80,108),Color.argb((designGlassAlpha()*.96f).toInt(),76,82,112))).apply { cornerRadius=dp(radius).toFloat(); setStroke(dp(7),Color.argb(designGlowAlpha(28),Color.red(tone),Color.green(tone),Color.blue(tone))) },
        GradientDrawable().apply { cornerRadius=dp(radius).toFloat(); setColor(Color.TRANSPARENT); setStroke(dp(5),Color.argb(designGlowAlpha(112),Color.red(tone),Color.green(tone),Color.blue(tone))) },
        GradientDrawable().apply { cornerRadius=dp(radius).toFloat(); setColor(Color.TRANSPARENT); setStroke(dp(2),Color.argb(designNeonAlpha(255),Color.red(tone),Color.green(tone),Color.blue(tone))) }
    ))

    private fun heroNeonGlass(radius:Int, dp:(Int)->Int)=LayerDrawable(arrayOf(
        GradientDrawable(GradientDrawable.Orientation.TL_BR,intArrayOf(Color.argb(198,5,22,44),Color.argb(188,15,26,56),Color.argb(190,36,13,54))).apply { cornerRadius=dp(radius).toFloat(); setStroke(dp(8),Color.argb(34,50,225,255)) },
        GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,intArrayOf(Color.argb(110,35,225,255),Color.argb(18,35,225,255),Color.argb(18,235,70,255),Color.argb(105,235,70,255))).apply { cornerRadius=dp(radius).toFloat(); setStroke(dp(5),Color.argb(110,100,225,255)) },
        GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,intArrayOf(Color.rgb(65,235,255),Color.rgb(70,225,255),Color.rgb(220,75,255))).apply { cornerRadius=dp(radius).toFloat(); setColor(Color.TRANSPARENT); setStroke(dp(2),Color.rgb(80,235,255)) }
    ))

    private fun overviewNeonGlass(radius:Int, tone:Int, dp:(Int)->Int)=LayerDrawable(arrayOf(
        GradientDrawable(GradientDrawable.Orientation.TL_BR,intArrayOf(Color.argb(designGlassAlpha(210),18,28,48),Color.argb((designGlassAlpha(200)*.96f).toInt(),27,38,62),Color.argb(designGlassAlpha(206),16,23,44))).apply { cornerRadius=dp(radius).toFloat(); setStroke(dp(5),Color.argb(designGlowAlpha(38),Color.red(tone),Color.green(tone),Color.blue(tone))) },
        GradientDrawable().apply { cornerRadius=dp(radius).toFloat(); setColor(Color.TRANSPARENT); setStroke(dp(2),Color.argb(designNeonAlpha(255),Color.red(tone),Color.green(tone),Color.blue(tone))) }
    ))

    private fun dashboardGlass(radius:Int, dp:(Int)->Int)=LayerDrawable(arrayOf(
        GradientDrawable(GradientDrawable.Orientation.TL_BR,intArrayOf(Color.argb(72,255,255,255),Color.argb(22,190,218,245),Color.argb(16,165,145,218),Color.argb(38,245,214,238))).apply { cornerRadius=dp(radius).toFloat(); setStroke(dp(1),Color.argb(72,120,164,205)) },
        GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,intArrayOf(Color.argb(190,255,255,255),Color.argb(58,255,255,255),Color.TRANSPARENT,Color.argb(48,70,105,165))).apply { cornerRadius=dp(radius).toFloat(); setStroke(dp(1),Color.argb(138,255,255,255)) },
        GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,intArrayOf(Color.argb(145,230,255,255),Color.argb(26,255,255,255),Color.TRANSPARENT,Color.argb(24,175,155,245),Color.argb(92,244,204,245))).apply { cornerRadius=dp(radius).toFloat() },
        GradientDrawable(GradientDrawable.Orientation.BL_TR,intArrayOf(Color.argb(72,72,132,205),Color.TRANSPARENT,Color.argb(68,245,190,225))).apply { cornerRadius=dp(radius).toFloat() }
    ))

    private fun renderDashboard(s: SleepSummary) {
        lastSummary = s
        val d = resources.displayMetrics.density
        fun dp(v: Int) = (v * d).toInt()
        fun fmt(m: Long) = "${m / 60} h ${m % 60} min"
        fun num(v: Double?, suffix: String) = v?.let { String.format(java.util.Locale.GERMANY, "%.1f %s", it, suffix) } ?: "–"
        fun row(icon: String, title: String, value: String): TextView = TextView(this).apply {
            text = "$icon  $title\n     $value"; textSize = 15f; setPadding(0, dp(7), 0, dp(7))
        }
        val tf = java.time.format.DateTimeFormatter.ofPattern("HH:mm").withZone(java.time.ZoneId.systemDefault())
        sleepCard.removeAllViews()
        sleepCard.background = null
        val theme=getSharedPreferences("sleepsync_ui",MODE_PRIVATE).getString("theme","dark")?:"dark"
        val sysDark=(resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK)==android.content.res.Configuration.UI_MODE_NIGHT_YES
        val light=theme=="light" || (theme=="system" && !sysDark)
        fun glass(vararg rgb:Int)=if(light) Color.argb(designGlassAlpha(),72,88,112) else Color.rgb(rgb[0],rgb[1],rgb[2])
        val historical=viewingHistoryNight
        sleepCard.addView(LinearLayout(this).apply {
            orientation=LinearLayout.HORIZONTAL; gravity=android.view.Gravity.CENTER_VERTICAL; setPadding(dp(2),0,dp(2),dp(8))
            addView(TextView(this@MainActivity).apply {
                text=if(historical) "HISTORISCHE NACHT" else "LETZTE NACHT"; textSize=11f; letterSpacing=.16f; setTextColor(if(light) Color.WHITE else accent2); setTypeface(typeface,Typeface.BOLD); if(light) setShadowLayer(dp(3).toFloat(),0f,dp(1).toFloat(),Color.BLACK)
                layoutParams=LinearLayout.LayoutParams(0,-2,1f)
            })
            addView(TextView(this@MainActivity).apply {
                text="${tf.format(java.time.Instant.ofEpochMilli(s.startMs))} – ${tf.format(java.time.Instant.ofEpochMilli(s.endMs))}"; textSize=12f; setTextColor(if(light) Color.rgb(224,232,250) else Color.rgb(151,158,190))
            })
        })
        val quality = ((s.lightMin + s.deepMin + s.remMin) * 100 / s.totalMin.coerceAtLeast(1)).toInt().coerceIn(0,100)
        sleepCard.addView(LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = android.view.Gravity.CENTER_VERTICAL; setPadding(dp(18),dp(19),dp(18),dp(19)); elevation=if(light) dp(10).toFloat() else dp(8).toFloat(); translationZ=if(light) dp(2).toFloat() else 0f; if(light) outlineProvider=android.view.ViewOutlineProvider.BACKGROUND
            background = if(light) GradientDrawable(GradientDrawable.Orientation.TL_BR,intArrayOf(Color.argb(designGlassAlpha(),72,88,112),Color.argb((designGlassAlpha()*.92f).toInt(),58,70,104))).apply { cornerRadius=dp(30).toFloat(); setStroke(dp(2),accent2) } else GradientDrawable(GradientDrawable.Orientation.TL_BR,intArrayOf(Color.rgb(58,25,105),Color.rgb(24,25,72),Color.rgb(6,55,66))).apply { cornerRadius=dp(30).toFloat(); setStroke(dp(1),Color.rgb(107,82,190)) }
            addView(LinearLayout(this@MainActivity).apply {
                orientation=LinearLayout.VERTICAL; layoutParams=LinearLayout.LayoutParams(0,-2,1f)
                addView(LinearLayout(this@MainActivity).apply { orientation=LinearLayout.HORIZONTAL; gravity=android.view.Gravity.CENTER_VERTICAL
                    addView(View(this@MainActivity).apply { background=GradientDrawable().apply { shape=GradientDrawable.OVAL; setColor(accent2) }; layoutParams=LinearLayout.LayoutParams(dp(7),dp(7)).apply { marginEnd=dp(7) } })
                    addView(TextView(this@MainActivity).apply { text="GESAMTSCHLAF"; textSize=10f; letterSpacing=.14f; setTextColor(if(light) Color.rgb(205,220,255) else Color.rgb(184,174,224)); setTypeface(typeface,Typeface.BOLD) })
                })
                addView(TextView(this@MainActivity).apply { text=fmt(s.totalMin); textSize=42f; setTextColor(Color.WHITE); setTypeface(typeface,Typeface.BOLD); setPadding(0,dp(2),0,0) })
                addView(TextView(this@MainActivity).apply { text="☾  Schlafzeit"; textSize=12f; setTextColor(if(light) Color.rgb(205,225,245) else Color.rgb(151,210,225)); setPadding(0,dp(2),0,0) })
            })
            addView(TextView(this@MainActivity).apply {
                text = "$quality%\nEFFIZIENZ"; gravity = android.view.Gravity.CENTER; textSize = 12f; setTypeface(typeface, Typeface.BOLD)
                setTextColor(Color.WHITE); setPadding(dp(14),dp(12),dp(14),dp(12))
                background = if(light) overviewNeonGlass(22,accent2,::dp) else GradientDrawable(GradientDrawable.Orientation.TL_BR,intArrayOf(Color.rgb(22,94,120),Color.rgb(77,45,145))).apply { cornerRadius=dp(22).toFloat(); setStroke(dp(1),Color.rgb(83,205,229)) }
            })
        })
        sleepCard.addView(TextView(this).apply { text="SCHLAFVERLAUF"; textSize=11f; letterSpacing=.14f; setTextColor(if(light) Color.WHITE else stageLight); setTypeface(typeface,Typeface.BOLD); if(light) setShadowLayer(dp(3).toFloat(),0f,dp(1).toFloat(),Color.BLACK); setPadding(dp(4),dp(18),0,dp(8)) })
        sleepCard.addView(sleepStageStrip(s))
        sleepCard.addView(LinearLayout(this).apply {
            orientation=LinearLayout.HORIZONTAL; setPadding(dp(2),dp(5),dp(2),0)
            addView(TextView(this@MainActivity).apply { text="☾  "+tf.format(java.time.Instant.ofEpochMilli(s.startMs)); textSize=10f; setTextColor(if(light) Color.rgb(245,248,255) else Color.rgb(118,128,161)); if(light) setShadowLayer(dp(2).toFloat(),0f,0f,Color.argb(190,0,0,0)); layoutParams=LinearLayout.LayoutParams(0,-2,1f) })
            addView(TextView(this@MainActivity).apply { text=tf.format(java.time.Instant.ofEpochMilli(s.endMs))+"  ☀"; textSize=10f; setTextColor(if(light) Color.rgb(245,248,255) else Color.rgb(118,128,161)); if(light) setShadowLayer(dp(2).toFloat(),0f,0f,Color.argb(190,0,0,0)) })
        })
        sleepCard.addView(LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = android.view.Gravity.CENTER_VERTICAL; setPadding(0,dp(9),0,dp(4))
            fun legend(name: String, tone: Int) = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.HORIZONTAL; gravity = android.view.Gravity.CENTER_VERTICAL
                addView(View(this@MainActivity).apply {
                    background = GradientDrawable().apply { shape=GradientDrawable.OVAL; setColor(tone) }
                    layoutParams=LinearLayout.LayoutParams(dp(7),dp(7)).apply { setMargins(0,0,dp(5),0) }
                })
                addView(TextView(this@MainActivity).apply { text=name; textSize=11f; setTextColor(if(light) Color.WHITE else Color.rgb(185,190,215)); setTypeface(typeface,Typeface.BOLD); if(light) setShadowLayer(dp(2).toFloat(),0f,0f,Color.argb(190,0,0,0)) })
                layoutParams=LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,1f)
            }
            addView(legend("Leicht",stageLight)); addView(legend("Tief",stageDeep)); addView(legend("REM",stageRem)); addView(legend("Wach",stageAwake))
        })
        sleepCard.addView(View(this).apply {
            background=GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,intArrayOf(Color.TRANSPARENT,Color.rgb(74,64,116),Color.TRANSPARENT))
            layoutParams=LinearLayout.LayoutParams(-1,dp(1)).apply { setMargins(dp(12),dp(7),dp(12),dp(3)) }
        })
        val sleepOnly = (s.lightMin + s.deepMin + s.remMin).coerceAtLeast(1)
        val deepPct = (s.deepMin * 100 / sleepOnly).toInt()
        val remPct = (s.remMin * 100 / sleepOnly).toInt()
        sleepCard.addView(MaterialCardView(this).apply {
            radius=dp(20).toFloat(); strokeWidth=if(light) dp(2) else dp(1); strokeColor=if(light) stageRem else Color.rgb(116,91,207); setCardBackgroundColor(Color.TRANSPARENT); if(light){ background=GradientDrawable().apply { cornerRadius=dp(20).toFloat(); setColor(Color.argb(designGlassAlpha(),72,88,112)); setStroke(dp(2),stageLight) }; cardElevation=dp(7).toFloat(); outlineAmbientShadowColor=stageLight; outlineSpotShadowColor=stageLight } else setCardBackgroundColor(Color.rgb(19,15,39))
            addView(LinearLayout(this@MainActivity).apply {
                orientation=LinearLayout.HORIZONTAL; gravity=android.view.Gravity.CENTER_VERTICAL; setPadding(dp(14),dp(12),dp(14),dp(12))
                addView(TextView(this@MainActivity).apply { text="〽  SCHLAF-\nARCHITEKTUR"; textSize=10f; letterSpacing=.10f; setTextColor(if(light) Color.rgb(225,235,255) else Color.rgb(171,155,220)); setTypeface(typeface,Typeface.BOLD); layoutParams=LinearLayout.LayoutParams(0,-2,1f) })
                addView(TextView(this@MainActivity).apply { text="${(s.lightMin*100/s.totalMin.coerceAtLeast(1)).toInt()}%\nLEICHT"; gravity=android.view.Gravity.CENTER; textSize=13f; setTextColor(if(light) Color.WHITE else stageLight); setTypeface(typeface,Typeface.BOLD); if(light) setShadowLayer(dp(2).toFloat(),0f,0f,stageLight); layoutParams=LinearLayout.LayoutParams(dp(62),-2) })
                addView(TextView(this@MainActivity).apply { text="$deepPct%\nTIEF"; gravity=android.view.Gravity.CENTER; textSize=13f; setTextColor(if(light) Color.WHITE else stageDeep); setTypeface(typeface,Typeface.BOLD); if(light) setShadowLayer(dp(2).toFloat(),0f,0f,stageDeep); layoutParams=LinearLayout.LayoutParams(dp(62),-2) })
                addView(TextView(this@MainActivity).apply { text="$remPct%\nREM"; gravity=android.view.Gravity.CENTER; textSize=13f; setTextColor(if(light) Color.WHITE else stageRem); setTypeface(typeface,Typeface.BOLD); if(light) setShadowLayer(dp(2).toFloat(),0f,0f,stageRem); layoutParams=LinearLayout.LayoutParams(dp(62),-2) })
            })
            layoutParams=LinearLayout.LayoutParams(-1,-2).apply { setMargins(0,dp(7),0,dp(7)) }
        })
        sleepCard.addView(LinearLayout(this).apply {
            orientation=LinearLayout.HORIZONTAL; gravity=android.view.Gravity.CENTER_VERTICAL; setPadding(dp(4),dp(10),0,dp(2))
            addView(TextView(this@MainActivity).apply { text="SCHLAFPHASEN"; textSize=11f; letterSpacing=.14f; setTextColor(if(light) Color.WHITE else stageRem); setTypeface(typeface,Typeface.BOLD); if(light) setShadowLayer(dp(3).toFloat(),0f,dp(1).toFloat(),Color.BLACK); layoutParams=LinearLayout.LayoutParams(0,-2,1f) })
            addView(TextView(this@MainActivity).apply { text=fmt(s.lightMin+s.deepMin+s.remMin); textSize=10f; setTypeface(typeface,Typeface.BOLD); setTextColor(stageRem); setPadding(dp(10),dp(4),dp(10),dp(4)); background=GradientDrawable().apply { cornerRadius=dp(13).toFloat(); setColor(if(light) Color.argb(220,240,230,255) else Color.rgb(35,19,54)); setStroke(dp(1),Color.rgb(86,48,119)) } })
        })
        sleepCard.addView(LinearLayout(this).apply {
            orientation=LinearLayout.HORIZONTAL; setPadding(dp(4),dp(2),dp(4),dp(3))
            fun phase(label:String,minutes:Long,tone:Int)=TextView(this@MainActivity).apply {
                text="$label  ${(minutes*100/s.totalMin.coerceAtLeast(1)).toInt()}%"; textSize=10f; setTextColor(if(light) Color.WHITE else tone); setTypeface(typeface,Typeface.BOLD); if(light) setShadowLayer(dp(3).toFloat(),0f,dp(1).toFloat(),Color.BLACK)
                gravity=android.view.Gravity.CENTER; layoutParams=LinearLayout.LayoutParams(0,-2,1f)
            }
            addView(phase("LEICHT",s.lightMin,stageLight)); addView(phase("TIEF",s.deepMin,stageDeep)); addView(phase("REM",s.remMin,stageRem)); addView(phase("WACH",s.awakeMin,stageAwake))
        })
        val stages = GridLayout(this).apply {
            columnCount = 2
            setPadding(0, dp(6), 0, dp(8))
            addView(metricCard("🌙","Leicht",fmt(s.lightMin), onClick={ showAllStageTimelines(s) }, sleep=s))
            addView(metricCard("🌑","Tief",fmt(s.deepMin), onClick={ showAllStageTimelines(s) }, sleep=s))
            addView(metricCard("🧠","REM",fmt(s.remMin), onClick={ showAllStageTimelines(s) }, sleep=s))
            addView(metricCard("👀","Wach",fmt(s.awakeMin), onClick={ showAllStageTimelines(s) }, sleep=s))
        }
        sleepCard.addView(stages)
        sleepCard.addView(View(this).apply {
            background=GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,intArrayOf(Color.TRANSPARENT,Color.rgb(33,104,122),Color.rgb(84,51,133),Color.TRANSPARENT))
            layoutParams=LinearLayout.LayoutParams(-1,dp(1)).apply { setMargins(dp(18),dp(15),dp(18),dp(3)) }
        })
        sleepCard.addView(LinearLayout(this).apply {
            orientation=LinearLayout.HORIZONTAL; gravity=android.view.Gravity.CENTER_VERTICAL; setPadding(dp(4),dp(8),dp(4),dp(4))
            addView(TextView(this@MainActivity).apply { text="GESUNDHEITSWERTE"; textSize=11f; letterSpacing=.14f; setTextColor(if(light) Color.WHITE else accent2); setTypeface(typeface,Typeface.BOLD); if(light) setShadowLayer(dp(3).toFloat(),0f,dp(1).toFloat(),Color.BLACK); layoutParams=LinearLayout.LayoutParams(0,-2,1f) })
            addView(TextView(this@MainActivity).apply { text="LIVE"; textSize=8f; letterSpacing=.12f; setTextColor(if(light) Color.rgb(18,121,92) else Color.rgb(76,225,169)); setPadding(dp(7),dp(3),dp(7),dp(3)); background=GradientDrawable().apply { cornerRadius=dp(10).toFloat(); setColor(if(light) Color.argb(205,218,250,239) else Color.rgb(7,34,28)); if(light) setStroke(dp(1),Color.rgb(76,205,164)) } })
            addView(View(this@MainActivity).apply { layoutParams=LinearLayout.LayoutParams(dp(7),dp(1)) })
            addView(TextView(this@MainActivity).apply { text="GARMIN  ●"; textSize=9f; letterSpacing=.08f; setTextColor(if(light) Color.rgb(15,126,98) else Color.rgb(86,230,166)); setTypeface(typeface,Typeface.BOLD); setPadding(dp(9),dp(4),dp(9),dp(4)); background=GradientDrawable().apply { cornerRadius=dp(13).toFloat(); setColor(if(light) Color.argb(210,220,249,243) else Color.rgb(8,37,31)); setStroke(dp(1),if(light) Color.rgb(63,194,166) else Color.rgb(24,95,73)) } })
        })
        val vitals = GridLayout(this).apply {
            columnCount = 2
            addView(metricCard("❤️","Puls",num(s.avgHr,"bpm"), onClick={ showMetricDetail("Puls","❤️",Color.rgb(255,82,126),s) }, series=s.heartRateSeries))
            addView(metricCard("🩸","SpO₂","Ø ${num(s.avgSpo2,"%")}\nMin. ${num(s.minSpo2,"%")}", onClick={ showMetricDetail("SpO₂","🩸",Color.rgb(44,205,255),s) }, series=s.spo2Series))
            addView(metricCard("🫁","Atmung","Ø ${num(s.avgResp,"/min")}\nMin. ${num(s.minResp,"/min")}", onClick={ showMetricDetail("Atmung","🫁",Color.rgb(80,225,184),s) }, series=s.respirationSeries))
            addView(metricCard("💓","HRV",num(s.avgHrv,"ms"), onClick={ showMetricDetail("HRV","💓",Color.rgb(213,96,255),s) }, series=s.hrvSeries))
        }
        sleepCard.addView(vitals)
        sleepCard.addView(MaterialCardView(this).apply {
            radius=dp(18).toFloat(); cardElevation=if(light) dp(10).toFloat() else 0f; strokeWidth=if(light) 0 else dp(1); strokeColor=Color.rgb(24,94,105); setCardBackgroundColor(Color.TRANSPARENT); if(light) background=GradientDrawable().apply { cornerRadius=dp(18).toFloat(); setColor(Color.argb(designGlassAlpha(),72,88,112)); setStroke(dp(1),Color.rgb(76,225,169)) } else setCardBackgroundColor(Color.rgb(7,25,31))
            layoutParams=LinearLayout.LayoutParams(-1,-2).apply { setMargins(0,dp(12),0,0) }
            addView(LinearLayout(this@MainActivity).apply {
                orientation=LinearLayout.HORIZONTAL; gravity=android.view.Gravity.CENTER_VERTICAL; setPadding(dp(14),dp(10),dp(14),dp(10))
                addView(TextView(this@MainActivity).apply { text="●"; textSize=12f; setTextColor(Color.rgb(76,225,169)); layoutParams=LinearLayout.LayoutParams(dp(24),-2) })
                addView(TextView(this@MainActivity).apply { text="Messwerte vollständig synchronisiert"; textSize=11f; setTextColor(if(light) Color.rgb(47,112,116) else Color.rgb(160,210,214)); layoutParams=LinearLayout.LayoutParams(0,-2,1f) })
                addView(TextView(this@MainActivity).apply { text="GARMIN"; textSize=9f; letterSpacing=.12f; setTypeface(typeface,Typeface.BOLD); setTextColor(accent2) })
            })
        })
        sleepCard.addView(LinearLayout(this).apply {
            orientation=LinearLayout.HORIZONTAL; gravity=android.view.Gravity.CENTER_VERTICAL; setPadding(dp(4),dp(16),0,dp(7))
            addView(TextView(this@MainActivity).apply { text="NACHT-INSIGHT"; textSize=11f; letterSpacing=.14f; setTextColor(stageRem); setTypeface(typeface,Typeface.BOLD); layoutParams=LinearLayout.LayoutParams(0,-2,1f) })
            addView(TextView(this@MainActivity).apply {
                text=when { quality>=90 -> "AUSGEZEICHNET"; quality>=80 -> "GUT"; else -> "IM BLICK BEHALTEN" }
                textSize=9f; letterSpacing=.08f; setTypeface(typeface,Typeface.BOLD); setTextColor(if(light) Color.rgb(22,105,145) else accent2); setPadding(dp(10),dp(5),dp(10),dp(5))
                background=GradientDrawable().apply { cornerRadius=dp(14).toFloat(); setColor(if(light) Color.argb(215,224,245,255) else Color.rgb(8,34,47)); setStroke(dp(1),if(light) Color.rgb(66,177,214) else Color.rgb(28,112,137)) }
            })
        })
        sleepCard.addView(MaterialCardView(this).apply {
            radius=dp(24).toFloat(); cardElevation=if(light) dp(14).toFloat() else dp(2).toFloat(); strokeWidth=if(light) 0 else dp(1); strokeColor=Color.rgb(81,62,137); setCardBackgroundColor(Color.TRANSPARENT); if(light) background=GradientDrawable().apply { cornerRadius=dp(24).toFloat(); setColor(Color.argb(designGlassAlpha(),72,88,112)); setStroke(dp(1),stageRem) } else setCardBackgroundColor(Color.rgb(19,15,39))
            addView(LinearLayout(this@MainActivity).apply {
                orientation=LinearLayout.HORIZONTAL; gravity=android.view.Gravity.CENTER_VERTICAL; setPadding(dp(15),dp(13),dp(15),dp(13))
                addView(TextView(this@MainActivity).apply {
                    text="✦"; textSize=20f; gravity=android.view.Gravity.CENTER; setTextColor(accent2); setPadding(0,dp(5),0,dp(5))
                    background=if(light) GradientDrawable(GradientDrawable.Orientation.TL_BR,intArrayOf(Color.argb(238,245,252,255),Color.argb(230,240,235,255))).apply { shape=GradientDrawable.OVAL; setStroke(dp(1),Color.rgb(116,126,224)) } else GradientDrawable(GradientDrawable.Orientation.TL_BR,intArrayOf(Color.rgb(17,62,78),Color.rgb(55,29,91))).apply { shape=GradientDrawable.OVAL; setStroke(dp(1),Color.rgb(48,151,177)) }
                    layoutParams=LinearLayout.LayoutParams(dp(36),dp(36)).apply { marginEnd=dp(11) }
                })
                addView(TextView(this@MainActivity).apply {
                    layoutParams=LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,1f)
                    text=(if (quality >= 90) "Hohe Schlafeffizienz" else if (quality >= 80) "Solide Schlafeffizienz" else "Schlafeffizienz") + "\n" + "$quality% deiner Bettzeit entfielen auf Schlafphasen."
                    textSize=13f; setTextColor(if(light) Color.WHITE else Color.rgb(220,224,244)); setTypeface(typeface,Typeface.BOLD)
                })
            })
            layoutParams=LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(dp(4),dp(10),dp(4),0) }
        })
    }

    override fun onDestroy() { super.onDestroy(); cancel() }
}

class HealthPermissionRationaleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        DynamicColors.applyToActivityIfAvailable(this)
        super.onCreate(savedInstanceState)
        setContentView(TextView(this).apply { textSize=18f; setPadding(48,80,48,48); text="Garmin Sleep for Tasker liest nur die von dir freigegebenen Health-Connect-Daten, um Schlafdauer, Schlafphasen und zugehörige Messwerte für deine eigene Tasker-Automation auszuwerten. Es werden keine Daten hochgeladen." })
    }
}
