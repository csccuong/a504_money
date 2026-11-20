package com.money.tracker.budget.planner.moneymanager.ui.common

import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.animation.DecelerateInterpolator
import android.widget.ProgressBar

class SplashProgressBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.progressBarStyleHorizontal
) : ProgressBar(context, attrs, defStyleAttr) {

    private var progressAnimator: ValueAnimator? = null

    init {
        max = 100
        progress = 15
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        startLoadingAnimation()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        progressAnimator?.cancel()
    }

    private fun startLoadingAnimation() {
        progressAnimator?.cancel()
        progressAnimator = ValueAnimator.ofInt(15, 95).apply {
            duration = 2800L
            interpolator = DecelerateInterpolator()
            addUpdateListener { animator ->
                progress = animator.animatedValue as Int
            }
            start()
        }
    }
}
