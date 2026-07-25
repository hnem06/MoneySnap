package com.devpro58.hnem06.moneysnap.presentation.stats

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.devpro58.hnem06.moneysnap.R

class CategoryDonutChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    data class Segment(
        val color: Int,
        val amount: Long
    )

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.progress_track)
        style = Paint.Style.STROKE
        strokeWidth = dp(12f)
        strokeCap = Paint.Cap.ROUND
    }

    private val segmentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(12f)
        strokeCap = Paint.Cap.ROUND
    }

    private var segments: List<Segment> = emptyList()

    fun setSegments(newSegments: List<Segment>) {
        segments = newSegments.filter { it.amount > 0 }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val size = minOf(width, height).toFloat()
        val padding = dp(14f)
        val rect = RectF(padding, padding, size - padding, size - padding)

        canvas.drawArc(rect, 0f, 360f, false, trackPaint)

        val total = segments.sumOf { it.amount }.coerceAtLeast(1L)
        var startAngle = -90f

        segments.forEach { segment ->
            val sweep = (segment.amount.toFloat() / total.toFloat()) * 360f
            segmentPaint.color = segment.color
            canvas.drawArc(rect, startAngle, sweep, false, segmentPaint)
            startAngle += sweep
        }
    }

    private fun dp(value: Float): Float =
        value * resources.displayMetrics.density
}
