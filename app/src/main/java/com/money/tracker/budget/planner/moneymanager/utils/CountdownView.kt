package com.money.tracker.budget.planner.moneymanager.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class CountdownView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        color = 0xFFFF5722.toInt()
    }

    private val ovalBounds = RectF()
    private var sweepAngle: Float = 360f

    fun setSweepAngle(angle: Float) {
        sweepAngle = angle
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val padding = strokePaint.strokeWidth / 2f
        ovalBounds.set(padding, padding, width - padding, height - padding)
        canvas.drawArc(ovalBounds, 270f, -sweepAngle, false, strokePaint)
    }
}
