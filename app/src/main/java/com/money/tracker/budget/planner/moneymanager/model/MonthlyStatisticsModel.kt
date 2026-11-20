package com.money.tracker.budget.planner.moneymanager.model

data class MonthlyStatisticsModel(
    val month: String,
    var expend: Double = 0.0,
    var income: Double = 0.0,
    var loan: Double = 0.0,
    var borrow: Double = 0.0,
    var balance: Double = 0.0
)
