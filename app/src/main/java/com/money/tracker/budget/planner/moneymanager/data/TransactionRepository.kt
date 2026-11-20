package com.money.tracker.budget.planner.moneymanager.data

import android.content.Context
import com.google.gson.reflect.TypeToken
import com.money.tracker.budget.planner.moneymanager.R
import com.money.tracker.budget.planner.moneymanager.model.BreakdownSortMode
import com.money.tracker.budget.planner.moneymanager.model.LoanDirectionFilter
import com.money.tracker.budget.planner.moneymanager.model.MonthlyStatisticsModel
import com.money.tracker.budget.planner.moneymanager.model.StatisticsFilter
import com.money.tracker.budget.planner.moneymanager.model.StatisticsPeriodType
import com.money.tracker.budget.planner.moneymanager.model.TransactionModel
import com.money.tracker.budget.planner.moneymanager.model.TransactionType
import com.money.tracker.budget.planner.moneymanager.utils.FormatUtils
import com.money.tracker.budget.planner.moneymanager.utils.LocaleHelper
import com.money.tracker.budget.planner.moneymanager.utils.YearMonthKey
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date

data class TransactionSummary(
    val totalBalance: Double,
    val monthIncome: Double,
    val monthExpense: Double,
    val monthLoan: Double
)

data class PeriodCashflow(
    val income: Double,
    val expense: Double,
    val loan: Double,
    val borrow: Double
) {
    val netSavings: Double get() = income - expense
    val netLoan: Double get() = loan - borrow
}

data class CategoryBreakdownItem(
    val categoryName: String,
    val amount: Double,
    val transactionCount: Int,
    val percentage: Float,
    val transactions: List<TransactionModel>
)

data class DayTransactionGroup(
    val dateKey: String,
    val date: Date?,
    val transactions: List<TransactionModel>,
    val netAmount: Double
)

data class HomeOverviewSeries(
    val labels: List<String>,
    val incomeValues: FloatArray,
    val expenseValues: FloatArray,
    val hasData: Boolean
)

class TransactionRepository(private val preferences: AppPreferences) {

    constructor(context: Context) : this(AppPreferences.getInstance(context))

    private val transactionListType = object : TypeToken<MutableList<TransactionModel>>() {}.type

    fun getTransactions(): MutableList<TransactionModel> {
        val json = preferences.getTransactionListJson()
        if (json.isEmpty()) return mutableListOf()
        return runCatching {
            preferences.gson.fromJson<MutableList<TransactionModel>>(json, transactionListType)
        }.getOrNull() ?: mutableListOf()
    }

    fun saveTransactions(transactions: List<TransactionModel>) {
        preferences.setTransactionListJson(preferences.gson.toJson(transactions))
    }

    fun getLastSavedTransaction(): TransactionModel? {
        val json = preferences.getLastTransactionJson()
        if (json.isEmpty()) return null
        return runCatching {
            preferences.gson.fromJson(json, TransactionModel::class.java)
        }.getOrNull()
    }

    fun addOrUpdateTransaction(
        transaction: TransactionModel,
        editPosition: Int
    ): List<TransactionModel> {
        val transactions = getTransactions()
        if (editPosition in transactions.indices) {
            transactions[editPosition] = transaction
        } else {
            transactions.add(transaction)
        }
        saveTransactions(transactions)
        return transactions
    }

    fun findTransactionIndex(target: TransactionModel): Int =
        getTransactions().indexOfFirst { it.matches(target) }

    fun deleteTransaction(target: TransactionModel): Int {
        val transactions = getTransactions()
        val index = transactions.indexOfFirst { it.matches(target) }
        if (index != -1) {
            transactions.removeAt(index)
            saveTransactions(transactions)
        }
        return index
    }

    fun toJson(transaction: TransactionModel): String = preferences.gson.toJson(transaction)

    fun toJson(transactions: List<TransactionModel>): String = preferences.gson.toJson(transactions)

    fun fromJson(json: String?): TransactionModel? {
        if (json.isNullOrEmpty()) return null
        return runCatching {
            preferences.gson.fromJson(json, TransactionModel::class.java)
        }.getOrNull()
    }

    fun parseListJson(json: String?): List<TransactionModel> {
        if (json.isNullOrEmpty()) return emptyList()
        return runCatching {
            preferences.gson.fromJson<List<TransactionModel>>(json, transactionListType)
        }.getOrNull() ?: emptyList()
    }

