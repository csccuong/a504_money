package com.money.tracker.budget.planner.moneymanager.data

import android.content.Context
import com.google.gson.reflect.TypeToken
import com.money.tracker.budget.planner.moneymanager.model.BudgetItem
import com.money.tracker.budget.planner.moneymanager.model.TransactionModel
import com.money.tracker.budget.planner.moneymanager.model.TransactionType
import com.money.tracker.budget.planner.moneymanager.utils.FormatUtils
import com.money.tracker.budget.planner.moneymanager.utils.YearMonthKey

data class BudgetCardState(
    val item: BudgetItem,
    val spent: Double,
    val transactionCount: Int
) {
    val limit: Double get() = item.totalAmount
    val remaining: Double get() = limit - spent
    val isOverBudget: Boolean get() = limit > 0.0 && spent > limit
    val usedPercentInt: Int
        get() = if (limit <= 0.0) 0 else ((spent / limit) * 100.0).toInt()
}

class BudgetRepository(
    private val preferences: AppPreferences,
    private val transactionRepository: TransactionRepository
) {

    constructor(context: Context) : this(
        AppPreferences.getInstance(context),
        TransactionRepository(context)
    )

    private val budgetListType = object : TypeToken<MutableList<BudgetItem>>() {}.type

    fun getBudgetItems(): MutableList<BudgetItem> {
        val json = preferences.getBudgetListJson()
        if (json.isEmpty()) return mutableListOf()
        return runCatching {
            preferences.gson.fromJson<MutableList<BudgetItem>>(json, budgetListType)
        }.getOrNull() ?: mutableListOf()
    }

    fun saveBudgetItem(item: BudgetItem) {
        val budgets = getBudgetItems()
        val withId = if (item.id.isBlank()) item.copy(id = BudgetItem.newId()) else item
        budgets.add(withId)
        saveBudgetItems(budgets)
    }

    fun updateBudgetItem(targetKey: String, updated: BudgetItem) {
        val budgets = getBudgetItems()
        val index = budgets.indexOfFirst { it.stableKey == targetKey || it.name == targetKey }
        if (index != -1) {
            budgets[index] = updated
            saveBudgetItems(budgets)
        }
    }

    fun deleteBudgetItem(targetKey: String): Boolean {
        val budgets = getBudgetItems()
        val index = budgets.indexOfFirst { it.stableKey == targetKey || it.name == targetKey }
        if (index == -1) return false
        budgets.removeAt(index)
        saveBudgetItems(budgets)
        return true
    }

    fun findBudgetByKey(targetKey: String): BudgetItem? =
        getBudgetItems().firstOrNull { it.stableKey == targetKey || it.name == targetKey }

    fun findCategoryBudget(categoryName: String): BudgetItem? =
        getBudgetItems().firstOrNull {
            !it.categoryName.isNullOrBlank() && it.categoryName.equals(categoryName, ignoreCase = true)
        }

    fun getCustomBudgets(): List<BudgetItem> =
        getBudgetItems().filter { !it.isCategoryLinked }

    private fun saveBudgetItems(items: List<BudgetItem>) {
        preferences.setBudgetListJson(preferences.gson.toJson(items))
    }

    fun getTotalBudget(): Double = preferences.getTotalBudget()

    fun setTotalBudget(amount: Double) {
        preferences.setTotalBudget(amount.coerceAtLeast(0.0))
    }

    fun getTotalExpenses(): Double = preferences.getTotalExpenses()

    fun setTotalExpenses(amount: Double) {
        preferences.setTotalExpenses(amount.coerceAtLeast(0.0))
    }

    fun getTotalAllocatedBudget(): Double = getBudgetItems().sumOf { it.totalAmount }

    fun getMonthlyTotalExpense(
        transactions: List<TransactionModel>,
        yearMonth: YearMonthKey
    ): Double = transactions
        .asSequence()
        .filter { tx ->
            if (tx.type != TransactionType.EXPENSE) return@filter false
            val date = FormatUtils.parseTransactionDate(tx.date) ?: return@filter false
            YearMonthKey.fromDate(date) == yearMonth
        }
        .sumOf { it.amountValue }

    fun getTransactionsForBudget(
        transactions: List<TransactionModel>,
        budget: BudgetItem,
        yearMonth: YearMonthKey
    ): List<TransactionModel> = transactions.filter { tx ->
        if (tx.type != TransactionType.EXPENSE) return@filter false
        val date = FormatUtils.parseTransactionDate(tx.date) ?: return@filter false
        if (YearMonthKey.fromDate(date) != yearMonth) return@filter false
        matchesBudget(tx, budget)
    }

    fun buildBudgetCardStates(
        transactions: List<TransactionModel>,
        yearMonth: YearMonthKey
    ): List<BudgetCardState> {
        val monthExpenses = transactions.filter { tx ->
            if (tx.type != TransactionType.EXPENSE) return@filter false
            val date = FormatUtils.parseTransactionDate(tx.date) ?: return@filter false
            YearMonthKey.fromDate(date) == yearMonth
        }
        return getBudgetItems().map { budget ->
            val matching = monthExpenses.filter { matchesBudget(it, budget) }
            BudgetCardState(
                item = budget,
                spent = matching.sumOf { it.amountValue },
                transactionCount = matching.size
            )
        }
    }

    private fun matchesBudget(tx: TransactionModel, budget: BudgetItem): Boolean {
        val explicitMatch = tx.budget.isNotBlank() &&
            !tx.budget.equals("None", ignoreCase = true) &&
            tx.budget.equals(budget.name, ignoreCase = true)
        if (explicitMatch) return true

        val linkedCategory = budget.categoryName
        return !linkedCategory.isNullOrBlank() &&
            tx.categoryName.equals(linkedCategory, ignoreCase = true)
    }

    fun getExpensesForBudget(budgetName: String, yearMonth: YearMonthKey = YearMonthKey.current()): Double {
        val budget = findBudgetByKey(budgetName) ?: BudgetItem(name = budgetName)
        return getTransactionsForBudget(transactionRepository.getTransactions(), budget, yearMonth)
            .sumOf { it.amountValue }
    }

    fun updateBudgetExpenses(budgetName: String) {
        val totalExpenses = getExpensesForBudget(budgetName, YearMonthKey.current())
        val budgets = getBudgetItems()
        budgets.firstOrNull { it.name == budgetName || it.stableKey == budgetName }?.let { item ->
            item.spentAmount = totalExpenses
        }
        saveBudgetItems(budgets)
    }

    fun recalculateTotalExpenses(
        transactions: List<TransactionModel>,
        yearMonth: YearMonthKey = YearMonthKey.current()
    ): Double {
        val total = getMonthlyTotalExpense(transactions, yearMonth)
        setTotalExpenses(total)
        return total
    }
}
