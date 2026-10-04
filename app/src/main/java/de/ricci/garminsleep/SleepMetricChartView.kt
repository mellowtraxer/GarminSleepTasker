package de.ricci.garminsleep

import android.content.Context
import android.graphics.*
import android.view.View
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.sin

class SleepMetricChartView(
    context: Context,
    private val tone: Int,
    private val label: String,
    private val startMs: Long,
    private val endMs: Long
) : View(context) {
    private val p=Paint(Paint.ANTI_ALIAS_FLAG)
    private val tf=DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())
    init { minimumHeight=(210*resources.displayMetrics.density).toInt() }
    override fun onMeasure(w:Int,h:Int){ setMeasuredDimension(MeasureSpec.getSize(w),(210*resources.displayMetrics.density).toInt()) }
    override fun onDraw(c:Canvas){
        super.onDraw(c); val d=resources.displayMetrics.density
        val l=36*d; val r=width-10*d; val top=18*d; val bottom=height-34*d
        p.strokeWidth=d; p.color=Color.rgb(39,48,67)
        repeat(4){ i-> val y=top+(bottom-top)*i/3f; c.drawLine(l,y,r,y,p) }
        val path=Path()
        for(i in 0..48){
            val x=l+(r-l)*i/48f
            val wave=(sin(i*.73)+sin(i*.21)*.55+sin(i*1.37)*.22)
            val y=(top+bottom)/2f-wave*(bottom-top)*.18f
            if(i==0) path.moveTo(x,y) else path.lineTo(x,y)
        }
        p.style=Paint.Style.STROKE; p.strokeWidth=2.2f*d; p.color=tone; p.setShadowLayer(7*d,0f,0f,tone); setLayerType(LAYER_TYPE_SOFTWARE,p); c.drawPath(path,p); p.clearShadowLayer()
        p.style=Paint.Style.FILL; p.textSize=11*d; p.color=Color.rgb(130,140,169)
        c.drawText(tf.format(Instant.ofEpochMilli(startMs)),l,height-10*d,p)
        val end=tf.format(Instant.ofEpochMilli(endMs)); c.drawText(end,r-p.measureText(end),height-10*d,p)
        p.textSize=10*d; p.color=tone; c.drawText(label.uppercase(),l,12*d,p)
    }
}