    fun filterByMonth(
        transactions: List<TransactionModel>,
        yearMonth: YearMonthKey
    ): List<TransactionModel> = transactions.filter { tx ->
        val date = FormatUtils.parseTransactionDate(tx.date) ?: return@filter false
        YearMonthKey.fromDate(date) == yearMonth
    }

    fun filterByMonthAndType(
        transactions: List<TransactionModel>,
        yearMonth: YearMonthKey,
        type: TransactionType
    ): List<TransactionModel> = transactions.filter { tx ->
        val date = FormatUtils.parseTransactionDate(tx.date) ?: return@filter false
        matchesTypeFilter(tx, type) && YearMonthKey.fromDate(date) == yearMonth
    }

    fun filterByYear(
        transactions: List<TransactionModel>,
        targetYear: Int
    ): List<TransactionModel> {
        val cal = Calendar.getInstance()
        return transactions.filter { tx ->
            val date = FormatUtils.parseTransactionDate(tx.date) ?: return@filter false
            cal.time = date
            cal.get(Calendar.YEAR) == targetYear
        }
    }

    fun filterByYearAndType(
        transactions: List<TransactionModel>,
        targetYear: Int,
        type: TransactionType
    ): List<TransactionModel> = filterByYear(transactions, targetYear).filter { matchesTypeFilter(it, type) }

    fun filterTransactions(
        transactions: List<TransactionModel>,
        yearMonth: YearMonthKey? = null,
        typeFilter: TransactionType? = null,
        categoryFilter: String? = null,
        searchQuery: String = "",
        context: Context? = null
    ): List<TransactionModel> {
        val normalizedQuery = searchQuery.trim().lowercase()
        val hasQuery = normalizedQuery.isNotEmpty()
        val hasCategory = !categoryFilter.isNullOrBlank()

        return transactions
            .withIndex()
            .filter { (_, tx) ->
                if (typeFilter != null && !matchesTypeFilter(tx, typeFilter)) return@filter false

                if (yearMonth != null) {
                    val date = FormatUtils.parseTransactionDate(tx.date) ?: return@filter false
                    if (YearMonthKey.fromDate(date) != yearMonth) return@filter false
                }

                if (hasCategory && !tx.categoryName.equals(categoryFilter, ignoreCase = true)) {
                    return@filter false
                }

                if (hasQuery) {
                    val localizedCat = context?.let { CategoryCatalog.localizedName(it, tx.categoryName) }.orEmpty()
                    val matched = tx.note.lowercase().contains(normalizedQuery) ||
                        tx.categoryName.lowercase().contains(normalizedQuery) ||
                        localizedCat.lowercase().contains(normalizedQuery) ||
                        (tx.lender?.lowercase()?.contains(normalizedQuery) == true) ||
                        tx.amount.lowercase().contains(normalizedQuery) ||
                        tx.amount.replace(",", "").contains(normalizedQuery) ||
                        tx.date.lowercase().contains(normalizedQuery)
                    if (!matched) return@filter false
                }
                true
            }
            .sortedWith(
                compareByDescending<IndexedValue<TransactionModel>> {
                    FormatUtils.parseTransactionDate(it.value.date)?.time ?: 0L
                }
                    .thenByDescending { it.value.time }
                    .thenByDescending { it.index }
            )
            .map { it.value }
    }

    fun extractAvailableCategories(transactions: List<TransactionModel>): List<String> {
        if (transactions.isEmpty()) return emptyList()
        val stats = LinkedHashMap<String, Pair<Int, Double>>()
        for (tx in transactions) {
            val cat = tx.categoryName.trim()
            if (cat.isEmpty()) continue
            val current = stats[cat]
            stats[cat] = if (current == null) {
                1 to tx.amountValue
            } else {
                (current.first + 1) to (current.second + tx.amountValue)
            }
        }
        return stats.entries
            .sortedWith(
                compareByDescending<Map.Entry<String, Pair<Int, Double>>> { it.value.first }
                    .thenByDescending { it.value.second }
            )
            .map { it.key }
    }

    private fun matchesTypeFilter(tx: TransactionModel, type: TransactionType): Boolean = when (type) {
        TransactionType.LOAN -> tx.type == TransactionType.LOAN || tx.type == TransactionType.BORROW
        else -> tx.type == type
    }

