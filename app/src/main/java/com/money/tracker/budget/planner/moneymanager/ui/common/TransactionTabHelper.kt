package com.money.tracker.budget.planner.moneymanager.ui.common

import android.content.Context
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorRes
import com.money.tracker.budget.planner.moneymanager.R
import com.money.tracker.budget.planner.moneymanager.databinding.ViewTransactionTypeTabsBinding
import com.money.tracker.budget.planner.moneymanager.model.TransactionType

data class TransactionTabViews(
    val container: View,
    val icon: ImageView,
    val label: TextView,
    val indicator: View
)

object TransactionTabHelper {

    fun bindListeners(
        tabsBinding: ViewTransactionTypeTabsBinding,
        onSelect: (TransactionType) -> Unit
    ) {
        tabsBinding.llExpend.setOnClickListener { onSelect(TransactionType.EXPENSE) }
        tabsBinding.llIncome.setOnClickListener { onSelect(TransactionType.INCOME) }
        tabsBinding.llLoan.setOnClickListener { onSelect(TransactionType.LOAN) }
    }

    fun updateSelection(
        tabsBinding: ViewTransactionTypeTabsBinding,
        selectedType: TransactionType
    ) {
        updateSelection(
            context = tabsBinding.root.context,
            selectedType = selectedType,
            expenseTab = TransactionTabViews(
                tabsBinding.llExpend,
                tabsBinding.ivExpend,
                tabsBinding.tvExpendLabel,
                tabsBinding.indicatorExpense
            ),
            incomeTab = TransactionTabViews(
                tabsBinding.llIncome,
                tabsBinding.ivIncome,
                tabsBinding.tvIncomeLabel,
                tabsBinding.indicatorIncome
            ),
            loanTab = TransactionTabViews(
                tabsBinding.llLoan,
                tabsBinding.ivLoan,
                tabsBinding.tvLoanLabel,
                tabsBinding.indicatorLoan
            )
        )
    }

    fun updateSelection(
        context: Context,
        selectedType: TransactionType,
        expenseTab: TransactionTabViews,
        incomeTab: TransactionTabViews,
        loanTab: TransactionTabViews,
        @ColorRes activeColorRes: Int = R.color.brand_primary,
        @ColorRes inactiveColorRes: Int = R.color.text_secondary
    ) {
        val inactiveColor = context.compatColor(inactiveColorRes)

        applyState(
            tab = expenseTab,
            isSelected = selectedType == TransactionType.EXPENSE,
            activeColor = context.compatColor(R.color.semantic_expense),
            inactiveColor = inactiveColor
        )
        applyState(
            tab = incomeTab,
            isSelected = selectedType == TransactionType.INCOME,
            activeColor = context.compatColor(R.color.semantic_income),
            inactiveColor = inactiveColor
        )
        applyState(
            tab = loanTab,
            isSelected = selectedType == TransactionType.LOAN || selectedType == TransactionType.BORROW,
            activeColor = context.compatColor(activeColorRes),
            inactiveColor = inactiveColor
        )
    }

    private fun applyState(
        tab: TransactionTabViews,
        isSelected: Boolean,
        activeColor: Int,
        inactiveColor: Int
    ) {
        val tintColor = if (isSelected) activeColor else inactiveColor
        tab.container.isSelected = isSelected
        tab.container.setBackgroundResource(
            if (isSelected) R.drawable.bg_segmented_selected else android.R.color.transparent
        )
        tab.icon.setColorFilter(tintColor)
        tab.label.setTextColor(tintColor)
        tab.indicator.visibility = View.GONE
    }
}
