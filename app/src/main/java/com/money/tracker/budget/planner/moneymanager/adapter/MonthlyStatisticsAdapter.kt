package com.money.tracker.budget.planner.moneymanager.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.money.tracker.budget.planner.moneymanager.R
import com.money.tracker.budget.planner.moneymanager.databinding.ItemMonthlyStatisticsBinding
import com.money.tracker.budget.planner.moneymanager.model.MonthlyStatisticsModel
import com.money.tracker.budget.planner.moneymanager.ui.common.compatColor
import com.money.tracker.budget.planner.moneymanager.utils.FormatUtils
import kotlin.math.abs

class MonthlyStatisticsAdapter(
    private val onMonthClick: (monthIndex: Int, item: MonthlyStatisticsModel) -> Unit
) : RecyclerView.Adapter<MonthlyStatisticsAdapter.StatisticsViewHolder>() {

    enum class DisplayMode { CASHFLOW, LOAN }

    private var monthlyStatistics: List<MonthlyStatisticsModel> = emptyList()
    private var displayMode: DisplayMode = DisplayMode.CASHFLOW

    @SuppressLint("NotifyDataSetChanged")
    fun submitData(items: List<MonthlyStatisticsModel>, mode: DisplayMode) {
        monthlyStatistics = items
        displayMode = mode
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StatisticsViewHolder {
        val binding = ItemMonthlyStatisticsBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return StatisticsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StatisticsViewHolder, position: Int) {
        holder.bind(monthlyStatistics[position], position, displayMode)
    }

    override fun getItemCount(): Int = monthlyStatistics.size

    inner class StatisticsViewHolder(
        private val binding: ItemMonthlyStatisticsBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: MonthlyStatisticsModel, position: Int, mode: DisplayMode) {
            val ctx = binding.root.context
            binding.tvMonth.text = item.month

            if (mode == DisplayMode.CASHFLOW) {
                binding.tvCol2.text = formatPositiveOrDash(item.income)
                binding.tvCol3.text = formatPositiveOrDash(item.expend)
                val net = item.balance
                binding.tvCol4.text = formatSignedOrDash(net, item.income > 0.0 || item.expend > 0.0)
                binding.tvCol4.setTextColor(
                    ctx.compatColor(
                        when {
                            net > 0 -> R.color.semantic_income
                            net < 0 -> R.color.semantic_expense
                            else -> R.color.text_muted
                        }
                    )
                )
            } else {
                binding.tvCol2.text = formatPositiveOrDash(item.loan)
                binding.tvCol3.text = formatPositiveOrDash(item.borrow)
                val netLoan = item.loan - item.borrow
                binding.tvCol4.text = formatSignedOrDash(netLoan, item.loan > 0.0 || item.borrow > 0.0)
                binding.tvCol4.setTextColor(
                    ctx.compatColor(
                        when {
                            netLoan > 0 -> R.color.semantic_income
                            netLoan < 0 -> R.color.semantic_expense
                            else -> R.color.text_muted
                        }
                    )
                )
            }

            binding.root.setBackgroundColor(
                ctx.compatColor(if (position % 2 == 0) R.color.surface_card else R.color.surface_input)
            )
            binding.root.setOnClickListener { onMonthClick(position, item) }
        }

        private fun formatPositiveOrDash(value: Double): String =
            if (value > 0.0) FormatUtils.formatCompactAmount(value) else "-"

        private fun formatSignedOrDash(value: Double, hasData: Boolean): String {
            if (!hasData) return "-"
            val prefix = if (value > 0.0) "+" else if (value < 0.0) "-" else ""
            return "$prefix${FormatUtils.formatCompactAmount(abs(value))}"
        }
    }
}
