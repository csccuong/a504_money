package com.money.tracker.budget.planner.moneymanager.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.money.tracker.budget.planner.moneymanager.R

class CircularProgressView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = DEFAULT_STROKE_WIDTH
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = TEXT_COLOR
        textSize = DEFAULT_TEXT_SIZE
        textAlign = Paint.Align.CENTER
    }

    private val arcBounds = RectF()
    private var progress: Int = 75
    private var progressColor: Int = DEFAULT_PROGRESS_COLOR
    private var showRemainingText: Boolean = true

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val padding = arcPaint.strokeWidth
        arcBounds.set(padding, padding, w - padding, h - padding)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        arcPaint.strokeWidth = if (showRemainingText) DEFAULT_STROKE_WIDTH else COMPACT_STROKE_WIDTH
        arcPaint.color = Color.WHITE
        canvas.drawArc(arcBounds, 0f, 360f, false, arcPaint)

        arcPaint.color = progressColor
        val sweepAngle = (360f * progress) / 100f
        canvas.drawArc(arcBounds, -90f, sweepAngle, false, arcPaint)

        val centerX = width / 2f
        val centerY = height / 2f
        textPaint.color = TEXT_COLOR

        if (showRemainingText) {
            textPaint.textSize = DEFAULT_TEXT_SIZE
            canvas.drawText(context.getString(R.string.remaining), centerX, centerY - 20f, textPaint)
            canvas.drawText("$progress%", centerX, centerY + 40f, textPaint)
        } else {
            textPaint.textSize = COMPACT_TEXT_SIZE
            canvas.drawText("$progress%", centerX, centerY + 20f, textPaint)
        }
    }

    fun setProgress(progress: Int) {
        this.progress = progress
        invalidate()
    }

    fun setProgressColor(color: Int) {
        this.progressColor = color
        invalidate()
    }

    fun setShowRemainingText(show: Boolean) {
        this.showRemainingText = show
        invalidate()
    }

    private companion object {
        const val DEFAULT_STROKE_WIDTH = 70f
        const val COMPACT_STROKE_WIDTH = 20f
        const val DEFAULT_TEXT_SIZE = 20f
        const val COMPACT_TEXT_SIZE = 30f
        val DEFAULT_PROGRESS_COLOR = Color.parseColor("#6072D6")
        val TEXT_COLOR = Color.parseColor("#1F2124")
    }
}
