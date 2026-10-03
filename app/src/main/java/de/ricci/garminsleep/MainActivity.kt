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
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
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
    private val garminClient by lazy { GarminConnectClient(this) }
    private val permissions = setOf(
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(OxygenSaturationRecord::class),
        HealthPermission.getReadPermission(RespiratoryRateRecord::class)
    )
    private val permissionLauncher = registerForActivityResult(PermissionController.createRequestPermissionResultContract()) { refresh() }

    override fun onCreate(savedInstanceState: Bundle?) {
        DynamicColors.applyToActivityIfAvailable(this)
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
        val sleepShell = MaterialCardView(this).apply { radius=dp(24).toFloat(); cardElevation=0f; strokeWidth=dp(1); addView(sleepCard) }
        val nav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER
            setPadding(0, dp(14), 0, dp(2))
            fun tab(label: String, action: () -> Unit) = MaterialButton(this@MainActivity).apply {
                text = label; isAllCaps = false; textSize = 11f
                layoutParams = LinearLayout.LayoutParams(0, dp(52), 1f)
                setOnClickListener { action() }
            }
            addView(tab("⌂\nÜbersicht") { showOverview() })
            addView(tab("≋\nVerlauf") { showHistoryPlaceholder() })
            addView(tab("▣\nKalender") { showCalendarPlaceholder() })
            addView(tab("⚙\nEinstellungen") { showSettings() })
        }
        val section = TextView(this).apply { text="Verbindungen & Automatik"; textSize=18f; setTypeface(typeface, Typeface.BOLD); setPadding(0,dp(22),0,dp(8)) }
        val grant = button("Health Connect · Berechtigungen") { permissionLauncher.launch(permissions) }
        val link = button("Garmin Connect · Verbinden") { showGarminLogin() }
        val unlink = button("Garmin Connect · Trennen") { garminClient.logout(); refresh() }
        val test = button("Schlafdaten neu laden") { testRead() }
        val sig = button("App-Signatur anzeigen") { showAppSignature() }
        val statusCard = MaterialCardView(this).apply { radius=dp(24).toFloat(); cardElevation=0f; strokeWidth=dp(1); addView(status) }
        val actions = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; addView(grant); addView(link); addView(unlink); addView(test); addView(sig) }
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(20),dp(20),dp(20),dp(32))
            addView(pageTitle); addView(pageSubtitle); addView(statusCard); addView(sleepShell); addView(nav); addView(section); addView(actions)
        }
        val scroll = ScrollView(this).apply { isFillViewport=true; clipToPadding=false; addView(box) }
        ViewCompat.setOnApplyWindowInsetsListener(scroll) { v, insets ->
            val bars=insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            v.setPadding(0,bars.top,0,bars.bottom); insets
        }
        setContentView(scroll)
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
        pageTitle.text = "SleepSync"
        pageSubtitle.text = "Deine letzte Nacht auf einen Blick"
        sleepCard.visibility = View.VISIBLE
    }

    private fun showHistoryPlaceholder() {
        pageTitle.text = "Verlauf"
        pageSubtitle.text = "Deine Nächte im Vergleich"
        sleepCard.removeAllViews()
        sleepCard.addView(TextView(this).apply {
            text = "📊  Schlafverlauf\n\nHier entsteht die Wochen- und Monatsansicht mit Schlafdauer, Phasen, SpO₂, Atmung und HRV."
            textSize = 17f; setPadding(0, 18, 0, 18)
        })
    }

    private fun showCalendarPlaceholder() {
        pageTitle.text = "Kalender"
        pageSubtitle.text = "Automatisch dokumentiert"
        sleepCard.removeAllViews()
        sleepCard.addView(TextView(this).apply {
            text = "📅  Garmin Schlaf\n\nKalender auswählen · Automatisch eintragen · Vorschau des nächsten Eintrags"
            textSize = 17f; setPadding(0, 18, 0, 18)
        })
    }

    private fun showSettings() {
        pageTitle.text = "Einstellungen"
        pageSubtitle.text = "Verbindungen, Automatik & Darstellung"
        sleepCard.removeAllViews()
        sleepCard.addView(TextView(this).apply {
            text = "⌚ Garmin Connect\n❤️ Health Connect\n⚡ Tasker Plugin\n🎨 Material You\n🔒 Datenschutz & Diagnose"
            textSize = 17f; setPadding(0, 18, 0, 18)
        })
    }

    private fun sleepStageBar(s: SleepSummary): String {
        val parts = listOf(s.lightMin, s.deepMin, s.remMin, s.awakeMin)
        val total = parts.sum().coerceAtLeast(1)
        return parts.joinToString(" ") { m -> "▰".repeat(((m * 18 / total).toInt()).coerceAtLeast(1)) }
    }

    private fun metricCard(icon: String, label: String, value: String): MaterialCardView {
        val d = resources.displayMetrics.density
        fun dp(v: Int) = (v * d).toInt()
        val body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(14), dp(14), dp(14))
            addView(TextView(this@MainActivity).apply { text = "$icon  $label"; textSize = 13f; alpha = .72f })
            addView(TextView(this@MainActivity).apply { text = value; textSize = 18f; setTypeface(typeface, Typeface.BOLD); setPadding(0,dp(6),0,0) })
        }
        return MaterialCardView(this).apply {
            radius = dp(18).toFloat(); cardElevation = 0f; strokeWidth = dp(1)
            layoutParams = GridLayout.LayoutParams().apply { width=0; height=GridLayout.LayoutParams.WRAP_CONTENT; columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1f); setMargins(dp(4),dp(4),dp(4),dp(4)) }
            addView(body)
        }
    }

    private fun renderDashboard(s: SleepSummary) {
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
        sleepCard.addView(TextView(this).apply {
            text = "🌙  ${fmt(s.totalMin)}"; textSize = 38f; setTypeface(typeface, Typeface.BOLD); setPadding(0, dp(3), 0, dp(10))
        })
        sleepCard.addView(TextView(this).apply {
            text = sleepStageBar(s); textSize = 22f; letterSpacing = .08f; setPadding(0, dp(4), 0, dp(2))
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
