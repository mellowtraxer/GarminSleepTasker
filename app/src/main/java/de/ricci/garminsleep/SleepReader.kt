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

data class MetricPoint(val timeMs: Long, val value: Double)
data class StagePoint(val startMs: Long, val endMs: Long, val stageLabel: String)

data class SleepSummary(
    val startMs: Long, val endMs: Long, val totalMin: Long,
    val lightMin: Long, val deepMin: Long, val remMin: Long,
    val awakeMin: Long, val sleepingMin: Long,
    val avgHr: Double?, val avgSpo2: Double?, val avgResp: Double?,
    val minSpo2: Double?, val minResp: Double?, val avgHrv: Double?,
    val source: String, val calendarText: String,
    val heartRateSeries: List<MetricPoint> = emptyList(),
    val spo2Series: List<MetricPoint> = emptyList(),
    val respirationSeries: List<MetricPoint> = emptyList(),
    val hrvSeries: List<MetricPoint> = emptyList(),
    val stageSeries: List<StagePoint> = emptyList()
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

        val heartSeries = heartRateSeries(sleep.startTime, sleep.endTime)
        val spo2Points = oxygenSeries(sleep.startTime, sleep.endTime)
        val respirationPoints = respirationSeries(sleep.startTime, sleep.endTime)
        // HRV is optional: older/existing installs may not have granted the newer
        // Health Connect HRV permission yet. Never let that block the whole night.
        val hrvPoints = runCatching { hrvSeries(sleep.startTime, sleep.endTime) }.getOrDefault(emptyList())
        val hr = heartSeries.map { it.value }.average().takeUnless { it.isNaN() }
        // Garmin does not currently export overnight SpO2/respiration to Health
        // Connect. Prefer HC if present, otherwise enrich from Garmin Connect
        // when the user has linked the account inside this app.
        val hcSpo2 = runCatching { averageSpo2(sleep.startTime, sleep.endTime) }.getOrNull()
        val hcResp = runCatching { averageRespiratoryRate(sleep.startTime, sleep.endTime) }.getOrNull()
        val sleepDate = sleep.endTime.atZone(ZoneId.systemDefault()).toLocalDate()
        val garmin = runCatching { GarminConnectClient(context).nightMetrics(sleepDate) }.getOrNull()
        val spo2 = hcSpo2 ?: garmin?.avgSpo2
        val resp = hcResp ?: garmin?.avgResp
        val minSpo2 = garmin?.minSpo2
        val minResp = garmin?.minResp
        val avgHrv = garmin?.avgHrv
        val text = makeCalendarText(sleep.startTime, sleep.endTime, total, light, deep, rem, awake, sleeping, hr, spo2, resp, minSpo2, minResp, avgHrv)

        return SleepSummary(
            sleep.startTime.toEpochMilli(), sleep.endTime.toEpochMilli(), total,
            light, deep, rem, awake, sleeping, hr, spo2, resp,
            minSpo2, minResp, avgHrv,
            sleep.metadata.dataOrigin.packageName, text, heartSeries, spo2Points, respirationPoints, hrvPoints,
            sleep.stages.mapNotNull { st ->
                val label = when(st.stage) {
                    SleepSessionRecord.STAGE_TYPE_LIGHT -> "Leicht"
                    SleepSessionRecord.STAGE_TYPE_DEEP -> "Tief"
                    SleepSessionRecord.STAGE_TYPE_REM -> "REM"
                    SleepSessionRecord.STAGE_TYPE_AWAKE -> "Wach"
                    else -> null
                }
                label?.let { StagePoint(st.startTime.toEpochMilli(), st.endTime.toEpochMilli(), it) }
            }
        )
    }

    private suspend fun heartRateSeries(start: Instant, end: Instant): List<MetricPoint> {
        val records = client.readRecords(ReadRecordsRequest(HeartRateRecord::class, TimeRangeFilter.between(start, end))).records
        return records.filter { it.metadata.dataOrigin.packageName == GARMIN_PACKAGE }.flatMap { it.samples }
            .filter { !it.time.isBefore(start) && !it.time.isAfter(end) }.sortedBy { it.time }
            .map { MetricPoint(it.time.toEpochMilli(), it.beatsPerMinute.toDouble()) }
    }

    private suspend fun oxygenSeries(start: Instant, end: Instant): List<MetricPoint> {
        val records=client.readRecords(ReadRecordsRequest(OxygenSaturationRecord::class,TimeRangeFilter.between(start,end))).records
        return records.filter { it.metadata.dataOrigin.packageName==GARMIN_PACKAGE }.sortedBy { it.time }.map { MetricPoint(it.time.toEpochMilli(),it.percentage.value) }
    }

    private suspend fun respirationSeries(start: Instant, end: Instant): List<MetricPoint> {
        val records=client.readRecords(ReadRecordsRequest(RespiratoryRateRecord::class,TimeRangeFilter.between(start,end))).records
        return records.filter { it.metadata.dataOrigin.packageName==GARMIN_PACKAGE }.sortedBy { it.time }.map { MetricPoint(it.time.toEpochMilli(),it.rate) }
    }

    private suspend fun hrvSeries(start: Instant, end: Instant): List<MetricPoint> {
        val records=client.readRecords(ReadRecordsRequest(HeartRateVariabilityRmssdRecord::class,TimeRangeFilter.between(start,end))).records
        return records.filter { it.metadata.dataOrigin.packageName==GARMIN_PACKAGE }.sortedBy { it.time }.map { MetricPoint(it.time.toEpochMilli(),it.heartRateVariabilityMillis) }
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

    private fun makeCalendarText(start: Instant, end: Instant, total: Long, light: Long, deep: Long, rem: Long, awake: Long, sleeping: Long, hr: Double?, spo2: Double?, resp: Double?, minSpo2: Double?, minResp: Double?, avgHrv: Double?): String {
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
            if (minSpo2 != null) appendLine("🩸 Min. SpO₂: ${n(minSpo2, "%")}")
            appendLine("🫁 Ø Atmung: ${n(resp, "/min")}")
            if (minResp != null) appendLine("🫁 Min. Atmung: ${n(minResp, "/min")}")
            if (avgHrv != null) append("💓 Ø HRV: ${n(avgHrv, "ms")}")
        }
    }
}
