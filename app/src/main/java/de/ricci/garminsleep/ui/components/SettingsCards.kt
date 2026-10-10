package de.ricci.garminsleep.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Cyan = Color(0xFF00F0FF)
private val Muted = Color(0xFFB5C5E2)

@Composable
private fun SectionTitle(title: String) {
    Text(title, color = Cyan, fontSize = 11.sp, fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 20.dp, bottom = 10.dp))
}

@Composable
private fun ActionCard(icon: String, title: String, subtitle: String, onClick: () -> Unit,
    modifier: Modifier = Modifier) {
    FrostedGlassCard(modifier = modifier.clickable(onClick = onClick)) {
        Text(icon, color = Cyan, fontSize = 25.sp)
        Spacer(Modifier.height(8.dp))
        Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(subtitle, color = Muted, fontSize = 12.sp)
    }
}

@Composable
fun SleepSyncSettingsCards(
    themeLabel: String,
    garminLinked: Boolean,
    onDesign: () -> Unit,
    onUpdates: () -> Unit,
    onAutomation: () -> Unit,
    onGarmin: () -> Unit,
    onHealth: () -> Unit,
    onPrivacy: () -> Unit,
    onAbout: () -> Unit,
    onTool: (Int) -> Unit
) {
    var toolsOpen by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 3.dp)) {
        FrostedGlassCard(modifier = Modifier.fillMaxWidth()) {
            Text("✦  SLEEPSYNC / YOUR UNIVERSE", color = Cyan, fontSize = 11.sp)
            Spacer(Modifier.height(14.dp))
            Text("Dein Kosmos.\nDeine Kontrolle.", color = Color.White,
                fontSize = 28.sp, fontWeight = FontWeight.Bold, lineHeight = 34.sp)
            Spacer(Modifier.height(10.dp))
            Text("Dein Schlaf. Deine Daten. Dein Design.", color = Muted, fontSize = 13.sp)
            Spacer(Modifier.height(16.dp))
            Text("●  ${if (garminLinked) "GARMIN VERBUNDEN" else "GARMIN OFFLINE"}   ✦  $themeLabel",
                color = Color(0xFF77EDD7), fontSize = 10.sp)
        }
        SectionTitle("✦  DEIN KOSMOS")
        ActionCard("✦", "Dein Universum. Deine Regeln.",
            "DESIGN STUDIO · Wallpaper, Farben, Effekte und Animationen",
            onDesign, Modifier.fillMaxWidth())
        SectionTitle("✦  SCHNELLZUGRIFF")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ActionCard("↻", "Updates", "Neue Version prüfen", onUpdates, Modifier.weight(1f))
            ActionCard("⚙", "Automatik", "Sync & Kalender", onAutomation, Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ActionCard("⌚", "Garmin", "Verbindung & Daten", onGarmin, Modifier.weight(1f))
            ActionCard("♥", "Health Connect", "Gesundheitsdaten", onHealth, Modifier.weight(1f))
        }
        SectionTitle("✦  TOOLS & DIAGNOSE")
        ActionCard("◈", "Datenschutz", "Lokale Daten & Berechtigungen", onPrivacy, Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        ActionCard("ⓘ", "Über SleepSync", "Version, Build & Informationen", onAbout, Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        ActionCard("⚙", "TOOLS & DIAGNOSE",
            "Verbindungen, Datenabruf und Systemprüfung", { toolsOpen = !toolsOpen }, Modifier.fillMaxWidth())
        if (toolsOpen) {
            val tools = listOf("Health Connect · Berechtigungen",
                "Garmin Connect · Verbinden", "Garmin Connect · Trennen",
                "Schlafdaten neu laden", "App-Signatur anzeigen")
            tools.forEachIndexed { index, title ->
                Spacer(Modifier.height(6.dp))
                ActionCard("↗", title, "Öffnen", { onTool(index) }, Modifier.fillMaxWidth())
            }
        }
        Text("NEVER GO BACK. ALWAYS FORWARD.  ✦",
            color = Color(0xFFA088E1), fontSize = 10.sp,
            modifier = Modifier.fillMaxWidth().padding(vertical = 28.dp))
    }
}
