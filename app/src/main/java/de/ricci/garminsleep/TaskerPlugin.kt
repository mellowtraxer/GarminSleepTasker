package de.ricci.garminsleep

import android.app.Activity
import android.content.Context
import android.os.Bundle
import com.joaomgcd.taskerpluginlibrary.action.TaskerPluginRunnerActionNoInput
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfig
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfigHelperNoInput
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfigNoInput
import com.joaomgcd.taskerpluginlibrary.input.TaskerInput
import com.joaomgcd.taskerpluginlibrary.output.TaskerOutputObject
import com.joaomgcd.taskerpluginlibrary.output.TaskerOutputVariable
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResult
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultSucess
import kotlinx.coroutines.runBlocking

@TaskerOutputObject
class GarminSleepOutput(
    @get:TaskerOutputVariable("start_ms", labelResIdName="start_ms", htmlLabelResIdName="start_ms_desc") val startMs: Long?,
    @get:TaskerOutputVariable("end_ms", labelResIdName="end_ms", htmlLabelResIdName="end_ms_desc") val endMs: Long?,
    @get:TaskerOutputVariable("total_min", labelResIdName="total_min", htmlLabelResIdName="total_min_desc") val totalMin: Long?,
    @get:TaskerOutputVariable("light_min", labelResIdName="light_min", htmlLabelResIdName="light_min_desc") val lightMin: Long?,
    @get:TaskerOutputVariable("deep_min", labelResIdName="deep_min", htmlLabelResIdName="deep_min_desc") val deepMin: Long?,
    @get:TaskerOutputVariable("rem_min", labelResIdName="rem_min", htmlLabelResIdName="rem_min_desc") val remMin: Long?,
    @get:TaskerOutputVariable("awake_min", labelResIdName="awake_min", htmlLabelResIdName="awake_min_desc") val awakeMin: Long?,
    @get:TaskerOutputVariable("sleeping_min", labelResIdName="sleeping_min", htmlLabelResIdName="sleeping_min_desc") val sleepingMin: Long?,
    @get:TaskerOutputVariable("avg_hr", labelResIdName="avg_hr", htmlLabelResIdName="avg_hr_desc") val avgHr: Double?,
    @get:TaskerOutputVariable("avg_spo2", labelResIdName="avg_spo2", htmlLabelResIdName="avg_spo2_desc") val avgSpo2: Double?,
    @get:TaskerOutputVariable("avg_resp", labelResIdName="avg_resp", htmlLabelResIdName="avg_resp_desc") val avgResp: Double?,
    @get:TaskerOutputVariable("min_spo2", labelResIdName="avg_spo2", htmlLabelResIdName="avg_spo2_desc") val minSpo2: Double?,
    @get:TaskerOutputVariable("min_resp", labelResIdName="avg_resp", htmlLabelResIdName="avg_resp_desc") val minResp: Double?,
    @get:TaskerOutputVariable("avg_hrv", labelResIdName="avg_hr", htmlLabelResIdName="avg_hr_desc") val avgHrv: Double?,
    @get:TaskerOutputVariable("source", labelResIdName="source", htmlLabelResIdName="source_desc") val source: String?,
    @get:TaskerOutputVariable("calendar_text", labelResIdName="calendar_text", htmlLabelResIdName="calendar_text_desc") val calendarText: String?
)

class GarminSleepRunner : TaskerPluginRunnerActionNoInput<GarminSleepOutput>() {
    override fun run(context: Context, input: TaskerInput<Unit>): TaskerPluginResult<GarminSleepOutput> = runBlocking {
        val s = SleepReader(context).latestGarminSleep()
        TaskerPluginResultSucess(GarminSleepOutput(s.startMs,s.endMs,s.totalMin,s.lightMin,s.deepMin,s.remMin,s.awakeMin,s.sleepingMin,s.avgHr,s.avgSpo2,s.avgResp,s.minSpo2,s.minResp,s.avgHrv,s.source,s.calendarText))
    }
}

class GarminSleepHelper(config: TaskerPluginConfig<Unit>) : TaskerPluginConfigHelperNoInput<GarminSleepOutput, GarminSleepRunner>(config) {
    override val runnerClass = GarminSleepRunner::class.java
    override val outputClass = GarminSleepOutput::class.java
    override fun addToStringBlurb(input: TaskerInput<Unit>, blurbBuilder: StringBuilder) { blurbBuilder.append("Neueste Garmin-Schlafsession aus Health Connect") }
}

class TaskerSleepActivity : Activity(), TaskerPluginConfigNoInput {
    override val context get() = applicationContext
    private val helper by lazy { GarminSleepHelper(this) }
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); helper.finishForTasker() }
}
