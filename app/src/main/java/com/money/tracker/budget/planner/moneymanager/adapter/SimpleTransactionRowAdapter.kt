package com.money.tracker.budget.planner.moneymanager.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.money.tracker.budget.planner.moneymanager.R
import com.money.tracker.budget.planner.moneymanager.data.CategoryCatalog
import com.money.tracker.budget.planner.moneymanager.data.TransactionRepository
import com.money.tracker.budget.planner.moneymanager.databinding.ItemTransactionBinding
import com.money.tracker.budget.planner.moneymanager.model.TransactionModel
import com.money.tracker.budget.planner.moneymanager.model.TransactionType
import com.money.tracker.budget.planner.moneymanager.ui.common.compatColor
import com.money.tracker.budget.planner.moneymanager.utils.FormatUtils

class SimpleTransactionRowAdapter(
    private val currency: String,
    private val onItemClick: (TransactionModel) -> Unit
) : RecyclerView.Adapter<SimpleTransactionRowAdapter.RowViewHolder>() {

    private var transactions: List<TransactionModel> = emptyList()

    @SuppressLint("NotifyDataSetChanged")
    fun submitList(items: List<TransactionModel>) {
        transactions = items
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RowViewHolder {
        val binding = ItemTransactionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return RowViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RowViewHolder, position: Int) {
        holder.bind(transactions[position])
    }

    override fun getItemCount(): Int = transactions.size

    inner class RowViewHolder(
        private val binding: ItemTransactionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(tx: TransactionModel) {
            val ctx = binding.root.context
            binding.ivCategory.setImageResource(CategoryCatalog.iconForCategory(tx.categoryName))
            binding.tvCategory.text = CategoryCatalog.localizedName(ctx, tx.categoryName)

            val subtitleParts = buildList {
                if (tx.note.isNotBlank()) add(tx.note.trim())
                if (!tx.lender.isNullOrBlank()) add(tx.lender.trim())
                if (tx.date.isNotBlank()) add(tx.date)
                if (tx.time.isNotBlank()) add(tx.time)
            }
            binding.tvTime.text = subtitleParts.joinToString(" • ")

            val (prefix, colorRes) = when {
                tx.type == TransactionType.INCOME -> "+" to R.color.semantic_income
                tx.type == TransactionType.EXPENSE -> "-" to R.color.semantic_expense
                TransactionRepository.isBorrowTransaction(tx) -> "-" to R.color.semantic_expense
                else -> "+" to R.color.semantic_income
            }

            binding.tvAmount.text = "$prefix$currency${FormatUtils.formatAmount(tx.amountValue)}"
            binding.tvAmount.setTextColor(ctx.compatColor(colorRes))
            binding.root.setOnClickListener { onItemClick(tx) }
        }
    }
}
