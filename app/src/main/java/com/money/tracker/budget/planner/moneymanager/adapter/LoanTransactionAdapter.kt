package com.money.tracker.budget.planner.moneymanager.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.money.tracker.budget.planner.moneymanager.R
import com.money.tracker.budget.planner.moneymanager.data.AppPreferences
import com.money.tracker.budget.planner.moneymanager.data.CategoryCatalog
import com.money.tracker.budget.planner.moneymanager.data.TransactionRepository
import com.money.tracker.budget.planner.moneymanager.databinding.ItemDateHeaderBinding
import com.money.tracker.budget.planner.moneymanager.databinding.ItemLoanSummaryHeaderBinding
import com.money.tracker.budget.planner.moneymanager.databinding.ItemTransactionBinding
import com.money.tracker.budget.planner.moneymanager.model.TransactionModel
import com.money.tracker.budget.planner.moneymanager.ui.common.compatColor
import com.money.tracker.budget.planner.moneymanager.ui.common.gone
import com.money.tracker.budget.planner.moneymanager.ui.common.show
import com.money.tracker.budget.planner.moneymanager.utils.FormatUtils
import com.money.tracker.budget.planner.moneymanager.utils.LocaleHelper
import java.util.Date
import kotlin.math.abs

class LoanTransactionAdapter(
    private val onTransactionClick: (TransactionModel) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private sealed interface LoanListItem {
        data class SummaryHeader(val borrowTotal: Double, val loanTotal: Double) : LoanListItem
        data class DateGroup(
            val dateKey: String,
            val parsedDate: Date?,
            val transactions: List<TransactionModel>
        ) : LoanListItem
    }

    private val items = mutableListOf<LoanListItem>()

    @SuppressLint("NotifyDataSetChanged")
    fun submitTransactions(transactions: List<TransactionModel>) {
        items.clear()
        val grouped = LinkedHashMap<String, Pair<Date?, MutableList<TransactionModel>>>()
        var totalBorrow = 0.0
        var totalLoan = 0.0

        for (tx in transactions) {
            val amount = tx.amountValue
            if (TransactionRepository.isBorrowTransaction(tx)) {
                totalBorrow += amount
            } else {
                totalLoan += amount
            }
            val parsed = FormatUtils.parseTransactionDate(tx.date)
            val key = if (parsed != null) FormatUtils.formatDayGroupKey(parsed) else tx.date
            grouped.getOrPut(key) { parsed to mutableListOf() }.second.add(tx)
        }

        items.add(LoanListItem.SummaryHeader(totalBorrow, totalLoan))
        grouped.entries
            .sortedByDescending { it.value.first?.time ?: 0L }
            .forEach { (dateKey, pair) ->
                items.add(
                    LoanListItem.DateGroup(
                        dateKey = dateKey,
                        parsedDate = pair.first,
                        transactions = pair.second.sortedByDescending { it.time }
                    )
                )
            }
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int = when (items[position]) {
        is LoanListItem.SummaryHeader -> VIEW_TYPE_SUMMARY
        is LoanListItem.DateGroup -> VIEW_TYPE_DATE_GROUP
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_SUMMARY -> SummaryHeaderViewHolder(
                ItemLoanSummaryHeaderBinding.inflate(inflater, parent, false)
            )
            VIEW_TYPE_DATE_GROUP -> DateGroupViewHolder(
                ItemDateHeaderBinding.inflate(inflater, parent, false)
            )
            else -> error("Unsupported viewType: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val currency = AppPreferences.getInstance(holder.itemView.context).getSelectedCurrencyOrDefault()
        when (val item = items[position]) {
            is LoanListItem.SummaryHeader -> (holder as SummaryHeaderViewHolder).bind(item, currency)
            is LoanListItem.DateGroup -> (holder as DateGroupViewHolder).bind(item, currency)
        }
    }

    override fun getItemCount(): Int = items.size

    private class SummaryHeaderViewHolder(
        private val binding: ItemLoanSummaryHeaderBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(header: LoanListItem.SummaryHeader, currency: String) {
            binding.tvBorrowAmount.text = "$currency${FormatUtils.formatAmount(header.borrowTotal)}"
            binding.tvLoanAmount.text = "$currency${FormatUtils.formatAmount(header.loanTotal)}"
        }
    }

    private inner class DateGroupViewHolder(
        private val binding: ItemDateHeaderBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.rvTransactions.layoutManager = LinearLayoutManager(binding.root.context)
            binding.rvTransactions.isNestedScrollingEnabled = false
        }

        fun bind(group: LoanListItem.DateGroup, currency: String) {
            val ctx = binding.root.context
            val parsedDate = group.parsedDate
            if (parsedDate != null) {
                val locale = LocaleHelper.currentLocale(ctx)
                binding.tvDateNumber.text = FormatUtils.formatDayNumber(parsedDate)
                binding.tvDayOfWeek.text = FormatUtils.formatDayOfWeek(parsedDate, locale)
                binding.tvMonthYear.text = FormatUtils.formatMonthYear(parsedDate, locale)
            } else {
                binding.tvDateNumber.text = ""
                binding.tvDayOfWeek.text = group.dateKey
                binding.tvMonthYear.text = ""
            }

            if (group.transactions.isNotEmpty()) {
                binding.divider.show()
                binding.rvTransactions.adapter = NestedLoanItemAdapter(group.transactions, currency)
                binding.rvTransactions.show()
            } else {
                binding.divider.gone()
                binding.rvTransactions.gone()
            }

            var totalLoan = 0.0
            var totalBorrow = 0.0
            for (tx in group.transactions) {
                if (TransactionRepository.isBorrowTransaction(tx)) {
                    totalBorrow += tx.amountValue
                } else {
                    totalLoan += tx.amountValue
                }
            }

            val net = totalLoan - totalBorrow
            val (colorRes, prefix) = when {
                net < 0 -> R.color.semantic_expense to "-$currency"
                net > 0 -> R.color.semantic_income to "+$currency"
                else -> R.color.text_primary to currency
            }
            binding.tvTotalAmount.text = "$prefix${FormatUtils.formatAmount(abs(net))}"
            binding.tvTotalAmount.setTextColor(ctx.compatColor(colorRes))
        }
    }

    private inner class NestedLoanItemAdapter(
        private val transactions: List<TransactionModel>,
        private val currency: String
    ) : RecyclerView.Adapter<LoanItemViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LoanItemViewHolder {
            val binding = ItemTransactionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return LoanItemViewHolder(binding)
        }

        override fun onBindViewHolder(holder: LoanItemViewHolder, position: Int) {
            holder.bind(transactions[position], currency)
        }

        override fun getItemCount(): Int = transactions.size
    }

    private inner class LoanItemViewHolder(
        private val binding: ItemTransactionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(transaction: TransactionModel, currency: String) {
            val ctx = binding.root.context
            binding.tvCategory.text = CategoryCatalog.localizedName(ctx, transaction.categoryName)
            binding.tvTime.text = buildLoanSubtitle(transaction)

            val isBorrow = TransactionRepository.isBorrowTransaction(transaction)
            val prefix = if (isBorrow) "-" else "+"
            val colorRes = if (isBorrow) R.color.semantic_expense else R.color.semantic_income

            binding.tvAmount.text = "$prefix$currency${FormatUtils.formatAmount(transaction.amountValue)}"
            binding.tvAmount.setTextColor(ctx.compatColor(colorRes))
            binding.ivCategory.setImageResource(CategoryCatalog.iconForCategory(transaction.categoryName))
            binding.root.setOnClickListener { onTransactionClick(transaction) }
        }

        private fun buildLoanSubtitle(tx: TransactionModel): String {
            val parts = listOfNotNull(
                tx.lender?.trim()?.takeIf { it.isNotEmpty() },
                tx.note.trim().takeIf { it.isNotEmpty() },
                tx.time.trim().takeIf { it.isNotEmpty() }
            )
            return parts.joinToString(" • ")
        }
    }

    private companion object {
        const val VIEW_TYPE_SUMMARY = 0
        const val VIEW_TYPE_DATE_GROUP = 1
    }
}
