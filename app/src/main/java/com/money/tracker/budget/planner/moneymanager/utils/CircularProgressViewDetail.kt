package com.money.tracker.budget.planner.moneymanager.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import com.money.tracker.budget.planner.moneymanager.R

class CircularProgressViewDetail @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val strokePx = dpToPx(10f)

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = strokePx
        strokeCap = Paint.Cap.ROUND
        color = TRACK_COLOR
    }

    private val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = strokePx
        strokeCap = Paint.Cap.ROUND
        color = DEFAULT_PROGRESS_COLOR
    }

    private val primaryTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = TEXT_PRIMARY
        textSize = spToPx(18f)
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    private val secondaryTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = TEXT_SECONDARY
        textSize = spToPx(11f)
        textAlign = Paint.Align.CENTER
    }

    private val arcBounds = RectF()
    private var progress: Int = 0
    private var progressColor: Int = DEFAULT_PROGRESS_COLOR
    private var centerSubLabel: String? = null
    private var showRemainingText: Boolean = true

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val inset = strokePx / 2f + dpToPx(2f)
        arcBounds.set(inset, inset, w - inset, h - inset)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.drawArc(arcBounds, 0f, 360f, false, trackPaint)

        arcPaint.color = progressColor
        val clampedSweep = (360f * progress.coerceIn(0, 100)) / 100f
        if (clampedSweep > 0f) {
            canvas.drawArc(arcBounds, -90f, clampedSweep, false, arcPaint)
        }

        val centerX = width / 2f
        val centerY = height / 2f

        val subLabel = centerSubLabel ?: if (showRemainingText) context.getString(R.string.remaining) else null
        if (!subLabel.isNullOrBlank()) {
            val primaryY = centerY - dpToPx(2f)
            val secondaryY = centerY + dpToPx(15f)
            canvas.drawText("$progress%", centerX, primaryY, primaryTextPaint)
            canvas.drawText(subLabel, centerX, secondaryY, secondaryTextPaint)
        } else {
            val baseline = centerY - (primaryTextPaint.descent() + primaryTextPaint.ascent()) / 2f
            canvas.drawText("$progress%", centerX, baseline, primaryTextPaint)
        }
    }

    fun setProgress(progress: Int) {
        this.progress = progress.coerceAtLeast(0)
        invalidate()
    }

    fun setProgressColor(color: Int) {
        this.progressColor = color
        invalidate()
    }

    fun setCenterSubLabel(label: String?) {
        this.centerSubLabel = label
        invalidate()
    }

    fun setShowRemainingText(show: Boolean) {
        this.showRemainingText = show
        invalidate()
    }

    private fun dpToPx(dp: Float): Float =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.displayMetrics)

    private fun spToPx(sp: Float): Float =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, resources.displayMetrics)

    private companion object {
        val DEFAULT_PROGRESS_COLOR = Color.parseColor("#5568D8")
        val TRACK_COLOR = Color.parseColor("#EAEFF8")
        val TEXT_PRIMARY = Color.parseColor("#141926")
        val TEXT_SECONDARY = Color.parseColor("#647087")
    }
}
