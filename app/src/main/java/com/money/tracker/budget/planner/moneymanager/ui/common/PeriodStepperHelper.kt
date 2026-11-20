package com.money.tracker.budget.planner.moneymanager.ui.common

import com.money.tracker.budget.planner.moneymanager.databinding.ViewPeriodStepperBinding

object PeriodStepperHelper {

    fun bind(
        stepperBinding: ViewPeriodStepperBinding,
        onPrevious: () -> Unit,
        onNext: () -> Unit,
        onLabelClick: (() -> Unit)? = null
    ) {
        stepperBinding.btnPrevPeriod.setOnClickListener { onPrevious() }
        stepperBinding.btnNextPeriod.setOnClickListener { onNext() }
        stepperBinding.tvPeriodLabel.setOnClickListener { onLabelClick?.invoke() }
    }

    fun setLabel(stepperBinding: ViewPeriodStepperBinding, text: String) {
        stepperBinding.tvPeriodLabel.text = text
    }
}
