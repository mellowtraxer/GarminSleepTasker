package de.ricci.garminsleep

import android.content.Context
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.widget.ScrollView
import kotlin.math.abs

/**
 * Vertical scrolling owns the gesture by default. A deliberate horizontal swipe
 * is intercepted only after direction lock; child charts may opt out via
 * requestDisallowInterceptTouchEvent.
 */
class PremiumSwipeScrollView(context: Context) : ScrollView(context) {
    var onPageSwipe: ((Int) -> Unit)? = null
    private val slop = ViewConfiguration.get(context).scaledTouchSlop.toFloat()
    private var downX = 0f
    private var downY = 0f
    private var downTime = 0L
    private var horizontal = false
    private var vertical = false

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = ev.x; downY = ev.y; downTime = ev.eventTime
                horizontal = false; vertical = false
                super.onInterceptTouchEvent(ev)
                return false
            }
            MotionEvent.ACTION_MOVE -> {
                if (vertical) return super.onInterceptTouchEvent(ev)
                val dx = ev.x - downX
                val dy = ev.y - downY
                if (!horizontal && abs(dy) > slop && abs(dy) > abs(dx) * 0.85f) {
                    vertical = true
                    return super.onInterceptTouchEvent(ev)
                }
                if (!horizontal && abs(dx) > slop * 2.5f && abs(dx) > abs(dy) * 1.8f) {
                    horizontal = true
                    parent?.requestDisallowInterceptTouchEvent(true)
                    return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                horizontal = false; vertical = false
            }
        }
        return if (horizontal) true else super.onInterceptTouchEvent(ev)
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        if (!horizontal) return super.onTouchEvent(ev)
        when (ev.actionMasked) {
            MotionEvent.ACTION_UP -> {
                val dx = ev.x - downX
                val dy = ev.y - downY
                val elapsed = (ev.eventTime - downTime).coerceAtLeast(1L)
                val speed = abs(dx) * 1000f / elapsed
                val deliberate = abs(dx) > width * 0.22f ||
                    (abs(dx) > slop * 8f && speed > 950f)
                if (deliberate && abs(dx) > abs(dy) * 1.5f) {
                    onPageSwipe?.invoke(if (dx < 0f) 1 else -1)
                }
                horizontal = false; vertical = false
                parent?.requestDisallowInterceptTouchEvent(false)
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                horizontal = false; vertical = false
                parent?.requestDisallowInterceptTouchEvent(false)
                return true
            }
        }
        return true
    }
}