    fun calculateCashflow(transactions: List<TransactionModel>): PeriodCashflow {
        var income = 0.0
        var expense = 0.0
        var loan = 0.0
        var borrow = 0.0

        for (tx in transactions) {
            val amount = tx.amountValue
            when {
                tx.type == TransactionType.INCOME -> income += amount
                tx.type == TransactionType.EXPENSE -> expense += amount
                isBorrowTransaction(tx) -> borrow += amount
                tx.type == TransactionType.LOAN -> loan += amount
            }
        }
        return PeriodCashflow(income = income, expense = expense, loan = loan, borrow = borrow)
    }

    fun filterByDateRange(
        transactions: List<TransactionModel>,
        startMillis: Long,
        endMillis: Long
    ): List<TransactionModel> = transactions.filter { tx ->
        val date = FormatUtils.parseTransactionDate(tx.date) ?: return@filter false
        val dayMillis = StatisticsFilter.normalizeStartOfDay(date.time)
        dayMillis in startMillis..endMillis
    }

    fun filterForStatistics(
        transactions: List<TransactionModel>,
        type: TransactionType,
        filter: StatisticsFilter
    ): List<TransactionModel> {
        val (startMillis, endMillis) = filter.dateRangeMillis()
        val selectedCats = filter.selectedCategories
        return transactions.filter { tx ->
            if (!matchesTypeFilter(tx, type)) return@filter false
            val date = FormatUtils.parseTransactionDate(tx.date) ?: return@filter false
            val dayMillis = StatisticsFilter.normalizeStartOfDay(date.time)
            if (dayMillis !in startMillis..endMillis) return@filter false

            if (type == TransactionType.LOAN) {
                val isBorrow = isBorrowTransaction(tx)
                when (filter.loanDirection) {
                    LoanDirectionFilter.LENT_ONLY -> if (isBorrow) return@filter false
                    LoanDirectionFilter.BORROWED_ONLY -> if (!isBorrow) return@filter false
                    LoanDirectionFilter.ALL -> Unit
                }
            }

            if (selectedCats.isNotEmpty()) {
                val txCat = tx.categoryName.ifBlank { "Other" }
                if (selectedCats.none { it.equals(txCat, ignoreCase = true) }) return@filter false
            }
            true
        }
    }

    fun calculateCategoryBreakdown(
        transactions: List<TransactionModel>,
        sortMode: BreakdownSortMode = BreakdownSortMode.AMOUNT_DESC,
        percentageDenominator: Double? = null
    ): List<CategoryBreakdownItem> {
        val grouped = LinkedHashMap<String, MutableList<TransactionModel>>()
        for (tx in transactions) {
            val key = tx.categoryName.ifBlank { "Other" }
            grouped.getOrPut(key) { mutableListOf() }.add(tx)
        }
        val grandTotal = percentageDenominator?.takeIf { it > 0.0 }
            ?: transactions.sumOf { it.amountValue }
        val items = grouped.entries.map { (category, txList) ->
            val sum = txList.sumOf { it.amountValue }
            val pct = if (grandTotal > 0.0) ((sum / grandTotal) * 100.0).toFloat() else 0f
            CategoryBreakdownItem(
                categoryName = category,
                amount = sum,
                transactionCount = txList.size,
                percentage = pct,
                transactions = txList.sortedByDescending {
                    FormatUtils.parseTransactionDate(it.date)?.time ?: 0L
                }
            )
        }
        return when (sortMode) {
            BreakdownSortMode.AMOUNT_DESC -> items.sortedByDescending { it.amount }
            BreakdownSortMode.COUNT_DESC -> items.sortedWith(
                compareByDescending<CategoryBreakdownItem> { it.transactionCount }
                    .thenByDescending { it.amount }
            )
        }
    }

