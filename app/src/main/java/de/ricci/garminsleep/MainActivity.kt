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
    override fun onDraw(c:Canvas){ super.onDraw(c); val d=resources.displayMetrics.density; val cx=width/2f; val cy=height/2f; p.color=if(active) Color.WHITE else Color.rgb(155,164,190); p.style=Paint.Style.STROKE
        when(kind){
            0->{ val q=Path(); q.moveTo(cx-9*d,cy); q.lineTo(cx,cy-8*d); q.lineTo(cx+9*d,cy); q.moveTo(cx-6*d,cy-2*d); q.lineTo(cx-6*d,cy+8*d); q.lineTo(cx+6*d,cy+8*d); q.lineTo(cx+6*d,cy-2*d); c.drawPath(q,p) }
            1->{ c.drawRoundRect(cx-9*d,cy-7*d,cx+9*d,cy+7*d,2*d,2*d,p); c.drawLine(cx-5*d,cy-2*d,cx-1*d,cy-2*d,p); c.drawLine(cx-5*d,cy+3*d,cx+4*d,cy+3*d,p); c.drawLine(cx+4*d,cy-4*d,cx+6*d,cy-4*d,p) }
            2->{ c.drawRoundRect(cx-8*d,cy-7*d,cx+8*d,cy+8*d,2*d,2*d,p); c.drawLine(cx-8*d,cy-2*d,cx+8*d,cy-2*d,p); c.drawLine(cx-4*d,cy-9*d,cx-4*d,cy-5*d,p); c.drawLine(cx+4*d,cy-9*d,cx+4*d,cy-5*d,p); p.style=Paint.Style.FILL; c.drawCircle(cx-3*d,cy+2*d,1.2f*d,p); c.drawCircle(cx+3*d,cy+2*d,1.2f*d,p) }
            else->{ c.drawCircle(cx,cy,3.2f*d,p); for(i in 0 until 8){ val a=Math.PI*2*i/8; c.drawLine(cx+(Math.cos(a)*6*d).toFloat(),cy+(Math.sin(a)*6*d).toFloat(),cx+(Math.cos(a)*9*d).toFloat(),cy+(Math.sin(a)*9*d).toFloat(),p) } }
        }
    }
}

