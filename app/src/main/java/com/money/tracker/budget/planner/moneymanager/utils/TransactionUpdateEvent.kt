package com.money.tracker.budget.planner.moneymanager.utils

import com.money.tracker.budget.planner.moneymanager.model.TransactionModel

data class TransactionUpdateEvent(
    val transactionList: List<TransactionModel>
)