    fun calculateTrendSeries(
        context: Context,
        transactions: List<TransactionModel>,
        filter: StatisticsFilter
    ): Pair<List<String>, FloatArray> {
        val locale = LocaleHelper.currentLocale(context)
        val cal = Calendar.getInstance()
        return when (filter.periodType) {
            StatisticsPeriodType.WEEK -> {
                val (startMillis, _) = filter.dateRangeMillis()
                val dayFmt = SimpleDateFormat("EEE", locale)
                val labels = (0 until 7).map { offset ->
                    cal.timeInMillis = startMillis
                    cal.add(Calendar.DAY_OF_YEAR, offset)
                    dayFmt.format(cal.time)
                }
                val totals = FloatArray(7)
                for (tx in transactions) {
                    val date = FormatUtils.parseTransactionDate(tx.date) ?: continue
                    val txDay = StatisticsFilter.normalizeStartOfDay(date.time)
                    val idx = ((txDay - startMillis) / 86_400_000L).toInt()
                    if (idx in 0 until 7) totals[idx] += tx.amountValue.toFloat()
                }
                labels to totals
            }
            StatisticsPeriodType.MONTH -> {
                val days = filter.anchorMonth.daysInMonth()
                val labels = (1..days).map { it.toString() }
                val totals = calculateDailyTotals(transactions, filter.anchorMonth)
                labels to totals
            }
            StatisticsPeriodType.YEAR -> {
                val shortMonths = context.resources.getStringArray(R.array.months_short)
                val labels = (0 until 12).map { shortMonths.getOrElse(it) { "${it + 1}" } }
                val totals = FloatArray(12)
                for (tx in transactions) {
                    val date = FormatUtils.parseTransactionDate(tx.date) ?: continue
                    cal.time = date
                    if (cal.get(Calendar.YEAR) == filter.anchorYear) {
                        val mIdx = cal.get(Calendar.MONTH).coerceIn(0, 11)
                        totals[mIdx] += tx.amountValue.toFloat()
                    }
                }
                labels to totals
            }
            StatisticsPeriodType.CUSTOM -> {
                val (startMillis, endMillis) = filter.dateRangeMillis()
                val dayCount = (((endMillis - startMillis) / 86_400_000L) + 1L).coerceAtLeast(1L).toInt()
                if (dayCount <= 31) {
                    val dayFmt = SimpleDateFormat("dd/MM", locale)
                    val labels = (0 until dayCount).map { offset ->
                        cal.timeInMillis = startMillis
                        cal.add(Calendar.DAY_OF_YEAR, offset)
                        dayFmt.format(cal.time)
                    }
                    val totals = FloatArray(dayCount)
                    for (tx in transactions) {
                        val date = FormatUtils.parseTransactionDate(tx.date) ?: continue
                        val txDay = StatisticsFilter.normalizeStartOfDay(date.time)
                        val idx = ((txDay - startMillis) / 86_400_000L).toInt()
                        if (idx in 0 until dayCount) totals[idx] += tx.amountValue.toFloat()
                    }
                    labels to totals
                } else {
                    val monthKeys = mutableListOf<YearMonthKey>()
                    val cursor = Calendar.getInstance().apply {
                        timeInMillis = startMillis
                        set(Calendar.DAY_OF_MONTH, 1)
                    }
                    val endCal = Calendar.getInstance().apply { timeInMillis = endMillis }
                    val endKey = YearMonthKey(endCal.get(Calendar.YEAR), endCal.get(Calendar.MONTH))
                    var guard = 0
                    while (guard++ < 36) {
                        val key = YearMonthKey(cursor.get(Calendar.YEAR), cursor.get(Calendar.MONTH))
                        monthKeys.add(key)
                        if (key == endKey) break
                        cursor.add(Calendar.MONTH, 1)
                    }
                    val shortMonths = context.resources.getStringArray(R.array.months_short)
                    val labels = monthKeys.map { k ->
                        "${shortMonths.getOrElse(k.monthIndex) { "${k.monthIndex + 1}" }} ${k.year % 100}"
                    }
                    val indexMap = monthKeys.withIndex().associate { it.value to it.index }
                    val totals = FloatArray(monthKeys.size)
                    for (tx in transactions) {
                        val date = FormatUtils.parseTransactionDate(tx.date) ?: continue
                        val idx = indexMap[YearMonthKey.fromDate(date)] ?: continue
                        totals[idx] += tx.amountValue.toFloat()
                    }
                    labels to totals
                }
            }
        }
    }

