package de.ricci.garminsleep

import androidx.activity.ComponentActivity
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.*
import kotlinx.coroutines.*

class MainActivity : ComponentActivity(), CoroutineScope by MainScope() {
    private lateinit var status: TextView
    private val permissions = setOf(
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(OxygenSaturationRecord::class),
        HealthPermission.getReadPermission(RespiratoryRateRecord::class)
    )
    private val permissionLauncher = registerForActivityResult(PermissionController.createRequestPermissionResultContract()) { refresh() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        status = TextView(this).apply { textSize = 17f; setPadding(32,32,32,32) }
        val grant = Button(this).apply { text = "Health-Connect-Berechtigungen"; setOnClickListener { permissionLauncher.launch(permissions) } }
        val test = Button(this).apply { text = "Garmin-Schlaf testen"; setOnClickListener { testRead() } }
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(24,48,24,24); addView(grant); addView(test); addView(status) }
        setContentView(ScrollView(this).apply { addView(box) })
        refresh()
    }

    private fun refresh() = launch {
        val sdk = HealthConnectClient.getSdkStatus(this@MainActivity)
        if (sdk != HealthConnectClient.SDK_AVAILABLE) { status.text = "Health Connect ist auf diesem Gerät nicht verfügbar."; return@launch }
        val granted = HealthConnectClient.getOrCreate(this@MainActivity).permissionController.getGrantedPermissions()
        status.text = if (granted.containsAll(permissions)) "✅ Berechtigungen vorhanden. Bereit für Tasker." else "⚠️ Bitte Health-Connect-Berechtigungen erteilen."
    }

    private fun testRead() = launch {
        status.text = "Lese Garmin-Schlaf…"
        status.text = try { SleepReader(this@MainActivity).latestGarminSleep().calendarText } catch (t: Throwable) { "❌ ${t.message}" }
    }

    override fun onDestroy() { super.onDestroy(); cancel() }
}

class HealthPermissionRationaleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(TextView(this).apply { textSize=18f; setPadding(48,80,48,48); text="Garmin Sleep for Tasker liest nur die von dir freigegebenen Health-Connect-Daten, um Schlafdauer, Schlafphasen und zugehörige Messwerte für deine eigene Tasker-Automation auszuwerten. Es werden keine Daten hochgeladen." })
    }
}