class MainActivity : ComponentActivity(), CoroutineScope by MainScope() {
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
    private val accent = Color.rgb(139, 92, 246)
    private val accent2 = Color.rgb(34, 211, 238)
    private val stageLight = Color.rgb(99, 190, 255)
    private val stageDeep = Color.rgb(95, 75, 220)
    private val stageRem = Color.rgb(183, 99, 255)
    private val stageAwake = Color.rgb(255, 164, 91)
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
        val theme=getSharedPreferences("sleepsync_ui",MODE_PRIVATE).getString("theme","dark")?:"dark"; val sysDark=(resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK)==android.content.res.Configuration.UI_MODE_NIGHT_YES; val light=theme=="light" || (theme=="system" && !sysDark); val primary=if(light) Color.rgb(24,29,48) else Color.WHITE; val secondary=if(light) Color.rgb(82,94,121) else Color.rgb(165,175,205); val selectedId=calendarPrefs().getLong("calendar_id",-1L)
        val shell=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(18),dp(18),dp(18),dp(12));background=GradientDrawable().apply{cornerRadius=dp(24).toFloat();setColor(if(light) Color.argb(246,247,250,255) else Color.rgb(12,15,35));setStroke(dp(1),stageRem)}}
        shell.addView(TextView(this).apply{text="📅  ZIELKALENDER";textSize=18f;setTextColor(stageRem);setTypeface(typeface,Typeface.BOLD);setPadding(0,0,0,dp(4))})
        shell.addView(TextView(this).apply{text="Wohin soll SleepSync deine Nächte schreiben?";textSize=12f;setTextColor(secondary);setPadding(0,0,0,dp(12))})
        val list=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}
        val dialog=AlertDialog.Builder(this).setView(ScrollView(this).apply{addView(shell)}).create()
        items.forEach{item->val selected=item.first==selectedId;list.addView(TextView(this).apply{text=(if(selected) "✓  " else "")+item.second+"\n"+item.third;textSize=14f;setTextColor(primary);setPadding(dp(14),dp(11),dp(14),dp(11));background=GradientDrawable().apply{cornerRadius=dp(13).toFloat();setColor(if(light) (if(selected) Color.argb(160,226,220,255) else Color.argb(145,229,235,248)) else Color.argb(120,40,29,70));if(selected)setStroke(dp(1),stageRem)};layoutParams=LinearLayout.LayoutParams(-1,-2).apply{setMargins(0,0,0,dp(7))};setOnClickListener{getSharedPreferences("sleepsync_calendar",MODE_PRIVATE).edit().putLong("calendar_id",item.first).putString("calendar_name",item.second).putString("calendar_account",item.third).apply();dialog.dismiss();showCalendarPlaceholder()}})}
        shell.addView(list);dialog.setOnShowListener{dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))};dialog.show()
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
        val request=PeriodicWorkRequestBuilder<SleepSyncWorker>(30,TimeUnit.MINUTES).build()
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
        status = TextView(this).apply { textSize = 14f; setPadding(dp(18),dp(14),dp(18),dp(14)) }
        sleepCard = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(18),dp(18),dp(18),dp(18)) }
        val bootTheme=getSharedPreferences("sleepsync_ui",MODE_PRIVATE).getString("theme","dark") ?: "dark"
        val bootSystemDark=(resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK)==android.content.res.Configuration.UI_MODE_NIGHT_YES
        val bootLight=bootTheme=="light" || (bootTheme=="system" && !bootSystemDark)
        pageTitle = TextView(this).apply { text = "SleepSync"; textSize = 30f; setTypeface(typeface, Typeface.BOLD); setTextColor(if(bootLight) Color.rgb(24,29,48) else Color.WHITE) }
        pageSubtitle = TextView(this).apply { text = "Dein Schlaf. Klar, automatisch, im Kalender."; textSize = 15f; setTextColor(if(bootLight) Color.rgb(85,94,122) else Color.rgb(184,194,224)); alpha = .82f; setPadding(0,dp(4),0,dp(16)) }
        val sleepShell = MaterialCardView(this).apply {
            radius=0f; cardElevation=0f; strokeWidth=0
            setCardBackgroundColor(Color.TRANSPARENT); addView(sleepCard)
        }
        val nav = LinearLayout(this).apply {
            orientation=LinearLayout.HORIZONTAL; gravity=android.view.Gravity.CENTER; setPadding(dp(6),dp(6),dp(6),dp(6))
            val tabs=mutableListOf<MaterialCardView>()
            fun activate(active:MaterialCardView)=tabs.forEachIndexed { index,card ->
                val on=card===active; val tone=Color.rgb(111,82,255)
                card.setCardBackgroundColor(if(on) Color.rgb(38,65,190) else Color.TRANSPARENT)
                card.strokeWidth=if(on) dp(1) else 0; card.strokeColor=Color.rgb(82,118,255); card.cardElevation=0f
                val box=card.getChildAt(0) as LinearLayout; (box.getChildAt(0) as BottomNavIconView).active=on
                (box.getChildAt(1) as TextView).setTextColor(if(on) Color.WHITE else Color.rgb(150,158,184))
            }
            fun tab(kind:Int,label:String,action:()->Unit)=MaterialCardView(this@MainActivity).apply {
                radius=dp(12).toFloat(); setCardBackgroundColor(Color.TRANSPARENT)
                layoutParams=LinearLayout.LayoutParams(0,dp(56),1f).apply { setMargins(dp(3),0,dp(3),0) }
                addView(LinearLayout(this@MainActivity).apply { orientation=LinearLayout.VERTICAL; gravity=android.view.Gravity.CENTER
                    addView(BottomNavIconView(this@MainActivity,kind),LinearLayout.LayoutParams(dp(27),dp(27)))
                    addView(TextView(this@MainActivity).apply { text=label; textSize=9f; gravity=android.view.Gravity.CENTER },LinearLayout.LayoutParams(-1,dp(18)))
                }); setOnClickListener { activate(this); action() }; tabs.add(this)
            }
            val home=tab(0,"Übersicht"){showOverview()}; addView(home)
            addView(tab(1,"Verlauf"){showHistoryPlaceholder()})
            addView(tab(2,"Kalender"){showCalendarPlaceholder()})
            addView(tab(3,"Einstellungen"){showSettings()})
            post { activate(home) }
        }
        actionsTitle = TextView(this).apply { text="Verbindungen & Automatik"; textSize=18f; setTypeface(typeface, Typeface.BOLD); setPadding(0,dp(22),0,dp(8)) }
        val grant = button("Health Connect · Berechtigungen") { permissionLauncher.launch(permissions) }
        val link = button("Garmin Connect · Verbinden") { showGarminLogin() }
        val unlink = button("Garmin Connect · Trennen") { garminClient.logout(); refresh() }
        val test = button("Schlafdaten neu laden") { testRead() }
        val sig = button("App-Signatur anzeigen") { showAppSignature() }
        val statusCard = MaterialCardView(this).apply {
            radius=dp(18).toFloat(); cardElevation=dp(2).toFloat(); strokeWidth=dp(1); strokeColor=if(bootLight) Color.argb(125,74,190,225) else Color.argb(115,91,176,255)
            setCardBackgroundColor(if(bootLight) Color.argb(210,240,247,255) else Color.argb(118,5,13,30))
            status.setPadding(dp(12),dp(7),dp(12),dp(7)); addView(status)
            layoutParams=LinearLayout.LayoutParams(-1,-2).apply { setMargins(0,dp(10),0,dp(12)) }
        }
        actionsBox = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; addView(grant); addView(link); addView(unlink); addView(test); addView(sig) }
        pageTitle.setTextColor(if(bootLight) Color.rgb(16,32,72) else Color.WHITE); pageTitle.textSize=31f; pageTitle.setTypeface(pageTitle.typeface,Typeface.BOLD); pageTitle.letterSpacing=-.025f
        pageTitle.setShadowLayer(10f,0f,dp(1).toFloat(),Color.argb(165,3,7,28))
        pageTitle.background=null
        pageTitle.setPadding(0,0,0,0)
        pageSubtitle.setTextColor(if(bootLight) Color.rgb(74,92,130) else Color.rgb(211,218,242)); pageSubtitle.textSize=13f; pageSubtitle.alpha=.90f; pageSubtitle.setShadowLayer(6f,0f,dp(1).toFloat(),Color.argb(190,2,5,20))
        status.setTextColor(if(bootLight) Color.rgb(31,100,119) else Color.rgb(166,238,244)); status.textSize=10f; status.letterSpacing=.08f; status.setTypeface(status.typeface,Typeface.BOLD)
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
        val scene = android.widget.FrameLayout(this).apply {
            addView(android.widget.ImageView(this@MainActivity).apply {
                scaleType=android.widget.ImageView.ScaleType.CENTER_CROP
                setImageResource(if(useLight) R.drawable.sleepsync_day else R.drawable.sleepsync_night)
                alpha=if(useLight) .34f else 1f
            }, android.widget.FrameLayout.LayoutParams(-1,-1))
            addView(View(this@MainActivity).apply {
                background=if(useLight) GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,intArrayOf(Color.argb(72,255,255,255),Color.argb(28,240,247,255),Color.argb(58,225,245,255))) else GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,intArrayOf(Color.TRANSPARENT,Color.argb(35,2,5,15),Color.argb(150,2,4,12)))
            }, android.widget.FrameLayout.LayoutParams(-1,-1))
            addView(box, android.widget.FrameLayout.LayoutParams(-1,-2))
        }
        val scroll = ScrollView(this).apply {
            isFillViewport=true; clipToPadding=false; background=nightAtmosphere; addView(scene)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,0,1f)
        }
        val navShell = MaterialCardView(this).apply {
            radius=dp(14).toFloat(); cardElevation=dp(4).toFloat(); strokeWidth=dp(1); strokeColor=if(useLight) Color.rgb(116,181,255) else Color.rgb(25,32,51)
            setCardBackgroundColor(if(useLight) Color.argb(235,248,251,255) else Color.rgb(6,12,25))
            foreground=GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,intArrayOf(Color.argb(20,34,211,238),Color.TRANSPARENT,Color.argb(24,183,99,255))).apply { cornerRadius=dp(32).toFloat() }
            addView(nav)
            layoutParams=LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,dp(68)).apply { setMargins(dp(18),dp(4),dp(18),dp(8)) }
        }
        val root = LinearLayout(this).apply {
            orientation=LinearLayout.VERTICAL; background=if(useLight) GradientDrawable(GradientDrawable.Orientation.TL_BR,intArrayOf(Color.rgb(248,250,255),Color.rgb(231,243,255),Color.rgb(239,233,255),Color.rgb(222,246,255))) else GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,intArrayOf(Color.rgb(8,12,31),nightBg)); addView(scroll); addView(navShell)
        }
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val bars=insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            v.setPadding(0,bars.top,0,bars.bottom); insets
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
        val hc = if (granted.containsAll(permissions)) "✅ Health Connect bereit." else "⚠️ Bitte Health-Connect-Berechtigungen erteilen."
        val gc = if (garminClient.isLinked()) "GARMIN  ●" else "GARMIN  ○"
        val hcShort = if (granted.containsAll(permissions)) "HEALTH CONNECT  ●" else "HEALTH CONNECT  ○"
        status.text = "$gc        $hcShort"
    }


    private fun showAppSignature() {
        val info = packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
        val cert = info.signingInfo?.apkContentsSigners?.firstOrNull()?.toByteArray()
        val sha = cert?.let { MessageDigest.getInstance("SHA-256").digest(it).joinToString("") { b -> "%02x".format(b) } } ?: "unbekannt"
        AlertDialog.Builder(this)
            .setTitle("Installierte App-Signatur")
            .setMessage("Paket: $packageName\nVersion: ${info.longVersionCode}\nSHA-256:\n$sha")
            .setPositiveButton("OK", null)
            .show()
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
        status.text = "Lese Garmin-Schlaf…"
        setLoadingGlow(true)
        try {
            val history = withContext(Dispatchers.IO) { SleepReader(this@MainActivity).garminHistory() }
            sleepHistory = history
            saveCachedHistory(history)
            val s = history.maxByOrNull { it.endMs } ?: error("Keine Garmin-Schlafsession gefunden")
            renderDashboard(s)
            withContext(Dispatchers.IO) { syncLatestNightToCalendar(s) }
            refresh()
        } catch (t: Throwable) {
            sleepCard.removeAllViews()
            sleepCard.addView(TextView(this@MainActivity).apply { text = "⚠️ Schlafdaten konnten nicht geladen werden\n${t.message.orEmpty()}"; textSize = 16f })
        } finally {
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
        val primary=if(light) Color.rgb(24,29,48) else Color.WHITE
        val secondary=if(light) Color.rgb(83,96,123) else Color.rgb(160,205,235)
        val muted=if(light) Color.rgb(104,116,143) else Color.rgb(135,150,180)
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
            val shell=MaterialCardView(this).apply { radius=dp(20).toFloat(); strokeWidth=dp(1); strokeColor=Color.argb(115,91,176,255); setCardBackgroundColor(if(light) Color.argb(224,247,250,255) else Color.argb(190,9,15,32)); layoutParams=LinearLayout.LayoutParams(-1,-2).apply{setMargins(0,0,0,dp(12))} }
            val box=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL }
            val rows=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; visibility=View.GONE }
            val head=LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; gravity=android.view.Gravity.CENTER_VERTICAL; setPadding(dp(15),dp(13),dp(15),dp(13))
                val title=TextView(this@MainActivity).apply { text="KW "+kw+" · "+year; textSize=16f; setTextColor(primary); setTypeface(typeface,Typeface.BOLD) }
                addView(title,LinearLayout.LayoutParams(0,-2,1f))
                addView(TextView(this@MainActivity).apply { text="Ø "+(avg/60)+" h "+(avg%60)+" min  ·  "+items.size+" Nächte"; textSize=11f; setTextColor(secondary) })
                addView(TextView(this@MainActivity).apply { text="  ▾"; textSize=18f; setTextColor(accent2) })
                setOnClickListener { rows.visibility=if(rows.visibility==View.VISIBLE) View.GONE else View.VISIBLE }
            }
            items.sortedByDescending{it.endMs}.forEach { s ->
                rows.addView(LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(dp(15),dp(10),dp(15),dp(12)); background=GradientDrawable().apply{setColor(if(light) Color.argb(92,224,237,250) else Color.argb(70,25,32,58))}; isClickable=true; isFocusable=true; setOnClickListener { showHistoryNight(s) }
                    addView(LinearLayout(this@MainActivity).apply { orientation=LinearLayout.HORIZONTAL
                        addView(TextView(this@MainActivity).apply { text=dateFmt.format(Instant.ofEpochMilli(s.endMs)); textSize=13f; setTextColor(if(light) Color.rgb(42,51,75) else Color.rgb(220,225,245)); layoutParams=LinearLayout.LayoutParams(0,-2,1f) })
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
        val primary=if(light) Color.rgb(24,29,48) else Color.WHITE
        val secondary=if(light) Color.rgb(82,94,121) else Color.rgb(150,165,195)
        val muted=if(light) Color.rgb(96,108,136) else Color.rgb(165,175,205)
        val glass=if(light) Color.argb(224,247,250,255) else Color.argb(225,12,18,40)
        actionsTitle.visibility=View.GONE; actionsBox.visibility=View.GONE
        pageTitle.text="Kalender"; pageSubtitle.text="Deine Nächte · automatisch dort, wo du sie willst"
        sleepCard.removeAllViews(); sleepCard.background=null
        fun card(title:String,sub:String,tone:Int,body:LinearLayout.()->Unit)=MaterialCardView(this).apply{
            radius=dp(23).toFloat();strokeWidth=dp(1);strokeColor=tone;setCardBackgroundColor(glass);cardElevation=dp(1).toFloat()
            layoutParams=LinearLayout.LayoutParams(-1,-2).apply{setMargins(0,0,0,dp(12))}
            addView(LinearLayout(this@MainActivity).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(18),dp(16),dp(18),dp(16))
                addView(TextView(this@MainActivity).apply{text=title;textSize=12f;letterSpacing=.08f;setTextColor(tone);setTypeface(typeface,Typeface.BOLD)})
                addView(TextView(this@MainActivity).apply{text=sub;textSize=11f;setTextColor(secondary);setPadding(0,dp(3),0,dp(12))});body()
            })
        }
        val s=lastSummary ?: sleepHistory.maxByOrNull{it.endMs}
        sleepCard.addView(card("✦  NÄCHSTER KALENDEREINTRAG","Vorschau deiner synchronisierten Nacht",accent2){
            if(s!=null){val tf=DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault());val fmt={m:Long->(m/60).toString()+" h "+(m%60).toString()+" min"}
                addView(TextView(this@MainActivity).apply{text="🌙  "+fmt(s.totalMin)+"     "+tf.format(Instant.ofEpochMilli(s.startMs))+" – "+tf.format(Instant.ofEpochMilli(s.endMs));textSize=22f;setTextColor(primary);setTypeface(typeface,Typeface.BOLD)})
                addView(LinearLayout(this@MainActivity).apply{orientation=LinearLayout.HORIZONTAL;setPadding(0,dp(13),0,dp(8));listOf(s.lightMin to stageLight,s.deepMin to stageDeep,s.remMin to stageRem,s.awakeMin to stageAwake).filter{it.first>0}.forEach{q->addView(View(this@MainActivity).apply{background=GradientDrawable().apply{cornerRadius=dp(5).toFloat();setColor(q.second)}},LinearLayout.LayoutParams(0,dp(9),q.first.toFloat()).apply{setMargins(0,0,dp(2),0)})}})
                addView(TextView(this@MainActivity).apply{text="Leicht "+fmt(s.lightMin)+"  ·  Tief "+fmt(s.deepMin)+"  ·  REM "+fmt(s.remMin)+"  ·  Wach "+fmt(s.awakeMin);textSize=11f;setTextColor(if(light) Color.rgb(77,90,118) else Color.rgb(190,200,225))})
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
            setTextColor(if(light) Color.rgb(38,52,112) else Color.WHITE)
            backgroundTintList=ColorStateList.valueOf(if(light) Color.rgb(239,243,255) else Color.rgb(64,63,205))
            strokeWidth=if(light) dp(1) else 0
            strokeColor=ColorStateList.valueOf(if(light) Color.rgb(91,104,255) else Color.TRANSPARENT)
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
            val cp=getSharedPreferences("sleepsync_calendar",MODE_PRIVATE); val selected=cp.getString("calendar_name",null); addView(TextView(this@MainActivity).apply{text=(selected ?: if(calendarPermissionReady()) "Kalender auswählen" else "Kalenderzugriff erlauben")+"  ›";textSize=16f;setTextColor(primary);setTypeface(typeface,Typeface.BOLD);setPadding(dp(12),dp(12),dp(12),dp(12));background=GradientDrawable().apply{cornerRadius=dp(15).toFloat();setColor(if(light) Color.argb(105,220,226,255) else Color.argb(150,45,29,73));setStroke(dp(1),if(light) Color.argb(105,170,128,255) else Color.TRANSPARENT)};isClickable=true;setOnClickListener{chooseCalendar()}})
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
        sleepCard.addView(TextView(this).apply{text=statusText;textSize=11f;setTextColor(if(calendarAutoEnabled()) (if(light) Color.rgb(22,143,112) else Color.rgb(88,220,183)) else muted);gravity=android.view.Gravity.CENTER;setPadding(0,dp(4),0,dp(12))})
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
        val settingsGrid = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(0,dp(8),0,0) }
        val prefs=getSharedPreferences("sleepsync_ui",MODE_PRIVATE)
        val selectedTheme=prefs.getString("theme","dark") ?: "dark"
        val selectedThemeLabel=when(selectedTheme) { "light"->"Neon Sunrise"; "system"->"Automatisch"; else->"OLED Night" }
        val sysDark=(resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK)==android.content.res.Configuration.UI_MODE_NIGHT_YES
        val settingsLight=selectedTheme=="light" || (selectedTheme=="system" && !sysDark)
        fun setting(icon:String, title:String, sub:String, color:Int, onClick:(() -> Unit)?=null) {
            val fill = if(settingsLight) Color.argb(224,247,250,255) else Color.rgb((Color.red(color)*0.14f).toInt()+8,(Color.green(color)*0.14f).toInt()+8,(Color.blue(color)*0.14f).toInt()+12)
            settingsGrid.addView(MaterialCardView(this).apply {
                radius=dp(22).toFloat(); cardElevation=dp(1).toFloat(); setCardBackgroundColor(fill)
                strokeWidth=dp(1); strokeColor=color
                layoutParams=LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT).apply{setMargins(0,dp(5),0,dp(5))}
                isClickable=onClick!=null; isFocusable=onClick!=null; if(onClick!=null) setOnClickListener { onClick() }
                addView(LinearLayout(this@MainActivity).apply {
                    orientation=LinearLayout.HORIZONTAL; gravity=android.view.Gravity.CENTER_VERTICAL; setPadding(dp(16),dp(14),dp(16),dp(14))
                    addView(TextView(this@MainActivity).apply {
                        text=icon; textSize=23f; gravity=android.view.Gravity.CENTER; setTextColor(color); setPadding(dp(6),dp(6),dp(6),dp(6))
                        background=GradientDrawable().apply { cornerRadius=dp(14).toFloat(); setColor(Color.argb(42,Color.red(color),Color.green(color),Color.blue(color))) }
                        layoutParams=LinearLayout.LayoutParams(dp(48),dp(48)).apply { setMargins(0,0,dp(12),0) }
                    })
                    addView(LinearLayout(this@MainActivity).apply {
                        orientation=LinearLayout.VERTICAL; layoutParams=LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,1f)
                        addView(TextView(this@MainActivity).apply { text=title; textSize=15f; setTextColor(if(settingsLight) Color.rgb(24,29,48) else Color.WHITE); setTypeface(typeface,Typeface.BOLD) })
                        addView(TextView(this@MainActivity).apply { text=sub; textSize=12f; setTextColor(if(settingsLight) Color.rgb(92,101,128) else Color.rgb(166,172,202)); setPadding(0,dp(3),0,0) })
                    })
                    addView(TextView(this@MainActivity).apply { text="›"; textSize=28f; setTextColor(color) })
                })
            })
        }
        val garminLabel=if(garminClient.isLinked()) "Verbunden · Schlafdaten synchronisieren" else "Nicht verbunden · Jetzt verbinden"
        setting("⌚","Garmin Connect",garminLabel,accent2) { showGarminSettings() }
        setting("♥","Health Connect","Berechtigungen & Gesundheitsdaten",stageRem) { showHealthSettings() }
        setting("⚡","Automatik","Hintergrund-Sync & Kalender",stageAwake) { showAutomationSettings() }
        setting("✦","Darstellung","$selectedThemeLabel · SleepSync",accent) { showAppearanceSettings() }
        setting("◈","Datenschutz","Lokale Daten & Diagnose",stageLight) { showPrivacySettings() }
        setting("ⓘ","Über SleepSync","Version, Build & Entwickler",Color.rgb(120,170,255)) { showAboutSettings() }
        sleepCard.addView(settingsGrid)
        actionsTitle.text="DIAGNOSE"; actionsTitle.setTextColor(if(settingsLight) Color.rgb(98,112,142) else stageAwake); actionsTitle.textSize=11f; actionsTitle.letterSpacing=.14f
        actionsTitle.setPadding(0,dp(14),0,dp(4))
        listOf(0,1,2,3,4).forEach { i ->
            val b=actionsBox.getChildAt(i) as? MaterialButton ?: return@forEach
            b.cornerRadius=dp(16)
            b.textSize=12f
            b.minHeight=dp(46)
            b.setTextColor(if(settingsLight) Color.rgb(38,48,76) else Color.rgb(220,224,244))
            b.backgroundTintList=ColorStateList.valueOf(if(settingsLight) Color.argb(220,240,245,255) else Color.rgb(14,17,34))
            b.strokeWidth=dp(1)
            b.strokeColor=ColorStateList.valueOf(if(settingsLight) Color.argb(175,105,132,190) else Color.rgb(48,55,89))
            b.layoutParams=(b.layoutParams ?: LinearLayout.LayoutParams(-1,-2)).apply { height=dp(46) }
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
        val titleId=resources.getIdentifier("alertTitle","id","android")
        if(titleId!=0) dialog.findViewById<TextView>(titleId)?.apply { setTextColor(Color.rgb(24,29,48)); setTypeface(typeface,Typeface.BOLD) }
        dialog.findViewById<TextView>(android.R.id.message)?.apply { setTextColor(Color.rgb(55,64,88)); textSize=16f }
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
        val lastCheck=p.getLong("last_background_check",0L)
        val lastAuto=p.getLong("last_auto_insert_at",0L)
        val fmt=DateTimeFormatter.ofPattern("dd.MM. · HH:mm").withZone(ZoneId.systemDefault())
        val selected=p.getString("calendar_name",null) ?: "Noch kein Zielkalender"
        val msg=buildString {
            append(if(calendarAutoEnabled()) "✓ Automatik ist aktiv" else "○ Automatik ist ausgeschaltet")
            append("\n\nZielkalender: ").append(selected)
            append("\nLetzte Hintergrundprüfung: ").append(if(lastCheck>0) fmt.format(Instant.ofEpochMilli(lastCheck))+" Uhr" else "noch keine")
            append("\nLetzter automatischer Eintrag: ").append(if(lastAuto>0) fmt.format(Instant.ofEpochMilli(lastAuto))+" Uhr" else "noch keiner")
            append("\n\nSleepSync prüft selbstständig im Hintergrund auf neue Schlafdaten. Tasker wird dafür nicht benötigt.")
        }
        AlertDialog.Builder(this)
            .setTitle("Automatik & Kalender")
            .setMessage(msg)
            .setPositiveButton(if(calendarAutoEnabled()) "Automatik ausschalten" else "Automatik einschalten") { _,_ -> p.edit().putBoolean("auto_enabled",!calendarAutoEnabled()).apply(); showSettings() }
            .setNeutralButton("Zielkalender") { _,_ -> chooseCalendar() }
            .setNegativeButton("Schließen",null)
            .show()
    }

    private fun showPrivacySettings() {
        val cached=sleepHistory.size
        AlertDialog.Builder(this)
            .setTitle("Datenschutz & Diagnose")
            .setMessage("SleepSync verarbeitet deine Schlaf- und Gesundheitsdaten lokal auf diesem Gerät. Garmin-Anmeldedaten werden nicht gespeichert; gespeichert werden nur die für die Verbindung benötigten OAuth-Tokens.\n\nLokaler Verlauf: $cached Nächte\nPaket: $packageName\n\nUnter Diagnose findest du technische Informationen zur installierten App.")
            .setPositiveButton("Diagnose") { _,_ -> showAppSignature() }
            .setNeutralButton("Verlauf löschen") { _,_ ->
                AlertDialog.Builder(this).setTitle("Lokalen Verlauf löschen?").setMessage("Der lokal zwischengespeicherte SleepSync-Verlauf wird gelöscht. Daten bei Garmin, Health Connect und im Kalender bleiben erhalten.").setPositiveButton("Löschen") { _,_ -> getSharedPreferences("sleepsync_history",MODE_PRIVATE).edit().clear().apply(); sleepHistory=emptyList(); showSettings() }.setNegativeButton("Abbrechen",null).show()
            }
            .setNegativeButton("Schließen",null)
            .show()
    }

    private fun showAboutSettings() {
        val info=packageManager.getPackageInfo(packageName,0)
        val versionName=info.versionName ?: "–"
        val versionCode=info.longVersionCode
        val installed=runCatching { DateTimeFormatter.ofPattern("dd.MM.yyyy · HH:mm").withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(info.lastUpdateTime)) }.getOrDefault("–")
        AlertDialog.Builder(this)
            .setTitle("Über SleepSync")
            .setMessage("SleepSync\nDein Schlaf. Klar, automatisch, im Kalender.\n\nEntwickelt von Riccardo Hoff\n© 2026\n\nVersion: $versionName\nBuild: $versionCode\nPaket: $packageName\nInstallierter Build: $installed\n\nGarmin → Health Connect → SleepSync → Kalender\n\nSleepSync ist ein unabhängiges Projekt und steht in keiner offiziellen Verbindung zu Garmin.")
            .setPositiveButton("Schließen",null)
            .setNeutralButton("App-Signatur") { _,_ -> showAppSignature() }
            .show()
    }

    private fun showAppearanceSettings() {
        val d=resources.displayMetrics.density; fun dp(v:Int)=(v*d).toInt()
        pageTitle.text="Wähle dein Design"; pageSubtitle.text="SleepSync so, wie du es magst"
        actionsTitle.visibility=View.GONE; actionsBox.visibility=View.GONE; sleepCard.removeAllViews()
        sleepCard.addView(TextView(this).apply { text="‹  Zurück zu Einstellungen"; textSize=12f; setTextColor(accent2); setPadding(dp(2),dp(8),0,dp(18)); setOnClickListener { showSettings() } })
        val prefs=getSharedPreferences("sleepsync_ui",MODE_PRIVATE); val current=prefs.getString("theme","dark") ?: "dark"
        val row=LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL }
        fun choice(key:String,title:String,sub:String,icon:String,bg:Int):MaterialCardView = MaterialCardView(this).apply {
            radius=dp(22).toFloat(); strokeWidth=dp(if(current==key) 2 else 1); strokeColor=if(current==key) accent else Color.rgb(65,72,104)
            setCardBackgroundColor(bg); isClickable=true; isFocusable=true
            layoutParams=LinearLayout.LayoutParams(0,dp(210),1f).apply { setMargins(dp(4),0,dp(4),0) }
            addView(LinearLayout(this@MainActivity).apply { orientation=LinearLayout.VERTICAL; gravity=android.view.Gravity.CENTER; setPadding(dp(8),dp(16),dp(8),dp(12))
                addView(TextView(this@MainActivity).apply { text=icon; textSize=42f; gravity=android.view.Gravity.CENTER })
                addView(TextView(this@MainActivity).apply { text=title; textSize=14f; setTypeface(typeface,Typeface.BOLD); setTextColor(if(key=="light") Color.rgb(25,28,42) else Color.WHITE); gravity=android.view.Gravity.CENTER; setPadding(0,dp(12),0,dp(5)) })
                addView(TextView(this@MainActivity).apply { text=sub; textSize=10f; setTextColor(if(key=="light") Color.rgb(80,85,105) else Color.rgb(165,175,205)); gravity=android.view.Gravity.CENTER })
                if(current==key) addView(TextView(this@MainActivity).apply { text="✓ AKTIV"; textSize=10f; setTextColor(accent); setTypeface(typeface,Typeface.BOLD); gravity=android.view.Gravity.CENTER; setPadding(0,dp(12),0,0) })
            })
            setOnClickListener { prefs.edit().putString("theme",key).apply(); recreate() }
        }
        row.addView(choice("dark","Dunkel","OLED Night","🌙",Color.rgb(10,15,34)))
        row.addView(choice("light","Hell","Neon Sunrise","☀️",Color.rgb(239,242,250)))
        row.addView(choice("system","Automatisch","System","◐",Color.rgb(24,27,45)))
        sleepCard.addView(row)
        sleepCard.addView(TextView(this).apply { text="Deine Auswahl gilt für die gesamte App."; textSize=11f; setTextColor(Color.rgb(155,165,195)); setPadding(dp(8),dp(18),dp(8),0) })
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

    private fun metricCard(icon: String, label: String, value: String, onClick: (() -> Unit)? = null, series: List<MetricPoint> = emptyList(), sleep: SleepSummary? = null): MaterialCardView {
        val d=resources.displayMetrics.density; fun dp(v:Int)=(v*d).toInt()
        val tone=when(label){"Leicht"->stageLight;"Tief"->stageDeep;"REM"->stageRem;"Wach"->stageAwake;"Puls"->Color.rgb(255,82,126);"SpO₂"->Color.rgb(44,205,255);"Atmung"->Color.rgb(80,225,184);"HRV"->Color.rgb(213,96,255);else->accent}
        val fill=when(label){"Leicht"->Color.rgb(10,32,48);"Tief"->Color.rgb(22,20,56);"REM"->Color.rgb(42,18,58);"Wach"->Color.rgb(54,31,16);"Puls"->Color.rgb(54,18,31);"SpO₂"->Color.rgb(9,37,49);"Atmung"->Color.rgb(10,42,34);"HRV"->Color.rgb(45,17,55);else->Color.rgb(15,18,38)}
        val theme=getSharedPreferences("sleepsync_ui",MODE_PRIVATE).getString("theme","dark")?:"dark"
        val sysDark=(resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK)==android.content.res.Configuration.UI_MODE_NIGHT_YES
        val light=theme=="light" || (theme=="system" && !sysDark)
        val valueText=TextView(this).apply{text=value;textSize=19f;setTextColor(if(light) Color.rgb(22,27,45) else Color.WHITE);setTypeface(typeface,Typeface.BOLD);setPadding(0,dp(7),0,dp(3))}
        val body=LinearLayout(this).apply{
            orientation=LinearLayout.VERTICAL;setPadding(dp(15),dp(14),dp(15),dp(12))
            addView(TextView(this@MainActivity).apply{text="$icon   ${label.uppercase()}";textSize=11f;letterSpacing=.08f;setTextColor(tone);setTypeface(typeface,Typeface.BOLD)})
            addView(valueText)
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
        return MaterialCardView(this).apply{
            radius=dp(21).toFloat();cardElevation=dp(2).toFloat();strokeWidth=dp(1);strokeColor=tone;setCardBackgroundColor(if(light) Color.argb(238,246,250,255) else fill)
            layoutParams=GridLayout.LayoutParams().apply{width=0;height=dp(if(label in listOf("Puls","SpO₂","Atmung","HRV")) 164 else 110);columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1f);setMargins(dp(4),dp(4),dp(4),dp(4))}
            addView(body);if(onClick!=null){isClickable=true;isFocusable=true;setOnClickListener{onClick()}}
        }
    }
    private fun showStageTimeline(label:String, tone:Int, minutes:Long, s:SleepSummary) {
        val d=resources.displayMetrics.density; fun dp(v:Int)=(v*d).toInt()
        val theme=getSharedPreferences("sleepsync_ui",MODE_PRIVATE).getString("theme","dark")?:"dark"
        val sysDark=(resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK)==android.content.res.Configuration.UI_MODE_NIGHT_YES
        val light=theme=="light" || (theme=="system" && !sysDark)
        val primary=if(light) Color.rgb(22,27,45) else Color.WHITE
        val secondary=if(light) Color.rgb(78,88,112) else Color.rgb(165,175,205)
        val timeColor=if(light) Color.rgb(91,104,132) else Color.rgb(135,147,180)
        pageTitle.text="Schlafphasen"; pageSubtitle.text=label+" · Verlauf dieser Nacht"
        actionsTitle.visibility=View.GONE; actionsBox.visibility=View.GONE; sleepCard.removeAllViews()
        sleepCard.addView(TextView(this).apply { text="‹  Zurück zur Übersicht"; textSize=12f; setTextColor(tone); setPadding(dp(2),dp(8),0,dp(14)); setOnClickListener { showOverview() } })
        val intervals=s.stageSeries.filter { it.stageLabel==label }
        sleepCard.addView(MaterialCardView(this).apply {
            radius=dp(24).toFloat(); strokeWidth=dp(1); strokeColor=tone; setCardBackgroundColor(if(light) Color.argb(238,246,250,255) else Color.argb(190,9,15,31))
            addView(LinearLayout(this@MainActivity).apply { orientation=LinearLayout.VERTICAL; setPadding(dp(18),dp(18),dp(18),dp(18))
                addView(TextView(this@MainActivity).apply { text=label.uppercase(); textSize=12f; setTextColor(tone); setTypeface(typeface,Typeface.BOLD) })
                addView(TextView(this@MainActivity).apply { text=(minutes/60).toString()+" h "+(minutes%60).toString()+" min"; textSize=31f; setTextColor(primary); setTypeface(typeface,Typeface.BOLD); setPadding(0,dp(8),0,dp(4)) })
                val pct=((minutes*100f)/s.totalMin.coerceAtLeast(1)).toInt()
                addView(TextView(this@MainActivity).apply { text=pct.toString()+" % der Nacht · "+intervals.size+" Abschnitte"; textSize=11f; setTextColor(secondary); setPadding(0,0,0,dp(14)) })
                addView(LinearLayout(this@MainActivity).apply { orientation=LinearLayout.HORIZONTAL
                    val duration=(s.endMs-s.startMs).coerceAtLeast(1); var cursor=s.startMs
                    fun seg(ms:Long,active:Boolean)=View(this@MainActivity).apply { background=GradientDrawable().apply { cornerRadius=dp(5).toFloat(); setColor(if(active) tone else if(light) Color.argb(72,120,135,165) else Color.argb(38,120,130,160)) }; layoutParams=LinearLayout.LayoutParams(0,dp(if(active) 54 else 18),(ms.toFloat()/duration).coerceAtLeast(.001f)).apply { gravity=android.view.Gravity.CENTER_VERTICAL; setMargins(dp(1),0,dp(1),0) } }
                    intervals.sortedBy { it.startMs }.forEach { st -> if(st.startMs>cursor) addView(seg(st.startMs-cursor,false)); addView(seg(st.endMs-st.startMs,true)); cursor=st.endMs }; if(cursor<s.endMs) addView(seg(s.endMs-cursor,false))
                })
                val tf=DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())
                addView(LinearLayout(this@MainActivity).apply { orientation=LinearLayout.HORIZONTAL; gravity=android.view.Gravity.CENTER_VERTICAL; setPadding(0,dp(9),0,0)
                    addView(TextView(this@MainActivity).apply { text=tf.format(Instant.ofEpochMilli(s.startMs)); textSize=10f; setTextColor(timeColor); layoutParams=LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,1f) })
                    addView(TextView(this@MainActivity).apply { text=tf.format(Instant.ofEpochMilli(s.startMs+(s.endMs-s.startMs)/2)); textSize=10f; gravity=android.view.Gravity.CENTER; setTextColor(timeColor); layoutParams=LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,1f) })
                    addView(TextView(this@MainActivity).apply { text=tf.format(Instant.ofEpochMilli(s.endMs)); textSize=10f; gravity=android.view.Gravity.END; setTextColor(timeColor); layoutParams=LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,1f) })
                })
            })
        })
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
            text="‹  Zurück zur Übersicht"; textSize=12f; setTextColor(tone); setPadding(px(2),px(8),0,px(12))
            setOnClickListener { showOverview() }
        })
        fun chartCard(name:String, glyph:String, color:Int, value:String, points:List<MetricPoint>) =
            MaterialCardView(this).apply {
                radius=px(24).toFloat(); strokeWidth=px(1); strokeColor=Color.argb(180,Color.red(color),Color.green(color),Color.blue(color))
                setCardBackgroundColor(if(light) Color.argb(224,244,248,255) else Color.argb(188,9,15,31)); cardElevation=px(3).toFloat()
                layoutParams=LinearLayout.LayoutParams(-1,-2).apply { setMargins(0,0,0,px(14)) }
                addView(LinearLayout(this@MainActivity).apply {
                    orientation=LinearLayout.VERTICAL; setPadding(px(16),px(15),px(16),px(12))
                    addView(LinearLayout(this@MainActivity).apply {
                        orientation=LinearLayout.HORIZONTAL; gravity=android.view.Gravity.CENTER_VERTICAL
                        addView(TextView(this@MainActivity).apply { text=glyph+"  "+name.uppercase(); textSize=12f; letterSpacing=.08f; setTextColor(color); setTypeface(typeface,Typeface.BOLD); layoutParams=LinearLayout.LayoutParams(0,-2,1f) })
                        addView(TextView(this@MainActivity).apply { text=value; textSize=22f; setTextColor(if(light) Color.rgb(20,31,62) else Color.WHITE); setTypeface(typeface,Typeface.BOLD); setShadowLayer(px(7).toFloat(),0f,0f,Color.argb(75,Color.red(color),Color.green(color),Color.blue(color))) })
                    })
                    if(points.isNotEmpty()) {
                        addView(TextView(this@MainActivity).apply {
                            text="●  "+points.size.toString()+" Messpunkte  ·  Garmin"
                            textSize=9f; setTextColor(Color.argb(175,Color.red(color),Color.green(color),Color.blue(color))); setPadding(0,px(4),0,0)
                        })
                        val min=points.minOf { it.value }; val max=points.maxOf { it.value }
                        fun fv(v:Double)=if(kotlin.math.abs(v-kotlin.math.round(v))<0.05) kotlin.math.round(v).toInt().toString() else String.format(java.util.Locale.GERMANY,"%.1f",v)
                        val unit=when(name) { "Puls"->"bpm"; "SpO₂"->"%"; "Atmung"->"/min"; "HRV"->"ms"; else->"" }
                        addView(TextView(this@MainActivity).apply {
                            text="MIN  "+fv(min)+" "+unit+"     •     MAX  "+fv(max)+" "+unit
                            textSize=10f; letterSpacing=.05f; setTextColor(if(light) Color.rgb(88,101,132) else Color.rgb(154,164,191)); setPadding(0,px(7),0,px(2))
                        })
                    }
                    addView(SleepMetricChartView(this@MainActivity,color,name,s.startMs,s.endMs,points))
                })
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
            textSize=10f; letterSpacing=.08f; setTextColor(if(light) Color.rgb(75,91,126) else Color.rgb(112,122,153)); setPadding(px(2),0,0,px(10))
        })
        cards.forEach { sleepCard.addView(it) }
        cards[listOf("Puls","SpO₂","Atmung","HRV").indexOf(label).coerceAtLeast(0)].post { cards[listOf("Puls","SpO₂","Atmung","HRV").indexOf(label).coerceAtLeast(0)].requestFocus() }
    }

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
        fun glass(vararg rgb:Int)=if(light) Color.argb(224,246,250,255) else Color.rgb(rgb[0],rgb[1],rgb[2])
        val historical=viewingHistoryNight
        sleepCard.addView(LinearLayout(this).apply {
            orientation=LinearLayout.HORIZONTAL; gravity=android.view.Gravity.CENTER_VERTICAL; setPadding(dp(2),0,dp(2),dp(8))
            addView(TextView(this@MainActivity).apply {
                text=if(historical) "HISTORISCHE NACHT" else "LETZTE NACHT"; textSize=11f; letterSpacing=.16f; setTextColor(accent2); setTypeface(typeface,Typeface.BOLD)
                layoutParams=LinearLayout.LayoutParams(0,-2,1f)
            })
            addView(TextView(this@MainActivity).apply {
                text="${tf.format(java.time.Instant.ofEpochMilli(s.startMs))} – ${tf.format(java.time.Instant.ofEpochMilli(s.endMs))}"; textSize=12f; setTextColor(if(light) Color.rgb(91,105,139) else Color.rgb(151,158,190))
            })
        })
        val quality = ((s.lightMin + s.deepMin + s.remMin) * 100 / s.totalMin.coerceAtLeast(1)).toInt().coerceIn(0,100)
        sleepCard.addView(LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = android.view.Gravity.CENTER_VERTICAL; setPadding(dp(18),dp(19),dp(18),dp(19)); elevation=dp(8).toFloat()
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR, if(light) intArrayOf(Color.rgb(248,245,255),Color.rgb(229,241,255),Color.rgb(221,250,249)) else intArrayOf(Color.rgb(58,25,105),Color.rgb(24,25,72),Color.rgb(6,55,66))).apply { cornerRadius=dp(30).toFloat(); setStroke(dp(1),Color.rgb(107,82,190)) }
            addView(LinearLayout(this@MainActivity).apply {
                orientation=LinearLayout.VERTICAL; layoutParams=LinearLayout.LayoutParams(0,-2,1f)
                addView(LinearLayout(this@MainActivity).apply { orientation=LinearLayout.HORIZONTAL; gravity=android.view.Gravity.CENTER_VERTICAL
                    addView(View(this@MainActivity).apply { background=GradientDrawable().apply { shape=GradientDrawable.OVAL; setColor(accent2) }; layoutParams=LinearLayout.LayoutParams(dp(7),dp(7)).apply { marginEnd=dp(7) } })
                    addView(TextView(this@MainActivity).apply { text="GESAMTSCHLAF"; textSize=10f; letterSpacing=.14f; setTextColor(if(light) Color.rgb(88,83,145) else Color.rgb(184,174,224)); setTypeface(typeface,Typeface.BOLD) })
                })
                addView(TextView(this@MainActivity).apply { text=fmt(s.totalMin); textSize=42f; setTextColor(if(light) Color.rgb(17,31,66) else Color.WHITE); setTypeface(typeface,Typeface.BOLD); setPadding(0,dp(2),0,0) })
                addView(TextView(this@MainActivity).apply { text="☾  Schlafzeit"; textSize=12f; setTextColor(if(light) Color.rgb(74,118,148) else Color.rgb(151,210,225)); setPadding(0,dp(2),0,0) })
            })
            addView(TextView(this@MainActivity).apply {
                text = "$quality%\nEFFIZIENZ"; gravity = android.view.Gravity.CENTER; textSize = 12f; setTypeface(typeface, Typeface.BOLD)
                setTextColor(Color.WHITE); setPadding(dp(14),dp(12),dp(14),dp(12))
                background = GradientDrawable(GradientDrawable.Orientation.TL_BR,intArrayOf(Color.rgb(22,94,120),Color.rgb(77,45,145))).apply { cornerRadius=dp(22).toFloat(); setStroke(dp(1),Color.rgb(83,205,229)) }
            })
        })
        sleepCard.addView(TextView(this).apply { text="SCHLAFVERLAUF"; textSize=11f; letterSpacing=.14f; setTextColor(stageLight); setTypeface(typeface,Typeface.BOLD); setPadding(dp(4),dp(18),0,dp(8)) })
        sleepCard.addView(sleepStageStrip(s))
        sleepCard.addView(LinearLayout(this).apply {
            orientation=LinearLayout.HORIZONTAL; setPadding(dp(2),dp(5),dp(2),0)
            addView(TextView(this@MainActivity).apply { text="☾  "+tf.format(java.time.Instant.ofEpochMilli(s.startMs)); textSize=10f; setTextColor(Color.rgb(118,128,161)); layoutParams=LinearLayout.LayoutParams(0,-2,1f) })
            addView(TextView(this@MainActivity).apply { text=tf.format(java.time.Instant.ofEpochMilli(s.endMs))+"  ☀"; textSize=10f; setTextColor(Color.rgb(118,128,161)) })
        })
        sleepCard.addView(LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = android.view.Gravity.CENTER_VERTICAL; setPadding(0,dp(9),0,dp(4))
            fun legend(name: String, tone: Int) = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.HORIZONTAL; gravity = android.view.Gravity.CENTER_VERTICAL
                addView(View(this@MainActivity).apply {
                    background = GradientDrawable().apply { shape=GradientDrawable.OVAL; setColor(tone) }
                    layoutParams=LinearLayout.LayoutParams(dp(7),dp(7)).apply { setMargins(0,0,dp(5),0) }
                })
                addView(TextView(this@MainActivity).apply { text=name; textSize=11f; setTextColor(if(light) Color.rgb(82,95,128) else Color.rgb(185,190,215)) })
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
            radius=dp(20).toFloat(); strokeWidth=dp(1); strokeColor=Color.rgb(116,91,207); setCardBackgroundColor(if(light) Color.argb(232,244,242,255) else Color.rgb(19,15,39))
            addView(LinearLayout(this@MainActivity).apply {
                orientation=LinearLayout.HORIZONTAL; gravity=android.view.Gravity.CENTER_VERTICAL; setPadding(dp(14),dp(12),dp(14),dp(12))
                addView(TextView(this@MainActivity).apply { text="SCHLAF-\nARCHITEKTUR"; textSize=10f; letterSpacing=.10f; setTextColor(if(light) Color.rgb(88,74,150) else Color.rgb(171,155,220)); setTypeface(typeface,Typeface.BOLD); layoutParams=LinearLayout.LayoutParams(0,-2,1f) })
                addView(TextView(this@MainActivity).apply { text="$deepPct%\nTIEF"; gravity=android.view.Gravity.CENTER; textSize=13f; setTextColor(stageDeep); setTypeface(typeface,Typeface.BOLD); layoutParams=LinearLayout.LayoutParams(dp(62),-2) })
                addView(TextView(this@MainActivity).apply { text="$remPct%\nREM"; gravity=android.view.Gravity.CENTER; textSize=13f; setTextColor(stageRem); setTypeface(typeface,Typeface.BOLD); layoutParams=LinearLayout.LayoutParams(dp(62),-2) })
            })
            layoutParams=LinearLayout.LayoutParams(-1,-2).apply { setMargins(0,dp(7),0,dp(7)) }
        })
        sleepCard.addView(LinearLayout(this).apply {
            orientation=LinearLayout.HORIZONTAL; gravity=android.view.Gravity.CENTER_VERTICAL; setPadding(dp(4),dp(10),0,dp(2))
            addView(TextView(this@MainActivity).apply { text="SCHLAFPHASEN"; textSize=11f; letterSpacing=.14f; setTextColor(stageRem); setTypeface(typeface,Typeface.BOLD); layoutParams=LinearLayout.LayoutParams(0,-2,1f) })
            addView(TextView(this@MainActivity).apply { text=fmt(s.lightMin+s.deepMin+s.remMin); textSize=10f; setTypeface(typeface,Typeface.BOLD); setTextColor(stageRem); setPadding(dp(10),dp(4),dp(10),dp(4)); background=GradientDrawable().apply { cornerRadius=dp(13).toFloat(); setColor(if(light) Color.argb(220,240,230,255) else Color.rgb(35,19,54)); setStroke(dp(1),Color.rgb(86,48,119)) } })
        })
        sleepCard.addView(LinearLayout(this).apply {
            orientation=LinearLayout.HORIZONTAL; setPadding(dp(4),dp(2),dp(4),dp(3))
            fun phase(label:String,minutes:Long,tone:Int)=TextView(this@MainActivity).apply {
                text="$label  ${(minutes*100/s.totalMin.coerceAtLeast(1)).toInt()}%"; textSize=10f; setTextColor(tone); setTypeface(typeface,Typeface.BOLD)
                gravity=android.view.Gravity.CENTER; layoutParams=LinearLayout.LayoutParams(0,-2,1f)
            }
            addView(phase("LEICHT",s.lightMin,stageLight)); addView(phase("TIEF",s.deepMin,stageDeep)); addView(phase("REM",s.remMin,stageRem)); addView(phase("WACH",s.awakeMin,stageAwake))
        })
        val stages = GridLayout(this).apply {
            columnCount = 2
            setPadding(0, dp(6), 0, dp(8))
            addView(metricCard("🌙","Leicht",fmt(s.lightMin), onClick={ showStageTimeline("Leicht", stageLight, s.lightMin, s) }, sleep=s))
            addView(metricCard("🌑","Tief",fmt(s.deepMin), onClick={ showStageTimeline("Tief", stageDeep, s.deepMin, s) }, sleep=s))
            addView(metricCard("🧠","REM",fmt(s.remMin), onClick={ showStageTimeline("REM", stageRem, s.remMin, s) }, sleep=s))
            addView(metricCard("👀","Wach",fmt(s.awakeMin), onClick={ showStageTimeline("Wach", stageAwake, s.awakeMin, s) }, sleep=s))
        }
        sleepCard.addView(stages)
        sleepCard.addView(View(this).apply {
            background=GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,intArrayOf(Color.TRANSPARENT,Color.rgb(33,104,122),Color.rgb(84,51,133),Color.TRANSPARENT))
            layoutParams=LinearLayout.LayoutParams(-1,dp(1)).apply { setMargins(dp(18),dp(15),dp(18),dp(3)) }
        })
        sleepCard.addView(LinearLayout(this).apply {
            orientation=LinearLayout.HORIZONTAL; gravity=android.view.Gravity.CENTER_VERTICAL; setPadding(dp(4),dp(8),dp(4),dp(4))
            addView(TextView(this@MainActivity).apply { text="GESUNDHEITSWERTE"; textSize=11f; letterSpacing=.14f; setTextColor(accent2); setTypeface(typeface,Typeface.BOLD); layoutParams=LinearLayout.LayoutParams(0,-2,1f) })
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
            radius=dp(18).toFloat(); cardElevation=0f; strokeWidth=dp(1); strokeColor=Color.rgb(24,94,105); setCardBackgroundColor(if(light) Color.argb(225,226,250,248) else Color.rgb(7,25,31))
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
            radius=dp(24).toFloat(); cardElevation=dp(2).toFloat(); strokeWidth=dp(1); strokeColor=Color.rgb(81,62,137); setCardBackgroundColor(if(light) Color.argb(230,244,239,255) else Color.rgb(19,15,39))
            addView(LinearLayout(this@MainActivity).apply {
                orientation=LinearLayout.HORIZONTAL; gravity=android.view.Gravity.CENTER_VERTICAL; setPadding(dp(15),dp(13),dp(15),dp(13))
                addView(TextView(this@MainActivity).apply {
                    text="✦"; textSize=20f; gravity=android.view.Gravity.CENTER; setTextColor(accent2); setPadding(0,dp(5),0,dp(5))
                    background=GradientDrawable(GradientDrawable.Orientation.TL_BR,intArrayOf(Color.rgb(17,62,78),Color.rgb(55,29,91))).apply { shape=GradientDrawable.OVAL; setStroke(dp(1),Color.rgb(48,151,177)) }
                    layoutParams=LinearLayout.LayoutParams(dp(36),dp(36)).apply { marginEnd=dp(11) }
                })
                addView(TextView(this@MainActivity).apply {
                    layoutParams=LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,1f)
                    text=(if (quality >= 90) "Hohe Schlafeffizienz" else if (quality >= 80) "Solide Schlafeffizienz" else "Schlafeffizienz") + "\n" + "$quality% deiner Bettzeit entfielen auf Schlafphasen."
                    textSize=13f; setTextColor(if(light) Color.rgb(28,39,72) else Color.rgb(220,224,244)); setTypeface(typeface,Typeface.BOLD)
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
