package com.money.tracker.budget.planner.moneymanager.model

import androidx.annotation.DrawableRes

data class CurrencyUnitModel(
    val symbol: String,
    val languageName: String,
    val code: String,
    var isChecked: Boolean = false,
    @DrawableRes val image: Int
)
