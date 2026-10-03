package de.ricci.garminsleep

import androidx.activity.ComponentActivity
import android.os.Bundle
import android.content.pm.PackageManager
import java.security.MessageDigest
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
import android.content.res.ColorStateList
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.*
import kotlinx.coroutines.*

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
    private val cardBg = Color.rgb(15, 17, 34)
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
        pageTitle = TextView(this).apply { text = "SleepSync"; textSize = 30f; setTypeface(typeface, Typeface.BOLD) }
        pageSubtitle = TextView(this).apply { text = "Dein Schlaf. Klar, automatisch, im Kalender."; textSize = 15f; alpha = .7f; setPadding(0,dp(4),0,dp(16)) }
        val sleepShell = MaterialCardView(this).apply {
            radius=dp(28).toFloat(); cardElevation=0f; strokeWidth=dp(1)
            setCardBackgroundColor(cardBg); strokeColor = Color.rgb(44,49,82); addView(sleepCard)
        }
        val nav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER
            setPadding(0, dp(14), 0, dp(2))
            fun tab(label: String, action: () -> Unit) = MaterialButton(this@MainActivity).apply {
                text = label; isAllCaps = false; textSize = 11f; cornerRadius = dp(18)
                setTextColor(Color.rgb(205, 210, 235))
                backgroundTintList = ColorStateList.valueOf(Color.rgb(15, 18, 35))
                strokeColor = ColorStateList.valueOf(Color.rgb(45, 52, 86)); strokeWidth = dp(1)
                insetTop = 0; insetBottom = 0
                layoutParams = LinearLayout.LayoutParams(0, dp(56), 1f).apply { setMargins(dp(3),0,dp(3),0) }
                setOnClickListener { action() }
            }
            addView(tab("◉\nÜbersicht") { showOverview() })
            addView(tab("▥\nVerlauf") { showHistoryPlaceholder() })
            addView(tab("▦\nKalender") { showCalendarPlaceholder() })
            addView(tab("⚙\nSettings") { showSettings() })
        }
        actionsTitle = TextView(this).apply { text="Verbindungen & Automatik"; textSize=18f; setTypeface(typeface, Typeface.BOLD); setPadding(0,dp(22),0,dp(8)) }
        val grant = button("Health Connect · Berechtigungen") { permissionLauncher.launch(permissions) }
        val link = button("Garmin Connect · Verbinden") { showGarminLogin() }
        val unlink = button("Garmin Connect · Trennen") { garminClient.logout(); refresh() }
        val test = button("Schlafdaten neu laden") { testRead() }
        val sig = button("App-Signatur anzeigen") { showAppSignature() }
        val statusCard = MaterialCardView(this).apply {
            radius=dp(22).toFloat(); cardElevation=0f; strokeWidth=dp(1)
            setCardBackgroundColor(Color.rgb(10,18,31)); strokeColor=Color.rgb(30,102,122); addView(status)
        }
        actionsBox = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; addView(grant); addView(link); addView(unlink); addView(test); addView(sig) }
        pageTitle.setTextColor(Color.WHITE)
        pageSubtitle.setTextColor(Color.rgb(151,158,190))
        status.setTextColor(Color.rgb(166,238,244))
        actionsTitle.setTextColor(Color.WHITE)
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(20),dp(20),dp(20),dp(24))
            addView(pageTitle); addView(pageSubtitle); addView(statusCard); addView(sleepShell); addView(actionsTitle); addView(actionsBox)
        }
        val scroll = ScrollView(this).apply {
            isFillViewport=true; clipToPadding=false; setBackgroundColor(nightBg); addView(box)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,0,1f)
        }
        val navShell = MaterialCardView(this).apply {
            radius=dp(28).toFloat(); cardElevation=dp(10).toFloat(); strokeWidth=dp(1); strokeColor=Color.rgb(48,54,91)
            setCardBackgroundColor(Color.rgb(10,12,25)); addView(nav)
            layoutParams=LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,dp(78)).apply { setMargins(dp(14),dp(4),dp(14),dp(10)) }
        }
        val root = LinearLayout(this).apply {
            orientation=LinearLayout.VERTICAL; setBackgroundColor(nightBg); addView(scroll); addView(navShell)
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
        val gc = if (garminClient.isLinked()) "● Garmin verbunden" else "○ Garmin nicht verbunden"
        status.text = "$gc   ·   $hc"
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
        pageSubtitle.text = "Deine letzte Nacht auf einen Blick"
        lastSummary?.let { renderDashboard(it) }
    }

    private fun showHistoryPlaceholder() {
        actionsTitle.visibility = View.GONE
        actionsBox.visibility = View.GONE
        pageTitle.text = "Verlauf"
        pageSubtitle.text = "Deine Nächte im Vergleich"
        sleepCard.removeAllViews()
        sleepCard.addView(TextView(this).apply {
            text = "Diese Woche"; textSize = 22f; setTypeface(typeface, Typeface.BOLD)
        })
        sleepCard.addView(TextView(this).apply {
            text = "  Mo     Di     Mi     Do     Fr     Sa     So\n  ▃      ▅      ▆      ▂      ▇      ▆      ▅"
            textSize = 21f; letterSpacing = .03f; setPadding(0,24,0,18)
        })
        sleepCard.addView(TextView(this).apply {
            text = "Ø Schlafdauer   7 h 42 min\n\nTrends für Schlafdauer, Tiefschlaf, REM, SpO₂, Atmung und HRV werden hier aus deinen gespeicherten Nächten aufgebaut."
            textSize = 16f; setPadding(0,8,0,18)
        })
    }

    private fun showCalendarPlaceholder() {
        actionsTitle.visibility = View.GONE
        actionsBox.visibility = View.GONE
        pageTitle.text = "Kalender"
        pageSubtitle.text = "Automatisch dokumentiert"
        sleepCard.removeAllViews()
        sleepCard.addView(TextView(this).apply {
            text = "📅  Schlaf automatisch eintragen"; textSize = 20f; setTypeface(typeface, Typeface.BOLD)
        })
        sleepCard.addView(TextView(this).apply {
            text = "Kalender   Garmin Schlaf 💤\nStatus       Automatik bereit\n\nVorschau\n💤 Garmin Schlaf\nSchlafdauer · Phasen · Puls · SpO₂ · Atmung · HRV"
            textSize = 16f; setPadding(0,18,0,18)
        })
        sleepCard.addView(MaterialButton(this).apply { text="Kalender auswählen"; isAllCaps=false })
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
        fun setting(icon:String, title:String, sub:String, color:Int) {
            settingsGrid.addView(MaterialCardView(this).apply {
                radius=dp(20).toFloat(); cardElevation=0f; setCardBackgroundColor(Color.rgb(18,21,43))
                strokeWidth=dp(1); strokeColor=Color.rgb(49,56,91)
                layoutParams=LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT).apply{setMargins(0,dp(5),0,dp(5))}
                addView(LinearLayout(this@MainActivity).apply {
                    orientation=LinearLayout.HORIZONTAL; gravity=android.view.Gravity.CENTER_VERTICAL; setPadding(dp(16),dp(14),dp(16),dp(14))
                    addView(TextView(this@MainActivity).apply { text=icon; textSize=25f; setTextColor(color); layoutParams=LinearLayout.LayoutParams(dp(44),LinearLayout.LayoutParams.WRAP_CONTENT) })
                    addView(TextView(this@MainActivity).apply { text="$title\n$sub"; textSize=15f; setTextColor(Color.WHITE); layoutParams=LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,1f) })
                    addView(TextView(this@MainActivity).apply { text="›"; textSize=28f; setTextColor(color) })
                })
            })
        }
        setting("⌚","Garmin Connect","Verbunden · Schlafdaten synchronisieren",accent2)
        setting("♥","Health Connect","Berechtigungen & Gesundheitsdaten",stageRem)
        setting("⚡","Automatik","Tasker & Kalender",stageAwake)
        setting("✦","Darstellung","OLED Night · SleepSync",accent)
        setting("◈","Datenschutz","Lokale Daten & Diagnose",stageLight)
        sleepCard.addView(settingsGrid)
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

    private fun metricCard(icon: String, label: String, value: String): MaterialCardView {
        val d = resources.displayMetrics.density
        fun dp(v: Int) = (v * d).toInt()
        val tone = when (label) {
            "Leicht" -> stageLight; "Tief" -> stageDeep; "REM" -> stageRem; "Wach" -> stageAwake
            "Puls" -> Color.rgb(255,82,126); "SpO₂" -> Color.rgb(44,205,255)
            "Atmung" -> Color.rgb(80,225,184); "HRV" -> Color.rgb(213,96,255)
            else -> accent
        }
        val body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(15), dp(14), dp(15), dp(14))
            addView(TextView(this@MainActivity).apply {
                text = "$icon   ${label.uppercase()}"; textSize = 11f; letterSpacing = .08f; setTextColor(tone); setTypeface(typeface, Typeface.BOLD)
            })
            addView(TextView(this@MainActivity).apply {
                text = value; textSize = 19f; setTextColor(Color.WHITE); setTypeface(typeface, Typeface.BOLD); setPadding(0,dp(7),0,dp(3))
            })
            addView(View(this@MainActivity).apply {
                background = GradientDrawable().apply { cornerRadius = dp(3).toFloat(); setColor(tone) }
                layoutParams = LinearLayout.LayoutParams(dp(38),dp(3))
            })
        }
        return MaterialCardView(this).apply {
            radius = dp(21).toFloat(); cardElevation = dp(2).toFloat(); strokeWidth = dp(1); strokeColor = tone
            setCardBackgroundColor(Color.rgb(15,18,38))
            layoutParams = GridLayout.LayoutParams().apply { width=0; height=GridLayout.LayoutParams.WRAP_CONTENT; columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1f); setMargins(dp(4),dp(4),dp(4),dp(4)) }
            addView(body)
        }
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
        sleepCard.addView(TextView(this).apply {
            text = "Letzte Nacht  ·  ${tf.format(java.time.Instant.ofEpochMilli(s.startMs))} – ${tf.format(java.time.Instant.ofEpochMilli(s.endMs))}"
            textSize = 14f; alpha = .7f
        })
        val quality = ((s.lightMin + s.deepMin + s.remMin) * 100 / s.totalMin.coerceAtLeast(1)).toInt().coerceIn(0,100)
        sleepCard.addView(LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = android.view.Gravity.CENTER_VERTICAL; setPadding(0,dp(5),0,dp(14))
            addView(TextView(this@MainActivity).apply {
                text = "☾  ${fmt(s.totalMin)}"; textSize = 42f; setTextColor(Color.WHITE); setTypeface(typeface, Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            addView(TextView(this@MainActivity).apply {
                text = "$quality%\nEFFIZIENZ"; gravity = android.view.Gravity.CENTER; textSize = 11f; setTypeface(typeface, Typeface.BOLD)
                setTextColor(accent2); setPadding(dp(13),dp(9),dp(13),dp(9))
                background = GradientDrawable().apply { cornerRadius=dp(20).toFloat(); setColor(Color.rgb(7,35,47)); setStroke(dp(1),Color.rgb(22,155,181)) }
            })
        })
        sleepCard.addView(sleepStageStrip(s))
        sleepCard.addView(TextView(this).apply {
            text = "● Leicht    ● Tief    ● REM    ● Wach"; textSize = 12f; alpha = .75f; setPadding(0,dp(7),0,dp(2))
        })
        sleepCard.addView(TextView(this).apply {
            val sleepOnly = (s.lightMin + s.deepMin + s.remMin).coerceAtLeast(1)
            val deepPct = (s.deepMin * 100 / sleepOnly).toInt()
            val remPct = (s.remMin * 100 / sleepOnly).toInt()
            text = "SCHLAFARCHITEKTUR\nTief  $deepPct %     ·     REM  $remPct %"
            textSize = 13f; setTextColor(Color.rgb(200,195,230)); setPadding(0, dp(4), 0, dp(10))
        })
        val stages = GridLayout(this).apply {
            columnCount = 2
            setPadding(0, dp(6), 0, dp(8))
            addView(metricCard("🌙","Leicht",fmt(s.lightMin)))
            addView(metricCard("🌑","Tief",fmt(s.deepMin)))
            addView(metricCard("🧠","REM",fmt(s.remMin)))
            addView(metricCard("👀","Wach",fmt(s.awakeMin)))
        }
        sleepCard.addView(stages)
        val vitals = GridLayout(this).apply {
            columnCount = 2
            addView(metricCard("❤️","Puls",num(s.avgHr,"bpm")))
            addView(metricCard("🩸","SpO₂","Ø ${num(s.avgSpo2,"%")}\nMin. ${num(s.minSpo2,"%")}"))
            addView(metricCard("🫁","Atmung","Ø ${num(s.avgResp,"/min")}\nMin. ${num(s.minResp,"/min")}"))
            addView(metricCard("💓","HRV",num(s.avgHrv,"ms")))
        }
        sleepCard.addView(vitals)
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
