package com.money.tracker.budget.planner.moneymanager.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.money.tracker.budget.planner.moneymanager.R
import com.money.tracker.budget.planner.moneymanager.data.AppPreferences
import com.money.tracker.budget.planner.moneymanager.data.CategoryCatalog
import com.money.tracker.budget.planner.moneymanager.data.DayTransactionGroup
import com.money.tracker.budget.planner.moneymanager.databinding.ItemDateHeaderBinding
import com.money.tracker.budget.planner.moneymanager.databinding.ItemTransactionBinding
import com.money.tracker.budget.planner.moneymanager.model.TransactionModel
import com.money.tracker.budget.planner.moneymanager.model.TransactionType
import com.money.tracker.budget.planner.moneymanager.ui.common.compatColor
import com.money.tracker.budget.planner.moneymanager.ui.common.gone
import com.money.tracker.budget.planner.moneymanager.ui.common.show
import com.money.tracker.budget.planner.moneymanager.utils.FormatUtils
import com.money.tracker.budget.planner.moneymanager.utils.LocaleHelper
import java.util.Calendar
import kotlin.math.abs

class TransactionAdapter(
    private val onTransactionClick: (TransactionModel) -> Unit
) : RecyclerView.Adapter<TransactionAdapter.DateGroupViewHolder>() {

    private var groups: List<DayTransactionGroup> = emptyList()

    @SuppressLint("NotifyDataSetChanged")
    fun submitGroups(newGroups: List<DayTransactionGroup>) {
        groups = newGroups
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DateGroupViewHolder {
        val binding = ItemDateHeaderBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DateGroupViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DateGroupViewHolder, position: Int) {
        holder.bind(groups[position])
    }

    override fun getItemCount(): Int = groups.size

    inner class DateGroupViewHolder(
        private val binding: ItemDateHeaderBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.rvTransactions.layoutManager = LinearLayoutManager(binding.root.context)
            binding.rvTransactions.isNestedScrollingEnabled = false
        }

        fun bind(group: DayTransactionGroup) {
            val ctx = binding.root.context
            val date = group.date
            if (date != null) {
                val locale = LocaleHelper.currentLocale(ctx)
                val calendar = Calendar.getInstance().apply { time = date }
                binding.tvDateNumber.text = FormatUtils.formatDayNumber(calendar.time)
                binding.tvDayOfWeek.text = FormatUtils.formatDayOfWeek(calendar.time, locale)
                binding.tvMonthYear.text = FormatUtils.formatMonthYear(calendar.time, locale)
            } else {
                binding.tvDateNumber.text = ""
                binding.tvDayOfWeek.text = group.dateKey
                binding.tvMonthYear.text = ""
            }

            val currency = AppPreferences.getInstance(ctx).getSelectedCurrencyOrDefault()
            val (prefix, colorRes) = when {
                group.netAmount < 0 -> "-$currency" to R.color.semantic_expense
                group.netAmount > 0 -> "+$currency" to R.color.semantic_income
                else -> currency to R.color.text_primary
            }
            binding.tvTotalAmount.text = "$prefix${FormatUtils.formatAmount(abs(group.netAmount))}"
            binding.tvTotalAmount.setTextColor(ctx.compatColor(colorRes))

            if (group.transactions.isNotEmpty()) {
                binding.divider.show()
                binding.rvTransactions.adapter = NestedTransactionAdapter(group.transactions, currency)
                binding.rvTransactions.show()
            } else {
                binding.divider.gone()
                binding.rvTransactions.gone()
            }
        }
    }

    private inner class NestedTransactionAdapter(
        private val transactions: List<TransactionModel>,
        private val currency: String
    ) : RecyclerView.Adapter<TransactionItemViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionItemViewHolder {
            val binding = ItemTransactionBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return TransactionItemViewHolder(binding)
        }

        override fun onBindViewHolder(holder: TransactionItemViewHolder, position: Int) {
            holder.bind(transactions[position], currency)
        }

        override fun getItemCount(): Int = transactions.size
    }

    private inner class TransactionItemViewHolder(
        private val binding: ItemTransactionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(transaction: TransactionModel, currency: String) {
            val ctx = binding.root.context
            binding.tvCategory.text = CategoryCatalog.localizedName(ctx, transaction.categoryName)
            binding.tvTime.text = buildSubtitle(transaction)

            val (prefix, colorRes) = when (transaction.type) {
                TransactionType.INCOME -> "+" to R.color.semantic_income
                TransactionType.EXPENSE -> "-" to R.color.semantic_expense
                TransactionType.LOAN, TransactionType.BORROW -> "" to R.color.brand_primary
            }

            binding.tvAmount.text = "$prefix$currency${FormatUtils.formatAmount(transaction.amountValue)}"
            binding.tvAmount.setTextColor(ctx.compatColor(colorRes))
            binding.ivCategory.setImageResource(CategoryCatalog.iconForCategory(transaction.categoryName))
            binding.root.setOnClickListener { onTransactionClick(transaction) }
        }

        private fun buildSubtitle(tx: TransactionModel): String {
            val note = tx.note.trim()
            return when {
                note.isNotEmpty() && tx.time.isNotBlank() -> "$note • ${tx.time}"
                note.isNotEmpty() -> note
                else -> tx.time
            }
        }
    }
}
