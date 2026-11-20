package com.money.tracker.budget.planner.moneymanager.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.money.tracker.budget.planner.moneymanager.R
import com.money.tracker.budget.planner.moneymanager.data.CategoryCatalog
import com.money.tracker.budget.planner.moneymanager.data.TransactionRepository
import com.money.tracker.budget.planner.moneymanager.databinding.ItemHomeRecentTransactionBinding
import com.money.tracker.budget.planner.moneymanager.model.TransactionModel
import com.money.tracker.budget.planner.moneymanager.model.TransactionType
import com.money.tracker.budget.planner.moneymanager.ui.common.compatColor
import com.money.tracker.budget.planner.moneymanager.ui.common.gone
import com.money.tracker.budget.planner.moneymanager.ui.common.show
import com.money.tracker.budget.planner.moneymanager.utils.FormatUtils
import com.money.tracker.budget.planner.moneymanager.utils.LocaleHelper
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date

data class HomeRecentItem(
    val stableId: String,
    val transaction: TransactionModel,
    val currency: String,
    val isLast: Boolean
)

class HomeRecentTransactionAdapter(
    private val onTransactionClick: (TransactionModel) -> Unit
) : ListAdapter<HomeRecentItem, HomeRecentTransactionAdapter.ViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHomeRecentTransactionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemHomeRecentTransactionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: HomeRecentItem) {
            val ctx = binding.root.context
            val tx = item.transaction

            binding.ivCategory.setImageResource(CategoryCatalog.iconForCategory(tx.categoryName))
            binding.tvCategory.text = CategoryCatalog.localizedName(ctx, tx.categoryName)
            binding.tvSubtitle.text = buildRelativeDateSubtitle(ctx, tx)

            val (prefix, colorRes) = when {
                tx.type == TransactionType.INCOME -> "+" to R.color.semantic_income
                tx.type == TransactionType.EXPENSE -> "-" to R.color.semantic_expense
                TransactionRepository.isBorrowTransaction(tx) -> "-" to R.color.semantic_expense
                else -> "+" to R.color.semantic_info
            }
            binding.tvAmount.text = "$prefix${item.currency}${FormatUtils.formatAmount(tx.amountValue)}"
            binding.tvAmount.setTextColor(ctx.compatColor(colorRes))

            if (item.isLast) binding.viewDivider.gone() else binding.viewDivider.show()
            binding.rowContent.setOnClickListener { onTransactionClick(tx) }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<HomeRecentItem>() {
            override fun areItemsTheSame(oldItem: HomeRecentItem, newItem: HomeRecentItem): Boolean =
                oldItem.stableId == newItem.stableId

            override fun areContentsTheSame(oldItem: HomeRecentItem, newItem: HomeRecentItem): Boolean =
                oldItem == newItem
        }

        fun mapItems(transactions: List<TransactionModel>, currency: String): List<HomeRecentItem> {
            val lastIdx = transactions.lastIndex
            return transactions.mapIndexed { idx, tx ->
                HomeRecentItem(
                    stableId = "${tx.date}_${tx.time}_${tx.categoryName}_${tx.amount}_${tx.note}_$idx",
                    transaction = tx,
                    currency = currency,
                    isLast = idx == lastIdx
                )
            }
        }

        private fun buildRelativeDateSubtitle(context: Context, tx: TransactionModel): String {
            val date = FormatUtils.parseTransactionDate(tx.date)
            val datePart = if (date != null) {
                formatSmartDateTime(context, date, tx.time)
            } else {
                listOf(tx.date, tx.time).filter { it.isNotBlank() }.joinToString(", ")
            }
            val extra = tx.lender?.trim()?.takeIf { it.isNotEmpty() }
                ?: tx.note.trim().takeIf { it.isNotEmpty() }
            return if (!extra.isNullOrBlank()) "$datePart • $extra" else datePart
        }

        private fun formatSmartDateTime(context: Context, date: Date, timeStr: String): String {
            val target = Calendar.getInstance().apply { time = date }
            val today = Calendar.getInstance()
            val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
            val cleanTime = timeStr.trim()

            return when {
                isSameDay(target, today) -> {
                    val label = context.getString(R.string.today)
                    if (cleanTime.isNotEmpty()) "$label, $cleanTime" else label
                }
                isSameDay(target, yesterday) -> {
                    val label = context.getString(R.string.yesterday)
                    if (cleanTime.isNotEmpty()) "$label, $cleanTime" else label
                }
                else -> {
                    val locale = LocaleHelper.currentLocale(context)
                    SimpleDateFormat("MMM dd, yyyy", locale).format(date)
                }
            }
        }

        private fun isSameDay(a: Calendar, b: Calendar): Boolean =
            a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
                a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)
    }
}
