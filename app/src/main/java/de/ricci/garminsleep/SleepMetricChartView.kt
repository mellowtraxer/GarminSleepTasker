package de.ricci.garminsleep

import android.content.Context
import android.graphics.*
import android.view.View
import android.view.MotionEvent
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
    private var selectedIndex: Int? = null
    private var touchX = 0f
    private val fillPaint=Paint(Paint.ANTI_ALIAS_FLAG)
    init { minimumHeight=(210*resources.displayMetrics.density).toInt() }
    override fun onMeasure(w:Int,h:Int){ setMeasuredDimension(MeasureSpec.getSize(w),(210*resources.displayMetrics.density).toInt()) }
    private fun formatValue(v: Double): String =
        if(kotlin.math.abs(v-kotlin.math.round(v)) < 0.05) kotlin.math.round(v).toInt().toString()
        else String.format(java.util.Locale.GERMANY,"%.1f",v)

    override fun onTouchEvent(e: MotionEvent): Boolean {
        if(points.isEmpty()) return super.onTouchEvent(e)
        when(e.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                val d=resources.displayMetrics.density; val l=36*d; val r=width-10*d
                val fraction=((e.x-l)/(r-l)).coerceIn(0f,1f)
                val target=startMs+((endMs-startMs)*fraction).toLong()
                selectedIndex=points.indices.minByOrNull { kotlin.math.abs(points[it].timeMs-target) }
                touchX=e.x
                parent?.requestDisallowInterceptTouchEvent(e.action==MotionEvent.ACTION_MOVE)
                invalidate(); return true
            }
            MotionEvent.ACTION_UP -> { performClick(); parent?.requestDisallowInterceptTouchEvent(false); return true }
            MotionEvent.ACTION_CANCEL -> { parent?.requestDisallowInterceptTouchEvent(false); return true }
        }
        return true
    }
    override fun performClick(): Boolean { super.performClick(); return true }
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
        if(points.size>=2) {
            val fill=Path(path); fill.lineTo(r,bottom); fill.lineTo(l,bottom); fill.close()
            fillPaint.style=Paint.Style.FILL
            fillPaint.shader=LinearGradient(0f,top,0f,bottom,Color.argb(90,Color.red(tone),Color.green(tone),Color.blue(tone)),Color.TRANSPARENT,Shader.TileMode.CLAMP)
            c.drawPath(fill,fillPaint); fillPaint.shader=null
        }
        p.style=Paint.Style.STROKE; p.strokeWidth=2.2f*d; p.color=tone; p.setShadowLayer(7*d,0f,0f,tone); setLayerType(LAYER_TYPE_SOFTWARE,p); c.drawPath(path,p); p.clearShadowLayer()
        p.style=Paint.Style.FILL; p.textSize=11*d; p.color=Color.rgb(130,140,169)
        c.drawText(tf.format(Instant.ofEpochMilli(startMs)),l,height-10*d,p)
        val end=tf.format(Instant.ofEpochMilli(endMs)); c.drawText(end,r-p.measureText(end),height-10*d,p)
        p.textSize=10*d; p.color=tone; c.drawText(label.uppercase(),l,12*d,p)
        if(points.size>=2) {
            val midMs=startMs+(endMs-startMs)/2
            val mid=tf.format(Instant.ofEpochMilli(midMs)); p.textSize=10*d; p.color=Color.rgb(105,115,145)
            c.drawText(mid,(l+r)/2-p.measureText(mid)/2,height-10*d,p)
            val min=points.minOf { it.value }; val max=points.maxOf { it.value }
            p.textSize=10*d; p.color=Color.rgb(154,163,190)
            val maxText="Max. "+formatValue(max)
            val minText="Min. "+formatValue(min)
            c.drawText(maxText,r-p.measureText(maxText),12*d,p)
            c.drawText(minText,r-p.measureText(minText),bottom+16*d,p)
        } else {
            p.textSize=11*d; p.color=Color.rgb(120,130,158)
            val msg="Keine Zeitreihe für diese Nacht verfügbar"
            c.drawText(msg,l,(top+bottom)/2f-10*d,p)
        }
        selectedIndex?.takeIf { it in points.indices && points.size>=2 }?.let { idx ->
            val pt=points[idx]; val minV=points.minOf { it.value }; val maxV=points.maxOf { it.value }; val span=(maxV-minV).coerceAtLeast(1.0)
            val x=l+(r-l)*((pt.timeMs-startMs).toDouble()/(endMs-startMs).coerceAtLeast(1)).coerceIn(0.0,1.0).toFloat()
            val y=bottom-(bottom-top)*((pt.value-minV)/span).toFloat()
            p.strokeWidth=d; p.color=Color.argb(150,Color.red(tone),Color.green(tone),Color.blue(tone)); c.drawLine(x,top,x,bottom,p)
            p.style=Paint.Style.FILL; p.color=tone; c.drawCircle(x,y,5*d,p); p.color=Color.WHITE; c.drawCircle(x,y,2*d,p)
            val unit=when(label) { "Puls"->"bpm"; "SpO₂"->"%"; "Atmung"->"/min"; "HRV"->"ms"; else->"" }
            val info=tf.format(Instant.ofEpochMilli(pt.timeMs))+"  ·  "+formatValue(pt.value)+" "+unit
            p.textSize=11*d; p.typeface=Typeface.DEFAULT_BOLD; val tw=p.measureText(info); val bx=(x-tw/2-10*d).coerceIn(l,r-tw-20*d)
            p.setShadowLayer(10*d,0f,3*d,Color.argb(120,Color.red(tone),Color.green(tone),Color.blue(tone)))
            p.color=Color.rgb(18,21,38); c.drawRoundRect(bx,top+7*d,bx+tw+20*d,top+35*d,14*d,14*d,p); p.clearShadowLayer()
            p.style=Paint.Style.STROKE; p.strokeWidth=d; p.color=Color.argb(180,Color.red(tone),Color.green(tone),Color.blue(tone))
            c.drawRoundRect(bx,top+7*d,bx+tw+20*d,top+35*d,14*d,14*d,p)
            p.style=Paint.Style.FILL; p.color=Color.WHITE; c.drawText(info,bx+10*d,top+26*d,p); p.typeface=Typeface.DEFAULT
        }
    }
}