    fun calculateDailyTotals(
        transactions: List<TransactionModel>,
        yearMonth: YearMonthKey
    ): FloatArray {
        val days = yearMonth.daysInMonth()
        val totals = FloatArray(days)
        val cal = Calendar.getInstance()
        for (tx in transactions) {
            val date = FormatUtils.parseTransactionDate(tx.date) ?: continue
            cal.time = date
            if (cal.get(Calendar.YEAR) == yearMonth.year && cal.get(Calendar.MONTH) == yearMonth.monthIndex) {
                val dayIdx = (cal.get(Calendar.DAY_OF_MONTH) - 1).coerceIn(0, days - 1)
                totals[dayIdx] += tx.amountValue.toFloat()
            }
        }
        return totals
    }

    /**
     * Groups a month's Income & Expense transactions into 7 evenly distributed day buckets
     * so the Home Overview dual-line chart always renders a smooth, scannable spline wave.
     */
    fun calculateHomeOverviewSeries(
        transactions: List<TransactionModel>,
        yearMonth: YearMonthKey
    ): HomeOverviewSeries {
        val bucketCount = 7
        val days = yearMonth.daysInMonth()
        val bucketBounds = (0 until bucketCount).map { i ->
            val startDay = (i * days) / bucketCount + 1
            val endDay = ((i + 1) * days) / bucketCount
            startDay to endDay
        }
        val labels = bucketBounds.map { (start, end) ->
            if (start == end) "$start" else "$start-$end"
        }

        val incomeValues = FloatArray(bucketCount)
        val expenseValues = FloatArray(bucketCount)
        var hasData = false
        val cal = Calendar.getInstance()

        for (tx in transactions) {
            if (tx.type != TransactionType.INCOME && tx.type != TransactionType.EXPENSE) continue
            val date = FormatUtils.parseTransactionDate(tx.date) ?: continue
            cal.time = date
            if (cal.get(Calendar.YEAR) != yearMonth.year || cal.get(Calendar.MONTH) != yearMonth.monthIndex) {
                continue
            }
            val dayOfMonth = cal.get(Calendar.DAY_OF_MONTH).coerceIn(1, days)
            val bucketIdx = bucketBounds.indexOfFirst { (s, e) -> dayOfMonth in s..e }
                .takeIf { it >= 0 } ?: (bucketCount - 1)
            val amount = tx.amountValue.toFloat()
            if (amount > 0f) hasData = true
            if (tx.type == TransactionType.INCOME) {
                incomeValues[bucketIdx] += amount
            } else {
                expenseValues[bucketIdx] += amount
            }
        }

        return HomeOverviewSeries(
            labels = labels,
            incomeValues = incomeValues,
            expenseValues = expenseValues,
            hasData = hasData
        )
    }

    fun calculateMonthlyTotalsForType(
        transactions: List<TransactionModel>,
        targetYear: Int,
        type: TransactionType
    ): FloatArray {
        val totals = FloatArray(12)
        val cal = Calendar.getInstance()
        for (tx in transactions) {
            if (!matchesTypeFilter(tx, type)) continue
            val date = FormatUtils.parseTransactionDate(tx.date) ?: continue
            cal.time = date
            if (cal.get(Calendar.YEAR) == targetYear) {
                val monthIdx = cal.get(Calendar.MONTH).coerceIn(0, 11)
                totals[monthIdx] += tx.amountValue.toFloat()
            }
        }
        return totals
    }

    fun groupByDisplayDay(transactions: List<TransactionModel>): List<DayTransactionGroup> =
        groupByDisplayDayUnified(transactions, loanOnlyMode = false)

    fun groupByDisplayDayUnified(
        transactions: List<TransactionModel>,
        loanOnlyMode: Boolean = false
    ): List<DayTransactionGroup> {
        val grouped = LinkedHashMap<String, MutableList<Triple<Date?, TransactionModel, Int>>>()
        transactions.forEachIndexed { idx, tx ->
            val date = FormatUtils.parseTransactionDate(tx.date)
            val dayKey = if (date != null) FormatUtils.formatDayGroupKey(date) else tx.date.ifBlank { "unknown" }
            grouped.getOrPut(dayKey) { mutableListOf() }.add(Triple(date, tx, idx))
        }

        return grouped.entries
            .sortedByDescending { (_, entries) -> entries.first().first?.time ?: 0L }
            .map { (dayKey, entries) ->
                val sortedTransactions = entries
                    .sortedWith(
                        compareByDescending<Triple<Date?, TransactionModel, Int>> { it.second.time }
                            .thenByDescending { it.third }
                    )
                    .map { it.second }

                val treatAsLoanDay = loanOnlyMode || sortedTransactions.all {
                    it.type == TransactionType.LOAN || it.type == TransactionType.BORROW
                }

                val netAmount = if (treatAsLoanDay) {
                    sortedTransactions.sumOf { tx ->
                        if (isBorrowTransaction(tx)) -tx.amountValue else tx.amountValue
                    }
                } else {
                    sortedTransactions.sumOf { tx ->
                        when (tx.type) {
                            TransactionType.EXPENSE -> -tx.amountValue
                            TransactionType.INCOME -> tx.amountValue
                            else -> 0.0
                        }
                    }
                }

                DayTransactionGroup(
                    dateKey = dayKey,
                    date = entries.first().first,
                    transactions = sortedTransactions,
                    netAmount = netAmount
                )
            }
    }

