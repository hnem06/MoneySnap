package com.devpro58.hnem06.moneysnap.presentation.stats

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.devpro58.hnem06.moneysnap.R

class SpendingLineChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.secondary_green)
        strokeWidth = dp(3f)
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val pointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.text_dark)
        style = Paint.Style.FILL
    }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.border_gray)
        strokeWidth = dp(1f)
        alpha = 130
    }

    private var values: List<Long> = emptyList()

    fun setValues(newValues: List<Long>) {
        values = newValues
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val widthAvailable = width - paddingLeft - paddingRight
        val heightAvailable = height - paddingTop - paddingBottom
        if (widthAvailable <= 0 || heightAvailable <= 0) return

        val left = paddingLeft.toFloat()
        val top = paddingTop.toFloat()
        val right = (width - paddingRight).toFloat()
        val bottom = (height - paddingBottom).toFloat()

        canvas.drawLine(left, bottom, right, bottom, gridPaint)

        val chartValues = values.ifEmpty { listOf(0L, 0L) }
        val max = chartValues.maxOrNull()?.coerceAtLeast(1L) ?: 1L
        val step = if (chartValues.size <= 1) widthAvailable.toFloat() else widthAvailable.toFloat() / (chartValues.size - 1)

        val points = chartValues.mapIndexed { index, amount ->
            val x = left + step * index
            val ratio = amount.toFloat() / max
            val y = bottom - (heightAvailable * ratio)
            x to y
        }

        val linePath = Path()
        points.forEachIndexed { index, (x, y) ->
            if (index == 0) linePath.moveTo(x, y) else linePath.lineTo(x, y)
        }

        val fillPath = Path(linePath).apply {
            lineTo(points.last().first, bottom)
            lineTo(points.first().first, bottom)
            close()
        }

        fillPaint.shader = LinearGradient(
            0f,
            top,
            0f,
            bottom,
            ContextCompat.getColor(context, R.color.secondary_green).withAlpha(38),
            Color.TRANSPARENT,
            Shader.TileMode.CLAMP
        )

        canvas.drawPath(fillPath, fillPaint)
        canvas.drawPath(linePath, linePaint)

        points.filterIndexed { index, _ ->
            index == points.lastIndex || index % 7 == 0
        }.forEach { (x, y) ->
            canvas.drawCircle(x, y, dp(4f), pointPaint)
        }
    }

    private fun Int.withAlpha(alpha: Int): Int =
        Color.argb(alpha, Color.red(this), Color.green(this), Color.blue(this))

    private fun dp(value: Float): Float =
        value * resources.displayMetrics.density
}
