package de.ricci.garminsleep

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import java.time.ZoneId

class SleepSyncWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val ctx=applicationContext
        val prefs=ctx.getSharedPreferences("sleepsync_calendar",Context.MODE_PRIVATE)
        if(!prefs.getBoolean("auto_enabled",true)) return Result.success()
        val calendarId=prefs.getLong("calendar_id",-1)
        if(calendarId<0 || ctx.checkSelfPermission(Manifest.permission.READ_CALENDAR)!=PackageManager.PERMISSION_GRANTED || ctx.checkSelfPermission(Manifest.permission.WRITE_CALENDAR)!=PackageManager.PERMISSION_GRANTED) return Result.success()
        return try {
            val hc=HealthConnectClient.getOrCreate(ctx)
            val sleepPermission=HealthPermission.getReadPermission(SleepSessionRecord::class)
            if(sleepPermission !in hc.permissionController.getGrantedPermissions()) return Result.success()
            val latest=SleepReader(ctx).latestGarminSleep(36)
            val projection=arrayOf(CalendarContract.Events._ID)
            val selection=CalendarContract.Events.CALENDAR_ID+"=? AND "+CalendarContract.Events.DTSTART+"=? AND "+CalendarContract.Events.DTEND+"=? AND "+CalendarContract.Events.TITLE+"=?"
            val args=arrayOf(calendarId.toString(),latest.startMs.toString(),latest.endMs.toString(),"💤 Garmin Schlaf")
            val exists=ctx.contentResolver.query(CalendarContract.Events.CONTENT_URI,projection,selection,args,null)?.use{it.moveToFirst()}==true
            if(!exists){
                val values=android.content.ContentValues().apply{
                    put(CalendarContract.Events.CALENDAR_ID,calendarId)
                    put(CalendarContract.Events.TITLE,"💤 Garmin Schlaf")
                    put(CalendarContract.Events.DTSTART,latest.startMs)
                    put(CalendarContract.Events.DTEND,latest.endMs)
                    put(CalendarContract.Events.EVENT_TIMEZONE,ZoneId.systemDefault().id)
                    put(CalendarContract.Events.DESCRIPTION,latest.calendarText+"\n\nSleepSync")
                }
                if(ctx.contentResolver.insert(CalendarContract.Events.CONTENT_URI,values)==null) return Result.retry()
                prefs.edit().putLong("last_inserted_end",latest.endMs).apply()
            }
            Result.success()
        } catch(_:Throwable){ Result.retry() }
    }
}