    fun calculateSummary(
        transactions: List<TransactionModel>,
        yearMonth: YearMonthKey
    ): TransactionSummary {
        var totalBalance = 0.0
        var monthIncome = 0.0
        var monthExpense = 0.0
        var monthLoan = 0.0

        for (tx in transactions) {
            val amount = tx.amountValue
            when (tx.type) {
                TransactionType.INCOME -> totalBalance += amount
                TransactionType.EXPENSE -> totalBalance -= amount
                else -> Unit
            }

            val txDate = FormatUtils.parseTransactionDate(tx.date) ?: continue
            if (YearMonthKey.fromDate(txDate) == yearMonth) {
                when (tx.type) {
                    TransactionType.INCOME -> monthIncome += amount
                    TransactionType.EXPENSE -> monthExpense += amount
                    TransactionType.LOAN, TransactionType.BORROW -> monthLoan += amount
                }
            }
        }

        return TransactionSummary(
            totalBalance = totalBalance,
            monthIncome = monthIncome,
            monthExpense = monthExpense,
            monthLoan = monthLoan
        )
    }

    fun calculateCategoryTotals(transactions: List<TransactionModel>): List<Pair<String, Double>> {
        val totals = LinkedHashMap<String, Double>()
        for (tx in transactions) {
            val category = tx.categoryName
            totals[category] = (totals[category] ?: 0.0) + tx.amountValue
        }
        return totals.entries
            .sortedByDescending { it.value }
            .map { it.key to it.value }
    }

    fun calculateYearlyMonthlyStatistics(
        context: Context,
        transactions: List<TransactionModel>,
        targetYear: Int = Calendar.getInstance().get(Calendar.YEAR)
    ): List<MonthlyStatisticsModel> {
        val monthLabels = context.resources.getStringArray(R.array.months_short)
        val monthlyModels = (0 until 12).map { index ->
            MonthlyStatisticsModel(month = monthLabels.getOrElse(index) { "${index + 1}" })
        }
        val calendar = Calendar.getInstance()

        for (tx in transactions) {
            val txDate = FormatUtils.parseTransactionDate(tx.date) ?: continue
            calendar.time = txDate
            if (calendar.get(Calendar.YEAR) != targetYear) continue

            val model = monthlyModels[calendar.get(Calendar.MONTH)]
            val amount = tx.amountValue

            when {
                tx.type == TransactionType.EXPENSE -> model.expend += amount
                tx.type == TransactionType.INCOME -> model.income += amount
                isBorrowTransaction(tx) -> model.borrow += amount
                tx.type == TransactionType.LOAN -> model.loan += amount
            }
            model.balance = model.income - model.expend
        }

        return monthlyModels
    }

    fun calculateAllTimeTotals(transactions: List<TransactionModel>): Triple<Double, Double, Double> {
        var totalExpend = 0.0
        var totalIncome = 0.0
        for (tx in transactions) {
            val amount = tx.amountValue
            when (tx.type) {
                TransactionType.EXPENSE -> totalExpend += amount
                TransactionType.INCOME -> totalIncome += amount
                else -> Unit
            }
        }
        val totalBalance = totalIncome - totalExpend
        return Triple(totalBalance, totalExpend, totalIncome)
    }

    companion object {
        fun isBorrowTransaction(tx: TransactionModel): Boolean =
            tx.type == TransactionType.BORROW ||
                (tx.type == TransactionType.LOAN && tx.categoryName.equals("Borrow", ignoreCase = true))
    }
}
