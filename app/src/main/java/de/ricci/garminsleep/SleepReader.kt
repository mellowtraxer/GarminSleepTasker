package de.ricci.garminsleep

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.*
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private const val GARMIN_PACKAGE = "com.garmin.android.apps.connectmobile"

data class SleepSummary(
    val startMs: Long, val endMs: Long, val totalMin: Long,
    val lightMin: Long, val deepMin: Long, val remMin: Long,
    val awakeMin: Long, val sleepingMin: Long,
    val avgHr: Double?, val avgSpo2: Double?, val avgResp: Double?,
    val source: String, val calendarText: String
)

class SleepReader(private val context: Context) {
    private val client by lazy { HealthConnectClient.getOrCreate(context) }

    suspend fun latestGarminSleep(hoursBack: Long = 36): SleepSummary {
        val now = Instant.now()
        val from = now.minus(Duration.ofHours(hoursBack))
        val response = client.readRecords(
            ReadRecordsRequest(
                recordType = SleepSessionRecord::class,
                timeRangeFilter = TimeRangeFilter.between(from, now),
                ascendingOrder = false
            )
        )
        val sleep = response.records
            .filter { it.metadata.dataOrigin.packageName == GARMIN_PACKAGE }
            .maxByOrNull { it.endTime }
            ?: error("Keine Garmin-Schlafsession in den letzten $hoursBack Stunden gefunden")

        fun stageMinutes(vararg types: Int): Long = sleep.stages
            .filter { it.stage in types }
            .sumOf { Duration.between(it.startTime, it.endTime).toMinutes() }

        val light = stageMinutes(SleepSessionRecord.STAGE_TYPE_LIGHT)
        val deep = stageMinutes(SleepSessionRecord.STAGE_TYPE_DEEP)
        val rem = stageMinutes(SleepSessionRecord.STAGE_TYPE_REM)
        val awake = stageMinutes(
            SleepSessionRecord.STAGE_TYPE_AWAKE,
            SleepSessionRecord.STAGE_TYPE_AWAKE_IN_BED,
            SleepSessionRecord.STAGE_TYPE_OUT_OF_BED
        )
        val sleeping = stageMinutes(SleepSessionRecord.STAGE_TYPE_SLEEPING)
        val total = Duration.between(sleep.startTime, sleep.endTime).toMinutes()

        val hr = averageHeartRate(sleep.startTime, sleep.endTime)
        val spo2 = averageSpo2(sleep.startTime, sleep.endTime)
        val resp = averageRespiratoryRate(sleep.startTime, sleep.endTime)
        val text = makeCalendarText(sleep.startTime, sleep.endTime, total, light, deep, rem, awake, sleeping, hr, spo2, resp)

        return SleepSummary(
            sleep.startTime.toEpochMilli(), sleep.endTime.toEpochMilli(), total,
            light, deep, rem, awake, sleeping, hr, spo2, resp,
            sleep.metadata.dataOrigin.packageName, text
        )
    }

    private suspend fun averageHeartRate(start: Instant, end: Instant): Double? {
        val records = client.readRecords(ReadRecordsRequest(HeartRateRecord::class, TimeRangeFilter.between(start, end))).records
        val samples = records.filter { it.metadata.dataOrigin.packageName == GARMIN_PACKAGE }.flatMap { it.samples }
        return samples.map { it.beatsPerMinute.toDouble() }.average().takeUnless { it.isNaN() }
    }

    private suspend fun averageSpo2(start: Instant, end: Instant): Double? {
        val records = client.readRecords(ReadRecordsRequest(OxygenSaturationRecord::class, TimeRangeFilter.between(start, end))).records
        val values = records.filter { it.metadata.dataOrigin.packageName == GARMIN_PACKAGE }.map { it.percentage.value }
        return values.average().takeUnless { it.isNaN() }
    }

    private suspend fun averageRespiratoryRate(start: Instant, end: Instant): Double? {
        val records = client.readRecords(ReadRecordsRequest(RespiratoryRateRecord::class, TimeRangeFilter.between(start, end))).records
        val values = records.filter { it.metadata.dataOrigin.packageName == GARMIN_PACKAGE }.map { it.rate }
        return values.average().takeUnless { it.isNaN() }
    }

    private fun fmtMin(min: Long) = "${min / 60} h ${min % 60} min"
    private fun n(v: Double?, suffix: String) = v?.let { String.format(java.util.Locale.GERMANY, "%.1f %s", it, suffix) } ?: "nicht verfügbar"

    private fun makeCalendarText(start: Instant, end: Instant, total: Long, light: Long, deep: Long, rem: Long, awake: Long, sleeping: Long, hr: Double?, spo2: Double?, resp: Double?): String {
        val tf = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())
        return buildString {
            appendLine("⌚ Garmin Schlaf")
            appendLine("${tf.format(start)} – ${tf.format(end)} Uhr")
            appendLine("💤 Gesamt: ${fmtMin(total)}")
            appendLine("🌙 Leicht: ${fmtMin(light)}")
            appendLine("🌑 Tief: ${fmtMin(deep)}")
            appendLine("🧠 REM: ${fmtMin(rem)}")
            appendLine("👀 Wach: ${fmtMin(awake)}")
            if (sleeping > 0) appendLine("😴 Nicht klassifiziert: ${fmtMin(sleeping)}")
            appendLine("❤️ Ø Puls: ${n(hr, "bpm")}")
            appendLine("🩸 Ø SpO₂: ${n(spo2, "%")}")
            append("🫁 Ø Atmung: ${n(resp, "/min")}")
        }
    }
}
