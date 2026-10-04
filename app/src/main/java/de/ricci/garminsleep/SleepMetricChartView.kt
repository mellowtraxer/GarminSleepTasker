package de.ricci.garminsleep

import android.content.Context
import android.graphics.*
import android.view.View
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class SleepMetricChartView(
    context: Context,
    private val tone: Int,
    private val label: String,
    private val startMs: Long,
    private val endMs: Long,
    private val points: List<MetricPoint> = emptyList()
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
        if(points.size >= 2) {
            val minV=points.minOf { it.value }; val maxV=points.maxOf { it.value }; val span=(maxV-minV).coerceAtLeast(1.0)
            points.forEachIndexed { i,pt ->
                val x=l+(r-l)*((pt.timeMs-startMs).toDouble()/(endMs-startMs).coerceAtLeast(1)).coerceIn(0.0,1.0).toFloat()
                val y=bottom-(bottom-top)*((pt.value-minV)/span).toFloat()
                if(i==0) path.moveTo(x,y) else path.lineTo(x,y)
            }
        } else {
            path.moveTo(l,(top+bottom)/2f); path.lineTo(r,(top+bottom)/2f)
        }
        p.style=Paint.Style.STROKE; p.strokeWidth=2.2f*d; p.color=tone; p.setShadowLayer(7*d,0f,0f,tone); setLayerType(LAYER_TYPE_SOFTWARE,p); c.drawPath(path,p); p.clearShadowLayer()
        p.style=Paint.Style.FILL; p.textSize=11*d; p.color=Color.rgb(130,140,169)
        c.drawText(tf.format(Instant.ofEpochMilli(startMs)),l,height-10*d,p)
        val end=tf.format(Instant.ofEpochMilli(endMs)); c.drawText(end,r-p.measureText(end),height-10*d,p)
        p.textSize=10*d; p.color=tone; c.drawText(label.uppercase(),l,12*d,p)
        if(points.size>=2) {
            val min=points.minOf { it.value }; val max=points.maxOf { it.value }
            p.textSize=10*d; p.color=Color.rgb(154,163,190)
            val maxText=String.format(java.util.Locale.GERMANY,"Max. %.1f",max)
            val minText=String.format(java.util.Locale.GERMANY,"Min. %.1f",min)
            c.drawText(maxText,r-p.measureText(maxText),12*d,p)
            c.drawText(minText,r-p.measureText(minText),bottom+16*d,p)
        } else {
            p.textSize=11*d; p.color=Color.rgb(120,130,158)
            val msg="Keine Zeitreihe für diese Nacht verfügbar"
            c.drawText(msg,l,(top+bottom)/2f-10*d,p)
        }
    }
}
