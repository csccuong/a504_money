package com.money.tracker.budget.planner.moneymanager.ui.common

import android.content.Context
import android.view.LayoutInflater
import android.widget.TextView
import com.money.tracker.budget.planner.moneymanager.R
import com.money.tracker.budget.planner.moneymanager.databinding.PickerMonthDialogBinding
import com.money.tracker.budget.planner.moneymanager.utils.YearMonthKey

object MonthPickerDialog {

    fun show(
        context: Context,
        currentMonth: YearMonthKey,
        onMonthSelected: (YearMonthKey) -> Unit
    ) {
        val binding = PickerMonthDialogBinding.inflate(LayoutInflater.from(context))
        val dialog = AppDialogHelper.createStyledDialog(context, binding.root, widthRatio = 0.90f)

        val shortMonths = context.resources.getStringArray(R.array.months_short)
        val todayMonth = YearMonthKey.current()
        var displayedYear = currentMonth.year
        var selectedMonthIndex = currentMonth.monthIndex.coerceIn(0, 11)

        val monthViews: List<TextView> = listOf(
            binding.monthJan, binding.monthFeb, binding.monthMar, binding.monthApr,
            binding.monthMay, binding.monthJun, binding.monthJul, binding.monthAug,
            binding.monthSep, binding.monthOct, binding.monthNov, binding.monthDec
        )

        val whiteColor = context.compatColor(R.color.white)
        val primaryColor = context.compatColor(R.color.brand_primary)
        val textPrimaryColor = context.compatColor(R.color.text_primary)

        fun renderSelection() {
            binding.tvYear.text = displayedYear.toString()
            monthViews.forEachIndexed { index, textView ->
                textView.text = shortMonths.getOrElse(index) { "${index + 1}" }
                val isSelected = index == selectedMonthIndex
                val isCurrentMonth = displayedYear == todayMonth.year && index == todayMonth.monthIndex
                when {
                    isSelected -> {
                        textView.setBackgroundResource(R.drawable.bg_selected_month)
                        textView.setTextColor(whiteColor)
                    }
                    isCurrentMonth -> {
                        textView.setBackgroundResource(R.drawable.bg_category_chip_selected)
                        textView.setTextColor(primaryColor)
                    }
                    else -> {
                        textView.setBackgroundResource(R.drawable.bg_category_chip_normal)
                        textView.setTextColor(textPrimaryColor)
                    }
                }
            }
        }

        renderSelection()

        binding.btnPreviousYear.setOnClickListener {
            displayedYear -= 1
            renderSelection()
        }
        binding.btnNextYear.setOnClickListener {
            displayedYear += 1
            renderSelection()
        }

        monthViews.forEachIndexed { index, textView ->
            textView.setOnClickListener {
                selectedMonthIndex = index
                renderSelection()
            }
        }

        binding.btnCancel.setOnClickListener { dialog.dismiss() }
        binding.btnSave.setOnClickListener {
            onMonthSelected(YearMonthKey(displayedYear, selectedMonthIndex))
            dialog.dismiss()
        }

        dialog.show()
    }
}
