package com.money.tracker.budget.planner.moneymanager.ui.common

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.money.tracker.budget.planner.moneymanager.R
import com.money.tracker.budget.planner.moneymanager.databinding.BottomSheetPeriodModeBinding
import com.money.tracker.budget.planner.moneymanager.model.StatisticsFilter
import com.money.tracker.budget.planner.moneymanager.model.StatisticsPeriodType

object StatisticsFilterSheetHelper {

    fun showDateRangePicker(
        context: Context,
        currentFilter: StatisticsFilter,
        onRangeSelected: (startLocalMillis: Long, endLocalMillis: Long) -> Unit
    ) {
        val (curStart, curEnd) = currentFilter.dateRangeMillis()
        AppPickerDialogHelper.showDateRangePicker(
            context = context,
            initialStartMillis = curStart,
            initialEndMillis = curEnd,
            onRangeSelected = onRangeSelected
        )
    }

    fun showPeriodSelector(
        context: Context,
        currentFilter: StatisticsFilter,
        onFilterSelected: (StatisticsFilter) -> Unit
    ) {
        val dialog = BottomSheetDialog(context, R.style.AppBottomSheetDialogTheme)
        val binding = BottomSheetPeriodModeBinding.inflate(LayoutInflater.from(context))
        dialog.setContentView(binding.root)

        binding.tvSubWeek.text = currentFilter.copy(periodType = StatisticsPeriodType.WEEK).displayPeriodLabel(context)
        binding.tvSubMonth.text = currentFilter.anchorMonth.displayLabel(context)
        binding.tvSubYear.text = currentFilter.anchorYear.toString()
        binding.tvSubCustom.text = if (currentFilter.periodType == StatisticsPeriodType.CUSTOM) {
            currentFilter.displayPeriodLabel(context)
        } else {
            context.getString(R.string.select_date_range)
        }

        if (currentFilter.isCurrentPeriod()) {
            binding.btnResetCurrent.gone()
        } else {
            binding.btnResetCurrent.show()
        }

        fun bindSelection(row: View, title: TextView, check: ImageView, selected: Boolean) {
            row.setBackgroundResource(
                if (selected) R.drawable.bg_category_chip_selected else R.drawable.bg_category_chip_normal
            )
            title.setTextColor(
                context.compatColor(if (selected) R.color.brand_primary else R.color.text_primary)
            )
            if (selected) check.show() else check.gone()
        }

        val activeType = currentFilter.periodType
        bindSelection(binding.rowPeriodWeek, binding.tvTitleWeek, binding.ivCheckWeek, activeType == StatisticsPeriodType.WEEK)
        bindSelection(binding.rowPeriodMonth, binding.tvTitleMonth, binding.ivCheckMonth, activeType == StatisticsPeriodType.MONTH)
        bindSelection(binding.rowPeriodYear, binding.tvTitleYear, binding.ivCheckYear, activeType == StatisticsPeriodType.YEAR)

        val isCustom = activeType == StatisticsPeriodType.CUSTOM
        bindSelection(binding.rowPeriodCustom, binding.tvTitleCustom, binding.ivCheckCustom, isCustom)
        if (isCustom) binding.ivArrowCustom.gone() else binding.ivArrowCustom.show()

        binding.btnResetCurrent.setOnClickListener {
            dialog.dismiss()
            onFilterSelected(currentFilter.resetPeriodToCurrent())
        }

        binding.rowPeriodWeek.setOnClickListener {
            dialog.dismiss()
            onFilterSelected(currentFilter.copy(periodType = StatisticsPeriodType.WEEK))
        }

        binding.rowPeriodMonth.setOnClickListener {
            dialog.dismiss()
            onFilterSelected(currentFilter.copy(periodType = StatisticsPeriodType.MONTH))
        }

        binding.rowPeriodYear.setOnClickListener {
            dialog.dismiss()
            onFilterSelected(currentFilter.copy(periodType = StatisticsPeriodType.YEAR))
        }

        binding.rowPeriodCustom.setOnClickListener {
            dialog.dismiss()
            showDateRangePicker(context, currentFilter) { startLocal, endLocal ->
                onFilterSelected(
                    currentFilter.copy(
                        periodType = StatisticsPeriodType.CUSTOM,
                        customStartMillis = startLocal,
                        customEndMillis = endLocal
                    )
                )
            }
        }

        dialog.setOnShowListener {
            dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
            dialog.behavior.skipCollapsed = true
        }
        dialog.show()
    }
}
