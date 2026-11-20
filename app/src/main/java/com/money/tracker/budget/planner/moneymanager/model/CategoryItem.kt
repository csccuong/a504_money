package com.money.tracker.budget.planner.moneymanager.model

import androidx.annotation.DrawableRes

data class CategoryItem(
    @DrawableRes val iconResource: Int,
    val name: String,
    var isSelected: Boolean = false
)
