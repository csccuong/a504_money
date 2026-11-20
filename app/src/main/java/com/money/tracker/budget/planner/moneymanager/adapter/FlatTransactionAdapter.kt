package com.money.tracker.budget.planner.moneymanager.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.money.tracker.budget.planner.moneymanager.R
import com.money.tracker.budget.planner.moneymanager.data.CategoryCatalog
import com.money.tracker.budget.planner.moneymanager.data.DayTransactionGroup
import com.money.tracker.budget.planner.moneymanager.data.TransactionRepository
import com.money.tracker.budget.planner.moneymanager.databinding.ItemFlatDayHeaderBinding
import com.money.tracker.budget.planner.moneymanager.databinding.ItemFlatTransactionBinding
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
import kotlin.math.abs

sealed interface FlatTransactionRow {
    val itemId: String

    data class DayHeader(
        val dateKey: String,
        val date: Date?,
        val transactionCount: Int,
        val netAmount: Double,
        val isCollapsed: Boolean,
        val isFirstHeader: Boolean,
        val currency: String
    ) : FlatTransactionRow {
        override val itemId: String get() = "header_$dateKey"
    }

    data class TransactionRow(
        val stableKey: String,
        val transaction: TransactionModel,
        val isLastInGroup: Boolean,
        val currency: String
    ) : FlatTransactionRow {
        override val itemId: String get() = "tx_$stableKey"
    }
}

