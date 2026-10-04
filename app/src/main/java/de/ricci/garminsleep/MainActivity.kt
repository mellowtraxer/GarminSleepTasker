package de.ricci.garminsleep

import androidx.activity.ComponentActivity
import android.os.Bundle
import android.content.pm.PackageManager
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
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.content.res.ColorStateList
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.*
import kotlinx.coroutines.*

private class NightLandscapeView(context: android.content.Context) : View(context) {
    private val p=Paint(Paint.ANTI_ALIAS_FLAG)
    private val stars=listOf(.08f to .11f,.16f to .19f,.27f to .09f,.39f to .15f,.52f to .07f,.64f to .18f,.77f to .10f,.88f to .16f,.94f to .06f)
    override fun onDraw(c: Canvas) {
        super.onDraw(c); val w=width.toFloat(); val h=height.toFloat(); val d=resources.displayMetrics.density; val sh=minOf(h,760f*d)
        p.shader=android.graphics.LinearGradient(0f,0f,0f,sh*.72f,intArrayOf(Color.rgb(13,8,48),Color.rgb(18,38,82),Color.rgb(5,9,24)),null,android.graphics.Shader.TileMode.CLAMP); c.drawRect(0f,0f,w,h,p); p.shader=null
        p.color=Color.argb(245,228,236,255); stars.forEachIndexed { i,s -> c.drawCircle(w*s.first,sh*s.second,(if(i%3==0)1.7f else 1f)*resources.displayMetrics.density,p) }
        val extraStars=52
        for(i in 0 until extraStars){ val x=((i*73)%101)/101f; val y=.035f+(((i*47)%31)/31f)*.27f; p.color=Color.argb(115+(i%5)*27,190+(i%3)*18,205+(i%2)*25,255); c.drawCircle(w*x,sh*y,(.65f+(i%4)*.22f)*resources.displayMetrics.density,p) }
        p.setShadowLayer(22*d,0f,0f,Color.argb(150,181,126,255)); setLayerType(LAYER_TYPE_SOFTWARE,p)
        p.color=Color.rgb(229,220,255); c.drawCircle(w*.82f,sh*.115f,15*d,p); p.clearShadowLayer()
        p.color=Color.rgb(16,18,57); c.drawCircle(w*.835f,sh*.105f,14*d,p)
        p.shader=android.graphics.RadialGradient(w*.34f,sh*.30f,w*.48f,intArrayOf(Color.argb(82,119,65,255),Color.argb(30,0,209,255),Color.TRANSPARENT),null,android.graphics.Shader.TileMode.CLAMP); c.drawCircle(w*.34f,sh*.30f,w*.48f,p); p.shader=null
        fun ridge(color:Int, base:Float, peaks:FloatArray) { val q=Path(); q.moveTo(0f,h*base); peaks.forEachIndexed { i,v -> q.lineTo(w*i/(peaks.size-1),sh*v) }; q.lineTo(w,sh*.62f); q.lineTo(0f,sh*.62f); q.close(); p.color=color; c.drawPath(q,p) }
        ridge(Color.rgb(30,37,82),.49f,floatArrayOf(.48f,.39f,.44f,.28f,.42f,.32f,.46f,.36f,.49f))
        p.color=Color.argb(80,145,160,220); p.strokeWidth=.8f*d
        c.drawLine(w*.31f,sh*.345f,w*.39f,sh*.28f,p); c.drawLine(w*.39f,sh*.28f,w*.47f,sh*.365f,p)
        c.drawLine(w*.57f,sh*.39f,w*.625f,sh*.32f,p); c.drawLine(w*.625f,sh*.32f,w*.69f,sh*.425f,p)
        ridge(Color.rgb(10,18,43),.56f,floatArrayOf(.55f,.45f,.51f,.39f,.53f,.43f,.57f,.47f,.56f))
        p.shader=android.graphics.LinearGradient(0f,sh*.54f,0f,h,intArrayOf(Color.argb(150,30,51,93),Color.rgb(3,5,13)),null,android.graphics.Shader.TileMode.CLAMP); c.drawRect(0f,sh*.54f,w,h,p); p.shader=null
        p.strokeWidth=1f*d
        for(i in 0..6){ val y=h*(.575f+i*.018f); p.color=Color.argb(28-i*3,115,196,255); c.drawLine(w*.08f,y,w*.92f,y,p) }
        p.shader=android.graphics.RadialGradient(w*.82f,sh*.59f,w*.24f,intArrayOf(Color.argb(54,173,128,255),Color.argb(20,40,190,255),Color.TRANSPARENT),null,android.graphics.Shader.TileMode.CLAMP); c.drawOval(w*.57f,sh*.555f,w*1.05f,sh*.72f,p); p.shader=null
        p.strokeWidth=.7f*d
        for(i in 0..9){ val yy=h*(.59f+i*.011f); val spread=w*(.035f+i*.014f); p.color=Color.argb(48-i*3,178,139,255); c.drawLine(w*.82f-spread,yy,w*.82f+spread,yy,p) }
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
    private var lastSummary: SleepSummary? = null
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
    private val permissionLauncher = registerForActivityResult(PermissionController.createRequestPermissionResultContract()) { refresh() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
            orientation = LinearLayout.HORIZONTAL; gravity = android.view.Gravity.CENTER; setPadding(dp(6),dp(7),dp(6),dp(7))
            val tabs = mutableListOf<MaterialButton>()
            fun activate(active: MaterialButton) = tabs.forEachIndexed { index,b ->
                val on = b === active
                val tone = intArrayOf(accent2,stageRem,stageAwake,accent)[index]
                b.setTextColor(if(on) tone else Color.rgb(120,128,158))
                b.backgroundTintList=ColorStateList.valueOf(if(on) Color.argb(48,Color.red(tone),Color.green(tone),Color.blue(tone)) else Color.TRANSPARENT)
                b.strokeWidth=if(on) dp(1) else 0
                b.strokeColor=ColorStateList.valueOf(tone)
                b.alpha=if(on) 1f else .62f
                b.elevation=if(on) dp(4).toFloat() else 0f
            }
            fun tab(label: String, action: () -> Unit): MaterialButton = MaterialButton(this@MainActivity).apply {
                text=label; isAllCaps=false; textSize=10f; cornerRadius=dp(18); insetTop=0; insetBottom=0; minWidth=0; minimumWidth=0
                layoutParams=LinearLayout.LayoutParams(0,dp(56),1f).apply { setMargins(dp(2),0,dp(2),0) }
                setOnClickListener { activate(this); action() }; tabs.add(this)
            }
            val home=tab("◉\nÜbersicht"){showOverview()}; addView(home)
            addView(tab("▥\nVerlauf"){showHistoryPlaceholder()})
            addView(tab("▦\nKalender"){showCalendarPlaceholder()})
            addView(tab("✦\nSettings"){showSettings()})
            post { activate(home) }
        }
        actionsTitle = TextView(this).apply { text="Verbindungen & Automatik"; textSize=18f; setTypeface(typeface, Typeface.BOLD); setPadding(0,dp(22),0,dp(8)) }
        val grant = button("Health Connect · Berechtigungen") { permissionLauncher.launch(permissions) }
        val link = button("Garmin Connect · Verbinden") { showGarminLogin() }
        val unlink = button("Garmin Connect · Trennen") { garminClient.logout(); refresh() }
        val test = button("Schlafdaten neu laden") { testRead() }
        val sig = button("App-Signatur anzeigen") { showAppSignature() }
        val statusCard = MaterialCardView(this).apply {
            radius=dp(18).toFloat(); cardElevation=dp(2).toFloat(); strokeWidth=dp(1); strokeColor=Color.argb(80,120,176,255)
            setCardBackgroundColor(Color.argb(172,7,17,31))
            status.setPadding(dp(12),dp(7),dp(12),dp(7)); addView(status)
            layoutParams=LinearLayout.LayoutParams(-1,-2).apply { setMargins(0,dp(10),0,dp(12)) }
        }
        actionsBox = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; addView(grant); addView(link); addView(unlink); addView(test); addView(sig) }
        pageTitle.setTextColor(Color.WHITE); pageTitle.textSize=30f; pageTitle.setTypeface(pageTitle.typeface,Typeface.BOLD); pageTitle.letterSpacing=-.02f
        pageTitle.setShadowLayer(18f,0f,0f,Color.argb(120,139,92,246))
        pageTitle.background=GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,intArrayOf(Color.argb(42,139,92,246),Color.argb(18,34,211,238),Color.TRANSPARENT)).apply { cornerRadius=dp(18).toFloat() }
        pageTitle.setPadding(dp(10),dp(5),dp(12),dp(5))
        pageSubtitle.setTextColor(Color.rgb(184,194,224)); pageSubtitle.textSize=13f; pageSubtitle.setShadowLayer(8f,0f,0f,Color.argb(90,40,120,255))
        status.setTextColor(Color.rgb(166,238,244)); status.textSize=10f; status.letterSpacing=.08f; status.setTypeface(status.typeface,Typeface.BOLD)
        actionsTitle.setTextColor(Color.WHITE)
        val brandGlow = View(this).apply {
            background=GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,intArrayOf(accent,accent2,stageRem,Color.TRANSPARENT)).apply { cornerRadius=dp(2).toFloat() }
            elevation=dp(3).toFloat()
            layoutParams=LinearLayout.LayoutParams(dp(118),dp(3)).apply { setMargins(0,dp(8),0,dp(2)) }
        }
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(20),dp(26),dp(20),dp(24))
            addView(pageTitle); addView(pageSubtitle); addView(brandGlow); addView(statusCard); addView(sleepShell); addView(actionsTitle); addView(actionsBox)
        }
        val savedTheme=getSharedPreferences("sleepsync_ui",MODE_PRIVATE).getString("theme","dark") ?: "dark"
        val systemDark=(resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK)==android.content.res.Configuration.UI_MODE_NIGHT_YES
        val useLight=savedTheme=="light" || (savedTheme=="system" && !systemDark)
        val nightAtmosphere = if(useLight) LayerDrawable(arrayOf(
            GradientDrawable(GradientDrawable.Orientation.TL_BR,intArrayOf(Color.rgb(248,250,255),Color.rgb(226,235,255),Color.rgb(242,246,255))),
            GradientDrawable(GradientDrawable.Orientation.TR_BL,intArrayOf(Color.argb(75,160,130,255),Color.TRANSPARENT,Color.argb(45,50,210,255)))
        )) else LayerDrawable(arrayOf(
            GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(Color.rgb(20,10,58), Color.rgb(8,30,68), Color.rgb(5,6,14), Color.rgb(2,3,9))),
            GradientDrawable(GradientDrawable.Orientation.TR_BL, intArrayOf(Color.argb(105,121,64,255), Color.TRANSPARENT, Color.argb(70,0,214,255)))
        ))
        val scene = android.widget.FrameLayout(this).apply {
            if(!useLight) addView(NightLandscapeView(this@MainActivity), android.widget.FrameLayout.LayoutParams(-1,-1))
            addView(View(this@MainActivity).apply {
                background=if(useLight) ColorDrawable(Color.argb(18,255,255,255)) else GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,intArrayOf(Color.TRANSPARENT,Color.argb(35,2,5,15),Color.argb(150,2,4,12)))
            }, android.widget.FrameLayout.LayoutParams(-1,-1))
            addView(box, android.widget.FrameLayout.LayoutParams(-1,-2))
        }
        val scroll = ScrollView(this).apply {
            isFillViewport=true; clipToPadding=false; background=nightAtmosphere; addView(scene)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,0,1f)
        }
        val navShell = MaterialCardView(this).apply {
            radius=dp(32).toFloat(); cardElevation=dp(14).toFloat(); strokeWidth=dp(1); strokeColor=Color.argb(125,118,105,210)
            setCardBackgroundColor(Color.argb(202,7,8,19))
            foreground=GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,intArrayOf(Color.argb(20,34,211,238),Color.TRANSPARENT,Color.argb(24,183,99,255))).apply { cornerRadius=dp(32).toFloat() }
            addView(nav)
            layoutParams=LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,dp(78)).apply { setMargins(dp(14),dp(4),dp(14),dp(10)) }
        }
        val root = LinearLayout(this).apply {
            orientation=LinearLayout.VERTICAL; background=if(useLight) ColorDrawable(Color.rgb(238,243,255)) else GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,intArrayOf(Color.rgb(8,12,31),nightBg)); addView(scroll); addView(navShell)
        }
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val bars=insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            v.setPadding(0,bars.top,0,bars.bottom); insets
        }
        setContentView(root)
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

    private fun testRead() = launch {
        status.text = "Lese Garmin-Schlaf…"
        try {
            val s = withContext(Dispatchers.IO) { SleepReader(this@MainActivity).latestGarminSleep() }
            renderDashboard(s)
            refresh()
        } catch (t: Throwable) {
            sleepCard.removeAllViews()
            sleepCard.addView(TextView(this@MainActivity).apply { text = "⚠️ Schlafdaten konnten nicht geladen werden\n${t.message.orEmpty()}"; textSize = 16f })
        }
    }

    private fun showOverview() {
        actionsTitle.visibility = View.GONE
        actionsBox.visibility = View.GONE
        pageTitle.text = "SleepSync"
        pageSubtitle.text = "Guten Morgen  ·  Deine letzte Nacht"
        lastSummary?.let { renderDashboard(it) }
    }

    private fun showHistoryPlaceholder() {
        val d=resources.displayMetrics.density; fun dp(v:Int)=(v*d).toInt()
        actionsTitle.visibility=View.GONE; actionsBox.visibility=View.GONE
        pageTitle.text="Verlauf"; pageSubtitle.text="Deine Nächte im Vergleich"
        sleepCard.removeAllViews(); sleepCard.background=null
        fun night(day:String,duration:String,time:String,segments:IntArray)=MaterialCardView(this).apply {
            radius=dp(20).toFloat(); strokeWidth=dp(1); strokeColor=Color.rgb(40,48,78); setCardBackgroundColor(Color.rgb(12,16,31))
            addView(LinearLayout(this@MainActivity).apply {
                orientation=LinearLayout.VERTICAL; setPadding(dp(14),dp(12),dp(14),dp(12))
                addView(LinearLayout(this@MainActivity).apply {
                    orientation=LinearLayout.HORIZONTAL
                    addView(TextView(this@MainActivity).apply { text=day; textSize=14f; setTextColor(Color.rgb(214,218,238)); layoutParams=LinearLayout.LayoutParams(0,-2,1f) })
                    addView(TextView(this@MainActivity).apply { text=duration; textSize=16f; setTextColor(Color.WHITE); setTypeface(typeface,Typeface.BOLD) })
                })
                addView(TextView(this@MainActivity).apply { text=time; textSize=11f; setTextColor(Color.rgb(126,135,167)); setPadding(0,dp(2),0,dp(8)) })
                addView(LinearLayout(this@MainActivity).apply {
                    orientation=LinearLayout.HORIZONTAL
                    val tones=intArrayOf(stageLight,stageDeep,stageRem,stageAwake)
                    segments.forEachIndexed { i,w -> addView(View(this@MainActivity).apply { background=GradientDrawable().apply { cornerRadius=dp(3).toFloat(); setColor(tones[i%4]) }; layoutParams=LinearLayout.LayoutParams(0,dp(8),w.toFloat()).apply { setMargins(0,0,dp(2),0) } }) }
                })
            })
            layoutParams=LinearLayout.LayoutParams(-1,-2).apply { setMargins(0,0,0,dp(9)) }
        }
        sleepCard.addView(TextView(this).apply { text="LETZTE NÄCHTE"; textSize=11f; letterSpacing=.14f; setTextColor(accent2); setTypeface(typeface,Typeface.BOLD); setPadding(0,0,0,dp(10)) })
        sleepCard.addView(night("Heute · So, 4. Okt.","8 h 50 min","00:29 – 09:19",intArrayOf(53,18,20,9)))
        sleepCard.addView(night("Gestern · Sa, 3. Okt.","7 h 42 min","23:58 – 07:40",intArrayOf(48,22,23,7)))
        sleepCard.addView(night("Fr, 2. Okt.","8 h 11 min","00:12 – 08:23",intArrayOf(51,19,21,9)))
        sleepCard.addView(night("Do, 1. Okt.","6 h 58 min","01:03 – 07:58",intArrayOf(56,16,19,9)))
    }
    private fun showCalendarPlaceholder() {
        val d=resources.displayMetrics.density; fun dp(v:Int)=(v*d).toInt()
        actionsTitle.visibility=View.GONE; actionsBox.visibility=View.GONE
        pageTitle.text="Kalendereintrag"; pageSubtitle.text="So landet deine Nacht im Kalender"
        sleepCard.removeAllViews(); sleepCard.background=null
        sleepCard.addView(MaterialCardView(this).apply {
            radius=dp(24).toFloat(); strokeWidth=dp(1); strokeColor=Color.rgb(106,70,220); setCardBackgroundColor(Color.rgb(24,17,48))
            addView(LinearLayout(this@MainActivity).apply {
                orientation=LinearLayout.VERTICAL; setPadding(dp(18),dp(17),dp(18),dp(17))
                addView(TextView(this@MainActivity).apply { text="🗓  GARMIN SCHLAF  💤"; textSize=13f; setTextColor(stageRem); setTypeface(typeface,Typeface.BOLD) })
                addView(TextView(this@MainActivity).apply { text="00:29 – 09:19 Uhr"; textSize=14f; setTextColor(Color.rgb(174,180,207)); setPadding(0,dp(5),0,dp(14)) })
                addView(TextView(this@MainActivity).apply { text="🌙  Gesamt   8 h 50 min\n🌙  Leicht     4 h 40 min\n🌑  Tief          1 h 38 min\n🧠  REM         1 h 46 min\n👀  Wach        0 h 46 min"; textSize=15f; setTextColor(Color.WHITE); setPadding(0,0,0,dp(12)) })
                addView(TextView(this@MainActivity).apply { text="❤️  Ø Puls 71,4 bpm     🩸 SpO₂ 98,0 %\n🫁  Atmung 15,0/min    💓 HRV 33,0 ms"; textSize=13f; setTextColor(Color.rgb(205,190,235)) })
            })
        })
        sleepCard.addView(MaterialCardView(this).apply {
            radius=dp(22).toFloat(); strokeWidth=dp(1); strokeColor=Color.rgb(33,104,126); setCardBackgroundColor(Color.rgb(8,31,42))
            addView(LinearLayout(this@MainActivity).apply {
                orientation=LinearLayout.HORIZONTAL; gravity=android.view.Gravity.CENTER_VERTICAL; setPadding(dp(16),dp(14),dp(16),dp(14))
                addView(TextView(this@MainActivity).apply { text="⚡"; textSize=24f; layoutParams=LinearLayout.LayoutParams(dp(42),-2) })
                addView(TextView(this@MainActivity).apply { text="AUTOMATISCH EINTRAGEN\nAktiv · Garmin Schlaf"; textSize=13f; setTextColor(accent2); setTypeface(typeface,Typeface.BOLD); layoutParams=LinearLayout.LayoutParams(0,-2,1f) })
                addView(TextView(this@MainActivity).apply { text="●"; textSize=25f; setTextColor(Color.rgb(86,230,166)) })
            })
            layoutParams=LinearLayout.LayoutParams(-1,-2).apply { setMargins(0,dp(12),0,0) }
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
        val settingsGrid = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(0,dp(8),0,0) }
        val prefs=getSharedPreferences("sleepsync_ui",MODE_PRIVATE)
        val selectedTheme=prefs.getString("theme","dark") ?: "dark"
        val selectedThemeLabel=when(selectedTheme) { "light"->"Hell"; "system"->"Automatisch"; else->"OLED Night" }
        val sysDark=(resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK)==android.content.res.Configuration.UI_MODE_NIGHT_YES
        val settingsLight=selectedTheme=="light" || (selectedTheme=="system" && !sysDark)
        fun setting(icon:String, title:String, sub:String, color:Int, onClick:(() -> Unit)?=null) {
            val fill = if(settingsLight) Color.rgb(248,250,255) else Color.rgb((Color.red(color)*0.14f).toInt()+8,(Color.green(color)*0.14f).toInt()+8,(Color.blue(color)*0.14f).toInt()+12)
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
        setting("⌚","Garmin Connect","Verbunden · Schlafdaten synchronisieren",accent2)
        setting("♥","Health Connect","Berechtigungen & Gesundheitsdaten",stageRem)
        setting("⚡","Automatik","Tasker & Kalender",stageAwake)
        setting("✦","Darstellung","$selectedThemeLabel · SleepSync",accent) { showAppearanceSettings() }
        setting("◈","Datenschutz","Lokale Daten & Diagnose",stageLight)
        sleepCard.addView(settingsGrid)
        actionsTitle.text="WERKZEUGE"; actionsTitle.setTextColor(stageAwake); actionsTitle.textSize=11f; actionsTitle.letterSpacing=.14f
        listOf(0,1,2,3,4).forEach { i ->
            val b=actionsBox.getChildAt(i) as? MaterialButton ?: return@forEach
            b.cornerRadius=dp(18); b.setTextColor(Color.rgb(220,224,244))
            b.backgroundTintList=ColorStateList.valueOf(Color.rgb(14,17,34)); b.strokeWidth=dp(1); b.strokeColor=ColorStateList.valueOf(Color.rgb(48,55,89))
        }
    }

    private fun showAppearanceSettings() {
        val d=resources.displayMetrics.density; fun dp(v:Int)=(v*d).toInt()
        pageTitle.text="Wähle dein Design"; pageSubtitle.text="SleepSync so, wie du es magst"
        actionsTitle.visibility=View.GONE; actionsBox.visibility=View.GONE; sleepCard.removeAllViews()
        sleepCard.addView(TextView(this).apply { text="‹  Zurück zu Einstellungen"; textSize=12f; setTextColor(accent); setPadding(dp(2),dp(8),0,dp(18)); setOnClickListener { showSettings() } })
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
        row.addView(choice("light","Hell","Weiß","☀️",Color.rgb(239,242,250)))
        row.addView(choice("system","Automatisch","System","◐",Color.rgb(24,27,45)))
        sleepCard.addView(row)
        sleepCard.addView(TextView(this).apply { text="Die Auswahl wird gespeichert. Die vollständige Theme-Umschaltung wird als nächster Schritt auf alle SleepSync-Flächen angewendet."; textSize=11f; setTextColor(Color.rgb(155,165,195)); setPadding(dp(8),dp(18),dp(8),0) })
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

    private fun metricCard(icon: String, label: String, value: String, onClick: (() -> Unit)? = null): MaterialCardView {
        val d = resources.displayMetrics.density
        fun dp(v: Int) = (v * d).toInt()
        val tone = when (label) {
            "Leicht" -> stageLight; "Tief" -> stageDeep; "REM" -> stageRem; "Wach" -> stageAwake
            "Puls" -> Color.rgb(255,82,126); "SpO₂" -> Color.rgb(44,205,255)
            "Atmung" -> Color.rgb(80,225,184); "HRV" -> Color.rgb(213,96,255)
            else -> accent
        }
        val fill = when (label) {
            "Leicht" -> Color.rgb(10,32,48); "Tief" -> Color.rgb(22,20,56); "REM" -> Color.rgb(42,18,58); "Wach" -> Color.rgb(54,31,16)
            "Puls" -> Color.rgb(54,18,31); "SpO₂" -> Color.rgb(9,37,49)
            "Atmung" -> Color.rgb(10,42,34); "HRV" -> Color.rgb(45,17,55)
            else -> Color.rgb(15,18,38)
        }
        val theme=getSharedPreferences("sleepsync_ui",MODE_PRIVATE).getString("theme","dark") ?: "dark"
        val sysDark=(resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK)==android.content.res.Configuration.UI_MODE_NIGHT_YES
        val light=theme=="light" || (theme=="system" && !sysDark)
        val cardFill=if(light) Color.rgb(248,250,255) else fill
        val valueColor=if(light) Color.rgb(22,27,45) else Color.WHITE
        val body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(15), dp(14), dp(15), dp(14))
            addView(TextView(this@MainActivity).apply {
                text = "$icon   ${label.uppercase()}"; textSize = 11f; letterSpacing = .08f; setTextColor(tone); setTypeface(typeface, Typeface.BOLD)
            })
            addView(TextView(this@MainActivity).apply {
                text = value; textSize = 19f; setTextColor(valueColor); setTypeface(typeface, Typeface.BOLD); setPadding(0,dp(7),0,dp(3))
            })
            addView(View(this@MainActivity).apply {
                background = GradientDrawable().apply { cornerRadius = dp(3).toFloat(); setColor(tone) }
                layoutParams = LinearLayout.LayoutParams(dp(38),dp(3))
            })
        }
        return MaterialCardView(this).apply {
            radius = dp(21).toFloat(); cardElevation = dp(2).toFloat(); strokeWidth = dp(1); strokeColor = tone
            setCardBackgroundColor(cardFill)
            layoutParams = GridLayout.LayoutParams().apply { width=0; height=GridLayout.LayoutParams.WRAP_CONTENT; columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1f); setMargins(dp(4),dp(4),dp(4),dp(4)) }
            addView(body)
            if (onClick != null) { isClickable=true; isFocusable=true; setOnClickListener { onClick() } }
        }
    }
    private fun showStageTimeline(label:String, tone:Int, minutes:Long, s:SleepSummary) {
        val d=resources.displayMetrics.density; fun dp(v:Int)=(v*d).toInt()
        pageTitle.text="Schlafphasen"; pageSubtitle.text=label+" · Verlauf dieser Nacht"
        actionsTitle.visibility=View.GONE; actionsBox.visibility=View.GONE; sleepCard.removeAllViews()
        sleepCard.addView(TextView(this).apply { text="‹  Zurück zur Übersicht"; textSize=12f; setTextColor(tone); setPadding(dp(2),dp(8),0,dp(14)); setOnClickListener { showOverview() } })
        val intervals=s.stageSeries.filter { it.stageLabel==label }
        sleepCard.addView(MaterialCardView(this).apply {
            radius=dp(24).toFloat(); strokeWidth=dp(1); strokeColor=tone; setCardBackgroundColor(Color.argb(190,9,15,31))
            addView(LinearLayout(this@MainActivity).apply { orientation=LinearLayout.VERTICAL; setPadding(dp(18),dp(18),dp(18),dp(18))
                addView(TextView(this@MainActivity).apply { text=label.uppercase(); textSize=12f; setTextColor(tone); setTypeface(typeface,Typeface.BOLD) })
                addView(TextView(this@MainActivity).apply { text=(minutes/60).toString()+" h "+(minutes%60).toString()+" min"; textSize=31f; setTextColor(Color.WHITE); setTypeface(typeface,Typeface.BOLD); setPadding(0,dp(8),0,dp(14)) })
                addView(LinearLayout(this@MainActivity).apply { orientation=LinearLayout.HORIZONTAL
                    val duration=(s.endMs-s.startMs).coerceAtLeast(1); var cursor=s.startMs
                    fun seg(ms:Long,active:Boolean)=View(this@MainActivity).apply { background=GradientDrawable().apply { cornerRadius=dp(5).toFloat(); setColor(if(active) tone else Color.argb(38,120,130,160)) }; layoutParams=LinearLayout.LayoutParams(0,dp(if(active) 54 else 18),(ms.toFloat()/duration).coerceAtLeast(.001f)).apply { gravity=android.view.Gravity.CENTER_VERTICAL; setMargins(dp(1),0,dp(1),0) } }
                    intervals.sortedBy { it.startMs }.forEach { st -> if(st.startMs>cursor) addView(seg(st.startMs-cursor,false)); addView(seg(st.endMs-st.startMs,true)); cursor=st.endMs }; if(cursor<s.endMs) addView(seg(s.endMs-cursor,false))
                })
                val tf=DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())
                addView(TextView(this@MainActivity).apply { text=tf.format(Instant.ofEpochMilli(s.startMs))+"  ·  "+intervals.size+" Abschnitte  ·  "+tf.format(Instant.ofEpochMilli(s.endMs)); textSize=10f; setTextColor(Color.rgb(135,147,180)); setPadding(0,dp(9),0,0) })
            })
        })
    }

    private fun showMetricDetail(label: String, icon: String, tone: Int, s: SleepSummary) {
        pageTitle.text = "Gesundheitswerte"
        pageSubtitle.text = "Berühren & entlang der Kurven fahren"
        actionsTitle.visibility=View.GONE; actionsBox.visibility=View.GONE
        sleepCard.removeAllViews(); sleepCard.background=null
        val d=resources.displayMetrics.density; fun px(v:Int)=(v*d).toInt()
        sleepCard.addView(TextView(this).apply {
            text="‹  Zurück zur Übersicht"; textSize=12f; setTextColor(tone); setPadding(px(2),px(8),0,px(12))
            setOnClickListener { showOverview() }
        })
        fun chartCard(name:String, glyph:String, color:Int, value:String, points:List<MetricPoint>) =
            MaterialCardView(this).apply {
                radius=px(24).toFloat(); strokeWidth=px(1); strokeColor=Color.argb(180,Color.red(color),Color.green(color),Color.blue(color))
                setCardBackgroundColor(Color.argb(188,9,15,31)); cardElevation=px(3).toFloat()
                layoutParams=LinearLayout.LayoutParams(-1,-2).apply { setMargins(0,0,0,px(14)) }
                addView(LinearLayout(this@MainActivity).apply {
                    orientation=LinearLayout.VERTICAL; setPadding(px(16),px(15),px(16),px(12))
                    addView(LinearLayout(this@MainActivity).apply {
                        orientation=LinearLayout.HORIZONTAL; gravity=android.view.Gravity.CENTER_VERTICAL
                        addView(TextView(this@MainActivity).apply { text=glyph+"  "+name.uppercase(); textSize=12f; letterSpacing=.08f; setTextColor(color); setTypeface(typeface,Typeface.BOLD); layoutParams=LinearLayout.LayoutParams(0,-2,1f) })
                        addView(TextView(this@MainActivity).apply { text=value; textSize=22f; setTextColor(Color.WHITE); setTypeface(typeface,Typeface.BOLD); setShadowLayer(px(7).toFloat(),0f,0f,Color.argb(75,Color.red(color),Color.green(color),Color.blue(color))) })
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
                            textSize=10f; letterSpacing=.05f; setTextColor(Color.rgb(154,164,191)); setPadding(0,px(7),0,px(2))
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
            textSize=10f; letterSpacing=.08f; setTextColor(Color.rgb(112,122,153)); setPadding(px(2),0,0,px(10))
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
        sleepCard.addView(LinearLayout(this).apply {
            orientation=LinearLayout.HORIZONTAL; gravity=android.view.Gravity.CENTER_VERTICAL; setPadding(dp(2),0,dp(2),dp(8))
            addView(TextView(this@MainActivity).apply {
                text="LETZTE NACHT"; textSize=11f; letterSpacing=.16f; setTextColor(accent2); setTypeface(typeface,Typeface.BOLD)
                layoutParams=LinearLayout.LayoutParams(0,-2,1f)
            })
            addView(TextView(this@MainActivity).apply {
                text="${tf.format(java.time.Instant.ofEpochMilli(s.startMs))} – ${tf.format(java.time.Instant.ofEpochMilli(s.endMs))}"; textSize=12f; setTextColor(Color.rgb(151,158,190))
            })
        })
        val quality = ((s.lightMin + s.deepMin + s.remMin) * 100 / s.totalMin.coerceAtLeast(1)).toInt().coerceIn(0,100)
        sleepCard.addView(LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = android.view.Gravity.CENTER_VERTICAL; setPadding(dp(18),dp(19),dp(18),dp(19)); elevation=dp(8).toFloat()
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(Color.rgb(58,25,105),Color.rgb(24,25,72),Color.rgb(6,55,66))).apply { cornerRadius=dp(30).toFloat(); setStroke(dp(1),Color.rgb(107,82,190)) }
            addView(LinearLayout(this@MainActivity).apply {
                orientation=LinearLayout.VERTICAL; layoutParams=LinearLayout.LayoutParams(0,-2,1f)
                addView(LinearLayout(this@MainActivity).apply { orientation=LinearLayout.HORIZONTAL; gravity=android.view.Gravity.CENTER_VERTICAL
                    addView(View(this@MainActivity).apply { background=GradientDrawable().apply { shape=GradientDrawable.OVAL; setColor(accent2) }; layoutParams=LinearLayout.LayoutParams(dp(7),dp(7)).apply { marginEnd=dp(7) } })
                    addView(TextView(this@MainActivity).apply { text="GESAMTSCHLAF"; textSize=10f; letterSpacing=.14f; setTextColor(Color.rgb(184,174,224)); setTypeface(typeface,Typeface.BOLD) })
                })
                addView(TextView(this@MainActivity).apply { text=fmt(s.totalMin); textSize=42f; setTextColor(Color.WHITE); setTypeface(typeface,Typeface.BOLD); setPadding(0,dp(2),0,0) })
                addView(TextView(this@MainActivity).apply { text="☾  Schlafzeit"; textSize=12f; setTextColor(Color.rgb(151,210,225)); setPadding(0,dp(2),0,0) })
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
                addView(TextView(this@MainActivity).apply { text=name; textSize=11f; setTextColor(Color.rgb(185,190,215)) })
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
            radius=dp(20).toFloat(); strokeWidth=dp(1); strokeColor=Color.rgb(74,57,126); setCardBackgroundColor(Color.rgb(19,15,39))
            addView(LinearLayout(this@MainActivity).apply {
                orientation=LinearLayout.HORIZONTAL; gravity=android.view.Gravity.CENTER_VERTICAL; setPadding(dp(14),dp(12),dp(14),dp(12))
                addView(TextView(this@MainActivity).apply { text="SCHLAF-\nARCHITEKTUR"; textSize=10f; letterSpacing=.10f; setTextColor(Color.rgb(171,155,220)); setTypeface(typeface,Typeface.BOLD); layoutParams=LinearLayout.LayoutParams(0,-2,1f) })
                addView(TextView(this@MainActivity).apply { text="$deepPct%\nTIEF"; gravity=android.view.Gravity.CENTER; textSize=13f; setTextColor(stageDeep); setTypeface(typeface,Typeface.BOLD); layoutParams=LinearLayout.LayoutParams(dp(62),-2) })
                addView(TextView(this@MainActivity).apply { text="$remPct%\nREM"; gravity=android.view.Gravity.CENTER; textSize=13f; setTextColor(stageRem); setTypeface(typeface,Typeface.BOLD); layoutParams=LinearLayout.LayoutParams(dp(62),-2) })
            })
            layoutParams=LinearLayout.LayoutParams(-1,-2).apply { setMargins(0,dp(7),0,dp(7)) }
        })
        sleepCard.addView(LinearLayout(this).apply {
            orientation=LinearLayout.HORIZONTAL; gravity=android.view.Gravity.CENTER_VERTICAL; setPadding(dp(4),dp(10),0,dp(2))
            addView(TextView(this@MainActivity).apply { text="SCHLAFPHASEN"; textSize=11f; letterSpacing=.14f; setTextColor(stageRem); setTypeface(typeface,Typeface.BOLD); layoutParams=LinearLayout.LayoutParams(0,-2,1f) })
            addView(TextView(this@MainActivity).apply { text=fmt(s.lightMin+s.deepMin+s.remMin); textSize=10f; setTypeface(typeface,Typeface.BOLD); setTextColor(stageRem); setPadding(dp(10),dp(4),dp(10),dp(4)); background=GradientDrawable().apply { cornerRadius=dp(13).toFloat(); setColor(Color.rgb(35,19,54)); setStroke(dp(1),Color.rgb(86,48,119)) } })
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
            addView(metricCard("🌙","Leicht",fmt(s.lightMin)) { showStageTimeline("Leicht", stageLight, s.lightMin, s) })
            addView(metricCard("🌑","Tief",fmt(s.deepMin)) { showStageTimeline("Tief", stageDeep, s.deepMin, s) })
            addView(metricCard("🧠","REM",fmt(s.remMin)) { showStageTimeline("REM", stageRem, s.remMin, s) })
            addView(metricCard("👀","Wach",fmt(s.awakeMin)) { showStageTimeline("Wach", stageAwake, s.awakeMin, s) })
        }
        sleepCard.addView(stages)
        sleepCard.addView(View(this).apply {
            background=GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,intArrayOf(Color.TRANSPARENT,Color.rgb(33,104,122),Color.rgb(84,51,133),Color.TRANSPARENT))
            layoutParams=LinearLayout.LayoutParams(-1,dp(1)).apply { setMargins(dp(18),dp(15),dp(18),dp(3)) }
        })
        sleepCard.addView(LinearLayout(this).apply {
            orientation=LinearLayout.HORIZONTAL; gravity=android.view.Gravity.CENTER_VERTICAL; setPadding(dp(4),dp(8),dp(4),dp(4))
            addView(TextView(this@MainActivity).apply { text="GESUNDHEITSWERTE"; textSize=11f; letterSpacing=.14f; setTextColor(accent2); setTypeface(typeface,Typeface.BOLD); layoutParams=LinearLayout.LayoutParams(0,-2,1f) })
            addView(TextView(this@MainActivity).apply { text="LIVE"; textSize=8f; letterSpacing=.12f; setTextColor(Color.rgb(76,225,169)); setPadding(dp(7),dp(3),dp(7),dp(3)); background=GradientDrawable().apply { cornerRadius=dp(10).toFloat(); setColor(Color.rgb(7,34,28)) } })
            addView(View(this@MainActivity).apply { layoutParams=LinearLayout.LayoutParams(dp(7),dp(1)) })
            addView(TextView(this@MainActivity).apply { text="GARMIN  ●"; textSize=9f; letterSpacing=.08f; setTextColor(Color.rgb(86,230,166)); setTypeface(typeface,Typeface.BOLD); setPadding(dp(9),dp(4),dp(9),dp(4)); background=GradientDrawable().apply { cornerRadius=dp(13).toFloat(); setColor(Color.rgb(8,37,31)); setStroke(dp(1),Color.rgb(24,95,73)) } })
        })
        val vitals = GridLayout(this).apply {
            columnCount = 2
            addView(metricCard("❤️","Puls",num(s.avgHr,"bpm")) { showMetricDetail("Puls", "❤️", Color.rgb(255,82,126), s) })
            addView(metricCard("🩸","SpO₂","Ø ${num(s.avgSpo2,"%")}\nMin. ${num(s.minSpo2,"%")}") { showMetricDetail("SpO₂", "🩸", Color.rgb(44,205,255), s) })
            addView(metricCard("🫁","Atmung","Ø ${num(s.avgResp,"/min")}\nMin. ${num(s.minResp,"/min")}") { showMetricDetail("Atmung", "🫁", Color.rgb(80,225,184), s) })
            addView(metricCard("💓","HRV",num(s.avgHrv,"ms")) { showMetricDetail("HRV", "💓", Color.rgb(213,96,255), s) })
        }
        sleepCard.addView(vitals)
        sleepCard.addView(MaterialCardView(this).apply {
            radius=dp(18).toFloat(); cardElevation=0f; strokeWidth=dp(1); strokeColor=Color.rgb(24,94,105); setCardBackgroundColor(Color.rgb(7,25,31))
            layoutParams=LinearLayout.LayoutParams(-1,-2).apply { setMargins(0,dp(12),0,0) }
            addView(LinearLayout(this@MainActivity).apply {
                orientation=LinearLayout.HORIZONTAL; gravity=android.view.Gravity.CENTER_VERTICAL; setPadding(dp(14),dp(10),dp(14),dp(10))
                addView(TextView(this@MainActivity).apply { text="●"; textSize=12f; setTextColor(Color.rgb(76,225,169)); layoutParams=LinearLayout.LayoutParams(dp(24),-2) })
                addView(TextView(this@MainActivity).apply { text="Messwerte vollständig synchronisiert"; textSize=11f; setTextColor(Color.rgb(160,210,214)); layoutParams=LinearLayout.LayoutParams(0,-2,1f) })
                addView(TextView(this@MainActivity).apply { text="GARMIN"; textSize=9f; letterSpacing=.12f; setTypeface(typeface,Typeface.BOLD); setTextColor(accent2) })
            })
        })
        sleepCard.addView(LinearLayout(this).apply {
            orientation=LinearLayout.HORIZONTAL; gravity=android.view.Gravity.CENTER_VERTICAL; setPadding(dp(4),dp(16),0,dp(7))
            addView(TextView(this@MainActivity).apply { text="NACHT-INSIGHT"; textSize=11f; letterSpacing=.14f; setTextColor(stageRem); setTypeface(typeface,Typeface.BOLD); layoutParams=LinearLayout.LayoutParams(0,-2,1f) })
            addView(TextView(this@MainActivity).apply {
                text=when { quality>=90 -> "AUSGEZEICHNET"; quality>=80 -> "GUT"; else -> "IM BLICK BEHALTEN" }
                textSize=9f; letterSpacing=.08f; setTypeface(typeface,Typeface.BOLD); setTextColor(accent2); setPadding(dp(10),dp(5),dp(10),dp(5))
                background=GradientDrawable().apply { cornerRadius=dp(14).toFloat(); setColor(Color.rgb(8,34,47)); setStroke(dp(1),Color.rgb(28,112,137)) }
            })
        })
        sleepCard.addView(MaterialCardView(this).apply {
            radius=dp(24).toFloat(); cardElevation=dp(2).toFloat(); strokeWidth=dp(1); strokeColor=Color.rgb(81,62,137); setCardBackgroundColor(Color.rgb(19,15,39))
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
                    textSize=13f; setTextColor(Color.rgb(220,224,244)); setTypeface(typeface,Typeface.BOLD)
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
