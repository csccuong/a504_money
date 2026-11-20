package com.money.tracker.budget.planner.moneymanager.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class TransactionModel(
    @SerializedName("transactionType")
    val transactionType: String = TransactionType.EXPENSE.value,
    @SerializedName("amount")
    val amount: String = "0",
    @SerializedName("currency")
    val currency: String = "",
    @SerializedName("categoryName")
    val categoryName: String = "",
    @SerializedName("categoryIcon")
    val categoryIcon: Int = 0,
    @SerializedName("budget")
    val budget: String = "None",
    @SerializedName("note")
    val note: String = "",
    @SerializedName("date")
    val date: String = "",
    @SerializedName("time")
    val time: String = "",
    @SerializedName("lender")
    val lender: String? = null
) : Serializable {

    val type: TransactionType
        get() = TransactionType.fromValue(transactionType)

    val amountValue: Double
        get() = amount.replace(",", "").toDoubleOrNull() ?: 0.0

    fun matches(other: TransactionModel): Boolean =
        date == other.date &&
            amount == other.amount &&
            categoryName == other.categoryName &&
            transactionType == other.transactionType &&
            time == other.time
}
