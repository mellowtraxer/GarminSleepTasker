package de.ricci.garminsleep

import androidx.activity.ComponentActivity
import android.os.Bundle
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import java.io.File
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
    private var pulseProgress=0f
    private val pulseAnimator=android.animation.ValueAnimator.ofFloat(0f,1f).apply {
        duration=9000L
        repeatCount=android.animation.ValueAnimator.INFINITE
        interpolator=android.view.animation.LinearInterpolator()
        addUpdateListener { pulseProgress=it.animatedValue as Float; if(touchX<0f && HistoryScrollGate.shouldRender()) invalidate() }
    }
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if(points.size>=2 && android.animation.ValueAnimator.areAnimatorsEnabled()) pulseAnimator.start()
    }
    override fun onDetachedFromWindow() {
        pulseAnimator.cancel()
        super.onDetachedFromWindow()
    }
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
        if(touchX<0f && android.animation.ValueAnimator.areAnimatorsEnabled()){
            val measure=android.graphics.PathMeasure(path,false)
            val length=measure.length
            if(length>0f){
                val d=resources.displayMetrics.density
                val fade=kotlin.math.sin(Math.PI*pulseProgress.toDouble()).toFloat().coerceAtLeast(0f)
                val pos=length*pulseProgress
                val trail=android.graphics.Path()
                measure.getSegment((pos-length*.085f).coerceAtLeast(0f),pos,trail,true)
                val glow=Paint(Paint.ANTI_ALIAS_FLAG).apply{
                    style=Paint.Style.STROKE;strokeCap=Paint.Cap.ROUND;strokeJoin=Paint.Join.ROUND
                    strokeWidth=4.5f*d
                    color=Color.argb((100*fade).toInt(),Color.red(tone),Color.green(tone),Color.blue(tone))
                    setShadowLayer(8f*d,0f,0f,tone)
                }
                c.drawPath(trail,glow)
                glow.clearShadowLayer()
                glow.strokeWidth=2.2f*d
                glow.color=Color.argb((225*fade).toInt(),255,255,255)
                c.drawPath(trail,glow)
                val xy=FloatArray(2)
                if(measure.getPosTan(pos.coerceIn(0f,length),xy,null)){
                    glow.style=Paint.Style.FILL
                    glow.color=Color.argb((190*fade).toInt(),Color.red(tone),Color.green(tone),Color.blue(tone))
                    glow.setShadowLayer(6f*d,0f,0f,tone)
                    c.drawCircle(xy[0],xy[1],2.8f*d,glow)
                    glow.clearShadowLayer()
                    glow.color=Color.argb((245*fade).toInt(),255,255,255)
                    c.drawCircle(xy[0],xy[1],1.3f*d,glow)
                }
            }
        }
        if(touchX>=0){val idx=selectedIndex();val xx=x(idx);val yy=y(points[idx].value);p.color=Color.argb(150,255,255,255);p.strokeWidth=resources.displayMetrics.density;p.style=Paint.Style.STROKE;c.drawLine(xx,0f,xx,h,p);p.style=Paint.Style.FILL;p.color=Color.WHITE;c.drawCircle(xx,yy,resources.displayMetrics.density*5f,p);p.color=tone;c.drawCircle(xx,yy,resources.displayMetrics.density*2.6f,p)}
    }
}


private object HistoryScrollGate {
    @Volatile var scrolling=false
    @Volatile var appVisible=true
    fun shouldRender():Boolean=appVisible && !scrolling
}

private class HistoryCardGlowView(context:android.content.Context, private val tone:Int):View(context){
    var expanded=false
        set(value){field=value;invalidate()}
    private val paint=Paint(Paint.ANTI_ALIAS_FLAG).apply{style=Paint.Style.STROKE}
    private var phase=0f
    private val animator=android.animation.ValueAnimator.ofFloat(0f,1f).apply{
        duration=5700L;repeatCount=android.animation.ValueAnimator.INFINITE
        interpolator=android.view.animation.LinearInterpolator()
        addUpdateListener{phase=it.animatedValue as Float;if(HistoryScrollGate.shouldRender())invalidate()}
    }
    init{setLayerType(LAYER_TYPE_SOFTWARE,null);isClickable=false;isFocusable=false}
    override fun onAttachedToWindow(){super.onAttachedToWindow();if(android.animation.ValueAnimator.areAnimatorsEnabled())animator.start()}
    override fun onDetachedFromWindow(){animator.cancel();super.onDetachedFromWindow()}
    override fun onDraw(canvas:Canvas){
        super.onDraw(canvas)
        if(width<=0||height<=0)return
        val d=resources.displayMetrics.density
        val wave=.5f+.5f*kotlin.math.sin((phase*2f*Math.PI).toFloat())
        val inset=7f*d
        val rect=android.graphics.RectF(inset,inset,width-inset,height-inset)
        val radius=20f*d
        paint.style=Paint.Style.STROKE
        paint.strokeWidth=(if(expanded)2.8f else 2.2f)*d
        paint.color=Color.argb((135+110*wave).toInt(),Color.red(tone),Color.green(tone),Color.blue(tone))
        paint.setShadowLayer((if(expanded)10f+12f*wave else 6f+9f*wave)*d,0f,0f,tone)
        canvas.drawRoundRect(rect,radius,radius,paint)
        paint.clearShadowLayer()
        paint.strokeWidth=1f*d
        paint.color=Color.argb((80+90*wave).toInt(),225,220,255)
        canvas.drawRoundRect(rect,radius,radius,paint)
    }
}

private class HistoryMoonView(context:android.content.Context, private val sleepMinutes:Long, private val tint:Int, private val premiumOrbit:Boolean=false):View(context){
    private var breath=0f
    private val pulse=android.animation.ValueAnimator.ofFloat(0f,1f).apply{
        duration=5400L;repeatCount=android.animation.ValueAnimator.INFINITE
        interpolator=android.view.animation.LinearInterpolator()
        addUpdateListener{breath=it.animatedValue as Float;if(HistoryScrollGate.shouldRender())invalidate()}
    }
    override fun onAttachedToWindow(){super.onAttachedToWindow();if(android.animation.ValueAnimator.areAnimatorsEnabled())pulse.start()}
    override fun onDetachedFromWindow(){pulse.cancel();super.onDetachedFromWindow()}

    private val paint=Paint(Paint.ANTI_ALIAS_FLAG)
    init{setLayerType(LAYER_TYPE_SOFTWARE,null);contentDescription="Schlafdauer-Symbol, keine astronomische Mondphase"}
    override fun onDraw(c:Canvas){
        super.onDraw(c)
        val d=resources.displayMetrics.density
        val r=kotlin.math.min(width,height)*.32f
        val x=width*.5f;val y=height*.5f
        val quality=when{sleepMinutes>=480L->3;sleepMinutes>=360L->2;sleepMinutes>=240L->1;else->0}
        val glow=when(quality){3->Color.rgb(126,210,255);2->Color.rgb(135,125,255);1->Color.rgb(188,100,255);else->Color.rgb(126,110,177)}
        paint.style=Paint.Style.FILL;paint.shader=null
        val wave=.5f+.5f*kotlin.math.sin((breath*2f*Math.PI).toFloat())
        // Layered volumetric halo: soft atmospheric bloom behind the illuminated sphere.
        val halo=android.graphics.RadialGradient(x,y,r*1.58f,
            intArrayOf(Color.argb((105+75*wave).toInt(),Color.red(glow),Color.green(glow),Color.blue(glow)),
                Color.argb((45+40*wave).toInt(),Color.red(glow),Color.green(glow),Color.blue(glow)),Color.TRANSPARENT),
            floatArrayOf(0f,.56f,1f),android.graphics.Shader.TileMode.CLAMP)
        paint.shader=halo;c.drawCircle(x,y,r*1.58f,paint);paint.shader=null
        paint.color=glow;paint.setShadowLayer((5f+10f*wave)*d,0f,0f,glow)
        c.drawCircle(x,y,r,paint);paint.clearShadowLayer()
        // Off-axis specular illumination and darker lower rim create the 3D curvature.
        paint.shader=android.graphics.RadialGradient(x-r*.42f,y-r*.48f,r*1.95f,
            intArrayOf(Color.WHITE,glow,Color.rgb(57,51,117),Color.rgb(13,17,42)),
            floatArrayOf(0f,.38f,.76f,1f),android.graphics.Shader.TileMode.CLAMP)
        c.drawCircle(x,y,r,paint);paint.shader=null
        if(quality<3){
            paint.color=Color.rgb(16,22,43)
            val cut=when(quality){2->.60f;1->.35f;else->.12f}
            c.drawCircle(x+r*cut,y-r*.16f,r*.91f,paint)
        }
        if(premiumOrbit) {
            // Three subtle drifting light motes, only for the large calendar preview.
            for(i in 0..2) {
                val angle=(breath*2f*Math.PI+i*2f*Math.PI/3f).toFloat()
                val orbit=r*(1.34f+i*.09f)
                val px=x+kotlin.math.cos(angle)*orbit
                val py=y+kotlin.math.sin(angle)*orbit*.72f
                val moteRadius=d*(1.2f+i*.25f)
                paint.color=Color.argb((125+95*wave).toInt(),Color.red(glow),Color.green(glow),Color.blue(glow))
                paint.setShadowLayer(5f*d,0f,0f,glow)
                c.drawCircle(px,py,moteRadius,paint)
                paint.clearShadowLayer()
            }
        }
        // No arc highlight: it appeared as a white scratch across crescent moons.
        paint.style=Paint.Style.FILL
        paint.shader=android.graphics.RadialGradient(x-r*.28f,y-r*.38f,r*.56f,
            intArrayOf(Color.argb((95+55*wave).toInt(),255,255,255),Color.TRANSPARENT),
            null,android.graphics.Shader.TileMode.CLAMP)
        c.drawCircle(x-r*.28f,y-r*.38f,r*.56f,paint);paint.shader=null
    }
}

private class HistoryStageBarView(context:android.content.Context,private val values:List<Pair<Long,Int>>):View(context){
    private val paint=Paint(Paint.ANTI_ALIAS_FLAG)
    private var phase=0f
    private val valid=values.filter{it.first>0L}
    private val total=valid.sumOf{it.first}.coerceAtLeast(1L).toFloat()
    private val bodyPaint=Paint(Paint.ANTI_ALIAS_FLAG)
    private val glowPaint=Paint(Paint.ANTI_ALIAS_FLAG)
    private val pulse=android.animation.ValueAnimator.ofFloat(0f,1f).apply{
        duration=6200L;repeatCount=android.animation.ValueAnimator.INFINITE
        interpolator=android.view.animation.LinearInterpolator()
        addUpdateListener{phase=it.animatedValue as Float;if(HistoryScrollGate.shouldRender())invalidate()}
    }
    init{setLayerType(LAYER_TYPE_HARDWARE,null)}
    override fun onAttachedToWindow(){super.onAttachedToWindow();if(android.animation.ValueAnimator.areAnimatorsEnabled())pulse.start()}
    override fun onDetachedFromWindow(){pulse.cancel();super.onDetachedFromWindow()}
    override fun onDraw(c:Canvas){
        super.onDraw(c)
        if(valid.isEmpty()||width<=0||height<=0)return
        val d=resources.displayMetrics.density
        val gap=3f*d
        val usable=(width-gap*(valid.size-1)).coerceAtLeast(1f)
        val wave=.5f+.5f*kotlin.math.sin((phase*2f*Math.PI).toFloat())
        var x=0f
        valid.forEach{(minutes,color)->
            val segmentWidth=usable*minutes/total
            val rect=android.graphics.RectF(x,2f*d,x+segmentWidth,(height-2f*d))
            // Flat luminous neon: no expensive blurred 3D shadow layers.
            paint.style=Paint.Style.FILL;paint.shader=null;paint.alpha=255
            // Wide translucent underlay gives the bright neon bloom without shadow blur.
            glowPaint.shader=null
            glowPaint.color=Color.argb((45+30*wave).toInt(),Color.red(color),Color.green(color),Color.blue(color))
            c.drawRoundRect(android.graphics.RectF(rect.left,rect.top-2f*d,rect.right,rect.bottom+2f*d),7f*d,7f*d,glowPaint)
            bodyPaint.shader=null
            bodyPaint.color=color
            c.drawRoundRect(rect,5f*d,5f*d,bodyPaint)
            paint.style=Paint.Style.STROKE;paint.strokeWidth=.8f*d
            paint.color=Color.argb((100+55*wave).toInt(),235,246,255)
            c.drawRoundRect(rect,5f*d,5f*d,paint)
            paint.style=Paint.Style.FILL
            val highlightX=width*phase
            if(highlightX>=x && highlightX<=x+segmentWidth){
                val fade=kotlin.math.sin(Math.PI*phase).toFloat().coerceIn(0f,1f)
                paint.color=Color.argb((200*fade).toInt(),255,255,255)
                c.drawCircle(highlightX,height*.5f,2.2f*d,paint)
            }
            x+=segmentWidth+gap
        }
    }
}

private class HistoryWaveView(context:android.content.Context, private val tone:Int, private val values:List<Long>):View(context){
    private val paint=Paint(Paint.ANTI_ALIAS_FLAG).apply{style=Paint.Style.STROKE;strokeCap=Paint.Cap.ROUND;strokeJoin=Paint.Join.ROUND}
    private var phase=0f
    private val animator=android.animation.ValueAnimator.ofFloat(0f,1f).apply{
        duration=6200L;repeatCount=android.animation.ValueAnimator.INFINITE
        interpolator=android.view.animation.LinearInterpolator()
        addUpdateListener{phase=it.animatedValue as Float;if(HistoryScrollGate.shouldRender())invalidate()}
    }
    init{setLayerType(LAYER_TYPE_SOFTWARE,null)}
    override fun onAttachedToWindow(){super.onAttachedToWindow();if(android.animation.ValueAnimator.areAnimatorsEnabled())animator.start()}
    override fun onDetachedFromWindow(){animator.cancel();super.onDetachedFromWindow()}
    override fun onDraw(c:Canvas){
        super.onDraw(c);if(width<=0||height<=0)return
        val d=resources.displayMetrics.density
        val w=width.toFloat();val h=height.toFloat()
        val points=if(values.size>=2)values else listOf(240L,300L,270L,360L,320L)
        val min=points.minOrNull()?:0L;val max=points.maxOrNull()?:1L
        val span=(max-min).coerceAtLeast(60L).toFloat()
        val path=Path()
        points.forEachIndexed{i,v->
            val x=i.toFloat()/(points.size-1).coerceAtLeast(1)*w
            val y=h*.72f-((v-min)/span)*h*.48f
            if(i==0)path.moveTo(x,y) else path.lineTo(x,y)
        }
        paint.color=Color.argb(120,Color.red(tone),Color.green(tone),Color.blue(tone))
        paint.strokeWidth=1.2f*d;paint.setShadowLayer(4f*d,0f,0f,tone);c.drawPath(path,paint)
        paint.color=tone;paint.strokeWidth=1.7f*d
        paint.setShadowLayer(8f*d,0f,0f,tone);c.drawPath(path,paint);paint.clearShadowLayer()
        val measure=android.graphics.PathMeasure(path,false)
        val len=measure.length
        if(len>0f){
            val pos=FloatArray(2)
            measure.getPosTan(len*phase,pos,null)
            val fade=kotlin.math.sin(Math.PI*phase).toFloat().coerceIn(0f,1f)
            paint.style=Paint.Style.FILL;paint.color=Color.argb((245*fade).toInt(),255,255,255)
            paint.setShadowLayer(10f*d*fade,0f,0f,tone);c.drawCircle(pos[0],pos[1],2.6f*d,paint)
            paint.clearShadowLayer();paint.style=Paint.Style.STROKE
        }
    }
}