class FlatTransactionAdapter(
    private val displayStyle: DisplayStyle = DisplayStyle.GROUPED_CARDS,
    private val collapsible: Boolean = true,
    private val onTransactionClick: (TransactionModel) -> Unit,
    private val onToggleDay: ((String) -> Unit)? = null
) : ListAdapter<FlatTransactionRow, RecyclerView.ViewHolder>(DIFF_CALLBACK) {

    enum class DisplayStyle {
        INLINE_CARD,
        GROUPED_CARDS
    }

    override fun getItemViewType(position: Int): Int = when (getItem(position)) {
        is FlatTransactionRow.DayHeader -> VIEW_TYPE_DAY_HEADER
        is FlatTransactionRow.TransactionRow -> VIEW_TYPE_TRANSACTION
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_DAY_HEADER -> DayHeaderViewHolder(
                ItemFlatDayHeaderBinding.inflate(inflater, parent, false)
            )
            VIEW_TYPE_TRANSACTION -> TransactionRowViewHolder(
                ItemFlatTransactionBinding.inflate(inflater, parent, false)
            )
            else -> error("Unsupported viewType: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = getItem(position)) {
            is FlatTransactionRow.DayHeader -> (holder as DayHeaderViewHolder).bind(row)
            is FlatTransactionRow.TransactionRow -> (holder as TransactionRowViewHolder).bind(row)
        }
    }

    private inner class DayHeaderViewHolder(
        private val binding: ItemFlatDayHeaderBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(row: FlatTransactionRow.DayHeader) {
            val ctx = binding.root.context
            val density = ctx.resources.displayMetrics.density
            val lp = binding.rootContainer.layoutParams as? ViewGroup.MarginLayoutParams
            if (lp != null) {
                lp.topMargin = when {
                    row.isFirstHeader -> 0
                    displayStyle == DisplayStyle.GROUPED_CARDS -> (10 * density).toInt()
                    else -> (8 * density).toInt()
                }
                binding.rootContainer.layoutParams = lp
            }

            if (displayStyle == DisplayStyle.GROUPED_CARDS) {
                binding.rootContainer.setBackgroundResource(
                    if (row.isCollapsed) R.drawable.bg_white else R.drawable.bg_flat_card_top
                )
                binding.headerContent.setPadding(
                    (14 * density).toInt(),
                    (11 * density).toInt(),
                    (14 * density).toInt(),
                    (11 * density).toInt()
                )
                binding.tvDateNumber.show()
                binding.tvDayCountBadge.show()
                binding.tvDayCountBadge.text = row.transactionCount.toString()
                if (row.isCollapsed) {
                    binding.viewHeaderDivider.gone()
                } else {
                    binding.viewHeaderDivider.show()
                }
            } else {
                binding.rootContainer.setBackgroundResource(0)
                binding.headerContent.setPadding(
                    0,
                    (6 * density).toInt(),
                    0,
                    (4 * density).toInt()
                )
                binding.tvDateNumber.gone()
                binding.tvDayCountBadge.gone()
                binding.viewHeaderDivider.gone()
            }

            val date = row.date
            if (date != null) {
                val locale = LocaleHelper.currentLocale(ctx)
                val relativeTitle = resolveRelativeDayLabel(ctx, date)
                binding.tvDateNumber.text = FormatUtils.formatDayNumber(date)
                if (displayStyle == DisplayStyle.INLINE_CARD) {
                    val shortDate = SimpleDateFormat("dd MMM", locale).format(date)
                    binding.tvDayPrimary.text = if (relativeTitle != null) {
                        "$relativeTitle • $shortDate"
                    } else {
                        SimpleDateFormat("EEE, dd MMM", locale).format(date)
                    }
                    binding.tvDaySecondary.gone()
                } else {
                    binding.tvDayPrimary.text = relativeTitle ?: FormatUtils.formatDayOfWeek(date, locale)
                    binding.tvDaySecondary.text = FormatUtils.formatMonthYear(date, locale)
                    binding.tvDaySecondary.show()
                }
            } else {
                binding.tvDateNumber.text = "•"
                binding.tvDayPrimary.text = row.dateKey
                binding.tvDaySecondary.gone()
            }

            val (prefix, colorRes) = when {
                row.netAmount < 0.0 -> "-${row.currency}" to R.color.semantic_expense
                row.netAmount > 0.0 -> "+${row.currency}" to R.color.semantic_income
                else -> row.currency to R.color.text_secondary
            }
            binding.tvDayNet.text = "$prefix${FormatUtils.formatAmount(abs(row.netAmount))}"
            binding.tvDayNet.setTextColor(ctx.compatColor(colorRes))

            if (collapsible) {
                binding.ivExpandChevron.show()
                binding.ivExpandChevron.rotation = if (row.isCollapsed) -90f else 0f
                binding.headerContent.isClickable = true
                binding.headerContent.setOnClickListener { onToggleDay?.invoke(row.dateKey) }
            } else {
                binding.ivExpandChevron.gone()
                binding.headerContent.isClickable = false
                binding.headerContent.setOnClickListener(null)
            }
        }
    }

    private inner class TransactionRowViewHolder(
        private val binding: ItemFlatTransactionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(row: FlatTransactionRow.TransactionRow) {
            val ctx = binding.root.context
            val density = ctx.resources.displayMetrics.density
            val tx = row.transaction

            if (displayStyle == DisplayStyle.GROUPED_CARDS) {
                binding.rootContainer.setBackgroundResource(
                    if (row.isLastInGroup) R.drawable.bg_flat_card_bottom else R.drawable.bg_flat_card_middle
                )
                binding.rowContent.setPadding(
                    (14 * density).toInt(),
                    (10 * density).toInt(),
                    (14 * density).toInt(),
                    (if (row.isLastInGroup) 12 else 10).times(density).toInt()
                )
                val divLp = binding.viewRowDivider.layoutParams as? ViewGroup.MarginLayoutParams
                if (divLp != null) {
                    divLp.marginStart = (64 * density).toInt()
                    divLp.marginEnd = (14 * density).toInt()
                    binding.viewRowDivider.layoutParams = divLp
                }
            } else {
                binding.rootContainer.setBackgroundResource(0)
                binding.rowContent.setPadding(
                    0,
                    (9 * density).toInt(),
                    0,
                    (9 * density).toInt()
                )
                val divLp = binding.viewRowDivider.layoutParams as? ViewGroup.MarginLayoutParams
                if (divLp != null) {
                    divLp.marginStart = (50 * density).toInt()
                    divLp.marginEnd = 0
                    binding.viewRowDivider.layoutParams = divLp
                }
            }

            if (row.isLastInGroup) {
                binding.viewRowDivider.gone()
            } else {
                binding.viewRowDivider.show()
            }

            binding.ivCategory.setImageResource(CategoryCatalog.iconForCategory(tx.categoryName))
            binding.tvCategory.text = CategoryCatalog.localizedName(ctx, tx.categoryName)
            binding.tvSubtitle.text = buildSubtitle(tx)

            val (prefix, colorRes) = when {
                tx.type == TransactionType.INCOME -> "+" to R.color.semantic_income
                tx.type == TransactionType.EXPENSE -> "-" to R.color.semantic_expense
                TransactionRepository.isBorrowTransaction(tx) -> "-" to R.color.semantic_expense
                else -> "+" to R.color.semantic_info
            }
            binding.tvAmount.text = "$prefix${row.currency}${FormatUtils.formatAmount(tx.amountValue)}"
            binding.tvAmount.setTextColor(ctx.compatColor(colorRes))
            binding.rowContent.setOnClickListener { onTransactionClick(tx) }
        }

        private fun buildSubtitle(tx: TransactionModel): String {
            val parts = listOfNotNull(
                tx.lender?.trim()?.takeIf { it.isNotEmpty() },
                tx.note.trim().takeIf { it.isNotEmpty() },
                tx.time.trim().takeIf { it.isNotEmpty() }
            )
            return parts.joinToString(" • ")
        }
    }

    companion object {
        private const val VIEW_TYPE_DAY_HEADER = 0
        private const val VIEW_TYPE_TRANSACTION = 1

        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<FlatTransactionRow>() {
            override fun areItemsTheSame(
                oldItem: FlatTransactionRow,
                newItem: FlatTransactionRow
            ): Boolean = oldItem.itemId == newItem.itemId

            override fun areContentsTheSame(
                oldItem: FlatTransactionRow,
                newItem: FlatTransactionRow
            ): Boolean = oldItem == newItem
        }

        fun flattenGroups(
            groups: List<DayTransactionGroup>,
            currency: String,
            collapsedDateKeys: Set<String> = emptySet(),
            overrideDayNetMap: Map<String, Double>? = null,
            overrideDayCountMap: Map<String, Int>? = null
        ): List<FlatTransactionRow> {
            val result = ArrayList<FlatTransactionRow>(groups.size * 4)
            groups.forEachIndexed { groupIdx, group ->
                val isCollapsed = collapsedDateKeys.contains(group.dateKey)
                val dayNet = overrideDayNetMap?.get(group.dateKey) ?: group.netAmount
                val dayCount = overrideDayCountMap?.get(group.dateKey) ?: group.transactions.size
                result.add(
                    FlatTransactionRow.DayHeader(
                        dateKey = group.dateKey,
                        date = group.date,
                        transactionCount = dayCount,
                        netAmount = dayNet,
                        isCollapsed = isCollapsed,
                        isFirstHeader = groupIdx == 0,
                        currency = currency
                    )
                )
                if (!isCollapsed) {
                    val lastIdx = group.transactions.lastIndex
                    group.transactions.forEachIndexed { txIdx, tx ->
                        val stableKey = "${group.dateKey}_${tx.time}_${tx.categoryName}_${tx.amount}_${tx.note}_$txIdx"
                        result.add(
                            FlatTransactionRow.TransactionRow(
                                stableKey = stableKey,
                                transaction = tx,
                                isLastInGroup = txIdx == lastIdx,
                                currency = currency
                            )
                        )
                    }
                }
            }
            return result
        }

        private fun resolveRelativeDayLabel(context: Context, date: Date): String? {
            val target = Calendar.getInstance().apply { time = date }
            val today = Calendar.getInstance()
            if (isSameDay(target, today)) return context.getString(R.string.today)
            val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
            if (isSameDay(target, yesterday)) return context.getString(R.string.yesterday)
            return null
        }

        private fun isSameDay(a: Calendar, b: Calendar): Boolean =
            a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
                a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)
    }
}
