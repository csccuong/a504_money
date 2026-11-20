package com.money.tracker.budget.planner.moneymanager.model

import android.content.Context
import androidx.annotation.StringRes
import com.money.tracker.budget.planner.moneymanager.R

enum class TransactionType(
    val value: String,
    @StringRes val labelRes: Int
) {
    EXPENSE("Expense", R.string.expense),
    INCOME("Income", R.string.income),
    LOAN("Loan", R.string.loan),
    BORROW("Borrow", R.string.borrow);

    fun localizedLabel(context: Context): String = context.getString(labelRes)

    companion object {
        fun fromValue(raw: String?): TransactionType =
            entries.firstOrNull { it.value.equals(raw, ignoreCase = true) } ?: EXPENSE
    }
}