private class StageNeonView(context:android.content.Context, private val stages:List<StagePoint>, private val target:String, private val tone:Int, private val startMs:Long, private val endMs:Long):View(context){
    private val p=Paint(Paint.ANTI_ALIAS_FLAG)
    private var breath=0f
    private val animator=android.animation.ValueAnimator.ofFloat(0f,1f).apply{
        duration=5200L;repeatCount=android.animation.ValueAnimator.INFINITE
        interpolator=android.view.animation.LinearInterpolator()
        addUpdateListener{breath=it.animatedValue as Float;if(HistoryScrollGate.shouldRender())invalidate()}
    }
    override fun onAttachedToWindow(){super.onAttachedToWindow();if(stages.isNotEmpty() && android.animation.ValueAnimator.areAnimatorsEnabled())animator.start()}
    override fun onDetachedFromWindow(){animator.cancel();super.onDetachedFromWindow()}
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
        val glowWave=(.5f+.5f*kotlin.math.sin((breath*2f*Math.PI).toFloat()))
        p.setShadowLayer(resources.displayMetrics.density*(5f+10f*glowWave),0f,0f,tone)
        p.strokeWidth=resources.displayMetrics.density*(2.3f+1.7f*glowWave)
        p.alpha=(145+110*glowWave).toInt();c.drawPath(path,p);p.clearShadowLayer();p.alpha=255
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
    private var highlightOverviewTab: (() -> Unit)? = null
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
    // Sample a tiny decoded bitmap once on import; never analyze images during scrolling.
    private fun analyzeWallpaperContrast(file:File) {
        runCatching {
            val opts=android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds=true }
            android.graphics.BitmapFactory.decodeFile(file.absolutePath,opts)
            val sample=(maxOf(opts.outWidth,opts.outHeight)/96).coerceAtLeast(1)
            val bitmap=android.graphics.BitmapFactory.decodeFile(file.absolutePath,
                android.graphics.BitmapFactory.Options().apply { inSampleSize=sample }) ?: return@runCatching
            var bright=0; var count=0
            try {
                for(y in 0 until bitmap.height step 3) for(x in 0 until bitmap.width step 3) {
                    val pixel=bitmap.getPixel(x,y)
                    val luma=(Color.red(pixel)*299+Color.green(pixel)*587+Color.blue(pixel)*114)/1000
                    if(luma>155) bright++
                    count++
                }
            } finally { bitmap.recycle() }
            val ratio=if(count>0) bright.toFloat()/count else 0f
            getSharedPreferences("sleepsync_design",MODE_PRIVATE).edit()
                .putInt("wallpaper_bright_percent",(ratio*100).toInt().coerceIn(0,100)).apply()
        }.onFailure {
            getSharedPreferences("sleepsync_design",MODE_PRIVATE).edit()
                .remove("wallpaper_bright_percent").apply()
        }
    }
    private fun designGlassAlpha(defaultAlpha:Int=168):Int {
        // Preserve the manual transparency value; apply only a modest, temporary
        // contrast boost for imported images with substantial bright regions.
        val transparency=designPercent("glass_strength",34)
        val base=(255f*(1f-transparency/100f)).toInt().coerceIn(0,255)
        val prefs=getSharedPreferences("sleepsync_design",MODE_PRIVATE)
        if(prefs.getString("wallpaper_source","builtin")!="custom") return base
        val bright=prefs.getInt("wallpaper_bright_percent",0)
        val boost=((bright-12).coerceAtLeast(0)*0.55f).toInt().coerceAtMost(38)
        return (base+boost).coerceAtMost(255)
    }
    private fun designGlassOverlayAlpha(maxAlpha:Int=110):Int =
        (maxAlpha*(designGlassAlpha()/255f)).toInt().coerceIn(0,maxAlpha)
    private fun designNeonAlpha(base:Int):Int=(base*(designPercent("neon_strength",100)/100f)).toInt().coerceIn(0,255)
    private fun designGlowAlpha(base:Int):Int=(base*(designPercent("glow_strength",100)/100f)).toInt().coerceIn(0,255)
    private fun designBlurRadius():Float=(60f*(designPercent("blur_strength",20)/100f)).coerceAtLeast(0f)
    private fun effectiveBlurRadius():Float = if(designPercent("blur_strength",20)<=0) 0f else designBlurRadius()
    private fun glassBlurView(radius:Int, tone:Int, dp:(Int)->Int, content:View):View {
        val blur=eightbitlab.com.blurview.BlurView(this).apply {
            outlineProvider=android.view.ViewOutlineProvider.BACKGROUND
            clipToOutline=true
            background=GradientDrawable().apply {
                cornerRadius=dp(radius).toFloat()
                setColor(Color.argb(designGlassAlpha(),72,88,112))
                setStroke(dp(2),Color.argb(designNeonAlpha(255),Color.red(tone),Color.green(tone),Color.blue(tone)))
            }
            settingsBlurTarget?.let { target ->
                setupWith(target,4f,true).setBlurEnabled(true).setBlurAutoUpdate(true)
                    .setBlurRadius(effectiveBlurRadius())
                    .setOverlayColor(Color.argb(designGlassOverlayAlpha(),72,88,112))
            }
            addView(content,android.widget.FrameLayout.LayoutParams(-1,-1))
        }
        return blur
    }
    private fun addBlurLayer(host:android.view.ViewGroup, radius:Int, dp:(Int)->Int) {
        if(designPercent("blur_strength",20)<=0) return
        val blur=eightbitlab.com.blurview.BlurView(this).apply {
            outlineProvider=android.view.ViewOutlineProvider.BACKGROUND
            clipToOutline=true
            background=GradientDrawable().apply { cornerRadius=dp(radius).toFloat(); setColor(Color.TRANSPARENT) }
            settingsBlurTarget?.let { target -> setupWith(target,4f,true).setBlurEnabled(true).setBlurAutoUpdate(true).setBlurRadius(effectiveBlurRadius()).setOverlayColor(Color.TRANSPARENT) }
        }
        host.addView(blur,0,android.view.ViewGroup.LayoutParams(-1,-1))
    }
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
    private fun generateDreamScape() {
        val night=lastSummary
        if(night==null || night.totalMin<=0L) {
            android.widget.Toast.makeText(this,"Für DreamScape werden zuerst Schlafdaten benötigt.",android.widget.Toast.LENGTH_LONG).show()
            return
        }
        runCatching {
            val w=1080;val h=2400
            val bitmap=android.graphics.Bitmap.createBitmap(w,h,android.graphics.Bitmap.Config.ARGB_8888)
            val canvas=android.graphics.Canvas(bitmap)
            canvas.drawColor(Color.BLACK)
            val brush=android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
            val total=(night.lightMin+night.deepMin+night.remMin+night.awakeMin).coerceAtLeast(1L)
            val deep=night.deepMin.toFloat()/total
            val rem=night.remMin.toFloat()/total
            val efficiency=(night.totalMin.toFloat()/(night.endMs-night.startMs).coerceAtLeast(1L)*60000f).coerceIn(0f,1f)
            val violet=Color.rgb(130+(rem*85).toInt(),65,190+(rem*60).toInt())
            val cyan=Color.rgb(35,145+(deep*100).toInt(),215)
            val seed=night.startMs xor (night.totalMin shl 11) xor (night.deepMin shl 5) xor night.remMin
            val random=java.util.Random(seed)
            // Subtle deterministic stars; preserve a predominantly true-black OLED canvas.
            repeat(130) {
                val x=random.nextFloat()*w
                val y=random.nextFloat()*h
                val alpha=35+random.nextInt(90)
                brush.color=Color.argb(alpha,140+random.nextInt(100),175,255)
                brush.style=android.graphics.Paint.Style.FILL
                canvas.drawCircle(x,y,.5f+random.nextFloat()*1.4f,brush)
            }
            val baseline=h*.53f
            val phases=night.stageSeries.filter { it.endMs>it.startMs }.sortedBy { it.startMs }
            val span=(night.endMs-night.startMs).coerceAtLeast(1L).toDouble()
            repeat(7) { layer ->
                val path=android.graphics.Path()
                val spread=18f+layer*27f
                for(x in 0..w step 5) {
                    val fraction=x.toDouble()/w
                    val time=night.startMs+(fraction*span).toLong()
                    val phase=phases.lastOrNull { time>=it.startMs && time<it.endMs }?.stageLabel?.trim()?.lowercase()
                    val amplitude=when(phase) { "tief"->.35f;"rem"->1.35f;"wach"->1.6f;else->.8f }
                    val y=baseline+layer*30f+
                        kotlin.math.sin(fraction*18.85+layer*.55).toFloat()*spread*amplitude+
                        kotlin.math.sin(fraction*44.0+layer*.9).toFloat()*spread*.25f
                    if(x==0)path.moveTo(x.toFloat(),y) else path.lineTo(x.toFloat(),y)
                }
                brush.style=android.graphics.Paint.Style.STROKE
                brush.strokeWidth=2f+(7-layer)*.3f
                brush.color=if(layer%2==0) cyan else violet
                brush.alpha=(125-layer*12).coerceAtLeast(35)
                brush.setShadowLayer(12f,0f,0f,brush.color)
                canvas.drawPath(path,brush)
                brush.clearShadowLayer()
            }
            // Four measured health timelines, using the same palette as the health cards.
            // Missing series are omitted instead of fabricating physiological measurements.
            val streams=listOf(
                Triple(night.heartRateSeries,Color.rgb(255,65,125),h*.30f),
                Triple(night.spo2Series,Color.rgb(164,255,55),h*.40f),
                Triple(night.respirationSeries,Color.rgb(57,234,204),h*.64f),
                Triple(night.hrvSeries,Color.rgb(207,78,240),h*.74f)
            )
            streams.forEach { (points,tone,centerY) ->
                val samples=points.filter { it.timeMs>=night.startMs && it.timeMs<=night.endMs && it.value.isFinite() }
                    .sortedBy { it.timeMs }
                if(samples.size>=2) {
                    val low=samples.minOf { it.value }
                    val high=samples.maxOf { it.value }
                    val range=(high-low).coerceAtLeast(1.0)
                    val path=android.graphics.Path()
                    samples.forEachIndexed { index,point ->
                        val x=((point.timeMs-night.startMs)/span*w).toFloat().coerceIn(0f,w.toFloat())
                        val normalized=((point.value-low)/range).toFloat()
                        val y=centerY+(0.5f-normalized)*h*.065f
                        if(index==0)path.moveTo(x,y) else path.lineTo(x,y)
                    }
                    brush.shader=null
                    brush.style=android.graphics.Paint.Style.STROKE
                    brush.strokeWidth=3.5f
                    brush.color=tone
                    brush.alpha=190
                    brush.setShadowLayer(14f,0f,0f,tone)
                    canvas.drawPath(path,brush)
                    brush.clearShadowLayer()
                }
            }
            brush.alpha=255
            brush.style=android.graphics.Paint.Style.FILL
            brush.shader=android.graphics.RadialGradient(w*.5f,baseline,w*.43f,
                intArrayOf(Color.argb((efficiency*40).toInt(),85,40,160),Color.TRANSPARENT),
                null,android.graphics.Shader.TileMode.CLAMP)
            canvas.drawCircle(w*.5f,baseline,w*.43f,brush)
            brush.shader=null
            val output=File(filesDir,"sleepsync_dreamscape.png")
            output.outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it) }
            bitmap.recycle()
            getSharedPreferences("sleepsync_design",MODE_PRIVATE).edit()
                .putBoolean("custom_enabled",true).putBoolean("wallpaper_enabled",true)
                .putString("wallpaper_source","dreamscape").apply()
            recreate()
        }.onFailure {
            android.widget.Toast.makeText(this,"DreamScape konnte nicht erstellt werden.",android.widget.Toast.LENGTH_LONG).show()
        }
    }
    private fun handleSharedWallpaper(incoming:android.content.Intent?) {
        if(incoming?.action!=android.content.Intent.ACTION_SEND || incoming.type?.startsWith("image/")!=true) return
        val imageUri=if(android.os.Build.VERSION.SDK_INT>=33)
            incoming.getParcelableExtra(android.content.Intent.EXTRA_STREAM,Uri::class.java)
        else @Suppress("DEPRECATION") (incoming.getParcelableExtra<Uri>(android.content.Intent.EXTRA_STREAM))
        if(imageUri==null) {
            android.widget.Toast.makeText(this,"Die Wallpaper-App hat kein Bild geteilt.",android.widget.Toast.LENGTH_LONG).show()
            return
        }
        // Post until the normal UI is ready; copying the image makes the import independent
        // of temporary permissions granted by the sharing app.
        window.decorView.post {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Wallpaper übernehmen?")
                .setMessage("Das geteilte Bild als SleepSync-Hintergrund verwenden?")
                .setNegativeButton("Abbrechen",null)
                .setPositiveButton("Übernehmen") { _,_ -> saveCustomWallpaper(imageUri) }
                .show()
        }
    }
    override fun onNewIntent(intent:android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleSharedWallpaper(intent)
    }
    private fun saveCustomWallpaper(uri:Uri?) {
        uri ?: return
        runCatching {
            runCatching { contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            val out=File(filesDir,"sleepsync_custom_wallpaper")
            val input=contentResolver.openInputStream(uri) ?: error("Kein Bildinhalt verfügbar")
            input.use { source -> out.outputStream().use { source.copyTo(it) } }
            analyzeWallpaperContrast(out)
            getSharedPreferences("sleepsync_design",MODE_PRIVATE).edit().putBoolean("custom_enabled",true).putBoolean("wallpaper_enabled",true).putString("wallpaper_source","custom").apply()
            recreate()
        }.onFailure { android.widget.Toast.makeText(this,"Wallpaper konnte nicht geladen werden.",android.widget.Toast.LENGTH_SHORT).show() }
    }
    private val photoPickerWallpaperLauncher = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? -> saveCustomWallpaper(uri) }
    // Installed wallpaper/image apps that expose an image provider can participate
    // through Android's standard content picker, without broad package visibility.
    private val wallpaperAppLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if(result.resultCode==android.app.Activity.RESULT_OK) {
            val uri=result.data?.data
            if(uri!=null) saveCustomWallpaper(uri)
            else android.widget.Toast.makeText(this,"Diese Wallpaper-App hat kein Bild übergeben.",android.widget.Toast.LENGTH_LONG).show()
        }
    }
    private fun openInstalledWallpaperApps() {
        val request=android.content.Intent(android.content.Intent.ACTION_GET_CONTENT).apply {
            type="image/*"
            addCategory(android.content.Intent.CATEGORY_OPENABLE)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        runCatching {
            wallpaperAppLauncher.launch(android.content.Intent.createChooser(request,"Wallpaper-App auswählen"))
        }.onFailure {
            android.widget.Toast.makeText(this,"Keine kompatible Wallpaper-Quelle gefunden.",android.widget.Toast.LENGTH_LONG).show()
        }
    }
    private val customWallpaperLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri ?: return@registerForActivityResult
        runCatching {
            runCatching { contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            val out=File(filesDir,"sleepsync_custom_wallpaper")
            contentResolver.openInputStream(uri)?.use { input -> out.outputStream().use { input.copyTo(it) } }
            getSharedPreferences("sleepsync_design",MODE_PRIVATE).edit().putBoolean("custom_enabled",true).putBoolean("wallpaper_enabled",true).putString("wallpaper_source","custom").apply()
            recreate()
        }.onFailure { android.widget.Toast.makeText(this,"Wallpaper konnte nicht geladen werden.",android.widget.Toast.LENGTH_SHORT).show() }
    }
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
            if(light && this is eightbitlab.com.blurview.BlurView){outlineProvider=android.view.ViewOutlineProvider.BACKGROUND;clipToOutline=true;settingsBlurTarget?.let{target->setupWith(target,4f,true).setBlurEnabled(true).setBlurAutoUpdate(true).setBlurRadius(effectiveBlurRadius()).setOverlayColor(Color.argb(designGlassOverlayAlpha(),72,88,112))}}
        }
        val shellContent=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(18),dp(18),dp(18),dp(12));background=null}
        shell.addView(shellContent,android.widget.FrameLayout.LayoutParams(-1,-2))
        shellContent.addView(TextView(this).apply{text="📅  ZIELKALENDER";textSize=18f;setTextColor(stageRem);setTypeface(typeface,Typeface.BOLD);setPadding(0,0,0,dp(4))})
        shellContent.addView(TextView(this).apply{text="Wohin soll SleepSync deine Nächte schreiben?";textSize=12f;setTextColor(secondary);setPadding(0,0,0,dp(12))})
        val dialog=AlertDialog.Builder(this).create()
        val list=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL }
        val scroll=ScrollView(this).apply {
            isFillViewport=false
            clipToPadding=false
            addView(list)
        }
        // Keep the header visible and the list comfortably scrollable on smaller phones.
        shellContent.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))
        val ordered=items.sortedWith(compareByDescending<Triple<Long,String,String>> { it.first==selectedId }.thenBy { it.second.lowercase() })
        ordered.forEach { item ->
            val selected=item.first==selectedId
            val row=LinearLayout(this).apply {
                orientation=LinearLayout.HORIZONTAL
                gravity=android.view.Gravity.CENTER_VERTICAL
                setPadding(dp(13),dp(12),dp(12),dp(12))
                background=GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,if(selected)
                    intArrayOf(Color.rgb(35,30,74),Color.rgb(20,35,61))
                else intArrayOf(Color.rgb(18,26,45),Color.rgb(21,25,49))).apply {
                    cornerRadius=dp(15).toFloat()
                    setStroke(dp(if(selected) 2 else 1),if(selected) stageRem else Color.argb(85,110,130,180))
                }
                isClickable=true
                isFocusable=true
                setOnClickListener {
                    getSharedPreferences("sleepsync_calendar",MODE_PRIVATE).edit()
                        .putLong("calendar_id",item.first)
                        .putString("calendar_name",item.second)
                        .putString("calendar_account",item.third).apply()
                    dialog.dismiss()
                    showCalendarPlaceholder()
                }
            }
            row.addView(TextView(this).apply {
                text=if(selected) "✓" else "▢"
                textSize=18f
                setTextColor(if(selected) Color.rgb(170,115,255) else Color.rgb(94,109,145))
                gravity=android.view.Gravity.CENTER
            },LinearLayout.LayoutParams(dp(30),-2))
            row.addView(LinearLayout(this).apply {
                orientation=LinearLayout.VERTICAL
                addView(TextView(this@MainActivity).apply {
                    text=item.second
                    textSize=14f
                    setTypeface(typeface,if(selected) Typeface.BOLD else Typeface.NORMAL)
                    setTextColor(Color.WHITE)
                    maxLines=2
                    ellipsize=android.text.TextUtils.TruncateAt.END
                })
                addView(TextView(this@MainActivity).apply {
                    text=item.third
                    textSize=11f
                    setTextColor(Color.rgb(155,172,202))
                    maxLines=1
                    ellipsize=android.text.TextUtils.TruncateAt.MIDDLE
                })
            },LinearLayout.LayoutParams(0,-2,1f))
            list.addView(row,LinearLayout.LayoutParams(-1,-2).apply { setMargins(0,0,0,dp(8)) })
        }
        val maxHeight=(resources.displayMetrics.heightPixels*0.76f).toInt()
        dialog.setView(shell)
        dialog.setOnShowListener {
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dialog.window?.setLayout((resources.displayMetrics.widthPixels*0.91f).toInt(),maxHeight)
        }
        // Size the content with the parent-specific LayoutParams before showing the dialog.
        // Generic ViewGroup.LayoutParams after show() can crash during FrameLayout layout.
        shellContent.layoutParams=android.widget.FrameLayout.LayoutParams(-1,maxHeight)
        dialog.show()

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
        if(savedInstanceState==null) handleSharedWallpaper(intent)
        scheduleBackgroundSleepSync()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val d = resources.displayMetrics.density
        fun dp(v: Int) = (v * d).toInt()
        fun button(label: String, action: () -> Unit) = MaterialButton(this).apply {
            text = label; isAllCaps = false; textSize = 15f; minHeight = dp(56); setOnClickListener { action() }
        }
        status = TextView(this).apply { textSize = 14f; setPadding(dp(12),dp(7),dp(12),dp(7)); gravity=android.view.Gravity.CENTER_VERTICAL; maxLines=1; setSingleLine(true); ellipsize=android.text.TextUtils.TruncateAt.END; includeFontPadding=false }
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
            highlightOverviewTab={ activate(home) }
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
            // Align the connection bar with the dashboard cards inside sleepCard (18dp horizontal padding).
            layoutParams=LinearLayout.LayoutParams(-1,-2).apply { setMargins(dp(18),dp(10),dp(18),dp(12)) }
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
                val custom=File(filesDir,"sleepsync_custom_wallpaper")
                if(designPrefs.getString("wallpaper_source","builtin")=="dreamscape" && File(filesDir,"sleepsync_dreamscape.png").exists()) setImageURI(Uri.fromFile(File(filesDir,"sleepsync_dreamscape.png")))
                else if(designPrefs.getString("wallpaper_source","builtin")=="custom" && custom.exists()) setImageURI(Uri.fromFile(custom))
                else setImageResource(if(useLight) R.drawable.sleepsync_day else R.drawable.sleepsync_night)
                alpha=1f
            }, android.widget.FrameLayout.LayoutParams(-1,-1))
            if(wallpaperOn && designPrefs.getString("wallpaper_source","builtin")=="dreamscape") {
                addView(object:View(this@MainActivity) {
                    private val paint=android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
                    private var progress=0f
                    private val animator=android.animation.ValueAnimator.ofFloat(0f,1f).apply {
                        duration=36000L
                        repeatCount=android.animation.ValueAnimator.INFINITE
                        repeatMode=android.animation.ValueAnimator.REVERSE
                        interpolator=android.view.animation.LinearInterpolator()
                        addUpdateListener { progress=it.animatedValue as Float;if(HistoryScrollGate.shouldRender())invalidate() }
                    }
                    override fun onAttachedToWindow() {
                        super.onAttachedToWindow()
                        if(android.provider.Settings.Global.getFloat(contentResolver,
                            android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,1f)>0f) animator.start()
                    }
                    override fun onDetachedFromWindow() { animator.cancel();super.onDetachedFromWindow() }
                    override fun onDraw(canvas:android.graphics.Canvas) {
                        super.onDraw(canvas)
                        val w=width.toFloat();val h=height.toFloat()
                        if(w<=0f||h<=0f)return
                        val t=progress*2f*Math.PI.toFloat()
                        val breathe=.5f+.5f*kotlin.math.sin(t)
                        val pulseDistance=1f-kotlin.math.cos(t*2f)
                        val pulse=kotlin.math.exp(-pulseDistance*7f)
                        val colors=intArrayOf(Color.rgb(255,62,134),Color.rgb(165,255,53),
                            Color.rgb(48,233,211),Color.rgb(208,75,242))
                        // Layered translucent silk ribbons with parallax, soft halo and a bright fold.
                        val density=resources.displayMetrics.density
                        for(layer in 0..3) {
                            val base=h*(.21f+layer*.18f)
                            val amplitude=h*(.035f+layer*.008f)*(1f+.23f*breathe)
                            val tone=colors[layer]
                            fun wave(u:Float,depth:Float):Float {
                                return kotlin.math.sin(u*12.0+layer*1.25+t*(1f+depth.toFloat())).toFloat()+
                                    .36f*kotlin.math.sin(u*28.0-layer*.7-t*2f+depth).toFloat()
                            }
                            // Back-to-front translucent surfaces create the illusion of twisting fabric.
                            for(depth in 0..3) {
                                val thickness=density*(14f+depth*7f)
                                val ribbon=android.graphics.Path()
                                for(step in 0..100) {
                                    val u=step/100f
                                    val x=w*u
                                    val y=base+amplitude*wave(u,depth*.38f)+
                                        kotlin.math.sin(u*9.0+t+depth).toFloat()*density*depth*5f
                                    if(step==0)ribbon.moveTo(x,y) else ribbon.lineTo(x,y)
                                }
                                paint.shader=null
                                paint.style=android.graphics.Paint.Style.STROKE
                                paint.strokeCap=android.graphics.Paint.Cap.ROUND
                                paint.strokeJoin=android.graphics.Paint.Join.ROUND
                                paint.strokeWidth=thickness
                                paint.color=Color.argb((10+depth*5+7*breathe).toInt(),
                                    Color.red(tone),Color.green(tone),Color.blue(tone))
                                canvas.drawPath(ribbon,paint)
                            }
                            val highlight=android.graphics.Path()
                            for(step in 0..110) {
                                val u=step/110f
                                val y=base+amplitude*wave(u,0f)
                                if(step==0)highlight.moveTo(0f,y) else highlight.lineTo(w*u,y)
                            }
                            paint.style=android.graphics.Paint.Style.STROKE
                            paint.strokeCap=android.graphics.Paint.Cap.ROUND
                            paint.strokeJoin=android.graphics.Paint.Join.ROUND
                            paint.shader=null
                            paint.strokeWidth=density*5f
                            paint.color=Color.argb((46+20*breathe+8*pulse).toInt(),
                                Color.red(tone),Color.green(tone),Color.blue(tone))
                            canvas.drawPath(highlight,paint)
                            paint.strokeWidth=density*1.35f
                            paint.color=Color.argb((125+40*breathe).toInt(),255,235,255)
                            canvas.drawPath(highlight,paint)
                            paint.style=android.graphics.Paint.Style.FILL
                            for(dot in 0..7) {
                                val u=(dot/8f+progress+layer*.13f)%1f
                                val y=base+amplitude*wave(u,0f)
                                paint.color=Color.argb((55+55*breathe).toInt(),
                                    Color.red(tone),Color.green(tone),Color.blue(tone))
                                canvas.drawCircle(w*u,y,density*(.9f+dot%3*.45f),paint)
                            }
                        }
                    }
                },android.widget.FrameLayout.LayoutParams(-1,-1))
            }
            if(!wallpaperOn) addView(View(this@MainActivity).apply {
                background=ColorDrawable(if(useLight) Color.rgb(38,45,64) else Color.rgb(5,6,14))
            },android.widget.FrameLayout.LayoutParams(-1,-1))
            addView(View(this@MainActivity).apply {
                background=if(useLight) GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,intArrayOf(Color.argb(34,255,255,255),Color.argb(12,240,247,255),Color.argb(24,225,245,255))) else GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,intArrayOf(Color.TRANSPARENT,Color.argb(35,2,5,15),Color.argb(150,2,4,12)))
            }, android.widget.FrameLayout.LayoutParams(-1,-1))
        }
        settingsBlurTarget=blurTarget
        // Scroll only the UI; keep the wallpaper in one fixed viewport layer.
        // A tall scroll-content ImageView can be cropped/re-measured mid-scroll,
        // creating the horizontal wallpaper seam visible above health metrics.
        val scene = android.widget.FrameLayout(this).apply {
            addView(box, android.widget.FrameLayout.LayoutParams(-1,-2))
        }
        val scroll = ScrollView(this).apply {
            isFillViewport=true
            clipToPadding=false
            background=ColorDrawable(Color.TRANSPARENT)
            addView(scene)
            layoutParams = android.widget.FrameLayout.LayoutParams(-1,-1)
        }
        // Pause expensive animation redraws while scrolling on any SleepSync page.
        val historyScrollResume=Runnable {
            HistoryScrollGate.scrolling=false
            sleepCard.invalidate()
            brandGlow.invalidate()
        }
        scroll.setOnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
            if(scrollY!=oldScrollY){
                HistoryScrollGate.scrolling=true
                scroll.removeCallbacks(historyScrollResume)
                scroll.postDelayed(historyScrollResume,220L)
            }
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
            // Exactly one viewport-sized wallpaper and blur source for all cards.
            // No scrolling, resizing, or duplicated wallpaper layers.
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
        styleHomeConnections()
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
                if(HistoryScrollGate.shouldRender())brandGlow.invalidate()
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
        val progressPrefix = "GARMIN  ●     HEALTH CONNECT  ●     "
        fun progress(percent:Int,stage:String) {
            status.text = progressPrefix + percent + " % · " + stage
        }
        progress(0,"Schlafdaten vorbereiten")
        setLoadingGlow(true)
        var currentDetail="Health Connect · Schlafsessions suchen"
        val startedAt=android.os.SystemClock.elapsedRealtime()
        val progressJob=launch {
            while(kotlinx.coroutines.currentCoroutineContext().isActive) {
                val elapsed=(android.os.SystemClock.elapsedRealtime()-startedAt)/1000L
                progress(90,currentDetail+" · "+elapsed+" s")
                kotlinx.coroutines.delay(1000)
            }
        }
        try {
            val history = withContext(Dispatchers.IO) {
                SleepReader(this@MainActivity).garminHistory(cachedNights = sleepHistory.toList(), onProgress = { detail ->
                    status.post {
                        // Ignore queued progress after the loading job has ended.
                        if (progressJob.isActive) {
                            currentDetail=detail
                            val elapsed=(android.os.SystemClock.elapsedRealtime()-startedAt)/1000L
                            progress(90,detail+" · "+elapsed+" s")
                        }
                    }
                })
            }
            progressJob.cancel()
            progress(92,"Schlafdaten zusammenführen")
            sleepHistory = mergeHistory(sleepHistory, history)
            progress(94,"Schlafverlauf speichern")
            saveCachedHistory(sleepHistory)
            progress(96,"Letzte Nacht darstellen")
            val latest = history.maxByOrNull { it.endMs } ?: error("Keine Garmin-Schlafsession gefunden")
            renderDashboard(latest)
            progress(98,"Kalender synchronisieren")
            withContext(Dispatchers.IO) { syncLatestNightToCalendar(latest) }
            progress(99,"Ansicht aktualisieren")
            refresh()
            progress(100,"Fertig")
        } catch (t: Throwable) {
            progressJob.cancel()
            progress(90,"Fehler beim Laden")
            sleepCard.removeAllViews()
            sleepCard.addView(TextView(this@MainActivity).apply { text = "⚠️ Schlafdaten konnten nicht geladen werden\n" + t.message.orEmpty(); textSize = 16f })
        } finally {
            progressJob.cancel()
            setLoadingGlow(false)
        }
    }

    private fun nextTimeAwareGreeting(): String {
        val hour=java.time.LocalTime.now().hour
        val timeOfDay=when(hour) {
            in 4..9 -> "Morgen"
            in 10..11 -> "Vormittag"
            in 12..17 -> "Nachmittag"
            in 18..21 -> "Abend"
            else -> "Nacht"
        }
        val openers=when(timeOfDay) {
            "Morgen" -> listOf("Ein frischer Start", "Die Welt erwacht", "Ein neuer Tag beginnt", "Guten Morgen", "Der Tag nimmt Fahrt auf", "Zeit für einen klaren Blick")
            "Vormittag" -> listOf("Mitten im Vormittag", "Der Tag läuft", "Zeit für deinen Schlafcheck", "Ein kurzer Rückblick", "Klarheit für deinen Tag", "Schon gut unterwegs")
            "Nachmittag" -> listOf("Schönen Nachmittag", "Mitten am Tag", "Ein Moment zum Durchatmen", "Zeit für einen Rückblick", "Dein Nachmittag, dein Überblick", "Der Tag ist in vollem Gange")
            "Abend" -> listOf("Guten Abend", "Der Tag klingt aus", "Ein ruhiger Moment", "Zeit zum Runterfahren", "Der Abend gehört dir", "Der Tag wird leiser")
            else -> listOf("Eine ruhige Nacht", "Wenn die Welt stiller wird", "Noch wach?", "Mitten in der Nacht", "Zeit für Ruhe", "Ein stiller Blick auf deinen Schlaf")
        }
        val endings=listOf(
            "deine Nacht im Überblick", "dein Schlaf auf einen Blick",
            "deine Schlafwerte im Fokus", "ein Blick auf die letzte Nacht",
            "deine Nacht, klar ausgewertet", "was dein Schlaf erzählt",
            "die Nacht in Zahlen", "dein persönlicher Schlafrückblick",
            "deine Erholung im Blick", "die wichtigsten Nachtwerte"
        )
        val candidates=openers.flatMap { opener -> endings.map { ending -> opener + " · " + ending } }
        val prefs=getSharedPreferences("sleepsync_greetings",MODE_PRIVATE)
        val previous=prefs.getString("last_greeting",null)
        val lastIndex=prefs.getInt("last_index",-1)
        // Shuffle without repeating until every combination for this daypart has appeared.
        val nextIndex=if(lastIndex<0 || lastIndex>=candidates.size-1 || prefs.getString("daypart",null)!=timeOfDay) 0 else lastIndex+1
        val seed=(java.time.LocalDate.now().toEpochDay().toInt()*31+timeOfDay.hashCode())
        val shuffled=candidates.shuffled(kotlin.random.Random(seed))
        val chosen=shuffled[nextIndex].let { if(it==previous) shuffled[(nextIndex+1)%shuffled.size] else it }
        prefs.edit().putString("last_greeting",chosen).putString("daypart",timeOfDay).putInt("last_index",nextIndex).apply()
        return chosen
    }

    private fun styleHomeConnections() {
        val density=resources.displayMetrics.density
        fun dp(value:Int)=(value*density).toInt()
        // Connections and automation are configured exclusively in Settings.
        actionsTitle.visibility=View.GONE
        actionsBox.visibility=View.GONE
        val light=getSharedPreferences("sleepsync_ui",MODE_PRIVATE).getString("theme","dark")=="light"
        val red=Color.rgb(255,70,82)
        val preferences=getSharedPreferences("sleepsync_dashboard",MODE_PRIVATE)
        var expanded=preferences.getBoolean("connections_expanded",false)
        actionsTitle.text=if(expanded) "VERBINDUNGEN & AUTOMATIK   ⌄" else "VERBINDUNGEN & AUTOMATIK   ›"
        actionsTitle.textSize=11f
        actionsTitle.letterSpacing=.14f
        actionsTitle.setTypeface(actionsTitle.typeface,Typeface.BOLD)
        actionsTitle.setTextColor(Color.WHITE)
        actionsTitle.setShadowLayer(dp(3).toFloat(),0f,dp(1).toFloat(),Color.BLACK)
        actionsTitle.setPadding(dp(4),dp(14),dp(4),dp(10))
        actionsTitle.isClickable=true
        actionsTitle.isFocusable=true
        for(i in 0 until minOf(5,actionsBox.childCount)) {
            val b=actionsBox.getChildAt(i) as? MaterialButton ?: continue
            b.cornerRadius=dp(18)
            b.textSize=12f
            b.minHeight=dp(48)
            b.setTextColor(Color.WHITE)
            b.backgroundTintList=ColorStateList.valueOf(if(light) Color.argb(designGlassAlpha(),72,88,112) else Color.rgb(14,17,34))
            b.strokeWidth=dp(2)
            b.strokeColor=ColorStateList.valueOf(red)
            b.setLayerType(View.LAYER_TYPE_SOFTWARE,null)
            b.paint.maskFilter=null
            b.setShadowLayer(dp(8).toFloat(),0f,0f,Color.argb(190,255,70,82))
            b.layoutParams=(b.layoutParams ?: LinearLayout.LayoutParams(-1,-2)).apply {
                height=dp(48)
                if(this is LinearLayout.LayoutParams) setMargins(0,dp(5),0,dp(5))
            }
            b.visibility=if(expanded) View.VISIBLE else View.GONE
        }
        actionsTitle.setOnClickListener {
            expanded=!expanded
            preferences.edit().putBoolean("connections_expanded",expanded).apply()
            val open=expanded
            actionsTitle.text=if(open) "VERBINDUNGEN & AUTOMATIK   ⌄" else "VERBINDUNGEN & AUTOMATIK   ›"
            for(i in 0 until minOf(5,actionsBox.childCount))
                actionsBox.getChildAt(i)?.visibility=if(open) View.VISIBLE else View.GONE
        }
    }

    private fun makeOverviewTextWhite(view: View) {
        if(view is TextView && view.tag!="sleepsync_colored_pill") view.setTextColor(Color.WHITE)
        if(view is android.view.ViewGroup) {
            for(i in 0 until view.childCount) makeOverviewTextWhite(view.getChildAt(i))
        }
    }

    private fun showOverview() {
        highlightOverviewTab?.invoke()
        styleHomeConnections()
        pageTitle.text = "SleepSync"
        pageSubtitle.text = nextTimeAwareGreeting()
        lastSummary?.let { renderDashboard(it) }
        makeOverviewTextWhite(sleepCard)
        makeOverviewTextWhite(actionsTitle)
        for(i in 0 until minOf(5,actionsBox.childCount)) makeOverviewTextWhite(actionsBox.getChildAt(i))
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
        HistoryScrollGate.scrolling=false
        pageTitle.text="Verlauf"; pageSubtitle.text="Deine Nächte · nach Kalenderwochen"
        sleepCard.removeAllViews(); sleepCard.background=null
        val tf=DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())
        val dateFmt=DateTimeFormatter.ofPattern("EEE, d. MMM",java.util.Locale.GERMAN).withZone(ZoneId.systemDefault())
        val weekFields=java.time.temporal.WeekFields.ISO
        val nights=(if(sleepHistory.isNotEmpty()) sleepHistory else listOfNotNull(lastSummary)).sortedByDescending { it.endMs }
        if(nights.isEmpty()){ sleepCard.addView(historyLoadingView()); return }
        // Aggregate trend metrics only; individual nights remain in the weekly cards below.
        val trendRow=LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL;gravity=android.view.Gravity.CENTER_VERTICAL }
        val today=java.time.LocalDate.now()
        val trendWindows=listOf(7,30,90)
        trendWindows.forEach { window ->
            val cutoff=today.minusDays(window.toLong()-1)
            val subset=nights.filter {
                !Instant.ofEpochMilli(it.endMs).atZone(ZoneId.systemDefault()).toLocalDate().isBefore(cutoff)
            }
            val avg=if(subset.isEmpty()) 0L else subset.map { it.totalMin }.average().toLong()
            val tile=LinearLayout(this).apply {
                orientation=LinearLayout.VERTICAL;gravity=android.view.Gravity.CENTER
                setPadding(dp(6),dp(12),dp(6),dp(12))
                background=GradientDrawable().apply {
                    cornerRadius=dp(15).toFloat()
                    setColor(Color.argb(designGlassAlpha(195),16,29,53))
                    setStroke(dp(1),if(window==7)stageRem else if(window==30)accent2 else stageLight)
                }
                addView(TextView(this@MainActivity).apply {
                    text="$window TAGE";textSize=10f;setTypeface(typeface,Typeface.BOLD);setTextColor(muted)
                })
                addView(TextView(this@MainActivity).apply {
                    text=if(subset.isEmpty()) "–" else "${avg/60} h ${avg%60} min"
                    textSize=15f;setTypeface(typeface,Typeface.BOLD);setTextColor(primary)
                    setPadding(0,dp(6),0,dp(4))
                })
                addView(TextView(this@MainActivity).apply {
                    text="${subset.size} Nächte";textSize=10f;setTextColor(secondary)
                })
            }
            trendRow.addView(tile,LinearLayout.LayoutParams(0,-2,1f).apply {setMargins(dp(3),0,dp(3),0)})
        }
        sleepCard.addView(TextView(this).apply {
            text="LANGZEIT-TREND  ·  Ø SCHLAFDAUER";textSize=11f;letterSpacing=.09f
            setTypeface(typeface,Typeface.BOLD);setTextColor(stageRem)
            setPadding(dp(8),dp(10),0,dp(10))
        })
        sleepCard.addView(trendRow,LinearLayout.LayoutParams(-1,-2).apply {setMargins(dp(3),0,dp(3),dp(18))})
        val grouped=nights.groupBy { s -> val z=Instant.ofEpochMilli(s.endMs).atZone(ZoneId.systemDefault()).toLocalDate(); (z.get(weekFields.weekBasedYear())*100)+z.get(weekFields.weekOfWeekBasedYear()) }
        val weeklyAverages=grouped.mapValues { (_,list)->list.map{it.totalMin}.average().toLong() }
        grouped.toSortedMap(compareByDescending<Int>{it}).forEach { (key,items) ->
            val year=key/100; val kw=key%100; val avg=weeklyAverages[key]?:0L
            val tone=if(kw%2==0) accent2 else stageRem
            val prevAvg=weeklyAverages.entries.filter{it.key<key}.maxByOrNull{it.key}?.value
            val delta=prevAvg?.let{avg-it}
            val shell:android.widget.FrameLayout=(if(light) eightbitlab.com.blurview.BlurView(this).apply{
                background=LayerDrawable(arrayOf(
                    GradientDrawable().apply{cornerRadius=dp(24).toFloat();setColor(Color.argb(designGlassAlpha(),72,88,112));setStroke(dp(4),Color.argb(designNeonAlpha(42),Color.red(tone),Color.green(tone),Color.blue(tone)))},
                    GradientDrawable().apply{cornerRadius=dp(24).toFloat();setColor(Color.TRANSPARENT);setStroke(dp(2),Color.argb(designNeonAlpha(255),Color.red(tone),Color.green(tone),Color.blue(tone)))}
                ))
                outlineProvider=android.view.ViewOutlineProvider.BACKGROUND;clipToOutline=true
                settingsBlurTarget?.let{target->setupWith(target,4f,true).setBlurEnabled(true).setBlurAutoUpdate(true).setBlurRadius(effectiveBlurRadius()).setOverlayColor(Color.argb(designGlassOverlayAlpha(),72,88,112))}
            } else android.widget.FrameLayout(this).apply{
                background=LayerDrawable(arrayOf(
                    GradientDrawable().apply{cornerRadius=dp(24).toFloat();setColor(Color.argb(210,12,18,40));setStroke(dp(4),Color.argb(48,Color.red(tone),Color.green(tone),Color.blue(tone)))},
                    GradientDrawable().apply{cornerRadius=dp(24).toFloat();setColor(Color.TRANSPARENT);setStroke(dp(2),tone)},
                    GradientDrawable(GradientDrawable.Orientation.TL_BR,intArrayOf(Color.argb(48,Color.red(tone),Color.green(tone),Color.blue(tone)),Color.TRANSPARENT,Color.argb(24,28,110,160))).apply{cornerRadius=dp(24).toFloat()}
                ))
            }).apply{layoutParams=LinearLayout.LayoutParams(-1,-2).apply{setMargins(dp(4),dp(9),dp(4),dp(9))}}
            val glowView=HistoryCardGlowView(this,tone)
            shell.addView(glowView,android.widget.FrameLayout.LayoutParams(-1,dp(76)))
            val box=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;layoutParams=android.widget.FrameLayout.LayoutParams(-1,-2)}
            val rows=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;visibility=View.GONE;setPadding(dp(9),0,dp(9),dp(12))}
            val head=LinearLayout(this).apply{
                orientation=LinearLayout.HORIZONTAL;gravity=android.view.Gravity.CENTER_VERTICAL;setPadding(dp(16),dp(15),dp(15),dp(15))
                val title=TextView(this@MainActivity).apply{text="KW $kw · $year";textSize=17f;setTextColor(primary);setTypeface(typeface,Typeface.BOLD);setShadowLayer(dp(4).toFloat(),0f,0f,tone)}
                addView(title,LinearLayout.LayoutParams(0,-2,1f))
                addView(HistoryWaveView(this@MainActivity,tone,items.sortedBy{it.endMs}.map{it.totalMin}).apply{
                    alpha=.9f
                },LinearLayout.LayoutParams(0,dp(33),.75f).apply{setMargins(dp(5),0,dp(7),0)})
                val summary=LinearLayout(this@MainActivity).apply{orientation=LinearLayout.VERTICAL;gravity=android.view.Gravity.END}
                summary.addView(TextView(this@MainActivity).apply{text="Ø ${avg/60} h ${avg%60} min  ·  ${items.size} Nächte";textSize=11f;setTextColor(primary);gravity=android.view.Gravity.END})
                if(delta!=null)summary.addView(TextView(this@MainActivity).apply{
                    val abs=kotlin.math.abs(delta)
                    text=(if(delta>=0)"▲ +" else "▼ −")+"${abs/60} h ${abs%60} min zur Vorwoche"
                    textSize=10f;setTextColor(if(delta>=0)Color.rgb(87,244,185) else Color.rgb(255,174,131));gravity=android.view.Gravity.END
                })
                addView(summary)
                val arrow=TextView(this@MainActivity).apply{text="  ▾";textSize=20f;setTextColor(tone)}
                addView(arrow)
                setOnClickListener{
                    val expanding=rows.visibility!=View.VISIBLE
                    rows.visibility=if(expanding)View.VISIBLE else View.GONE
                    arrow.text=if(expanding)"  ▴" else "  ▾"
                    glowView.expanded=expanding
                    if(expanding){rows.alpha=0f;rows.animate().alpha(1f).setDuration(350L).start()}
                }
            }
            val startDate=items.minOf{it.endMs}
            val endDate=items.maxOf{it.endMs}
            rows.addView(TextView(this).apply{
                text="${items.size} Nächte · "+dateFmt.format(Instant.ofEpochMilli(startDate))+" – "+dateFmt.format(Instant.ofEpochMilli(endDate))
                textSize=12f;setTextColor(secondary);setPadding(dp(9),dp(2),0,dp(7))
            })
            items.sortedByDescending{it.endMs}.forEach { night ->
                val nightCard=LinearLayout(this).apply{
                    orientation=LinearLayout.VERTICAL;setPadding(dp(13),dp(12),dp(13),dp(12))
                    background=LayerDrawable(arrayOf(
                        GradientDrawable(GradientDrawable.Orientation.TL_BR,intArrayOf(Color.argb(155,24,33,65),Color.argb(170,12,26,48))).apply{cornerRadius=dp(17).toFloat()},
                        GradientDrawable().apply{cornerRadius=dp(17).toFloat();setColor(Color.TRANSPARENT);setStroke(dp(1),Color.argb(95,Color.red(tone),Color.green(tone),Color.blue(tone)))}
                    ))
                    isClickable=true;isFocusable=true;setOnClickListener{showHistoryNight(night)}
                    layoutParams=LinearLayout.LayoutParams(-1,-2).apply{setMargins(0,dp(4),0,dp(4))}
                }
                nightCard.addView(LinearLayout(this).apply{
                    orientation=LinearLayout.HORIZONTAL;gravity=android.view.Gravity.CENTER_VERTICAL
                    addView(HistoryMoonView(this@MainActivity,night.totalMin,tone),LinearLayout.LayoutParams(dp(36),dp(36)))
                    addView(LinearLayout(this@MainActivity).apply{
                        orientation=LinearLayout.VERTICAL
                        addView(TextView(this@MainActivity).apply{text=dateFmt.format(Instant.ofEpochMilli(night.endMs));textSize=13f;setTextColor(primary);setTypeface(typeface,Typeface.BOLD)})
                        addView(TextView(this@MainActivity).apply{text=tf.format(Instant.ofEpochMilli(night.startMs))+" – "+tf.format(Instant.ofEpochMilli(night.endMs));textSize=10f;setTextColor(muted)})
                    },LinearLayout.LayoutParams(0,-2,1f))
                    addView(TextView(this@MainActivity).apply{text="${night.totalMin/60} h ${night.totalMin%60} min";textSize=14f;setTextColor(primary);setTypeface(typeface,Typeface.BOLD)})
                    addView(TextView(this@MainActivity).apply{text="  ›";textSize=24f;setTextColor(tone)})
                })
                nightCard.addView(HistoryStageBarView(this,
                    listOf(night.lightMin to stageLight,night.deepMin to stageDeep,night.remMin to stageRem,night.awakeMin to stageAwake)
                ),LinearLayout.LayoutParams(-1,dp(16)).apply{setMargins(dp(36),dp(5),dp(12),0)})
                rows.addView(nightCard)
            }
            rows.addView(LinearLayout(this).apply{
                orientation=LinearLayout.HORIZONTAL;gravity=android.view.Gravity.CENTER;setPadding(dp(3),dp(13),dp(3),dp(3))
                listOf("LEICHT" to stageLight,"TIEF" to stageDeep,"REM" to stageRem,"WACH" to stageAwake).forEach{(label,color)->
                    addView(TextView(this@MainActivity).apply{
                        text="● $label";textSize=10f;setTextColor(color);gravity=android.view.Gravity.CENTER
                    },LinearLayout.LayoutParams(0,-2,1f))
                }
            })
            val details=TextView(this).apply{
                text="▥  Wochen-Details";textSize=13f;setTextColor(Color.WHITE)
                setTypeface(typeface,Typeface.BOLD);gravity=android.view.Gravity.CENTER
                setPadding(dp(16),dp(11),dp(16),dp(11))
                background=GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,intArrayOf(Color.rgb(18,104,181),Color.rgb(89,29,167))).apply{
                    cornerRadius=dp(23).toFloat();setStroke(dp(2),tone)
                }
                setOnClickListener{
                    val best=items.maxByOrNull{it.totalMin}
                    val sum=items.sumOf{it.totalMin}
                    android.app.AlertDialog.Builder(this@MainActivity)
                        .setTitle("KW $kw · $year")
                        .setMessage("Nächte: ${items.size}\\nDurchschnitt: ${avg/60} h ${avg%60} min\\nGesamtschlaf: ${sum/60} h ${sum%60} min"+
                            (best?.let{"\\nLängste Nacht: ${it.totalMin/60} h ${it.totalMin%60} min"}?:""))
                        .setPositiveButton("Schließen",null).show()
                }
            }
            rows.addView(details,LinearLayout.LayoutParams(-2,-2).apply{gravity=android.view.Gravity.END;setMargins(0,dp(8),dp(8),0)})
            box.addView(head);box.addView(rows)
            // Match the decoration to actual content height, not the viewport.
            // In particular the expanded dates must sit INSIDE the glowing outline.
            box.addOnLayoutChangeListener { _, _, _, _, bottom, _, _, _, _ ->
                val desired=bottom.coerceAtLeast(dp(76))
                if(glowView.layoutParams.height!=desired){
                    glowView.layoutParams=glowView.layoutParams.apply { height=desired }
                }
            }
            shell.addView(box);sleepCard.addView(shell)
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
        val glass=Color.argb(designGlassAlpha(if(light) 168 else 205),if(light) 72 else 12,if(light) 88 else 18,if(light) 112 else 40)
        // Calendar animations are short, interaction-driven and never loop.
        actionsTitle.visibility=View.GONE; actionsBox.visibility=View.GONE
        pageTitle.text="Kalender"; pageSubtitle.text="Deine Nächte · automatisch dort, wo du sie willst"
        sleepCard.removeAllViews(); sleepCard.background=null
        fun card(title:String,sub:String,tone:Int,body:LinearLayout.()->Unit):View {
            val fill=Color.argb(designGlassAlpha(if(light) 168 else 205),if(light) 72 else 12,if(light) 88 else 18,if(light) 112 else 40)
            val cleanBottom=title.contains("LETZTE") || title.contains("STATUS")
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
                )) else GradientDrawable(GradientDrawable.Orientation.TL_BR,if(cleanBottom) intArrayOf(
                    Color.rgb(12,21,39),Color.rgb(20,23,48),Color.rgb(11,26,43)
                ) else intArrayOf(
                    Color.argb(244,17,26,59),
                    Color.argb(237,28,21,66),
                    Color.argb(242,10,35,55)
                )).apply {
                    cornerRadius=dp(22).toFloat()
                    setStroke(dp(2),Color.argb(225,Color.red(tone),Color.green(tone),Color.blue(tone)))
                }
                layoutParams=android.widget.FrameLayout.LayoutParams(-1,-2)
                if(light && this is eightbitlab.com.blurview.BlurView){
                    outlineProvider=android.view.ViewOutlineProvider.BACKGROUND;clipToOutline=true
                    settingsBlurTarget?.let{target->setupWith(target,4f,true).setBlurEnabled(true).setBlurAutoUpdate(true).setBlurRadius(effectiveBlurRadius()).setOverlayColor(Color.argb(designGlassOverlayAlpha(),72,88,112))}
                }
                addView(LinearLayout(this@MainActivity).apply{
                    orientation=LinearLayout.VERTICAL;setPadding(dp(18),dp(16),dp(18),dp(16));background=null
                    addView(TextView(this@MainActivity).apply{
                        text=title;textSize=12f;letterSpacing=.08f
                        setTextColor(tone);setTypeface(typeface,Typeface.BOLD)
                    })
                    addView(TextView(this@MainActivity).apply{text=sub;textSize=11f;setTextColor(if(light) Color.argb(215,245,248,255) else secondary);setPadding(0,dp(3),0,dp(12))})
                    body()
                })
            }
            host.addView(glassCard)
            return host
        }
        val s=lastSummary ?: sleepHistory.maxByOrNull{it.endMs}
        sleepCard.addView(card("✦  LETZTE ERFASSTE NACHT","Kompakte Vorschau für den Kalenderexport",accent2){
            if(s!=null){val tf=DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault());val fmt={m:Long->(m/60).toString()+" h "+(m%60).toString()+" min"}
                addView(LinearLayout(this@MainActivity).apply {
                    orientation=LinearLayout.HORIZONTAL
                    gravity=android.view.Gravity.CENTER_VERTICAL
                    addView(HistoryMoonView(this@MainActivity,s.totalMin,stageRem,true).apply {
                        contentDescription="SleepMoon · Vorschau der letzten Nacht"
                    },LinearLayout.LayoutParams(dp(76),dp(76)).apply { rightMargin=dp(16) })
                    addView(TextView(this@MainActivity).apply {
                        text=fmt(s.totalMin)+"\n"+tf.format(Instant.ofEpochMilli(s.startMs))+" – "+tf.format(Instant.ofEpochMilli(s.endMs))+"\n"+DateTimeFormatter.ofPattern("EEE, dd. MMMM",java.util.Locale.GERMAN).withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(s.endMs))
                        textSize=19f;setTextColor(primary);setTypeface(typeface,Typeface.BOLD)
                        setLineSpacing(dp(3).toFloat(),1f)
                    },LinearLayout.LayoutParams(0,-2,1f))
                })
            } else addView(TextView(this@MainActivity).apply{text="Noch keine Nacht synchronisiert";setTextColor(primary)})
        })
        sleepCard.addView(card("⚡  AUTOMATIK","Neue Nächte selbstständig eintragen",Color.rgb(74,224,181)){
            addView(LinearLayout(this@MainActivity).apply{gravity=android.view.Gravity.CENTER_VERTICAL
                addView(TextView(this@MainActivity).apply{text=if(calendarAutoEnabled()) "●  Aktiv  ·  Neue Nächte werden geprüft" else "○  Automatik deaktiviert";textSize=13f;setTextColor(primary);setTypeface(typeface,Typeface.BOLD);layoutParams=LinearLayout.LayoutParams(0,-2,1f)})
                addView(android.widget.Switch(this@MainActivity).apply {
                    isChecked=calendarAutoEnabled()
                    thumbTintList=ColorStateList.valueOf(if(calendarAutoEnabled()) Color.rgb(94,242,201) else Color.rgb(182,177,197))
                    setOnCheckedChangeListener { _,checked ->
                        calendarPrefs().edit().putBoolean("auto_enabled",checked).apply()
                        animate().alpha(.65f).setDuration(110).withEndAction {
                            showCalendarPlaceholder()
                        }.start()
                    }
                })
            })
        val bgPrefs=calendarPrefs()
        val lastCheck=bgPrefs.getLong("last_background_check",0L)
        val lastAuto=bgPrefs.getLong("last_auto_insert_at",0L)
        val statusFmt=DateTimeFormatter.ofPattern("dd.MM. · HH:mm").withZone(ZoneId.systemDefault())
        val selectedCalendar=bgPrefs.getString("calendar_name",null)
        val lastNightForStatus=lastSummary ?: sleepHistory.maxByOrNull{it.endMs}
        val existingNight=if(calendarPermissionReady() && bgPrefs.getLong("calendar_id",-1)>=0 && lastNightForStatus!=null)
            runCatching{calendarEventExists(lastNightForStatus)}.getOrNull() else null
        val statusText=buildString{
            append(if(calendarAutoEnabled()) "●  Automatik aktiv" else "○  Automatik aus")
            append("\\nZielkalender: ").append(selectedCalendar ?: "nicht ausgewählt")
            append("\\nLetzte Nacht: ").append(when(existingNight){true->"✓ Im Zielkalender vorhanden";false->if(calendarPermissionReady() && selectedCalendar!=null) "○ Noch nicht eingetragen" else "Kalenderzugriff oder Ziel fehlt";null->"Status nicht prüfbar"})
            append("\nLetzte Hintergrundprüfung: ")
            append(if(lastCheck>0) statusFmt.format(Instant.ofEpochMilli(lastCheck))+" Uhr" else "noch keine")
            append("\nLetzter automatischer Eintrag: ")
            append(if(lastAuto>0) statusFmt.format(Instant.ofEpochMilli(lastAuto))+" Uhr" else "noch keiner")
        }
        addView(LinearLayout(this@MainActivity).apply { orientation=LinearLayout.VERTICAL; setPadding(0,dp(12),0,0)
            addView(TextView(this@MainActivity).apply { text="◈  SYNCHRONISATIONSSTATUS";textSize=11f;setTypeface(typeface,Typeface.BOLD);setTextColor(Color.rgb(94,242,201));setPadding(0,0,0,dp(8)) })
            addView(LinearLayout(this@MainActivity).apply {
                orientation=LinearLayout.HORIZONTAL;gravity=android.view.Gravity.CENTER_VERTICAL
                addView(TextView(this@MainActivity).apply {
                    text=statusText;textSize=12f
                    setTextColor(if(light) Color.rgb(235,240,252) else muted)
                },LinearLayout.LayoutParams(0,-2,1f))
                addView(TextView(this@MainActivity).apply {
                    text=if(lastAuto>0) "✓  Eingetragen" else if(calendarAutoEnabled()) "●  Aktiv" else "○  Pausiert"
                    textSize=11f
                    setTypeface(typeface,Typeface.BOLD)
                    setTextColor(if(calendarAutoEnabled()) Color.rgb(106,240,199) else muted)
                    gravity=android.view.Gravity.CENTER
                    setPadding(dp(11),dp(7),dp(11),dp(7))
                    background=GradientDrawable().apply {
                        cornerRadius=dp(30).toFloat()
                        setColor(Color.argb(230,14,39,48))
                        setStroke(dp(1),Color.argb(180,65,204,178))
                    }
                },LinearLayout.LayoutParams(-2,-2).apply { leftMargin=dp(8) })
            })
        })
        })
        val actionButton=MaterialButton(this).apply{
            text="▣    JETZT EINTRAGEN    ❯";isAllCaps=false;textSize=16f;setTypeface(typeface,Typeface.BOLD)
            setTextColor(Color.WHITE)
            backgroundTintList=ColorStateList.valueOf(Color.TRANSPARENT)
            strokeWidth=0
            strokeColor=ColorStateList.valueOf(if(light) Color.rgb(95,125,255) else Color.rgb(117,209,255))
            cornerRadius=dp(18);insetTop=0;insetBottom=0;layoutParams=android.widget.FrameLayout.LayoutParams(-1,-1)
            setOnClickListener{
                animate().scaleX(.975f).scaleY(.975f).setDuration(90).withEndAction {
                    animate().scaleX(1f).scaleY(1f).setDuration(190).start()
                }.start()
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
        }
        sleepCard.addView(android.widget.FrameLayout(this).apply {
            background=GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,intArrayOf(
                Color.rgb(19,103,166),Color.rgb(56,48,167),Color.rgb(143,31,194)
            )).apply { cornerRadius=dp(19).toFloat();setStroke(dp(2),Color.rgb(118,212,255)) }
            elevation=dp(5).toFloat()
            addView(actionButton)
        },LinearLayout.LayoutParams(-1,dp(72)).apply { setMargins(0,dp(4),0,dp(14)) })
        sleepCard.addView(card("📅  ZIELKALENDER","Wähle einen Kalender auf diesem Gerät",stageRem){
            val cp=getSharedPreferences("sleepsync_calendar",MODE_PRIVATE); val selected=cp.getString("calendar_name",null); addView(TextView(this@MainActivity).apply{text=(selected ?: if(calendarPermissionReady()) "Kalender auswählen" else "Kalenderzugriff erlauben")+"  ⌄";textSize=16f;setTextColor(primary);setTypeface(typeface,Typeface.BOLD);setPadding(dp(12),dp(12),dp(12),dp(12));background=GradientDrawable().apply{cornerRadius=dp(15).toFloat();setColor(if(light) Color.argb(designGlassAlpha(),38,46,92) else Color.argb(205,26,24,63));setStroke(dp(1),stageRem)};isClickable=true;setOnClickListener{chooseCalendar()}})
            addView(TextView(this@MainActivity).apply{text="Google · Outlook · Exchange und weitere verfügbare Android-Kalender";textSize=11f;setTextColor(muted);setPadding(0,dp(10),0,0)})
        })
        sleepCard.addView(card("◷  KALENDER-STATUS","Erfasste Nächte · tatsächlichen Eintragsstatus prüfen",stageLight){
            val recent=sleepHistory.sortedByDescending{it.endMs}.take(4)
            val canVerify=calendarPermissionReady() && calendarPrefs().getLong("calendar_id",-1)>=0
            if(recent.isEmpty()) addView(TextView(this@MainActivity).apply{text="Noch keine Einträge";setTextColor(if(light) Color.rgb(92,104,132) else Color.rgb(180,190,215))})
            recent.forEachIndexed { index,s0 ->
                val df=DateTimeFormatter.ofPattern("EEE, dd.MM.",java.util.Locale.GERMAN).withZone(ZoneId.systemDefault())
                val clock=DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())
                val row=LinearLayout(this@MainActivity).apply {
                    orientation=LinearLayout.HORIZONTAL
                    gravity=android.view.Gravity.CENTER_VERTICAL
                    setPadding(dp(7),dp(9),dp(7),dp(9))
                    background=GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                        intArrayOf(Color.argb(100,30,47,92),Color.argb(65,27,25,63))).apply {
                        cornerRadius=dp(13).toFloat()
                        setStroke(dp(1),Color.argb(85,85,196,248))
                    }
                    addView(HistoryMoonView(this@MainActivity,s0.totalMin,stageRem),
                        LinearLayout.LayoutParams(dp(38),dp(38)).apply{rightMargin=dp(8)})
                    addView(LinearLayout(this@MainActivity).apply {
                        orientation=LinearLayout.VERTICAL
                        addView(TextView(this@MainActivity).apply {
                            text=df.format(Instant.ofEpochMilli(s0.endMs))
                            textSize=12f;setTypeface(typeface,Typeface.BOLD);setTextColor(primary)
                        })
                        addView(TextView(this@MainActivity).apply {
                            text=clock.format(Instant.ofEpochMilli(s0.startMs))+" – "+clock.format(Instant.ofEpochMilli(s0.endMs))
                            textSize=10f;setTextColor(muted)
                        })
                    },LinearLayout.LayoutParams(0,-2,1f))
                    addView(TextView(this@MainActivity).apply {
                        text=if(canVerify) (if(runCatching{calendarEventExists(s0)}.getOrDefault(false)) "✓ Im Kalender" else "○ Ausstehend") else "○ Nicht prüfbar"
                        textSize=12f;setTextColor(primary);setTypeface(typeface,Typeface.BOLD)
                    })
                    addView(TextView(this@MainActivity).apply {
                        // These are recorded nights, not proof of successful calendar insertion.
                        text="›";textSize=22f;setTextColor(Color.rgb(104,229,213))
                        setPadding(dp(8),0,0,0)
                    })
                }
                addView(row,LinearLayout.LayoutParams(-1,-2).apply { bottomMargin=dp(6) })
                if(android.animation.ValueAnimator.areAnimatorsEnabled()) {
                    row.alpha=0f
                    row.animate().alpha(1f).setStartDelay(index*55L).setDuration(250L).start()
                }
            }
        })

    }
    private fun showSettings() {
        val density=resources.displayMetrics.density
        fun dp(n:Int)=(n*density).toInt()
        pageTitle.text="SleepSync"
        pageSubtitle.text="Einstellungen · Dein Schlaf. Dein Stil. Deine Kontrolle."
        actionsTitle.visibility=View.GONE
        actionsBox.visibility=View.GONE
        sleepCard.removeAllViews()
        val ui=getSharedPreferences("sleepsync_ui",MODE_PRIVATE)
        val mode=ui.getString("theme","dark") ?: "dark"
        val themeLabel=when(mode){"light"->"Neon Sunrise";"system"->"Automatisch";else->"OLED Night"}
        val cyan=Color.rgb(70,220,248)
        val violet=Color.rgb(169,114,255)
        val ink=Color.rgb(8,13,33)
        val muted=Color.rgb(181,197,226)
        fun panel(color:Int)=GradientDrawable(GradientDrawable.Orientation.TL_BR,intArrayOf(
            Color.argb(247,5,13,38),Color.argb(244,13,16,53),Color.argb(249,5,11,33)
        )).apply {cornerRadius=dp(24).toFloat();setStroke(dp(1),Color.argb(150,Color.red(color),Color.green(color),Color.blue(color)))}
        fun label(t:String)=TextView(this).apply{
            text=t;textSize=10f;letterSpacing=.17f;setTypeface(typeface,Typeface.BOLD)
            setTextColor(cyan);setPadding(dp(6),dp(17),0,dp(10))
        }
        fun tile(symbol:String,title:String,subtitle:String,color:Int,click:()->Unit):LinearLayout {
            return LinearLayout(this).apply{
                orientation=LinearLayout.VERTICAL
                setPadding(dp(16),dp(18),dp(14),dp(17))
                background=panel(color)
                isClickable=true;isFocusable=true
                setOnClickListener{click()}
                addView(TextView(this@MainActivity).apply{
                    text=symbol;textSize=26f;setTextColor(color)
                    setShadowLayer(dp(8).toFloat(),0f,0f,color)
                })
                addView(TextView(this@MainActivity).apply{
                    text=title;textSize=17f;setTypeface(typeface,Typeface.BOLD)
                    setTextColor(Color.WHITE);setPadding(0,dp(10),0,dp(5))
                })
                addView(TextView(this@MainActivity).apply{
                    text=subtitle;textSize=11f;setTextColor(muted)
                    maxLines=2
                })
            }
        }
        fun cosmicArtwork(moon:Boolean):View = object:View(this@MainActivity) {
            private val brush=Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            private val bitmap=android.graphics.BitmapFactory.decodeResource(resources,
                if(moon) R.drawable.cosmic_crescent else R.drawable.cosmic_planet)
            override fun onDraw(canvas:Canvas) {
                val w=width.toFloat();val h=height.toFloat()
                if(w<=0f||h<=0f)return
                // Keep the artwork on the right, while preserving a quiet text area.
                val dst=android.graphics.RectF(w*.39f,0f,w,h)
                val srcAspect=bitmap.width.toFloat()/bitmap.height
                val dstAspect=dst.width()/dst.height()
                val src=if(srcAspect>dstAspect) {
                    val crop=(bitmap.height*dstAspect).toInt()
                    val left=(bitmap.width-crop)/2
                    android.graphics.Rect(left,0,left+crop,bitmap.height)
                } else {
                    val crop=(bitmap.width/dstAspect).toInt()
                    val top=(bitmap.height-crop)/2
                    android.graphics.Rect(0,top,bitmap.width,top+crop)
                }
                canvas.drawBitmap(bitmap,src,dst,brush)
                brush.shader=android.graphics.LinearGradient(0f,0f,w,0f,
                    intArrayOf(0xFF10142F.toInt(),0xF5101432.toInt(),0x8A101431.toInt(),0x10101431),
                    floatArrayOf(0f,.42f,.73f,1f),Shader.TileMode.CLAMP)
                canvas.drawRect(0f,0f,w,h,brush)
                brush.shader=null
            }
        }
        // Editorial masthead: the whole screen is a navigation dashboard, not a list of settings.
        sleepCard.addView(android.widget.FrameLayout(this).apply {
            background=GradientDrawable(GradientDrawable.Orientation.TL_BR,intArrayOf(
                0xFF101735.toInt(),0xFF241347.toInt(),0xFF081B3E.toInt()
            )).apply { cornerRadius=dp(29).toFloat();setStroke(dp(2),cyan) }
            clipToOutline=true;outlineProvider=android.view.ViewOutlineProvider.BACKGROUND
            addView(cosmicArtwork(false),android.widget.FrameLayout.LayoutParams(-1,-1))
            addView(LinearLayout(this@MainActivity).apply{
            orientation=LinearLayout.VERTICAL
            setPadding(dp(23),dp(25),dp(23),dp(23))
            addView(TextView(this@MainActivity).apply{
                text="✦  SLEEPSYNC / YOUR UNIVERSE";textSize=10f;letterSpacing=.14f
                setTypeface(typeface,Typeface.BOLD);setTextColor(cyan)
            })
            addView(TextView(this@MainActivity).apply{
                text="Dein Kosmos.\nDeine Kontrolle."
                textSize=29f;setTypeface(typeface,Typeface.BOLD);setTextColor(Color.WHITE)
                setPadding(0,dp(16),0,dp(11))
                setShadowLayer(dp(14).toFloat(),0f,0f,violet)
            })
            addView(TextView(this@MainActivity).apply{
                text="Dein Schlaf. Deine Daten. Dein Design."
                textSize=13f;setTextColor(Color.rgb(212,223,248))
            })
            addView(TextView(this@MainActivity).apply{
                text="●  ${if(garminClient.isLinked()) "GARMIN VERBUNDEN" else "GARMIN OFFLINE"}     ✦  $themeLabel"
                textSize=10f;setTextColor(Color.rgb(119,237,215))
                setPadding(0,dp(19),0,0)
            })
            },android.widget.FrameLayout.LayoutParams(-1,-2))
        },LinearLayout.LayoutParams(-1,-2).apply{setMargins(dp(3),dp(5),dp(3),dp(10))})
        sleepCard.addView(label("✦  DEIN KOSMOS"))
        val studio=android.widget.FrameLayout(this).apply {
            background=GradientDrawable(GradientDrawable.Orientation.TL_BR,intArrayOf(
                Color.rgb(14,11,46),Color.rgb(57,17,98),Color.rgb(9,47,92),Color.rgb(7,12,35)
            )).apply {cornerRadius=dp(27).toFloat();setStroke(dp(2),cyan)}
            clipToOutline=true;outlineProvider=android.view.ViewOutlineProvider.BACKGROUND
            setOnClickListener{showDesignStudio()}
            addView(cosmicArtwork(true),android.widget.FrameLayout.LayoutParams(-1,-1))
            addView(LinearLayout(this@MainActivity).apply {
                orientation=LinearLayout.VERTICAL;setPadding(dp(20),dp(22),dp(17),dp(20))
                addView(TextView(this@MainActivity).apply {
                    text="✦  DESIGN STUDIO";textSize=11f;letterSpacing=.16f
                    setTypeface(typeface,Typeface.BOLD);setTextColor(Color.rgb(131,225,255))
                })
                addView(TextView(this@MainActivity).apply {
                    text="Dein Universum.\nDeine Regeln.";textSize=26f
                    setTypeface(typeface,Typeface.BOLD);setTextColor(Color.WHITE)
                    setPadding(0,dp(13),0,dp(8))
                    setShadowLayer(dp(12).toFloat(),0f,0f,violet)
                })
                addView(TextView(this@MainActivity).apply {
                    text="Wallpaper, Farben, Effekte\nund Animationen."
                    textSize=12f;setTextColor(Color.rgb(225,231,255))
                })
                addView(TextView(this@MainActivity).apply {
                    text="✦  DESIGN STUDIO ÖFFNEN   →"
                    textSize=12f;setTypeface(typeface,Typeface.BOLD)
                    setTextColor(Color.rgb(105,244,255));setPadding(0,dp(24),0,0)
                })
            },android.widget.FrameLayout.LayoutParams(-1,-1))
        }
        sleepCard.addView(studio,LinearLayout.LayoutParams(-1,dp(245)).apply {
            setMargins(dp(3),0,dp(3),dp(10))
        })
        sleepCard.addView(label("✦  SCHNELLZUGRIFF"))
        val quick=android.widget.GridLayout(this).apply{columnCount=2}
        val quickItems=listOf(
            tile("↻","Updates","Neue Version prüfen",cyan){checkForPreviewUpdate()},
            tile("⚙","Automatik","Sync & Kalender",violet){showAutomationSettings()},
            tile("⌚","Garmin","Verbindung & Daten",Color.rgb(241,113,225)){showGarminSettings()},
            tile("♥","Health Connect","Gesundheitsdaten",cyan){showHealthSettings()}
        )
        quickItems.forEachIndexed{i,v->
            quick.addView(v,android.widget.GridLayout.LayoutParams(
                android.widget.GridLayout.spec(i/2),android.widget.GridLayout.spec(i%2,1f)
            ).apply{width=0;columnSpec=android.widget.GridLayout.spec(i%2,1f);setMargins(dp(3),dp(3),dp(3),dp(3))})
        }
        sleepCard.addView(quick)
        sleepCard.addView(label("✦  TOOLS & DIAGNOSE"))
        fun compact(symbol:String,title:String,sub:String,color:Int,click:()->Unit) {
            sleepCard.addView(LinearLayout(this).apply{
                orientation=LinearLayout.HORIZONTAL;gravity=android.view.Gravity.CENTER_VERTICAL
                setPadding(dp(17),dp(15),dp(15),dp(15))
                background=panel(color)
                setOnClickListener{click()}
                addView(TextView(this@MainActivity).apply{
                    text=symbol;textSize=22f;setTextColor(color)
                },LinearLayout.LayoutParams(dp(42),-2))
                addView(LinearLayout(this@MainActivity).apply{
                    orientation=LinearLayout.VERTICAL
                    addView(TextView(this@MainActivity).apply{
                        text=title;textSize=15f;setTypeface(typeface,Typeface.BOLD);setTextColor(Color.WHITE)
                    })
                    addView(TextView(this@MainActivity).apply{
                        text=sub;textSize=11f;setTextColor(muted);setPadding(0,dp(3),0,0)
                    })
                },LinearLayout.LayoutParams(0,-2,1f))
                addView(TextView(this@MainActivity).apply{text="›";textSize=25f;setTextColor(color)})
            },LinearLayout.LayoutParams(-1,-2).apply{setMargins(dp(3),dp(4),dp(3),dp(4))})
        }
        compact("◈","Datenschutz","Lokale Daten & Berechtigungen",Color.rgb(131,189,255)){showPrivacySettings()}
        compact("ⓘ","Über SleepSync","Version, Build & Informationen",violet){showAboutSettings()}
        var toolsOpen=false
        val toolContainer=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;visibility=View.GONE}
        compact("⚙","TOOLS & DIAGNOSE","Verbindungen, Datenabruf und Systemprüfung",cyan){
            toolsOpen=!toolsOpen
            toolContainer.visibility=if(toolsOpen)View.VISIBLE else View.GONE
        }
        val toolNames=listOf("Health Connect · Berechtigungen","Garmin Connect · Verbinden","Garmin Connect · Trennen","Schlafdaten neu laden","App-Signatur anzeigen")
        for(i in 0 until minOf(5,actionsBox.childCount)){
            val original=actionsBox.getChildAt(i)
            toolContainer.addView(TextView(this).apply{
                text="↗   ${toolNames[i]}";textSize=13f;setTextColor(Color.rgb(213,232,250))
                setPadding(dp(21),dp(15),dp(14),dp(15))
                background=panel(Color.rgb(77,136,199))
                setOnClickListener{original.performClick()}
            },LinearLayout.LayoutParams(-1,-2).apply{setMargins(dp(9),dp(3),dp(9),dp(3))})
        }
        sleepCard.addView(toolContainer)
        sleepCard.addView(TextView(this).apply{
            text="NEVER GO BACK. ALWAYS FORWARD.  ✦"
            textSize=10f;letterSpacing=.11f;gravity=android.view.Gravity.CENTER
            setTextColor(Color.rgb(160,136,225))
            setPadding(0,dp(30),0,dp(28))
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
            .setTitle("Garmin Connect · Meine Geräte")
            .setMessage(if(linked) "Garmin Connect ist verbunden. Geräteerkennung liest nur verfügbare Modell-, Firmware- und Sync-Informationen aus." else "Verbinde SleepSync zuerst mit Garmin Connect.")
            .setPositiveButton(if(linked) "Geräte erkennen" else "Verbinden") { _,_ ->
                if(!linked) showGarminLogin()
                else {
                    val progress=AlertDialog.Builder(this).setTitle("Garmin-Geräte")
                        .setMessage("Geräteinformationen werden abgefragt …")
                        .setCancelable(false).create()
                    progress.show()
                    CoroutineScope(Dispatchers.Main).launch {
                        val devices=withContext(Dispatchers.IO) { runCatching { garminClient.devices() }.getOrDefault(emptyList()) }
                        val insights=withContext(Dispatchers.IO) {
                            runCatching { garminClient.dailyInsights(java.time.LocalDate.now()) }.getOrDefault(emptyList())
                        }
                        progress.dismiss()
                        val message=if(devices.isEmpty())
                            "Garmin hat keine auswertbaren Geräteinformationen geliefert. Deine Schlafdatenverbindung bleibt unverändert."
                        else devices.joinToString("\n\n") { d ->
                            val extra=if(d.details.isEmpty()) "Keine weiteren Metadaten geliefert."
                                else d.details.joinToString("\n") { (label,value) -> "$label: $value" }
                            "⌚ ${d.name}\nModell: ${d.model ?: "Nicht verfügbar"}\nFirmware: ${d.firmware ?: "Nicht verfügbar"}\nLetzter Sync: ${d.lastSync ?: "Nicht verfügbar"}\n\nWEITERE GERÄTEDATEN\n$extra"
                        }
                        val wellness=if(insights.isEmpty())
                            "\n\nTAGESDATEN\nKeine zusätzlichen Garmin-Werte verfügbar."
                        else "\n\nTAGESDATEN · HEUTE\n"+insights.joinToString("\n") { "${it.label}: ${it.value}" }
                        showGarminIntelligence(devices.firstOrNull()?.name ?: "Garmin Connect", insights, message)

                    }
                }
            }
            .setNeutralButton(if(linked) "Schlafdaten laden" else null) { _,_ -> testRead() }
            .setNegativeButton("Schließen",null)
            .create()
        dialog.setOnShowListener { styleSleepSyncDialog(dialog) }
        dialog.show()
    }

    private fun showGarminIntelligence(
        deviceName:String,
        insights:List<GarminConnectClient.DailyInsight>,
        deviceDetails:String
    ) {
        val density=resources.displayMetrics.density
        fun dp(n:Int)=(n*density).toInt()
        fun surface(colors:IntArray,stroke:Int):GradientDrawable =
            GradientDrawable(GradientDrawable.Orientation.TL_BR,colors).apply {
                cornerRadius=dp(22).toFloat()
                setStroke(dp(1),stroke)
            }
        val cyan=Color.rgb(43,217,242)
        val violet=Color.rgb(170,103,255)
        val root=LinearLayout(this).apply {
            orientation=LinearLayout.VERTICAL
            setPadding(dp(18),dp(20),dp(18),dp(18))
            background=surface(intArrayOf(0xFF080D22.toInt(),0xFF1A1038.toInt()),cyan)
        }
        fun label(text:String,size:Float,color:Int,bold:Boolean=false):TextView =
            TextView(this).apply {
                this.text=text
                textSize=size
                setTextColor(color)
                if(bold) setTypeface(null,Typeface.BOLD)
            }
        root.addView(label("✦ GARMIN INTELLIGENCE",12f,cyan,true))
        root.addView(label("Dein Körper. Deine Daten.",23f,Color.WHITE,true).apply {
            setPadding(0,dp(8),0,dp(5))
        })
        root.addView(label("⌚ $deviceName · Live aus Garmin Connect",12f,0xFFB4C4E4.toInt()).apply {
            setPadding(0,0,0,dp(18))
        })
        val grid=GridLayout(this).apply { columnCount=2 }
        val metrics=listOf(
            Triple("BODY BATTERY","Body Battery",0xFF32E6B2.toInt()),
            Triple("STRESS Ø","Stress",0xFFB67CFF.toInt()),
            Triple("RUHEPULS","Ruhepuls",0xFFFF668D.toInt()),
            Triple("SCHRITTE","Schritte",cyan),
            Triple("STRESS MAX","Stress Maximum",0xFFFFB45C.toInt())
        )
        metrics.forEachIndexed { index,(title,key,color) ->
            val raw=insights.firstOrNull { it.label==key }?.value
                ?: if(key=="Stress") insights.firstOrNull { it.label=="Stress Ø" }?.value else null
            val tile=LinearLayout(this).apply {
                orientation=LinearLayout.VERTICAL
                setPadding(dp(13),dp(13),dp(10),dp(13))
                background=surface(intArrayOf(0xFF13243B.toInt(),0xFF1D1238.toInt()),color)
            }
            tile.addView(label("✦ $title",10f,color,true))
            tile.addView(label(raw ?: "—",29f,Color.WHITE,true).apply {
                setPadding(0,dp(7),0,dp(2))
            })
            tile.addView(label(if(raw==null) "Nicht verfügbar" else "Garmin Tageswert",10f,0xFFB4C4E4.toInt()))
            val params=GridLayout.LayoutParams().apply {
                width=0
                columnSpec=GridLayout.spec(index%2,1f)
                setMargins(dp(3),dp(3),dp(3),dp(3))
            }
            grid.addView(tile,params)
        }
        root.addView(grid)
        root.addView(label("✦ GERÄTEINFORMATIONEN",11f,violet,true).apply {
            setPadding(0,dp(18),0,dp(8))
        })
        root.addView(label(deviceDetails,12f,0xFFD3DDF4.toInt()).apply {
            setPadding(dp(10),dp(10),dp(10),dp(10))
            background=surface(intArrayOf(0xFF121D35.toInt(),0xFF17132A.toInt()),0xFF574C91.toInt())
        })
        root.addView(label("Nur tatsächlich gelieferte Werte · Keine Schätzungen",10f,0xFF91A2C7.toInt()).apply {
            setPadding(0,dp(14),0,0)
        })
        lateinit var dialog:AlertDialog
        val actions=LinearLayout(this).apply {
            orientation=LinearLayout.HORIZONTAL
            setPadding(0,dp(18),0,dp(4))
        }
        val testButton=label("✦ 30 TAGE TESTEN",13f,cyan,true).apply {
            gravity=android.view.Gravity.CENTER
            setPadding(dp(10),dp(15),dp(10),dp(15))
            background=surface(intArrayOf(0xFF14344A.toInt(),0xFF172644.toInt()),cyan)
            setOnClickListener {
                dialog.dismiss()
                val loading=AlertDialog.Builder(this@MainActivity)
                    .setTitle("Garmin · Historische Daten")
                    .setMessage("Prüfe sieben Stichproben aus 30 Tagen …")
                    .setCancelable(false).create()
                loading.show()
                Thread {
                    val result=runCatching { garminClient.historicalAvailability(30) }
                        .getOrElse { "Abruf fehlgeschlagen: ${it.javaClass.simpleName}" }
                    runOnUiThread {
                        if(!isFinishing && !isDestroyed) {
                            loading.dismiss()
                            AlertDialog.Builder(this@MainActivity)
                                .setTitle("Garmin · 30-Tage-Datencheck")
                                .setMessage(result)
                                .setPositiveButton("FERTIG",null).show()
                        }
                    }
                }.start()
            }
        }
        actions.addView(testButton,LinearLayout.LayoutParams(0,dp(52),1f).apply {
            marginEnd=dp(8)
        })
        actions.addView(label("SCHLIESSEN",12f,0xFFB7C9E8.toInt(),true).apply {
            gravity=android.view.Gravity.CENTER
            setOnClickListener { dialog.dismiss() }
        },LinearLayout.LayoutParams(dp(102),dp(52)))
        root.addView(actions)
        val scroll=ScrollView(this).apply {
            isFillViewport=false
            addView(root)
        }
        dialog=AlertDialog.Builder(this).setView(scroll).create()
        dialog.setOnShowListener {
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dialog.window?.setLayout((resources.displayMetrics.widthPixels*0.93f).toInt(),ViewGroup.LayoutParams.WRAP_CONTENT)
        }
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

    // Shared optical DNA for the appearance selector and Design Studio.
    private fun opticsGlassBackground(tone:Int, radius:Int=22):LayerDrawable {
        val d=resources.displayMetrics.density
        val edge=(2*d).toInt().coerceAtLeast(1)
        return LayerDrawable(arrayOf(
            GradientDrawable().apply { cornerRadius=radius*d; setColor(Color.argb(designGlassAlpha(),72,88,112)); setStroke((4*d).toInt().coerceAtLeast(1),Color.argb(designNeonAlpha(42),Color.red(tone),Color.green(tone),Color.blue(tone))) },
            GradientDrawable().apply { cornerRadius=radius*d; setColor(Color.TRANSPARENT); setStroke(edge,Color.argb(designNeonAlpha(255),Color.red(tone),Color.green(tone),Color.blue(tone))) }
        ))
    }

    private fun showDesignStudio() {
        val d=resources.displayMetrics.density; fun dp(v:Int)=(v*d).toInt()
        val p=getSharedPreferences("sleepsync_design",MODE_PRIVATE)
        pageTitle.text="Design Studio";pageSubtitle.text="Gestalte deinen persönlichen Schlafkosmos"
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
        fun row(title:String,sub:String,tone:Int,click:()->Unit)=sleepCard.addView(eightbitlab.com.blurview.BlurView(this).apply{
            background=opticsGlassBackground(tone);outlineProvider=android.view.ViewOutlineProvider.BACKGROUND;clipToOutline=true
            settingsBlurTarget?.let{target->setupWith(target,4f,true).setBlurEnabled(true).setBlurAutoUpdate(true).setBlurRadius(effectiveBlurRadius()).setOverlayColor(Color.argb(designGlassOverlayAlpha(),72,88,112))}
            layoutParams=LinearLayout.LayoutParams(-1,-2).apply{setMargins(0,dp(4),0,dp(4))}
            addView(LinearLayout(this@MainActivity).apply{orientation=LinearLayout.HORIZONTAL;gravity=android.view.Gravity.CENTER_VERTICAL;setPadding(dp(15),dp(12),dp(15),dp(12))
                addView(View(this@MainActivity).apply{background=GradientDrawable().apply{shape=GradientDrawable.OVAL;setColor(tone);setStroke(dp(2),Color.WHITE)};layoutParams=LinearLayout.LayoutParams(dp(30),dp(30)).apply{marginEnd=dp(13)}})
                addView(LinearLayout(this@MainActivity).apply{orientation=LinearLayout.VERTICAL;addView(TextView(this@MainActivity).apply{text=title;textSize=15f;setTypeface(typeface,Typeface.BOLD);setTextColor(Color.WHITE)});addView(TextView(this@MainActivity).apply{text=sub;textSize=11f;setTextColor(Color.rgb(220,228,245))})},LinearLayout.LayoutParams(0,-2,1f))
                addView(TextView(this@MainActivity).apply{text="›";textSize=27f;setTextColor(tone)})
            });setOnClickListener{click()}
        })
        sleepCard.addView(TextView(this).apply {
            text="✦  SLEEPSYNC / ATELIER";textSize=11f;letterSpacing=.18f
            setTypeface(typeface,Typeface.BOLD);setTextColor(Color.rgb(132,220,255))
            setPadding(dp(5),dp(7),0,dp(16))
        })
        val activeWallpaper=p.getString("wallpaper_source","builtin") ?: "builtin"
        val wallpaperFile=when(activeWallpaper) {
            "dreamscape" -> File(filesDir,"sleepsync_dreamscape.png")
            "custom" -> File(filesDir,"sleepsync_custom_wallpaper")
            else -> null
        }
        val previewFrame=android.widget.FrameLayout(this).apply {
            background=opticsGlassBackground(Color.rgb(99,213,255),27)
            clipToOutline=true
            outlineProvider=android.view.ViewOutlineProvider.BACKGROUND
            layoutParams=LinearLayout.LayoutParams(-1,dp(340)).apply{setMargins(0,0,0,dp(14))}
        }
        val previewImage=android.widget.ImageView(this).apply {
            scaleType=android.widget.ImageView.ScaleType.CENTER_CROP
            if(p.getBoolean("wallpaper_enabled",true) && wallpaperFile?.exists()==true) {
                setImageURI(Uri.fromFile(wallpaperFile))
            } else {
                setImageDrawable(GradientDrawable(GradientDrawable.Orientation.BL_TR,intArrayOf(
                    Color.rgb(12,8,37),Color.rgb(62,20,110),Color.rgb(15,62,114),Color.rgb(4,8,25)
                )))
            }
        }
        previewFrame.addView(previewImage,android.widget.FrameLayout.LayoutParams(-1,-1))
        if(wallpaperFile?.exists()!=true && p.getBoolean("wallpaper_enabled",true)) {
            previewFrame.addView(object:View(this) {
                private val brush=Paint(Paint.ANTI_ALIAS_FLAG)
                override fun onDraw(canvas:Canvas) {
                    super.onDraw(canvas)
                    val cx=width*.70f;val cy=height*.39f;val radius=dp(73).toFloat()
                    brush.shader=android.graphics.RadialGradient(cx,cy,radius*1.8f,
                        intArrayOf(Color.argb(175,240,93,226),Color.argb(45,112,68,242),Color.TRANSPARENT),
                        null,android.graphics.Shader.TileMode.CLAMP)
                    canvas.drawCircle(cx,cy,radius*1.8f,brush)
                    brush.shader=android.graphics.LinearGradient(cx-radius,cy-radius,cx+radius,cy+radius,
                        Color.rgb(255,190,226),Color.rgb(146,57,224),android.graphics.Shader.TileMode.CLAMP)
                    canvas.drawCircle(cx,cy,radius,brush)
                    brush.shader=null;brush.color=Color.rgb(20,12,52)
                    canvas.drawCircle(cx+radius*.35f,cy-radius*.16f,radius*.92f,brush)
                }
            },android.widget.FrameLayout.LayoutParams(-1,-1))
        }
        previewFrame.addView(View(this).apply {
            background=GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,intArrayOf(
                Color.TRANSPARENT,Color.argb(140,4,8,29),Color.rgb(7,10,29)
            ))
        },android.widget.FrameLayout.LayoutParams(-1,-1))
        previewFrame.addView(LinearLayout(this).apply {
            orientation=LinearLayout.VERTICAL
            setPadding(dp(20),dp(15),dp(20),dp(19))
            addView(TextView(this@MainActivity).apply {
                text="✦  AKTUELLES WALLPAPER";textSize=10f;letterSpacing=.13f
                setTypeface(typeface,Typeface.BOLD);setTextColor(Color.rgb(116,235,255))
            })
            addView(android.widget.Space(this@MainActivity),LinearLayout.LayoutParams(1,0,1f))
            addView(TextView(this@MainActivity).apply {
                text=when {
                    !p.getBoolean("wallpaper_enabled",true) -> "OLED Black"
                    activeWallpaper=="dreamscape" -> "DreamScape"
                    activeWallpaper=="custom" -> "Dein Wallpaper"
                    else -> "SleepSync Cosmos"
                }
                textSize=25f;setTypeface(typeface,Typeface.BOLD);setTextColor(Color.WHITE)
                setShadowLayer(dp(10).toFloat(),0f,0f,Color.rgb(92,89,230))
            })
            addView(TextView(this@MainActivity).apply {
                text="Dein Schlafkosmos. In deinem Licht."
                textSize=12f;setTextColor(Color.rgb(209,227,247))
                setPadding(0,dp(4),0,dp(12))
            })
            addView(TextView(this@MainActivity).apply {
                text="✦  WALLPAPER WECHSELN  ↗"
                textSize=11f;setTypeface(typeface,Typeface.BOLD)
                setTextColor(Color.rgb(104,231,255))
            })
        },android.widget.FrameLayout.LayoutParams(-1,-1))
        sleepCard.addView(previewFrame)
        sleepCard.addView(TextView(this).apply {
            text="WALLPAPER GALERIE";textSize=11f;letterSpacing=.16f
            setTypeface(typeface,Typeface.BOLD);setTextColor(Color.rgb(165,196,255))
            setPadding(dp(5),dp(4),0,dp(10))
        })
        val wallpaperGallery=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL}
        listOf(
            Triple("DreamScape","✦",Color.rgb(116,80,216)),
            Triple("SleepSync","☾",Color.rgb(31,146,192)),
            Triple("Eigenes","▧",Color.rgb(219,88,176)),
            Triple("OLED","●",Color.rgb(39,47,76))
        ).forEachIndexed{index,(name,symbol,tone)->
            val selected=when(index){
                0->activeWallpaper=="dreamscape" && p.getBoolean("wallpaper_enabled",true)
                1->activeWallpaper=="builtin" && p.getBoolean("wallpaper_enabled",true)
                2->activeWallpaper=="custom" && p.getBoolean("wallpaper_enabled",true)
                else->!p.getBoolean("wallpaper_enabled",true)
            }
            wallpaperGallery.addView(LinearLayout(this).apply {
                orientation=LinearLayout.VERTICAL;gravity=android.view.Gravity.CENTER
                setPadding(dp(3),dp(13),dp(3),dp(12))
                background=GradientDrawable(GradientDrawable.Orientation.TL_BR,intArrayOf(
                    tone,Color.rgb(11,14,35)
                )).apply {
                    cornerRadius=dp(16).toFloat()
                    setStroke(dp(if(selected) 3 else 1),if(selected) Color.rgb(123,243,255) else tone)
                }
                addView(TextView(this@MainActivity).apply {
                    text=symbol;textSize=29f;gravity=android.view.Gravity.CENTER
                    setTextColor(Color.WHITE)
                    setShadowLayer(dp(9).toFloat(),0f,0f,tone)
                })
                addView(TextView(this@MainActivity).apply {
                    text=name;textSize=10f;gravity=android.view.Gravity.CENTER
                    setTypeface(typeface,Typeface.BOLD);setTextColor(Color.WHITE)
                    setPadding(0,dp(8),0,0)
                })
                setOnClickListener {
                    when(index) {
                        0->{
                            if(File(filesDir,"sleepsync_dreamscape.png").exists()){
                                p.edit().putBoolean("wallpaper_enabled",true).putString("wallpaper_source","dreamscape").putBoolean("custom_enabled",true).apply()
                                showDesignStudio()
                            } else generateDreamScape()
                        }
                        1->{p.edit().putBoolean("wallpaper_enabled",true).putString("wallpaper_source","builtin").putBoolean("custom_enabled",true).apply();showDesignStudio()}
                        2->photoPickerWallpaperLauncher.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        3->{p.edit().putBoolean("wallpaper_enabled",false).putBoolean("custom_enabled",true).apply();showDesignStudio()}
                    }
                }
            },LinearLayout.LayoutParams(0,dp(96),1f).apply{setMargins(dp(3),0,dp(3),dp(14))})
        }
        sleepCard.addView(wallpaperGallery)
        sleepCard.addView(TextView(this).apply {
            text="FARBSCHEMA";textSize=11f;letterSpacing=.16f
            setTypeface(typeface,Typeface.BOLD);setTextColor(Color.rgb(177,201,255))
            setPadding(dp(5),dp(9),0,dp(12))
        })
        val palettes=listOf(
            Triple("Blau",Color.rgb(75,126,255),Color.rgb(35,220,255)),
            Triple("Lila",Color.rgb(161,73,242),Color.rgb(105,73,255)),
            Triple("Cyan",Color.rgb(10,212,234),Color.rgb(33,125,255)),
            Triple("Pink",Color.rgb(245,62,156),Color.rgb(161,69,249)),
            Triple("Orange",Color.rgb(255,155,49),Color.rgb(255,85,117)),
            Triple("Grün",Color.rgb(33,216,151),Color.rgb(24,168,207)),
            Triple("Gold",Color.rgb(255,197,65),Color.rgb(255,120,54)),
            Triple("Dynamisch",Color.rgb(140,82,250),Color.rgb(27,217,243))
        )
        val currentAccent=p.getInt("accent",defs[0])
        val colorGrid=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL}
        palettes.forEachIndexed{index,(name,primary,secondary)->
            val selected=currentAccent==primary
            val tile=LinearLayout(this).apply{
                orientation=LinearLayout.VERTICAL;gravity=android.view.Gravity.CENTER
                setPadding(dp(3),dp(11),dp(3),dp(9))
                background=null
                val orb=android.widget.FrameLayout(this@MainActivity).apply{
                    background=GradientDrawable(GradientDrawable.Orientation.TL_BR,intArrayOf(
                        Color.WHITE,primary,secondary,Color.rgb(21,15,62)
                    )).apply{shape=GradientDrawable.OVAL;setStroke(dp(if(selected) 3 else 2),if(selected) Color.rgb(128,248,255) else Color.argb(205,255,255,255))}
                    elevation=dp(if(selected) 16 else 7).toFloat()
                }
                addView(orb,LinearLayout.LayoutParams(dp(68),dp(68)))
                addView(TextView(this@MainActivity).apply{
                    text=name;textSize=10f;gravity=android.view.Gravity.CENTER
                    setTextColor(if(selected) Color.rgb(130,236,255) else Color.WHITE)
                    setTypeface(typeface,if(selected) Typeface.BOLD else Typeface.NORMAL)
                    setPadding(0,dp(9),0,0)
                })
                setOnClickListener{
                    p.edit().putInt("accent",primary).putInt("accent2",secondary).putBoolean("custom_enabled",true).apply()
                    showDesignStudio()
                }
            }
            colorGrid.addView(tile,LinearLayout.LayoutParams(dp(94),dp(112)).apply{setMargins(dp(3),dp(3),dp(3),dp(3))})
        }
        sleepCard.addView(LinearLayout(this).apply {
            orientation=LinearLayout.VERTICAL;setPadding(dp(9),dp(11),dp(9),dp(11))
            background=GradientDrawable(GradientDrawable.Orientation.TL_BR,intArrayOf(
                Color.argb(248,8,14,38),Color.argb(246,24,13,55)
            )).apply {cornerRadius=dp(23).toFloat();setStroke(dp(1),Color.rgb(112,116,229))}
            addView(android.widget.HorizontalScrollView(this@MainActivity).apply {
                isHorizontalScrollBarEnabled=false;addView(colorGrid)
            })
        },LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(16)})
        // Live preview
        sleepCard.addView(eightbitlab.com.blurview.BlurView(this).apply{val tone=designColor("accent2",defs[1]);background=opticsGlassBackground(tone);outlineProvider=android.view.ViewOutlineProvider.BACKGROUND;clipToOutline=true;settingsBlurTarget?.let{target->setupWith(target,4f,true).setBlurEnabled(true).setBlurAutoUpdate(true).setBlurRadius(effectiveBlurRadius()).setOverlayColor(Color.argb(designGlassOverlayAlpha(),72,88,112))};layoutParams=LinearLayout.LayoutParams(-1,dp(142)).apply{setMargins(0,0,0,dp(8))}
            addView(LinearLayout(this@MainActivity).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(18),dp(15),dp(18),dp(12));addView(TextView(this@MainActivity).apply{text="LIVE-VORSCHAU";textSize=10f;letterSpacing=.14f;setTextColor(designColor("accent2",defs[1]));setTypeface(typeface,Typeface.BOLD)});addView(TextView(this@MainActivity).apply{text="7 h 42 min";textSize=29f;setTextColor(Color.WHITE);setTypeface(typeface,Typeface.BOLD)});addView(LinearLayout(this@MainActivity).apply{orientation=LinearLayout.HORIZONTAL;listOf("Leicht" to 2,"Tief" to 3,"REM" to 4,"Wach" to 5).forEach{(n,i)->addView(TextView(this@MainActivity).apply{text=n;textSize=10f;gravity=android.view.Gravity.CENTER;setTextColor(defs[i]);background=GradientDrawable().apply{cornerRadius=dp(8).toFloat();setColor(Color.argb(42,Color.red(defs[i]),Color.green(defs[i]),Color.blue(defs[i])));setStroke(dp(1),defs[i])}},LinearLayout.LayoutParams(0,dp(30),1f).apply{setMargins(dp(2),0,dp(2),0)})}})})
        })
        section("✦  COLOR ATELIER")
        val colorDetails=LinearLayout(this).apply {orientation=LinearLayout.VERTICAL;visibility=View.GONE}
        val colorToggle=TextView(this).apply {
            text="◈  DEINE FARB-DNA    ·    10 AKZENTE   ⌄"
            textSize=13f;setTypeface(typeface,Typeface.BOLD)
            setTextColor(Color.rgb(164,222,255));gravity=android.view.Gravity.CENTER_VERTICAL
            setPadding(dp(17),dp(17),dp(12),dp(17))
            background=GradientDrawable(GradientDrawable.Orientation.TL_BR,intArrayOf(Color.rgb(11,19,47),Color.rgb(27,15,57))).apply{cornerRadius=dp(22).toFloat();setStroke(dp(1),Color.rgb(128,114,233))}
            setOnClickListener {
                colorDetails.visibility=if(colorDetails.visibility==View.VISIBLE) View.GONE else View.VISIBLE
                text=if(colorDetails.visibility==View.VISIBLE)
                    "◈  COLOR ATELIER SCHLIESSEN    ⌃"
                else "◈  DEINE FARB-DNA    ·    10 AKZENTE   ⌄"
            }
        }
        sleepCard.addView(colorToggle,LinearLayout.LayoutParams(-1,-2).apply{setMargins(0,dp(4),0,dp(8))})
        names.indices.forEach{i->val tone=p.getInt(keys[i],defs[i]);val item=LinearLayout(this).apply {
            background=GradientDrawable(GradientDrawable.Orientation.TL_BR,intArrayOf(Color.rgb(9,17,41),Color.rgb(24,17,51))).apply{cornerRadius=dp(18).toFloat();setStroke(dp(1),Color.argb(150,Color.red(tone),Color.green(tone),Color.blue(tone)))}
            layoutParams=LinearLayout.LayoutParams(-1,-2).apply{setMargins(0,dp(3),0,dp(3))}
            addView(LinearLayout(this@MainActivity).apply {
                orientation=LinearLayout.HORIZONTAL;gravity=android.view.Gravity.CENTER_VERTICAL
                setPadding(dp(15),dp(14),dp(15),dp(14))
                addView(View(this@MainActivity).apply {
                    background=GradientDrawable().apply{shape=GradientDrawable.OVAL;setColor(tone);setStroke(dp(2),Color.WHITE)}
                },LinearLayout.LayoutParams(dp(37),dp(37)).apply{marginEnd=dp(13)})
                addView(TextView(this@MainActivity).apply{
                    text=names[i];textSize=14f;setTextColor(Color.WHITE);setTypeface(typeface,Typeface.BOLD)
                },LinearLayout.LayoutParams(0,-2,1f))
                addView(TextView(this@MainActivity).apply{
                    text=String.format("#%06X",0xFFFFFF and tone);textSize=11f;setTextColor(Color.rgb(183,205,229))
                })
            })
            setOnClickListener{pickFullColor(i)}
        };colorDetails.addView(item)}
        sleepCard.addView(colorDetails)
        section("✦  WALLPAPER & QUELLEN")
        val wallpaperSource=p.getString("wallpaper_source","builtin")?:"builtin"
        row("Wallpaper",if(!p.getBoolean("wallpaper_enabled",true)) "Aus · einfarbiger Hintergrund" else if(wallpaperSource=="dreamscape") "Aktiv · DreamScape OLED" else if(wallpaperSource=="custom") "Aktiv · Eigenes Wallpaper" else "Aktiv · SleepSync Wallpaper",Color.rgb(70,205,225)){
            val choices=arrayOf("SleepSync Wallpaper","Eigenes Wallpaper · Google Fotos / Galerie","Eigenes Wallpaper · Dateien","Installierte Wallpaper-Apps · Bild auswählen","✨ DreamScape · Aus Schlafdaten generieren","Kein Wallpaper","✨ KI OLED Studio · demnächst")
            val dlg=AlertDialog.Builder(this).setTitle("Wallpaper").setItems(choices){_,which->
                when(which){
                    0->{p.edit().putBoolean("wallpaper_enabled",true).putString("wallpaper_source","builtin").putBoolean("custom_enabled",true).apply();recreate()}
                    1->photoPickerWallpaperLauncher.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    2->customWallpaperLauncher.launch(arrayOf("image/*"))
                    3->openInstalledWallpaperApps()
                    4->generateDreamScape()
                    5->{p.edit().putBoolean("wallpaper_enabled",false).putBoolean("custom_enabled",true).apply();recreate()}
                    6->android.widget.Toast.makeText(this,"KI OLED Studio ist vorbereitet – echte OLED-KI-Generierung folgt als eigener Schritt.",android.widget.Toast.LENGTH_LONG).show()
                }
            }.create()
            dlg.setOnShowListener{styleSleepSyncDialog(dlg)};dlg.show()
        }
        section("✦  LICHTLABOR")
        fun slider(title:String,key:String,value:Int,max:Int,tone:Int) {
            val panel=LinearLayout(this).apply {
                orientation=LinearLayout.VERTICAL;setPadding(dp(18),dp(16),dp(18),dp(13))
                background=GradientDrawable(GradientDrawable.Orientation.TL_BR,intArrayOf(
                    Color.argb(248,7,15,38),Color.argb(244,24,15,54)
                )).apply {
                    cornerRadius=dp(22).toFloat()
                    setStroke(dp(1),Color.argb(175,Color.red(tone),Color.green(tone),Color.blue(tone)))
                }
            }
            val valueLabel=TextView(this).apply {
                text="$value%";textSize=15f;setTypeface(typeface,Typeface.BOLD);setTextColor(tone)
            }
            panel.addView(LinearLayout(this).apply {
                orientation=LinearLayout.HORIZONTAL;gravity=android.view.Gravity.CENTER_VERTICAL
                addView(TextView(this@MainActivity).apply {
                    text=title;textSize=16f;setTypeface(typeface,Typeface.BOLD);setTextColor(Color.WHITE)
                },LinearLayout.LayoutParams(0,-2,1f))
                addView(valueLabel)
            })
            val demo=View(this).apply {
                background=GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,intArrayOf(
                    Color.argb(170,Color.red(tone),Color.green(tone),Color.blue(tone)),
                    Color.argb(70,58,45,119),Color.rgb(9,19,47)
                )).apply{cornerRadius=dp(13).toFloat();setStroke(dp(1),tone)}
                alpha=.4f+value*.006f
            }
            panel.addView(demo,LinearLayout.LayoutParams(-1,dp(54)).apply {
                topMargin=dp(13);bottomMargin=dp(8)
            })
            panel.addView(android.widget.SeekBar(this).apply {
                this.max=max;progress=value
                progressTintList=ColorStateList.valueOf(tone);thumbTintList=ColorStateList.valueOf(tone)
                setOnSeekBarChangeListener(object:android.widget.SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(s:android.widget.SeekBar?,v:Int,u:Boolean) {
                        valueLabel.text="$v%";demo.alpha=.4f+v*.006f
                        if(u)p.edit().putInt(key,v).putBoolean("custom_enabled",true).apply()
                    }
                    override fun onStartTrackingTouch(s:android.widget.SeekBar?){}
                    override fun onStopTrackingTouch(s:android.widget.SeekBar?){showDesignStudio()}
                })
            })
            sleepCard.addView(panel,LinearLayout.LayoutParams(-1,-2).apply {
                setMargins(0,dp(5),0,dp(7))
            })
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

            val card=(eightbitlab.com.blurview.BlurView(this)).apply {
                background=opticsGlassBackground(tone)
                isClickable=true
                isFocusable=true
                setOnClickListener { prefs.edit().putString("theme",key).apply(); recreate() }
                outlineProvider=android.view.ViewOutlineProvider.BACKGROUND
                clipToOutline=true
                settingsBlurTarget?.let { target ->
                    setupWith(target,4f,true).setBlurEnabled(true).setBlurAutoUpdate(true).setBlurRadius(effectiveBlurRadius()).setOverlayColor(Color.argb(designGlassOverlayAlpha(),72,88,112))
                }

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

        val studio=eightbitlab.com.blurview.BlurView(this).apply {
            background=opticsGlassBackground(Color.rgb(255,92,205))
            outlineProvider=android.view.ViewOutlineProvider.BACKGROUND;clipToOutline=true
            settingsBlurTarget?.let{target->setupWith(target,4f,true).setBlurEnabled(true).setBlurAutoUpdate(true).setBlurRadius(effectiveBlurRadius()).setOverlayColor(Color.argb(designGlassOverlayAlpha(),72,88,112))}
            layoutParams=LinearLayout.LayoutParams(-1,-2).apply{setMargins(dp(4),dp(10),dp(4),dp(7))}
            addView(LinearLayout(this@MainActivity).apply {
                orientation=LinearLayout.HORIZONTAL;gravity=android.view.Gravity.CENTER_VERTICAL;setPadding(dp(16),dp(15),dp(16),dp(15))
                addView(TextView(this@MainActivity).apply{text="🎨";textSize=30f;gravity=android.view.Gravity.CENTER;layoutParams=LinearLayout.LayoutParams(dp(58),dp(58))})
                addView(LinearLayout(this@MainActivity).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(14),0,0,0);addView(TextView(this@MainActivity).apply{text="Design Studio";textSize=17f;setTypeface(typeface,Typeface.BOLD);setTextColor(Color.WHITE)});addView(TextView(this@MainActivity).apply{text="Farben frei personalisieren";textSize=12f;setTextColor(Color.rgb(220,226,242));setPadding(0,dp(3),0,0)})},LinearLayout.LayoutParams(0,-2,1f))
                addView(TextView(this@MainActivity).apply{text="›";textSize=30f;setTextColor(Color.rgb(255,92,205))})
            })
            setOnClickListener {
                try {
                    showDesignStudio()
                } catch (error: Exception) {
                    val trace=android.util.Log.getStackTraceString(error)
                    android.util.Log.e("SleepSyncDesignStudio","Failed to open Design Studio",error)
                    AlertDialog.Builder(this@MainActivity)
                        .setTitle("Design Studio – Fehlerdiagnose")
                        .setMessage(trace.take(12000))
                        .setPositiveButton("Kopieren") { _, _ ->
                            val clipboard=getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            clipboard.setPrimaryClip(android.content.ClipData.newPlainText("SleepSync Fehler",trace))
                            android.widget.Toast.makeText(this@MainActivity,"Fehlerbericht kopiert",android.widget.Toast.LENGTH_SHORT).show()
                        }
                        .setNegativeButton("Schließen",null)
                        .show()
                }
            }
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
        val density=resources.displayMetrics.density
        fun dp(v:Int)=(v*density).toInt()
        val formatter=java.time.format.DateTimeFormatter.ofPattern("HH:mm")
            .withZone(java.time.ZoneId.systemDefault())
        val tones=mapOf("leicht" to stageLight,"tief" to stageDeep,"rem" to stageRem,"wach" to stageAwake)
        val segments=s.stageSeries.filter { it.endMs>it.startMs && it.endMs>s.startMs && it.startMs<s.endMs }
            .sortedBy { it.startMs }
        val values=listOf(Triple("Leicht",s.lightMin,stageLight),Triple("Tief",s.deepMin,stageDeep),
            Triple("REM",s.remMin,stageRem),Triple("Wach",s.awakeMin,stageAwake))
        val total=values.sumOf { it.second.toLong() }.coerceAtLeast(1L)
        fun showPhase(name:String,minutes:Long) {
            val pct=(minutes*100/total).toInt()
            android.app.AlertDialog.Builder(this@MainActivity)
                .setTitle("Schlafphase · $name")
                .setMessage("$name · $minutes min ($pct %)\\nSchlafzeit: ${formatter.format(java.time.Instant.ofEpochMilli(s.startMs))}–${formatter.format(java.time.Instant.ofEpochMilli(s.endMs))}")
                .setPositiveButton("Schließen",null)
                .setNeutralButton("Phasen im Detail") { _,_ -> showAllStageTimelines(s) }
                .show()
        }
        return LinearLayout(this).apply {
            orientation=LinearLayout.VERTICAL
            if(segments.isNotEmpty() && s.endMs>s.startMs) {
                addView(object:View(this@MainActivity) {
                    private val paint=android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
                    private var selectedTime:Long?=null
                    private val barTop=dp(48).toFloat()
                    private val barHeight=dp(24).toFloat()
                    private val span=(s.endMs-s.startMs).toDouble()
                    private fun xAt(t:Long)=(((t-s.startMs)/span)*width).toFloat().coerceIn(0f,width.toFloat())
                    private fun stageAt(t:Long)=segments.lastOrNull { t>=it.startMs && t<it.endMs }
                    init {
                        isClickable=true
                        isFocusable=true
                        contentDescription="Interaktive Schlaf-Timeline. Finger bewegen, um Schlafphase und Uhrzeit zu sehen."
                    }
                    override fun onDraw(canvas:android.graphics.Canvas) {
                        super.onDraw(canvas)
                        val w=width.toFloat()
                        if(w<=0f)return
                        val radius=barHeight/2f
                        paint.style=android.graphics.Paint.Style.FILL
                        paint.shader=null
                        paint.color=Color.argb(110,29,34,71)
                        canvas.drawRoundRect(0f,barTop,w,barTop+barHeight,radius,radius,paint)
                        canvas.save()
                        val clip=android.graphics.Path().apply {
                            addRoundRect(0f,barTop,w,barTop+barHeight,radius,radius,android.graphics.Path.Direction.CW)
                        }
                        canvas.clipPath(clip)
                        segments.forEach { segment ->
                            val left=xAt(segment.startMs)
                            val right=xAt(segment.endMs)
                            if(right>left) {
                                val tone=tones[segment.stageLabel.trim().lowercase()]?:Color.rgb(100,112,145)
                                paint.color=tone
                                paint.shader=android.graphics.LinearGradient(left,barTop,right.coerceAtLeast(left+1f),barTop+barHeight,
                                    intArrayOf(tone,android.graphics.Color.argb(210,Color.red(tone),Color.green(tone),Color.blue(tone))),
                                    null,android.graphics.Shader.TileMode.CLAMP)
                                canvas.drawRect(left,barTop,right,barTop+barHeight,paint)
                                paint.shader=null
                            }
                        }
                        canvas.restore()
                        selectedTime?.let { time ->
                            val x=xAt(time)
                            val stage=stageAt(time)
                            val tone=stage?.let { tones[it.stageLabel.trim().lowercase()] }?:Color.WHITE
                            paint.color=Color.WHITE
                            paint.strokeWidth=dp(2).toFloat()
                            canvas.drawLine(x,barTop-dp(6),x,barTop+barHeight+dp(6),paint)
                            paint.color=tone
                            canvas.drawCircle(x,barTop+barHeight/2f,dp(5).toFloat(),paint)
                            paint.color=Color.WHITE
                            canvas.drawCircle(x,barTop+barHeight/2f,dp(2).toFloat(),paint)
                            val phase=stage?.stageLabel?:"Keine Daten"
                            val label="${formatter.format(java.time.Instant.ofEpochMilli(time))}  ·  $phase"
                            paint.typeface=android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT,android.graphics.Typeface.BOLD)
                            paint.textSize=dp(12).toFloat()
                            val bubbleWidth=(paint.measureText(label)+dp(28)).coerceAtMost(w)
                            val center=x.coerceIn(bubbleWidth/2f,w-bubbleWidth/2f)
                            val left=center-bubbleWidth/2f
                            paint.color=Color.argb(238,16,22,49)
                            canvas.drawRoundRect(left,dp(4).toFloat(),left+bubbleWidth,dp(37).toFloat(),dp(13).toFloat(),dp(13).toFloat(),paint)
                            paint.color=tone
                            canvas.drawCircle(left+dp(12),dp(20).toFloat(),dp(3).toFloat(),paint)
                            paint.color=Color.WHITE
                            canvas.drawText(label,left+dp(20),dp(25).toFloat(),paint)
                        }
                    }
                    override fun onTouchEvent(event:android.view.MotionEvent):Boolean {
                        when(event.actionMasked) {
                            android.view.MotionEvent.ACTION_DOWN,android.view.MotionEvent.ACTION_MOVE -> {
                                parent?.requestDisallowInterceptTouchEvent(true)
                                selectedTime=(s.startMs+((event.x.coerceIn(0f,width.toFloat())/width.coerceAtLeast(1))*(s.endMs-s.startMs)).toLong())
                                    .coerceIn(s.startMs,s.endMs-1)
                                invalidate()
                                return true
                            }
                            android.view.MotionEvent.ACTION_UP -> {
                                parent?.requestDisallowInterceptTouchEvent(false)
                                performClick()
                                return true
                            }
                            android.view.MotionEvent.ACTION_CANCEL -> {
                                parent?.requestDisallowInterceptTouchEvent(false)
                                return true
                            }
                        }
                        return true
                    }
                    override fun performClick():Boolean { super.performClick();return true }
                },LinearLayout.LayoutParams(-1,dp(78)))
            } else {
                addView(LinearLayout(this@MainActivity).apply {
                    orientation=LinearLayout.HORIZONTAL
                    background=GradientDrawable().apply { cornerRadius=dp(10).toFloat();setColor(Color.rgb(43,37,72)) }
                    values.forEach { (name,minutes,color) ->
                        addView(View(this@MainActivity).apply {
                            background=GradientDrawable().apply { cornerRadius=dp(8).toFloat();setColor(color) }
                            contentDescription="$name: $minutes Minuten"
                            isClickable=true
                            setOnClickListener { showPhase(name,minutes.toLong()) }
                            layoutParams=LinearLayout.LayoutParams(0,dp(20),minutes.coerceAtLeast(1).toFloat()).apply {
                                setMargins(dp(1),0,dp(1),0)
                            }
                        })
                    }
                },LinearLayout.LayoutParams(-1,dp(20)))
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
        val card:View=if(light) {
            // Overview cards now use the exact same BlurView recipe as Settings.
            eightbitlab.com.blurview.BlurView(this).apply {
                background=LayerDrawable(arrayOf(
                    GradientDrawable().apply {
                        cornerRadius=dp(22).toFloat()
                        setColor(Color.argb(designGlassAlpha(),72,88,112))
                        setStroke(dp(4),Color.argb(42,Color.red(tone),Color.green(tone),Color.blue(tone)))
                    },
                    GradientDrawable().apply {
                        cornerRadius=dp(22).toFloat()
                        setColor(Color.TRANSPARENT)
                        setStroke(dp(2),Color.argb(255,Color.red(tone),Color.green(tone),Color.blue(tone)))
                    },
                    GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,intArrayOf(
                        Color.argb(if(sleep!=null) 36 else 0,255,255,255),
                        Color.TRANSPARENT,
                        Color.argb(if(sleep!=null) 30 else 0,6,8,25)
                    )).apply { cornerRadius=dp(22).toFloat() }
                ))
                outlineProvider=android.view.ViewOutlineProvider.BACKGROUND
                clipToOutline=true
                settingsBlurTarget?.let { target ->
                    setupWith(target,4f,true).setBlurEnabled(true).setBlurAutoUpdate(true)
                        .setBlurRadius(effectiveBlurRadius())
                        .setOverlayColor(Color.argb(designGlassOverlayAlpha(),72,88,112))
                }
                addView(body,android.widget.FrameLayout.LayoutParams(-1,-1))
                if(onClick!=null){isClickable=true;isFocusable=true;setOnClickListener{onClick()}}
            }
        } else MaterialCardView(this).apply{
            radius=dp(21).toFloat();cardElevation=dp(2).toFloat();strokeWidth=dp(2);strokeColor=tone;setCardBackgroundColor(fill)
            addView(body);if(onClick!=null){isClickable=true;isFocusable=true;setOnClickListener{onClick()}}
        }
        return if(light) card.apply { layoutParams=GridLayout.LayoutParams().apply{width=0;height=dp(if(label in listOf("Puls","SpO₂","Atmung","HRV")) 176 else 122);columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1f);setMargins(dp(4),dp(4),dp(4),dp(4))} } else card.apply { layoutParams=GridLayout.LayoutParams().apply{width=0;height=dp(if(label in listOf("Puls","SpO₂","Atmung","HRV")) 164 else 110);columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1f);setMargins(dp(4),dp(4),dp(4),dp(4))} }
    }
    private fun showStageTimeline(label:String, tone:Int, minutes:Long, s:SleepSummary) {
        showAllStageTimelines(s)
    }

    private fun showAllStageTimelines(s:SleepSummary) {
        highlightOverviewTab?.invoke()
        val d=resources.displayMetrics.density; fun dp(v:Int)=(v*d).toInt()
        val theme=getSharedPreferences("sleepsync_ui",MODE_PRIVATE).getString("theme","dark")?:"dark"
        val sysDark=(resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK)==android.content.res.Configuration.UI_MODE_NIGHT_YES
        val light=theme=="light" || (theme=="system" && !sysDark)
        val primary=Color.WHITE
        val secondary=if(light) Color.rgb(225,232,248) else Color.rgb(165,175,205)
        val timeColor=if(light) Color.rgb(240,245,255) else Color.rgb(184,198,225)
        val tf=DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())
        fun fmtMin(m:Long)=if(m>=60) (m/60).toString()+" h "+(m%60).toString()+" min" else m.toString()+" min"
        pageTitle.text="Schlafphasen"; pageSubtitle.text="Die Architektur deiner Nacht"
        actionsTitle.visibility=View.GONE; actionsBox.visibility=View.GONE; sleepCard.removeAllViews()
        sleepCard.addView(TextView(this).apply { text="‹  Zurück zur Übersicht"; textSize=12f; setTextColor(accent2); setPadding(dp(2),dp(8),0,dp(14)); setOnClickListener { showOverview() } })

        // One-glance summary: sleep duration + full-night composition.
        sleepCard.addView((if(light) eightbitlab.com.blurview.BlurView(this) else android.widget.FrameLayout(this)).apply {
            background=LayerDrawable(arrayOf(
                GradientDrawable().apply{cornerRadius=dp(22).toFloat();setColor(if(light) Color.argb(designGlassAlpha(),72,88,112) else Color.argb(190,9,15,31));setStroke(dp(4),Color.argb(42,Color.red(accent2),Color.green(accent2),Color.blue(accent2)))},
                GradientDrawable().apply{cornerRadius=dp(22).toFloat();setColor(Color.TRANSPARENT);setStroke(dp(2),Color.argb(255,Color.red(accent2),Color.green(accent2),Color.blue(accent2)))},
                GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,intArrayOf(Color.argb(if(light) 60 else 22,255,255,255),Color.TRANSPARENT,Color.argb(if(light) 35 else 20,6,8,25))).apply{cornerRadius=dp(22).toFloat()}
            ))
            if(light && this is eightbitlab.com.blurview.BlurView){outlineProvider=android.view.ViewOutlineProvider.BACKGROUND;clipToOutline=true;settingsBlurTarget?.let{target->setupWith(target,4f,true).setBlurEnabled(true).setBlurAutoUpdate(true).setBlurRadius(effectiveBlurRadius()).setOverlayColor(Color.argb(designGlassOverlayAlpha(),72,88,112))}}
            layoutParams=LinearLayout.LayoutParams(-1,-2).apply{setMargins(0,0,0,dp(12))}
            addView(LinearLayout(this@MainActivity).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(18),dp(16),dp(18),dp(16))
                addView(TextView(this@MainActivity).apply{text="NACHT-ZUSAMMENFASSUNG";textSize=10f;letterSpacing=.12f;setTextColor(if(light) Color.rgb(159,214,255) else Color.rgb(146,191,255));setTypeface(typeface,Typeface.BOLD)})
                addView(TextView(this@MainActivity).apply{text=fmtMin(s.totalMin);textSize=30f;setTextColor(primary);setTypeface(typeface,Typeface.BOLD);setPadding(0,dp(5),0,dp(2))})
                addView(TextView(this@MainActivity).apply{text=tf.format(Instant.ofEpochMilli(s.startMs))+" – "+tf.format(Instant.ofEpochMilli(s.endMs))+"  ·  Schlafdauer";textSize=11f;setTextColor(secondary)})
                addView(LinearLayout(this@MainActivity).apply{orientation=LinearLayout.HORIZONTAL;setPadding(0,dp(13),0,dp(8))
                    listOf(s.lightMin to stageLight,s.deepMin to stageDeep,s.remMin to stageRem,s.awakeMin to stageAwake).forEach{q->if(q.first>0)addView(View(this@MainActivity).apply{background=GradientDrawable().apply{cornerRadius=dp(5).toFloat();setColor(q.second)}},LinearLayout.LayoutParams(0,dp(10),q.first.toFloat()).apply{setMargins(0,0,dp(2),0)})}
                })
                addView(LinearLayout(this@MainActivity).apply {
                    orientation=LinearLayout.HORIZONTAL
                    gravity=android.view.Gravity.CENTER_VERTICAL
                    val phases=listOf(
                        Triple("Leicht",s.lightMin,stageLight),
                        Triple("Tief",s.deepMin,stageDeep),
                        Triple("REM",s.remMin,stageRem),
                        Triple("Wach",s.awakeMin,stageAwake)
                    )
                    phases.forEach { (name,minutes,color) ->
                        addView(LinearLayout(this@MainActivity).apply {
                            orientation=LinearLayout.HORIZONTAL
                            gravity=android.view.Gravity.CENTER_VERTICAL
                            addView(View(this@MainActivity).apply {
                                background=GradientDrawable().apply { shape=GradientDrawable.OVAL;setColor(color) }
                            },LinearLayout.LayoutParams(dp(6),dp(6)).apply { rightMargin=dp(4) })
                            addView(TextView(this@MainActivity).apply {
                                text=name+" "+fmtMin(minutes)
                                textSize=9f
                                setTextColor(secondary)
                                setSingleLine(true)
                            })
                        },LinearLayout.LayoutParams(0,-2,1f))
                    }
                })
            })
        })

        fun stageCard(label:String, minutes:Long, stageTone:Int) {
            val intervals=s.stageSeries.filter{it.stageLabel==label}.sortedBy{it.startMs}
            val pct=((minutes*100f)/s.totalMin.coerceAtLeast(1)).toInt()
            val readableTone=if(light) Color.rgb((Color.red(stageTone)+255)/2,(Color.green(stageTone)+255)/2,(Color.blue(stageTone)+255)/2) else stageTone
            sleepCard.addView((if(light) eightbitlab.com.blurview.BlurView(this) else android.widget.FrameLayout(this)).apply {
                background=LayerDrawable(arrayOf(
                    GradientDrawable().apply{cornerRadius=dp(22).toFloat();setColor(if(light) Color.argb(designGlassAlpha(),72,88,112) else Color.argb(190,9,15,31));setStroke(dp(4),Color.argb(42,Color.red(stageTone),Color.green(stageTone),Color.blue(stageTone)))},
                    GradientDrawable().apply{cornerRadius=dp(22).toFloat();setColor(Color.TRANSPARENT);setStroke(dp(2),Color.argb(255,Color.red(stageTone),Color.green(stageTone),Color.blue(stageTone)))},
                    GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,intArrayOf(
                        Color.argb(if(light) 64 else 25,255,255,255),
                        Color.TRANSPARENT,
                        Color.argb(if(light) 44 else 25,6,8,25)
                    )).apply{cornerRadius=dp(22).toFloat()}
                ))
                if(light && this is eightbitlab.com.blurview.BlurView){outlineProvider=android.view.ViewOutlineProvider.BACKGROUND;clipToOutline=true;settingsBlurTarget?.let{target->setupWith(target,4f,true).setBlurEnabled(true).setBlurAutoUpdate(true).setBlurRadius(effectiveBlurRadius()).setOverlayColor(Color.argb(designGlassOverlayAlpha(),72,88,112))}}
                layoutParams=LinearLayout.LayoutParams(-1,-2).apply{setMargins(0,0,0,dp(12))}
                addView(LinearLayout(this@MainActivity).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(18),dp(15),dp(18),dp(15))
                    addView(LinearLayout(this@MainActivity).apply{orientation=LinearLayout.HORIZONTAL;gravity=android.view.Gravity.CENTER_VERTICAL
                        addView(TextView(this@MainActivity).apply{text=label.uppercase();textSize=11f;letterSpacing=.08f;setTextColor(readableTone);setTypeface(typeface,Typeface.BOLD);setShadowLayer(dp(3).toFloat(),0f,0f,stageTone)},LinearLayout.LayoutParams(0,-2,1f))
                        addView(TextView(this@MainActivity).apply{text=pct.toString()+" %";textSize=18f;setTextColor(readableTone);setTypeface(typeface,Typeface.BOLD);setShadowLayer(dp(3).toFloat(),0f,0f,stageTone)})
                    })
                    addView(TextView(this@MainActivity).apply{text=fmtMin(minutes);textSize=27f;setTextColor(primary);setTypeface(typeface,Typeface.BOLD);setPadding(0,dp(3),0,dp(2))})
                    addView(TextView(this@MainActivity).apply{text=intervals.size.toString()+" "+if(intervals.size==1)"Abschnitt" else "Abschnitte";textSize=10f;setTextColor(secondary);setPadding(0,0,0,dp(9))})
                    addView(LinearLayout(this@MainActivity).apply{orientation=LinearLayout.HORIZONTAL
                        val duration=(s.endMs-s.startMs).coerceAtLeast(1);var cursor=s.startMs
                        fun seg(ms:Long,active:Boolean)=View(this@MainActivity).apply{
                            val segmentView=this
                            if(active && android.animation.ValueAnimator.areAnimatorsEnabled()){
                                val anim=android.animation.ValueAnimator.ofFloat(0f,1f).apply{
                                    this.duration=5200L;repeatCount=android.animation.ValueAnimator.INFINITE
                                    interpolator=android.view.animation.LinearInterpolator()
                                    addUpdateListener{v->
                                        val wave=(.5f+.5f*kotlin.math.sin(((v.animatedValue as Float)*2f*Math.PI).toFloat()))
                                        segmentView.alpha=.58f+.42f*wave
                                        segmentView.elevation=dp(2).toFloat()+dp(12)*wave
                                        segmentView.scaleY=.94f+.06f*wave
                                    }
                                }
                                addOnAttachStateChangeListener(object:android.view.View.OnAttachStateChangeListener{
                                    override fun onViewAttachedToWindow(v:View){anim.start()}
                                    override fun onViewDetachedFromWindow(v:View){anim.cancel()}
                                })
                            }
                            if(active)background=LayerDrawable(arrayOf(
                            GradientDrawable().apply{cornerRadius=dp(5).toFloat();setColor(Color.argb(68,Color.red(stageTone),Color.green(stageTone),Color.blue(stageTone)))},
                            GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,intArrayOf(
                                Color.argb(235,Color.red(stageTone),Color.green(stageTone),Color.blue(stageTone)),
                                Color.argb(110,Color.red(stageTone),Color.green(stageTone),Color.blue(stageTone)),
                                Color.argb(42,Color.red(stageTone),Color.green(stageTone),Color.blue(stageTone))
                            )).apply{cornerRadius=dp(4).toFloat()},
                            GradientDrawable().apply{cornerRadius=dp(4).toFloat();setColor(Color.TRANSPARENT);setStroke(dp(1),Color.argb(125,255,255,255))}
                        ));layoutParams=LinearLayout.LayoutParams(0,dp(if(active)44 else 1),(ms.toFloat()/duration).coerceAtLeast(.001f)).apply{gravity=android.view.Gravity.CENTER_VERTICAL;setMargins(if(active)dp(1) else 0,0,if(active)dp(1) else 0,0)}}
                        intervals.forEach{st->if(st.startMs>cursor)addView(seg(st.startMs-cursor,false));addView(seg(st.endMs-st.startMs,true));cursor=st.endMs};if(cursor<s.endMs)addView(seg(s.endMs-cursor,false))
                    })
                    if(intervals.isEmpty()) addView(TextView(this@MainActivity).apply {
                        text="Keine Wachphasen erkannt"
                        textSize=12f;setTextColor(secondary);gravity=android.view.Gravity.CENTER
                        setPadding(0,dp(12),0,dp(12))
                    })
                    addView(LinearLayout(this@MainActivity).apply{orientation=LinearLayout.HORIZONTAL;setPadding(0,dp(7),0,0)
                        addView(TextView(this@MainActivity).apply{text=tf.format(Instant.ofEpochMilli(s.startMs));textSize=10f;setTextColor(timeColor);layoutParams=LinearLayout.LayoutParams(0,-2,1f)})
                        addView(TextView(this@MainActivity).apply{text=tf.format(Instant.ofEpochMilli(s.startMs+(s.endMs-s.startMs)/2));textSize=10f;gravity=android.view.Gravity.CENTER;setTextColor(timeColor);layoutParams=LinearLayout.LayoutParams(0,-2,1f)})
                        addView(TextView(this@MainActivity).apply{text=tf.format(Instant.ofEpochMilli(s.endMs));textSize=10f;gravity=android.view.Gravity.END;setTextColor(timeColor);layoutParams=LinearLayout.LayoutParams(0,-2,1f)})
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
            return (if(light) eightbitlab.com.blurview.BlurView(this) else android.widget.FrameLayout(this)).apply {
                background=LayerDrawable(arrayOf(
                    GradientDrawable().apply {
                        cornerRadius=px(22).toFloat()
                        setColor(if(light) Color.argb(designGlassAlpha(),72,88,112) else Color.argb(188,9,15,31))
                        setStroke(px(4),Color.argb(42,Color.red(tokenColor),Color.green(tokenColor),Color.blue(tokenColor)))
                    },
                    GradientDrawable().apply {
                        cornerRadius=px(22).toFloat()
                        setColor(Color.TRANSPARENT)
                        setStroke(px(2),Color.argb(255,Color.red(tokenColor),Color.green(tokenColor),Color.blue(tokenColor)))
                    }
                ))
                if(light && this is eightbitlab.com.blurview.BlurView) {
                    outlineProvider=android.view.ViewOutlineProvider.BACKGROUND
                    clipToOutline=true
                    settingsBlurTarget?.let { target ->
                        setupWith(target,4f,true).setBlurEnabled(true).setBlurAutoUpdate(true)
                            .setBlurRadius(effectiveBlurRadius())
                            .setOverlayColor(Color.argb(designGlassOverlayAlpha(),72,88,112))
                    }
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
        // Organize the overview without discarding any of the existing interactive cards.
        fun collapseDashboardSection(start:Int,heading:String,summary:String,tone:Int) {
            val details=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL;visibility=View.GONE }
            while(sleepCard.childCount>start) {
                val child=sleepCard.getChildAt(start)
                sleepCard.removeViewAt(start)
                details.addView(child)
            }
            val header=LinearLayout(this).apply {
                orientation=LinearLayout.HORIZONTAL
                gravity=android.view.Gravity.CENTER_VERTICAL
                setPadding(dp(15),dp(15),dp(13),dp(15))
                background=GradientDrawable(GradientDrawable.Orientation.TL_BR,
                    intArrayOf(Color.argb(226,24,31,59),Color.argb(219,16,23,46))).apply {
                    cornerRadius=dp(21).toFloat()
                    setStroke(dp(1),tone)
                }
                addView(LinearLayout(this@MainActivity).apply {
                    orientation=LinearLayout.VERTICAL
                    layoutParams=LinearLayout.LayoutParams(0,-2,1f)
                    addView(TextView(this@MainActivity).apply {
                        text=heading;textSize=14f;setTypeface(typeface,Typeface.BOLD)
                        setTextColor(Color.WHITE)
                    })
                    addView(TextView(this@MainActivity).apply {
                        text=summary;textSize=11f
                        setTextColor(Color.rgb(192,205,232))
                        setPadding(0,dp(5),0,0)
                    })
                })
                val arrow=TextView(this@MainActivity).apply {
                    text="⌄";textSize=24f;setTextColor(tone)
                    gravity=android.view.Gravity.CENTER
                    setPadding(dp(10),0,dp(3),0)
                }
                addView(arrow)
                isClickable=true;isFocusable=true
                contentDescription="$heading, Details anzeigen oder ausblenden"
                setOnClickListener {
                    val open=details.visibility!=View.VISIBLE
                    details.visibility=if(open)View.VISIBLE else View.GONE
                    arrow.text=if(open)"⌃" else "⌄"
                }
            }
            sleepCard.addView(header,LinearLayout.LayoutParams(-1,-2).apply {
                setMargins(dp(4),dp(12),dp(4),dp(5))
            })
            sleepCard.addView(details)
        }

        val theme=getSharedPreferences("sleepsync_ui",MODE_PRIVATE).getString("theme","dark")?:"dark"
        val sysDark=(resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK)==android.content.res.Configuration.UI_MODE_NIGHT_YES
        val light=theme=="light" || (theme=="system" && !sysDark)
        fun glass(vararg rgb:Int)=if(light) Color.argb(designGlassAlpha(),72,88,112) else Color.rgb(rgb[0],rgb[1],rgb[2])
        fun settingsStyleCard(tone:Int, radius:Int=22, content:View):View {
            if(!light) return content
            val host=android.widget.FrameLayout(this).apply { clipChildren=false;clipToPadding=false }
            host.addView(object:View(this) {
                private val p=Paint(Paint.ANTI_ALIAS_FLAG).apply{style=Paint.Style.STROKE;strokeJoin=Paint.Join.ROUND}
                init{setLayerType(View.LAYER_TYPE_SOFTWARE,null)}
                override fun onDraw(canvas:Canvas){
                    val q=dp(1).toFloat();p.strokeWidth=dp(3).toFloat()
                    p.color=Color.argb(210,Color.red(tone),Color.green(tone),Color.blue(tone))
                    p.maskFilter=android.graphics.BlurMaskFilter(dp(14).toFloat(),android.graphics.BlurMaskFilter.Blur.OUTER)
                    canvas.drawRoundRect(q,q,width-q,height-q,dp(radius).toFloat(),dp(radius).toFloat(),p)
                    p.maskFilter=null;p.strokeWidth=dp(2).toFloat();p.color=tone
                    canvas.drawRoundRect(q,q,width-q,height-q,dp(radius).toFloat(),dp(radius).toFloat(),p)
                }
            },android.widget.FrameLayout.LayoutParams(-1,-1))
            host.addView(eightbitlab.com.blurview.BlurView(this).apply {
                background=LayerDrawable(arrayOf(
                    GradientDrawable().apply{cornerRadius=dp(radius).toFloat();setColor(Color.argb(designGlassAlpha(),72,88,112));setStroke(dp(4),Color.argb(42,Color.red(tone),Color.green(tone),Color.blue(tone)))},
                    GradientDrawable().apply{cornerRadius=dp(radius).toFloat();setColor(Color.TRANSPARENT);setStroke(dp(2),tone)}
                ))
                outlineProvider=android.view.ViewOutlineProvider.BACKGROUND;clipToOutline=true
                settingsBlurTarget?.let{target->setupWith(target,4f,true).setBlurEnabled(true).setBlurAutoUpdate(true).setBlurRadius(effectiveBlurRadius()).setOverlayColor(Color.argb(designGlassOverlayAlpha(),72,88,112))}
                addView(content,android.widget.FrameLayout.LayoutParams(-1,-2))
            },android.widget.FrameLayout.LayoutParams(-1,-2))
            return host
        }
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
        // Vorläufige Nacht-Einschätzung: Schlafdauer begrenzt die Effizienzbewertung.
        // Die Schlafeffizienz selbst bleibt als unabhängiger Messwert sichtbar.
        val referenceNights = sleepHistory.filter { it.startMs != s.startMs && it.totalMin > 0 && it.endMs <= s.endMs }
            .sortedByDescending { it.endMs }.take(14)
        val personalAverage = if(referenceNights.size >= 3) referenceNights.map { it.totalMin }.average().toInt() else null
        val durationScore = when {
            s.totalMin >= 480 -> 100
            s.totalMin >= 420 -> 90
            s.totalMin >= 360 -> 72
            s.totalMin >= 300 -> 48
            else -> 25
        }
        val nightRating = (durationScore * 0.75 + quality * 0.25).toInt().coerceIn(0,100)
        val durationDelta = personalAverage?.let { s.totalMin - it }
        val phaseTotal = (s.lightMin + s.deepMin + s.remMin + s.awakeMin).coerceAtLeast(1)
        val deepShare = s.deepMin * 100 / phaseTotal
        val remShare = s.remMin * 100 / phaseTotal
        val nightTitle = when {
            s.totalMin < 360 && quality >= 90 -> "KURZ, ABER RUHIG"
            s.totalMin < 360 -> "ZU WENIG SCHLAF"
            durationDelta != null && durationDelta <= -60 -> "KÜRZER ALS GEWOHNT"
            s.awakeMin >= 30 -> "UNRUHIGE NACHT"
            s.totalMin >= 480 && quality >= 90 -> "LANG UND EFFIZIENT"
            else -> "DEINE NACHT IM FOKUS"
        }
        val nightExplanation = when {
            s.totalMin < 360 && quality >= 90 -> "Hohe Effizienz ($quality %), aber nur ${s.totalMin / 60} h ${s.totalMin % 60} min Schlaf. Effizienz ersetzt keine Schlafdauer."
            s.totalMin < 360 -> "Die Nacht war mit ${s.totalMin / 60} h ${s.totalMin % 60} min kurz. Die erfasste Effizienz lag bei $quality %."
            s.awakeMin >= 30 -> "${s.awakeMin} Minuten Wachzeit wurden erfasst. Deine Effizienz lag bei $quality %."
            else -> "${s.totalMin / 60} h ${s.totalMin % 60} min Schlaf bei $quality % Effizienz. Tiefschlaf $deepShare %, REM $remShare %."
        }
        val comparison = durationDelta?.let { delta ->
            "${kotlin.math.abs(delta)} min ${if (delta >= 0) "über" else "unter"} deinem persönlichen Schnitt aus ${referenceNights.size} Nächten"
        } ?: "Persönlicher Vergleich ab drei früheren Nächten"
        sleepCard.addView(settingsStyleCard(accent2,30,LinearLayout(this).apply {
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
            // SleepSync Ultimate · compact, duration-aware score orbit.
            // The score is an orientation value, not a medical assessment.
            addView(LinearLayout(this@MainActivity).apply {
                orientation=LinearLayout.VERTICAL; gravity=android.view.Gravity.CENTER
                layoutParams=LinearLayout.LayoutParams(dp(108),-2)
                addView(android.widget.FrameLayout(this@MainActivity).apply {
                    layoutParams=LinearLayout.LayoutParams(dp(94),dp(94))
                    addView(object:View(this@MainActivity) {
                        private val ringPaint=Paint(Paint.ANTI_ALIAS_FLAG).apply { style=Paint.Style.STROKE; strokeCap=Paint.Cap.ROUND }
                        override fun onDraw(canvas:Canvas) {
                            super.onDraw(canvas)
                            val inset=dp(7).toFloat()
                            val bounds=android.graphics.RectF(inset,inset,width-inset,height-inset)
                            ringPaint.strokeWidth=dp(6).toFloat()
                            ringPaint.color=Color.argb(95,180,195,235)
                            canvas.drawArc(bounds,0f,360f,false,ringPaint)
                            ringPaint.color=when { nightRating>=80 -> Color.rgb(76,235,190); nightRating>=60 -> Color.rgb(255,200,86); else -> Color.rgb(255,104,135) }
                            canvas.drawArc(bounds,-90f,360f*nightRating/100f,false,ringPaint)
                        }
                    },android.widget.FrameLayout.LayoutParams(-1,-1))
                    addView(TextView(this@MainActivity).apply {
                        text="$nightRating"; textSize=29f; gravity=android.view.Gravity.CENTER
                        setTextColor(Color.WHITE); setTypeface(typeface,Typeface.BOLD)
                    },android.widget.FrameLayout.LayoutParams(-1,-1))
                })
                addView(TextView(this@MainActivity).apply {
                    text="SLEEP SCORE"; textSize=9f; letterSpacing=.08f; gravity=android.view.Gravity.CENTER
                    setTextColor(Color.WHITE); setTypeface(typeface,Typeface.BOLD)
                },LinearLayout.LayoutParams(-1,-2).apply { topMargin=dp(4) })
                addView(TextView(this@MainActivity).apply {
                    text="$quality% EFFIZIENZ"; textSize=10f; gravity=android.view.Gravity.CENTER
                    setTextColor(Color.WHITE)
                },LinearLayout.LayoutParams(-1,-2).apply { topMargin=dp(3) })
                contentDescription="Sleep Score $nightRating von 100, vorläufige Bewertung aus Schlafdauer und Effizienz. Schlafeffizienz $quality Prozent."
            })
        }))
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
        val sleepOnly = (s.lightMin + s.deepMin + s.remMin).coerceAtLeast(1)
        val deepPct = (s.deepMin * 100 / sleepOnly).toInt()
        val remPct = (s.remMin * 100 / sleepOnly).toInt()
        sleepCard.addView(MaterialCardView(this).apply {
            radius=dp(20).toFloat(); strokeWidth=if(light) dp(2) else dp(1); strokeColor=if(light) stageRem else Color.rgb(116,91,207); setCardBackgroundColor(Color.TRANSPARENT); if(light){ background=GradientDrawable().apply { cornerRadius=dp(20).toFloat(); setColor(Color.argb(designGlassAlpha(),72,88,112)); setStroke(dp(2),stageLight) }; cardElevation=dp(7).toFloat(); outlineAmbientShadowColor=stageLight; outlineSpotShadowColor=stageLight; addBlurLayer(this,20,::dp) } else setCardBackgroundColor(Color.rgb(19,15,39))
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
            addView(TextView(this@MainActivity).apply { text=fmt(s.lightMin+s.deepMin+s.remMin); tag="sleepsync_colored_pill"; textSize=9f; letterSpacing=.08f; setTypeface(typeface,Typeface.BOLD); setTextColor(if(light) Color.WHITE else stageRem); setPadding(dp(9),dp(4),dp(9),dp(4)); background=GradientDrawable().apply { cornerRadius=dp(13).toFloat(); setColor(if(light) Color.rgb(62,28,102) else Color.rgb(35,19,54)); setStroke(dp(1),if(light) Color.rgb(194,105,255) else Color.rgb(86,48,119)) } })
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
        val healthSectionStart=sleepCard.childCount
        sleepCard.addView(LinearLayout(this).apply {
            orientation=LinearLayout.HORIZONTAL; gravity=android.view.Gravity.CENTER_VERTICAL; setPadding(dp(4),dp(8),dp(4),dp(4))
            addView(TextView(this@MainActivity).apply { text="GESUNDHEITSWERTE"; textSize=11f; letterSpacing=.14f; setTextColor(if(light) Color.WHITE else accent2); setTypeface(typeface,Typeface.BOLD); if(light) setShadowLayer(dp(3).toFloat(),0f,dp(1).toFloat(),Color.BLACK); layoutParams=LinearLayout.LayoutParams(0,-2,1f) })
            addView(TextView(this@MainActivity).apply { text="LIVE"; tag="sleepsync_colored_pill"; textSize=8f; letterSpacing=.12f; setTextColor(Color.rgb(9,68,44)); setPadding(dp(7),dp(3),dp(7),dp(3)); background=GradientDrawable().apply { cornerRadius=dp(10).toFloat(); setColor(Color.rgb(113,244,173)); setStroke(dp(1),Color.rgb(26,184,105)) } })
            addView(View(this@MainActivity).apply { layoutParams=LinearLayout.LayoutParams(dp(7),dp(1)) })
            addView(TextView(this@MainActivity).apply { text="GARMIN  ●"; tag="sleepsync_colored_pill"; textSize=9f; letterSpacing=.08f; setTextColor(Color.rgb(7,66,92)); setTypeface(typeface,Typeface.BOLD); setPadding(dp(9),dp(4),dp(9),dp(4)); background=GradientDrawable().apply { cornerRadius=dp(13).toFloat(); setColor(Color.rgb(124,225,255)); setStroke(dp(1),Color.rgb(32,165,218)) } })
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
            radius=dp(18).toFloat(); cardElevation=if(light) dp(10).toFloat() else 0f; strokeWidth=if(light) 0 else dp(1); strokeColor=Color.rgb(24,94,105); setCardBackgroundColor(Color.TRANSPARENT); if(light) { background=GradientDrawable().apply { cornerRadius=dp(18).toFloat(); setColor(Color.argb(designGlassAlpha(),72,88,112)); setStroke(dp(1),Color.rgb(76,225,169)) }; addBlurLayer(this,18,::dp) } else setCardBackgroundColor(Color.rgb(7,25,31))
            layoutParams=LinearLayout.LayoutParams(-1,-2).apply { setMargins(0,dp(12),0,0) }
            addView(LinearLayout(this@MainActivity).apply {
                orientation=LinearLayout.HORIZONTAL; gravity=android.view.Gravity.CENTER_VERTICAL; setPadding(dp(14),dp(10),dp(14),dp(10))
                addView(TextView(this@MainActivity).apply { text="●"; textSize=12f; setTextColor(Color.rgb(76,225,169)); layoutParams=LinearLayout.LayoutParams(dp(24),-2) })
                addView(TextView(this@MainActivity).apply { text="Messwerte vollständig synchronisiert"; textSize=11f; setTextColor(if(light) Color.rgb(47,112,116) else Color.rgb(160,210,214)); layoutParams=LinearLayout.LayoutParams(0,-2,1f) })
                addView(TextView(this@MainActivity).apply { text="GARMIN"; textSize=9f; letterSpacing=.12f; setTypeface(typeface,Typeface.BOLD); setTextColor(accent2) })
            })
        })
        collapseDashboardSection(healthSectionStart,"GESUNDHEITSWERTE",
            "Puls ${num(s.avgHr,"bpm")}  ·  SpO₂ ${num(s.avgSpo2,"%")}  ·  Atmung & HRV",accent2)
        sleepCard.addView(LinearLayout(this).apply {
            orientation=LinearLayout.HORIZONTAL; gravity=android.view.Gravity.CENTER_VERTICAL; setPadding(dp(4),dp(16),0,dp(7))
            addView(TextView(this@MainActivity).apply { text="NACHT-INSIGHT"; textSize=11f; letterSpacing=.14f; setTextColor(stageRem); setTypeface(typeface,Typeface.BOLD); layoutParams=LinearLayout.LayoutParams(0,-2,1f) })
            addView(TextView(this@MainActivity).apply {
                text=nightTitle
                tag="sleepsync_colored_pill"; textSize=9f; letterSpacing=.08f; setTypeface(typeface,Typeface.BOLD); setTextColor(if(nightRating>=80) Color.rgb(5,69,39) else if(nightRating>=60) Color.rgb(90,62,0) else Color.rgb(105,16,26)); setPadding(dp(10),dp(5),dp(10),dp(5))
                background=GradientDrawable().apply { cornerRadius=dp(14).toFloat(); val pillColor=if(nightRating>=80) Color.rgb(111,245,153) else if(nightRating>=60) Color.rgb(255,219,91) else Color.rgb(255,125,132); setColor(pillColor); setStroke(dp(1),if(nightRating>=80) Color.rgb(31,172,91) else if(nightRating>=60) Color.rgb(209,154,20) else Color.rgb(216,54,70)) }
            })
        })
        sleepCard.addView(MaterialCardView(this).apply {
            radius=dp(24).toFloat(); cardElevation=if(light) dp(14).toFloat() else dp(2).toFloat(); strokeWidth=if(light) 0 else dp(1); strokeColor=Color.rgb(81,62,137); setCardBackgroundColor(Color.TRANSPARENT); if(light) { background=GradientDrawable().apply { cornerRadius=dp(24).toFloat(); setColor(Color.argb(designGlassAlpha(),72,88,112)); setStroke(dp(1),stageRem) }; addBlurLayer(this,24,::dp) } else setCardBackgroundColor(Color.rgb(19,15,39))
            addView(LinearLayout(this@MainActivity).apply {
                orientation=LinearLayout.HORIZONTAL; gravity=android.view.Gravity.CENTER_VERTICAL; setPadding(dp(15),dp(13),dp(15),dp(13))
                addView(TextView(this@MainActivity).apply {
                    text="✦"; textSize=20f; gravity=android.view.Gravity.CENTER; setTextColor(accent2); setPadding(0,dp(5),0,dp(5))
                    background=if(light) GradientDrawable(GradientDrawable.Orientation.TL_BR,intArrayOf(Color.argb(238,245,252,255),Color.argb(230,240,235,255))).apply { shape=GradientDrawable.OVAL; setStroke(dp(1),Color.rgb(116,126,224)) } else GradientDrawable(GradientDrawable.Orientation.TL_BR,intArrayOf(Color.rgb(17,62,78),Color.rgb(55,29,91))).apply { shape=GradientDrawable.OVAL; setStroke(dp(1),Color.rgb(48,151,177)) }
                    layoutParams=LinearLayout.LayoutParams(dp(36),dp(36)).apply { marginEnd=dp(11) }
                })
                addView(TextView(this@MainActivity).apply {
                    layoutParams=LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,1f)
                    text=nightExplanation + "\n" + comparison
                    textSize=13f; setTextColor(if(light) Color.WHITE else Color.rgb(220,224,244)); setTypeface(typeface,Typeface.BOLD)
                })
            })
            layoutParams=LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(dp(4),dp(10),dp(4),0) }
        })
        val dnaSectionStart=sleepCard.childCount
        // Sleep DNA 2.0: a touch-driven fingerprint, entirely derived from measured intervals.
        val dnaSegments=s.stageSeries.filter { it.endMs>it.startMs && it.endMs>s.startMs && it.startMs<s.endMs }
            .sortedBy { it.startMs }
        sleepCard.addView(TextView(this).apply {
            text="SLEEP DNA  ·  DEINE NACHTSIGNATUR"
            textSize=11f; letterSpacing=.13f; setTypeface(typeface,Typeface.BOLD)
            setTextColor(if(light) Color.WHITE else stageRem)
            setPadding(dp(5),dp(20),0,dp(8))
        })
        val dnaDetail=TextView(this).apply {
            text="Außen: Schlafverlauf  ·  Innen: Phasenanteile"
            textSize=11f; gravity=android.view.Gravity.CENTER
            setTextColor(if(light) Color.WHITE else Color.rgb(194,204,235))
            setPadding(dp(8),dp(8),dp(8),dp(12))
        }
        sleepCard.addView(MaterialCardView(this).apply {
            radius=dp(24).toFloat()
            cardElevation=if(light) dp(10).toFloat() else dp(2).toFloat()
            strokeWidth=dp(1)
            strokeColor=Color.argb(180,147,105,235)
            setCardBackgroundColor(Color.TRANSPARENT)
            if(light) {
                background=GradientDrawable().apply {
                    cornerRadius=dp(24).toFloat()
                    setColor(Color.argb(designGlassAlpha(),72,88,112))
                    setStroke(dp(1),Color.argb(180,147,105,235))
                }
                addBlurLayer(this,24,::dp)
            } else setCardBackgroundColor(Color.argb(220,22,25,56))
            addView(object:View(this@MainActivity) {
                private val ink=android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
                private var dnaBreath=0f
                private val dnaAnimator=android.animation.ValueAnimator.ofFloat(0f,1f).apply {
                    duration=5200L
                    repeatCount=android.animation.ValueAnimator.INFINITE
                    interpolator=android.view.animation.LinearInterpolator()
                    addUpdateListener { dnaBreath=it.animatedValue as Float;if(HistoryScrollGate.shouldRender())invalidate() }
                }
                override fun onAttachedToWindow() {
                    super.onAttachedToWindow()
                    if(android.animation.ValueAnimator.areAnimatorsEnabled()) dnaAnimator.start()
                }
                override fun onDetachedFromWindow() {
                    dnaAnimator.cancel()
                    super.onDetachedFromWindow()
                }
                private var focusedTime:Long?=null
                private val palette=mapOf("leicht" to stageLight,"tief" to stageDeep,"rem" to stageRem,"wach" to stageAwake)
                private val span=(s.endMs-s.startMs).coerceAtLeast(1L).toDouble()
                private val clock=java.time.format.DateTimeFormatter.ofPattern("HH:mm")
                    .withZone(java.time.ZoneId.systemDefault())
                private fun angle(t:Long)=(((t-s.startMs)/span)*360.0).toFloat().coerceIn(0f,360f)
                init {
                    setLayerType(View.LAYER_TYPE_SOFTWARE,null)
                    isClickable=true; isFocusable=true
                    contentDescription="Interaktive Sleep DNA. Ring berühren, um Schlafphase und Uhrzeit zu erkunden."
                }
                override fun onDraw(canvas:android.graphics.Canvas) {
                    super.onDraw(canvas)
                    val w=width.toFloat();val h=height.toFloat()
                    if(w<=0f||h<=0f)return
                    val cx=w/2f;val cy=h/2f
                    val breathWave=.5f+.5f*kotlin.math.sin((dnaBreath*2f*Math.PI).toFloat())
                    val outer=kotlin.math.min(w,h)*.36f
                    val inner=outer-dp(37)
                    val track=android.graphics.RectF(cx-outer,cy-outer,cx+outer,cy+outer)
                    val innerTrack=android.graphics.RectF(cx-inner,cy-inner,cx+inner,cy+inner)
                    ink.shader=null
                    ink.style=android.graphics.Paint.Style.STROKE
                    ink.strokeCap=android.graphics.Paint.Cap.ROUND
                    ink.strokeWidth=dp(2).toFloat()
                    ink.color=Color.argb(85,174,178,229)
                    canvas.drawCircle(cx,cy,outer,ink)
                    ink.strokeWidth=dp(1).toFloat()
                    ink.color=Color.argb(55,170,145,245)
                    canvas.drawCircle(cx,cy,outer+dp(12),ink)

                    if(dnaSegments.isNotEmpty()) {
                        dnaSegments.forEach { phase ->
                            val start=angle(phase.startMs);val sweep=angle(phase.endMs)-start
                            if(sweep<=0f)return@forEach
                            val tone=palette[phase.stageLabel.trim().lowercase()]?:Color.rgb(122,134,161)
                            ink.color=tone
                            ink.strokeWidth=(resources.displayMetrics.density*(5.5f+1.2f*breathWave))
                            ink.setShadowLayer((resources.displayMetrics.density*(3f+10f*breathWave)),0f,0f,tone)
                            canvas.drawArc(track,start-90f,sweep,false,ink)
                            ink.clearShadowLayer()
                            ink.strokeWidth=dp(1.2f.toInt()).toFloat()
                            ink.color=Color.argb(90,Color.red(tone),Color.green(tone),Color.blue(tone))
                            canvas.drawArc(track,start-90f,sweep,false,ink)
                        }
                        // Separate proportional ring: phase shares across the complete night.
                        val phaseValues=listOf(s.lightMin to stageLight,s.deepMin to stageDeep,
                            s.remMin to stageRem,s.awakeMin to stageAwake)
                        val phaseSum=phaseValues.sumOf { it.first.toLong() }.coerceAtLeast(1L)
                        var phaseStart=-90f
                        val phaseRadius=inner
                        val phaseTrack=android.graphics.RectF(cx-phaseRadius,cy-phaseRadius,
                            cx+phaseRadius,cy+phaseRadius)
                        phaseValues.forEach { (minutes,tone) ->
                            val sweep=minutes.toFloat()/phaseSum*360f
                            ink.color=tone
                            ink.strokeWidth=(resources.displayMetrics.density*(3.7f+.7f*breathWave))
                            ink.setShadowLayer((resources.displayMetrics.density*(2f+6f*breathWave)),0f,0f,tone)
                            canvas.drawArc(phaseTrack,phaseStart,sweep,false,ink)
                            ink.clearShadowLayer()
                            phaseStart+=sweep
                        }
                    } else {
                        val values=listOf(s.lightMin to stageLight,s.deepMin to stageDeep,s.remMin to stageRem,s.awakeMin to stageAwake)
                        val total=values.sumOf { it.first.toLong() }.coerceAtLeast(1L)
                        var start=-90f
                        values.forEach { (minutes,tone) ->
                            val sweep=minutes.toFloat()/total*360f
                            ink.color=tone;ink.strokeWidth=(resources.displayMetrics.density*(5.5f+1.2f*breathWave))
                            ink.setShadowLayer((resources.displayMetrics.density*(3f+10f*breathWave)),0f,0f,tone)
                            canvas.drawArc(track,start,sweep,false,ink)
                            ink.clearShadowLayer()
                            start+=sweep
                        }
                    }
                    focusedTime?.let { time ->
                        val rad=Math.toRadians((angle(time)-90f).toDouble())
                        val px=cx+kotlin.math.cos(rad).toFloat()*outer
                        val py=cy+kotlin.math.sin(rad).toFloat()*outer
                        ink.style=android.graphics.Paint.Style.FILL
                        ink.color=Color.WHITE
                        canvas.drawCircle(px,py,dp(5).toFloat(),ink)
                    }
                    ink.style=android.graphics.Paint.Style.FILL
                    ink.textAlign=android.graphics.Paint.Align.CENTER
                    ink.typeface=Typeface.create(Typeface.DEFAULT,Typeface.BOLD)
                    ink.color=Color.WHITE
                    val phase=focusedTime?.let { time -> dnaSegments.lastOrNull { time>=it.startMs && time<it.endMs } }
                    ink.textSize=dp(29).toFloat()
                    val primary=if(focusedTime==null) String.format(java.util.Locale.GERMANY,"%d:%02d",s.totalMin/60,s.totalMin%60)
                        else clock.format(java.time.Instant.ofEpochMilli(focusedTime!!))
                    canvas.drawText(primary,cx,cy+dp(1),ink)
                    ink.textSize=dp(10).toFloat()
                    ink.color=Color.rgb(202,211,247)
                    canvas.drawText(if(focusedTime==null) "SCHLAFDAUER" else phase?.stageLabel?.uppercase()?:"KEINE PHASENDATEN",
                        cx,cy+dp(22),ink)
                }
                override fun onTouchEvent(event:android.view.MotionEvent):Boolean {
                    when(event.actionMasked) {
                        android.view.MotionEvent.ACTION_DOWN,android.view.MotionEvent.ACTION_MOVE -> {
                            parent?.requestDisallowInterceptTouchEvent(true)
                            val degrees=((Math.toDegrees(kotlin.math.atan2(
                                (event.y-height/2f).toDouble(),(event.x-width/2f).toDouble()))+450.0)%360.0)
                            focusedTime=(s.startMs+(degrees/360.0*span).toLong()).coerceIn(s.startMs,s.endMs-1)
                            val phase=dnaSegments.lastOrNull { segment -> focusedTime?.let { it>=segment.startMs && it<segment.endMs }==true }
                            dnaDetail.text=if(phase!=null) "${clock.format(java.time.Instant.ofEpochMilli(focusedTime!!))}  ·  ${phase.stageLabel}  ·  ${((phase.endMs-phase.startMs)/60000L).coerceAtLeast(1L)} min" else "Keine Schlafphase für diese Uhrzeit"
                            invalidate()
                            return true
                        }
                        android.view.MotionEvent.ACTION_UP -> {
                            parent?.requestDisallowInterceptTouchEvent(false)
                            performClick();return true
                        }
                        android.view.MotionEvent.ACTION_CANCEL -> {
                            parent?.requestDisallowInterceptTouchEvent(false);return true
                        }
                    }
                    return true
                }
                override fun performClick():Boolean { super.performClick();return true }
            }.apply {
                layoutParams=android.widget.FrameLayout.LayoutParams(-1,dp(250))
            })
            layoutParams=LinearLayout.LayoutParams(-1,-2).apply { setMargins(dp(4),0,dp(4),dp(5)) }
        })
        val dnaLegend=LinearLayout(this).apply {
            orientation=LinearLayout.HORIZONTAL
            gravity=android.view.Gravity.CENTER
            setPadding(dp(5),dp(9),dp(5),dp(8))
            listOf(Triple("TIEF",s.deepMin,stageDeep),Triple("LEICHT",s.lightMin,stageLight),
                Triple("REM",s.remMin,stageRem),Triple("WACH",s.awakeMin,stageAwake)).forEach { (name,mins,tone) ->
                addView(LinearLayout(this@MainActivity).apply {
                    orientation=LinearLayout.VERTICAL
                    gravity=android.view.Gravity.CENTER
                    addView(TextView(this@MainActivity).apply {
                        text="● $name";textSize=10f;setTextColor(tone)
                        gravity=android.view.Gravity.CENTER
                    })
                    addView(TextView(this@MainActivity).apply {
                        text="${mins/60}:${(mins%60).toString().padStart(2,'0')} h"
                        textSize=12f;setTypeface(typeface,Typeface.BOLD)
                        setTextColor(Color.WHITE);gravity=android.view.Gravity.CENTER
                    })
                },LinearLayout.LayoutParams(0,-2,1f))
            }
        }
        sleepCard.addView(dnaLegend)
        sleepCard.addView(dnaDetail)

        // SleepDNA insights: measured proportions, without inventing a medical sleep score.
        val dnaMeasured=(s.lightMin+s.deepMin+s.remMin).coerceAtLeast(0)
        val dnaTotal=(dnaMeasured+s.awakeMin.coerceAtLeast(0)).coerceAtLeast(1)
        fun dnaPercent(minutes:Long)=((minutes.coerceAtLeast(0L)*100f)/dnaTotal).toInt()
        val dnaInsights=LinearLayout(this).apply {
            orientation=LinearLayout.VERTICAL
            setPadding(dp(17),dp(15),dp(17),dp(15))
            background=GradientDrawable(GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.argb(225,25,29,62),Color.argb(215,13,30,48))).apply {
                cornerRadius=dp(20).toFloat()
                setStroke(dp(1),Color.argb(145,143,108,238))
            }
            addView(TextView(this@MainActivity).apply {
                text="DEINE SLEEPDNA-ANALYSE"
                textSize=12f;letterSpacing=.12f
                setTypeface(typeface,Typeface.BOLD)
                setTextColor(Color.rgb(200,169,255))
            })
            val dominant=listOf("Leichtschlaf" to s.lightMin,"Tiefschlaf" to s.deepMin,"REM-Schlaf" to s.remMin)
                .maxByOrNull { it.second }
            val analysis=if(dnaMeasured<=0) "Für diese Nacht liegen keine auswertbaren Schlafphasen vor."
                else "Größter Schlafanteil: ${dominant?.first ?: "Unbekannt"}. Die Anteile basieren auf den aufgezeichneten Minuten."
            addView(TextView(this@MainActivity).apply {
                text=analysis
                textSize=12f;setTextColor(Color.rgb(222,231,250))
                setPadding(0,dp(8),0,dp(12))
            })
            val items=listOf(Triple("LEICHT",s.lightMin,stageLight),
                Triple("TIEF",s.deepMin,stageDeep),Triple("REM",s.remMin,stageRem),
                Triple("WACH",s.awakeMin,stageAwake))
            items.forEach { (label,minutes,tone) ->
                addView(LinearLayout(this@MainActivity).apply {
                    orientation=LinearLayout.VERTICAL
                    setPadding(0,dp(5),0,dp(5))
                    addView(TextView(this@MainActivity).apply {
                        text="$label  ·  ${dnaPercent(minutes)} %"
                        textSize=11f;setTextColor(Color.WHITE)
                    })
                    addView(android.widget.ProgressBar(this@MainActivity,null,
                        android.R.attr.progressBarStyleHorizontal).apply {
                        max=1000;progress=(minutes.coerceAtLeast(0)*1000L/dnaTotal).toInt()
                        progressTintList=android.content.res.ColorStateList.valueOf(tone)
                        progressBackgroundTintList=android.content.res.ColorStateList.valueOf(Color.argb(65,170,185,220))
                        layoutParams=LinearLayout.LayoutParams(-1,dp(5)).apply{topMargin=dp(4)}
                    })
                })
            }
            addView(TextView(this@MainActivity).apply {
                text="Anteile der erfassten Schlaf- und Wachphasen · keine medizinische Bewertung"
                textSize=10f;setTextColor(Color.rgb(163,177,208))
                setPadding(0,dp(9),0,0)
            })
        }
        // Fingerprint metrics use the recorded, chronologically ordered stage intervals.
        val fingerprintStages=dnaSegments.filter { it.endMs>it.startMs }
        val transitions=fingerprintStages.zipWithNext().count { (a,b) ->
            a.stageLabel.trim().lowercase()!=b.stageLabel.trim().lowercase()
        }
        val awakeIntervals=fingerprintStages.filter {
            it.stageLabel.trim().lowercase().contains("wach")
        }
        val awakeEvents=awakeIntervals.size
        val fingerprintMinutes=((s.endMs-s.startMs)/60000L).coerceAtLeast(1L)
        val changesPerHour=transitions*60f/fingerprintMinutes
        val dnaFingerprint=LinearLayout(this).apply {
            orientation=LinearLayout.VERTICAL
            setPadding(dp(17),dp(15),dp(17),dp(15))
            background=GradientDrawable(GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.argb(230,23,27,59),Color.argb(219,15,34,50))).apply {
                cornerRadius=dp(20).toFloat()
                setStroke(dp(1),Color.argb(160,97,200,245))
            }
            addView(TextView(this@MainActivity).apply {
                text="SLEEPDNA  ·  DEIN FINGERPRINT"
                textSize=12f;letterSpacing=.10f
                setTypeface(typeface,Typeface.BOLD)
                setTextColor(Color.rgb(137,216,255))
            })
            val stats=if(fingerprintStages.size<2)
                "Für die Schlafarchitektur fehlen ausreichend Phasenintervalle."
            else "Phasenwechsel: $transitions  ·  ${String.format(java.util.Locale.GERMANY,"%.1f",changesPerHour)} pro Stunde"
            addView(TextView(this@MainActivity).apply {
                text=stats
                textSize=13f;setTextColor(Color.WHITE)
                setPadding(0,dp(10),0,dp(7))
            })
            addView(TextView(this@MainActivity).apply {
                text=if(fingerprintStages.isEmpty()) "Keine aufgezeichneten Phasenintervalle."
                    else "Erfasste Wachintervalle: $awakeEvents  ·  Wachzeit: ${s.awakeMin} min"
                textSize=12f;setTextColor(Color.rgb(205,185,250))
            })
            addView(TextView(this@MainActivity).apply {
                text="Phasenwechsel und Wachintervalle laut Aufzeichnung · keine medizinische Bewertung"
                textSize=10f;setTextColor(Color.rgb(163,177,208))
                setPadding(0,dp(11),0,0)
            })
        }
        sleepCard.addView(dnaFingerprint,LinearLayout.LayoutParams(-1,-2).apply {
            setMargins(dp(4),dp(8),dp(4),dp(8))
        })
        sleepCard.addView(dnaInsights,LinearLayout.LayoutParams(-1,-2).apply{
            setMargins(dp(4),dp(9),dp(4),dp(9))
        })

        // SleepDNA trend: compare actual measured nights, without a synthetic score.
        val recentDnaNights=(sleepHistory+listOf(s))
            .distinctBy { it.endMs }
            .filter { it.totalMin>0L && it.endMs<=s.endMs }
            .sortedByDescending { it.endMs }
            .take(7)
        if(recentDnaNights.size>=2) {
            val previous=recentDnaNights.drop(1)
            val baseline=previous.map { it.totalMin.toDouble() }.average()
            val delta=s.totalMin-baseline.toLong()
            val trendText=when {
                kotlin.math.abs(delta)<5L -> "Deine Schlafdauer liegt ungefähr auf dem Niveau der vorherigen Nächte."
                delta>0L -> "Du hast ${delta} Minuten länger geschlafen als im Durchschnitt der ${previous.size} vorherigen Nächte."
                else -> "Du hast ${-delta} Minuten kürzer geschlafen als im Durchschnitt der ${previous.size} vorherigen Nächte."
            }
            sleepCard.addView(LinearLayout(this).apply {
                orientation=LinearLayout.VERTICAL
                setPadding(dp(17),dp(14),dp(17),dp(15))
                background=GradientDrawable(GradientDrawable.Orientation.TL_BR,
                    intArrayOf(Color.argb(225,21,35,62),Color.argb(215,24,21,55))).apply {
                    cornerRadius=dp(20).toFloat()
                    setStroke(dp(1),Color.argb(135,94,193,239))
                }
                addView(TextView(this@MainActivity).apply {
                    text="SLEEPDNA  ·  7-NÄCHTE-TREND"
                    textSize=12f;letterSpacing=.10f
                    setTypeface(typeface,Typeface.BOLD)
                    setTextColor(Color.rgb(137,216,255))
                })
                addView(TextView(this@MainActivity).apply {
                    text=trendText
                    textSize=13f;setTextColor(Color.WHITE)
                    setPadding(0,dp(10),0,dp(6))
                })
                // Compare sleep-stage composition only for nights with measured stage minutes.
                val stageBaseline=previous.filter { it.lightMin+it.deepMin+it.remMin>0L }
                if(s.lightMin+s.deepMin+s.remMin>0L && stageBaseline.isNotEmpty()) {
                    fun stageTrend(label:String,current:Long,average:Long,tone:Int):LinearLayout {
                        val difference=current-average
                        val arrow=when {
                            kotlin.math.abs(difference)<5L -> "≈"
                            difference>0L -> "▲"
                            else -> "▼"
                        }
                        val value=if(kotlin.math.abs(difference)<5L) "ähnlich"
                            else "${if(difference>0L) "+" else "−"}${kotlin.math.abs(difference)} min"
                        return LinearLayout(this@MainActivity).apply {
                            orientation=LinearLayout.HORIZONTAL
                            gravity=android.view.Gravity.CENTER_VERTICAL
                            setPadding(0,dp(7),0,dp(5))
                            addView(TextView(this@MainActivity).apply {
                                text="$label  ·  ${current} min"
                                textSize=12f;setTextColor(Color.WHITE)
                                layoutParams=LinearLayout.LayoutParams(0,-2,1f)
                            })
                            addView(TextView(this@MainActivity).apply {
                                text="$arrow $value"
                                textSize=12f;setTypeface(typeface,Typeface.BOLD)
                                setTextColor(tone)
                            })
                        }
                    }
                    addView(TextView(this@MainActivity).apply {
                        text="SCHLAFPHASEN IM VERGLEICH"
                        textSize=10f;letterSpacing=.10f
                        setTypeface(typeface,Typeface.BOLD)
                        setTextColor(Color.rgb(187,196,226))
                        setPadding(0,dp(12),0,dp(3))
                    })
                    addView(stageTrend("Tiefschlaf",s.deepMin,
                        stageBaseline.map { it.deepMin }.average().toLong(),stageDeep))
                    addView(stageTrend("REM-Schlaf",s.remMin,
                        stageBaseline.map { it.remMin }.average().toLong(),stageRem))
                    addView(TextView(this@MainActivity).apply {
                        text="Schlafphasenvergleich mit ${stageBaseline.size} früheren Nächten mit Phasendaten"
                        textSize=10f;setTextColor(Color.rgb(164,187,214))
                        setPadding(0,dp(4),0,dp(5))
                    })
                }
                addView(TextView(this@MainActivity).apply {
                    text="Vergleich mit ${previous.size} früheren erfassten Nächten · Schlafdauer, keine medizinische Bewertung"
                    textSize=10f;setTextColor(Color.rgb(164,187,214))
                })
            },LinearLayout.LayoutParams(-1,-2).apply {
                setMargins(dp(4),dp(7),dp(4),dp(10))
            })
        }


        // Personal 14-night signature: compare with earlier recorded nights only.
        val dna14=(sleepHistory+listOf(s)).distinctBy { it.endMs }
            .filter { it.totalMin>0L && it.endMs<=s.endMs }
            .sortedByDescending { it.endMs }.take(14)
        val dna14Baseline=dna14.filter { it.endMs!=s.endMs }
        if(dna14Baseline.size>=3) {
            val typicalMinutes=dna14Baseline.map { it.totalMin.toDouble() }.average().toLong()
            val deviation=s.totalMin-typicalMinutes
            val typicalDeep=dna14Baseline.filter { it.lightMin+it.deepMin+it.remMin>0L }
            val typicalRem=typicalDeep
            val stageAvailable=s.lightMin+s.deepMin+s.remMin>0L && typicalDeep.size>=3
            fun fmtDNA(minutes:Long)="${minutes/60} h ${(minutes%60).toString().padStart(2,'0')} min"
            val dnaSignature=LinearLayout(this).apply {
                orientation=LinearLayout.VERTICAL
                setPadding(dp(17),dp(15),dp(17),dp(15))
                background=GradientDrawable(GradientDrawable.Orientation.TL_BR,
                    intArrayOf(Color.argb(226,32,24,66),Color.argb(218,12,37,55))).apply {
                    cornerRadius=dp(20).toFloat()
                    setStroke(dp(1),Color.argb(155,178,112,247))
                }
                addView(TextView(this@MainActivity).apply {
                    text="SLEEPDNA  ·  DEINE 14-NÄCHTE-SIGNATUR"
                    textSize=12f;letterSpacing=.08f
                    setTypeface(typeface,Typeface.BOLD)
                    setTextColor(Color.rgb(212,164,255))
                })
                addView(TextView(this@MainActivity).apply {
                    text="Typische Schlafdauer: ${fmtDNA(typicalMinutes)}"
                    textSize=13f;setTypeface(typeface,Typeface.BOLD)
                    setTextColor(Color.WHITE)
                    setPadding(0,dp(10),0,dp(5))
                })
                val deviationLabel=when {
                    kotlin.math.abs(deviation)<10L -> "Diese Nacht liegt nahe an deinem bisherigen Durchschnitt."
                    deviation>0L -> "Diese Nacht: ${deviation} min länger als dein bisheriger Durchschnitt."
                    else -> "Diese Nacht: ${-deviation} min kürzer als dein bisheriger Durchschnitt."
                }
                addView(TextView(this@MainActivity).apply {
                    text=deviationLabel;textSize=12f
                    setTextColor(Color.rgb(196,222,247))
                })
                if(stageAvailable) {
                    val avgDeep=typicalDeep.map { it.deepMin.toDouble() }.average().toLong()
                    val avgRem=typicalRem.map { it.remMin.toDouble() }.average().toLong()
                    addView(TextView(this@MainActivity).apply {
                        text="Typischer Tiefschlaf: ${fmtDNA(avgDeep)}  ·  REM: ${fmtDNA(avgRem)}"
                        textSize=11f;setTextColor(Color.rgb(210,191,252))
                        setPadding(0,dp(9),0,0)
                    })
                }
                addView(TextView(this@MainActivity).apply {
                    text="Dein persönlicher Vergleich mit ${dna14Baseline.size} früheren Nächten · kein medizinischer Referenzwert"
                    textSize=10f;setTextColor(Color.rgb(166,183,209))
                    setPadding(0,dp(11),0,0)
                })
            }
            sleepCard.addView(dnaSignature,LinearLayout.LayoutParams(-1,-2).apply {
                setMargins(dp(4),dp(8),dp(4),dp(9))
            })
        }

        collapseDashboardSection(dnaSectionStart,"SLEEPDNA  ·  DEINE NACHTSIGNATUR",
            "Interaktiver Schlafring · Phasenanalyse · 7-Nächte-Trend",stageRem)

        makeOverviewTextWhite(sleepCard)
    }

    override fun onResume() {
        super.onResume()
        HistoryScrollGate.appVisible=true
        if(::sleepCard.isInitialized) sleepCard.invalidate()
        if(::brandGlow.isInitialized) brandGlow.invalidate()
    }

    override fun onPause() {
        HistoryScrollGate.appVisible=false
        super.onPause()
    }

    override fun onDestroy() {
        brandGlowAnimator?.cancel()
        brandGlowAnimator=null
        super.onDestroy()
        cancel()
    }
}

class HealthPermissionRationaleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        DynamicColors.applyToActivityIfAvailable(this)
        super.onCreate(savedInstanceState)
        setContentView(TextView(this).apply { textSize=18f; setPadding(48,80,48,48); text="Garmin Sleep for Tasker liest nur die von dir freigegebenen Health-Connect-Daten, um Schlafdauer, Schlafphasen und zugehörige Messwerte für deine eigene Tasker-Automation auszuwerten. Es werden keine Daten hochgeladen." })
    }
}
