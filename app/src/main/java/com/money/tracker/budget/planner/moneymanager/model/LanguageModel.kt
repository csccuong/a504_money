package com.money.tracker.budget.planner.moneymanager.model

import androidx.annotation.DrawableRes

data class LanguageModel(
    val languageName: String,
    val isoLanguage: String,
    var isChecked: Boolean = false,
    @DrawableRes val image: Int = 0
)
