package com.money.tracker.budget.planner.moneymanager.ui.common

import android.content.Context
import android.view.LayoutInflater
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.money.tracker.budget.planner.moneymanager.R
import com.money.tracker.budget.planner.moneymanager.adapter.SimpleTransactionRowAdapter
import com.money.tracker.budget.planner.moneymanager.databinding.BottomSheetCategoryTransactionsBinding
import com.money.tracker.budget.planner.moneymanager.model.TransactionModel

object TransactionListSheetHelper {

    fun show(
        context: Context,
        title: String,
        subtitle: String,
        totalText: String,
        iconRes: Int? = null,
        currency: String,
        transactions: List<TransactionModel>,
        onTransactionClick: (TransactionModel) -> Unit
    ) {
        val dialog = BottomSheetDialog(context, R.style.AppBottomSheetDialogTheme)
        val binding = BottomSheetCategoryTransactionsBinding.inflate(LayoutInflater.from(context))
        dialog.setContentView(binding.root)

        binding.tvSheetTitle.text = title
        binding.tvSheetSubtitle.text = subtitle
        binding.tvSheetTotal.text = totalText

        if (iconRes != null) {
            binding.ivSheetIcon.setImageResource(iconRes)
            binding.ivSheetIcon.show()
        } else {
            binding.ivSheetIcon.gone()
        }

        if (transactions.isEmpty()) {
            binding.rvSheetTransactions.gone()
            binding.tvSheetEmpty.show()
        } else {
            binding.tvSheetEmpty.gone()
            binding.rvSheetTransactions.show()
            binding.rvSheetTransactions.layoutManager = LinearLayoutManager(context)
            val adapter = SimpleTransactionRowAdapter(currency) { tx ->
                dialog.dismiss()
                onTransactionClick(tx)
            }
            binding.rvSheetTransactions.adapter = adapter
            adapter.submitList(transactions)
        }

        dialog.setOnShowListener {
            dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        }
        dialog.show()
    }
}
