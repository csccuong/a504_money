package com.money.tracker.budget.planner.moneymanager.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.money.tracker.budget.planner.moneymanager.R
import com.money.tracker.budget.planner.moneymanager.data.BudgetCardState
import com.money.tracker.budget.planner.moneymanager.data.CategoryCatalog
import com.money.tracker.budget.planner.moneymanager.databinding.ItemBudgetBinding
import com.money.tracker.budget.planner.moneymanager.ui.common.compatColor
import com.money.tracker.budget.planner.moneymanager.utils.FormatUtils
import kotlin.math.abs

class BudgetAdapter(
    private val onBudgetClick: (BudgetCardState) -> Unit
) : RecyclerView.Adapter<BudgetAdapter.BudgetViewHolder>() {

    private var states: List<BudgetCardState> = emptyList()
    private var currency: String = "$"

    @SuppressLint("NotifyDataSetChanged")
    fun submitStates(newStates: List<BudgetCardState>, currencySymbol: String) {
        states = newStates
        currency = currencySymbol
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BudgetViewHolder {
        val binding = ItemBudgetBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BudgetViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BudgetViewHolder, position: Int) {
        holder.bind(states[position])
    }

    override fun getItemCount(): Int = states.size

    inner class BudgetViewHolder(
        private val binding: ItemBudgetBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(state: BudgetCardState) {
            val ctx = binding.root.context
            val item = state.item

            val iconRes = if (item.isCategoryLinked) {
                CategoryCatalog.iconForCategory(item.categoryName)
            } else {
                R.drawable.ic_budget
            }
            binding.ivBudgetIcon.setImageResource(iconRes)
            binding.tvBudgetName.text = item.displayTitle(ctx)

            val subtitle = if (item.isCategoryLinked) {
                "${ctx.getString(R.string.auto_tracked)} • ${ctx.getString(R.string.transactions_count, state.transactionCount)}"
            } else {
                "${ctx.getString(R.string.custom_budget)} • ${ctx.getString(R.string.transactions_count, state.transactionCount)}"
            }
            binding.tvBudgetSubtitle.text = subtitle

            val pct = state.usedPercentInt
            val (accentColorRes, badgeBgRes) = when {
                state.isOverBudget -> R.color.semantic_expense to R.drawable.bg_badge_red
                pct >= 80 -> R.color.semantic_warning to R.drawable.bg_badge_amber
                else -> R.color.semantic_income to R.drawable.bg_badge_green
            }
            val accentColor = ctx.compatColor(accentColorRes)

            binding.tvBudgetPercent.text = "$pct%"
            binding.tvBudgetPercent.setTextColor(accentColor)
            binding.tvBudgetPercent.setBackgroundResource(badgeBgRes)

            binding.progressBudgetBar.setIndicatorColor(accentColor)
            binding.progressBudgetBar.setProgressCompat(pct.coerceIn(0, 100), false)

            binding.tvBudgetAmount.text = ctx.getString(
                R.string.spent_of_value,
                "$currency${FormatUtils.formatAmount(state.spent)}",
                "$currency${FormatUtils.formatAmount(state.limit)}"
            )

            if (state.isOverBudget) {
                binding.tvBudgetRemaining.text = ctx.getString(
                    R.string.over_by_value,
                    "$currency${FormatUtils.formatAmount(abs(state.remaining))}"
                )
                binding.tvBudgetRemaining.setTextColor(ctx.compatColor(R.color.semantic_expense))
            } else {
                binding.tvBudgetRemaining.text = "${ctx.getString(R.string.left)} $currency${FormatUtils.formatAmount(state.remaining)}"
                binding.tvBudgetRemaining.setTextColor(ctx.compatColor(R.color.text_primary))
            }

            binding.root.setOnClickListener { onBudgetClick(state) }
        }
    }
}
