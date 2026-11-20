package com.money.tracker.budget.planner.moneymanager.model

import android.content.Context
import com.money.tracker.budget.planner.moneymanager.utils.LocaleHelper
import com.money.tracker.budget.planner.moneymanager.utils.YearMonthKey
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date

enum class StatisticsPeriodType {
    WEEK,
    MONTH,
    YEAR,
    CUSTOM
}

enum class LoanDirectionFilter {
    ALL,
    LENT_ONLY,
    BORROWED_ONLY
}

enum class BreakdownSortMode {
    AMOUNT_DESC,
    COUNT_DESC
}

data class StatisticsFilter(
    val periodType: StatisticsPeriodType = StatisticsPeriodType.MONTH,
    val anchorMonth: YearMonthKey = YearMonthKey.current(),
    val anchorYear: Int = YearMonthKey.current().year,
    val weekStartMillis: Long = startOfCurrentWeekMillis(),
    val customStartMillis: Long = startOfDaysAgoMillis(29),
    val customEndMillis: Long = endOfTodayMillis(),
    val selectedCategories: Set<String> = emptySet(),
    val loanDirection: LoanDirectionFilter = LoanDirectionFilter.ALL,
    val sortMode: BreakdownSortMode = BreakdownSortMode.AMOUNT_DESC
) {

    fun dateRangeMillis(): Pair<Long, Long> = when (periodType) {
        StatisticsPeriodType.WEEK -> {
            val start = normalizeStartOfDay(weekStartMillis)
            val end = Calendar.getInstance().apply {
                timeInMillis = start
                add(Calendar.DAY_OF_YEAR, 6)
                setToEndOfDay()
            }.timeInMillis
            start to end
        }
        StatisticsPeriodType.MONTH -> {
            val startCal = Calendar.getInstance().apply {
                set(anchorMonth.year, anchorMonth.monthIndex, 1)
                setToStartOfDay()
            }
            val endCal = Calendar.getInstance().apply {
                set(anchorMonth.year, anchorMonth.monthIndex, anchorMonth.daysInMonth())
                setToEndOfDay()
            }
            startCal.timeInMillis to endCal.timeInMillis
        }
        StatisticsPeriodType.YEAR -> {
            val startCal = Calendar.getInstance().apply {
                set(anchorYear, Calendar.JANUARY, 1)
                setToStartOfDay()
            }
            val endCal = Calendar.getInstance().apply {
                set(anchorYear, Calendar.DECEMBER, 31)
                setToEndOfDay()
            }
            startCal.timeInMillis to endCal.timeInMillis
        }
        StatisticsPeriodType.CUSTOM -> {
            val start = normalizeStartOfDay(minOf(customStartMillis, customEndMillis))
            val end = normalizeEndOfDay(maxOf(customStartMillis, customEndMillis))
            start to end
        }
    }

    fun previousDateRangeMillis(): Pair<Long, Long> = when (periodType) {
        StatisticsPeriodType.WEEK -> {
            val (curStart, _) = dateRangeMillis()
            val prevStart = Calendar.getInstance().apply {
                timeInMillis = curStart
                add(Calendar.DAY_OF_YEAR, -7)
                setToStartOfDay()
            }.timeInMillis
            val prevEnd = Calendar.getInstance().apply {
                timeInMillis = prevStart
                add(Calendar.DAY_OF_YEAR, 6)
                setToEndOfDay()
            }.timeInMillis
            prevStart to prevEnd
        }
        StatisticsPeriodType.MONTH -> copy(anchorMonth = anchorMonth.previous()).dateRangeMillis()
        StatisticsPeriodType.YEAR -> copy(anchorYear = anchorYear - 1).dateRangeMillis()
        StatisticsPeriodType.CUSTOM -> {
            val (curStart, curEnd) = dateRangeMillis()
            val daysSpan = (((curEnd - curStart) / DAY_MILLIS) + 1L).coerceAtLeast(1L).toInt()
            val prevEnd = Calendar.getInstance().apply {
                timeInMillis = curStart
                add(Calendar.DAY_OF_YEAR, -1)
                setToEndOfDay()
            }.timeInMillis
            val prevStart = Calendar.getInstance().apply {
                timeInMillis = curStart
                add(Calendar.DAY_OF_YEAR, -daysSpan)
                setToStartOfDay()
            }.timeInMillis
            prevStart to prevEnd
        }
    }

    fun stepPrevious(): StatisticsFilter = when (periodType) {
        StatisticsPeriodType.WEEK -> copy(
            weekStartMillis = Calendar.getInstance().apply {
                timeInMillis = weekStartMillis
                add(Calendar.DAY_OF_YEAR, -7)
                setToStartOfDay()
            }.timeInMillis
        )
        StatisticsPeriodType.MONTH -> {
            val prev = anchorMonth.previous()
            copy(anchorMonth = prev, anchorYear = prev.year)
        }
        StatisticsPeriodType.YEAR -> copy(anchorYear = anchorYear - 1)
        StatisticsPeriodType.CUSTOM -> {
            val (prevStart, prevEnd) = previousDateRangeMillis()
            copy(customStartMillis = prevStart, customEndMillis = prevEnd)
        }
    }

    fun stepNext(): StatisticsFilter = when (periodType) {
        StatisticsPeriodType.WEEK -> copy(
            weekStartMillis = Calendar.getInstance().apply {
                timeInMillis = weekStartMillis
                add(Calendar.DAY_OF_YEAR, 7)
                setToStartOfDay()
            }.timeInMillis
        )
        StatisticsPeriodType.MONTH -> {
            val next = anchorMonth.next()
            copy(anchorMonth = next, anchorYear = next.year)
        }
        StatisticsPeriodType.YEAR -> copy(anchorYear = anchorYear + 1)
        StatisticsPeriodType.CUSTOM -> {
            val (curStart, curEnd) = dateRangeMillis()
            val daysSpan = (((curEnd - curStart) / DAY_MILLIS) + 1L).coerceAtLeast(1L).toInt()
            val nextStart = Calendar.getInstance().apply {
                timeInMillis = curEnd
                add(Calendar.DAY_OF_YEAR, 1)
                setToStartOfDay()
            }.timeInMillis
            val nextEnd = Calendar.getInstance().apply {
                timeInMillis = nextStart
                add(Calendar.DAY_OF_YEAR, daysSpan - 1)
                setToEndOfDay()
            }.timeInMillis
            copy(customStartMillis = nextStart, customEndMillis = nextEnd)
        }
    }

    fun resetPeriodToCurrent(): StatisticsFilter {
        val currentMonth = YearMonthKey.current()
        return when (periodType) {
            StatisticsPeriodType.WEEK -> copy(weekStartMillis = startOfCurrentWeekMillis())
            StatisticsPeriodType.MONTH -> copy(anchorMonth = currentMonth, anchorYear = currentMonth.year)
            StatisticsPeriodType.YEAR -> copy(anchorYear = currentMonth.year)
            StatisticsPeriodType.CUSTOM -> copy(
                periodType = StatisticsPeriodType.MONTH,
                anchorMonth = currentMonth,
                anchorYear = currentMonth.year
            )
        }
    }

    fun isCurrentPeriod(): Boolean {
        val nowMonth = YearMonthKey.current()
        return when (periodType) {
            StatisticsPeriodType.WEEK -> normalizeStartOfDay(weekStartMillis) == startOfCurrentWeekMillis()
            StatisticsPeriodType.MONTH -> anchorMonth == nowMonth
            StatisticsPeriodType.YEAR -> anchorYear == nowMonth.year
            StatisticsPeriodType.CUSTOM -> false
        }
    }

    fun activeAdvancedFilterCount(currentType: TransactionType): Int {
        var count = 0
        if (selectedCategories.isNotEmpty()) count += 1
        if (currentType == TransactionType.LOAN && loanDirection != LoanDirectionFilter.ALL) count += 1
        if (sortMode != BreakdownSortMode.AMOUNT_DESC) count += 1
        return count
    }

    fun displayPeriodLabel(context: Context): String {
        val locale = LocaleHelper.currentLocale(context)
        return when (periodType) {
            StatisticsPeriodType.WEEK -> {
                val (start, end) = dateRangeMillis()
                val fmt = SimpleDateFormat("dd MMM", locale)
                "${fmt.format(Date(start))} – ${fmt.format(Date(end))}"
            }
            StatisticsPeriodType.MONTH -> anchorMonth.shortDisplayLabel(context)
            StatisticsPeriodType.YEAR -> anchorYear.toString()
            StatisticsPeriodType.CUSTOM -> {
                val (start, end) = dateRangeMillis()
                val startCal = Calendar.getInstance().apply { timeInMillis = start }
                val endCal = Calendar.getInstance().apply { timeInMillis = end }
                val sameYear = startCal.get(Calendar.YEAR) == endCal.get(Calendar.YEAR)
                val fmt = SimpleDateFormat(if (sameYear) "dd MMM" else "dd MMM yy", locale)
                "${fmt.format(Date(start))} – ${fmt.format(Date(end))}"
            }
        }
    }

    fun displayFullPeriodLabel(context: Context): String = when (periodType) {
        StatisticsPeriodType.MONTH -> anchorMonth.displayLabel(context)
        else -> displayPeriodLabel(context)
    }

    companion object {
        private const val DAY_MILLIS = 86_400_000L

        fun startOfCurrentWeekMillis(): Long = Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            if (timeInMillis > System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, -7)
            }
            setToStartOfDay()
        }.timeInMillis

        fun startOfDaysAgoMillis(daysAgo: Int): Long = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -daysAgo)
            setToStartOfDay()
        }.timeInMillis

        fun endOfTodayMillis(): Long = Calendar.getInstance().apply {
            setToEndOfDay()
        }.timeInMillis

        fun normalizeStartOfDay(millis: Long): Long = Calendar.getInstance().apply {
            timeInMillis = millis
            setToStartOfDay()
        }.timeInMillis

        fun normalizeEndOfDay(millis: Long): Long = Calendar.getInstance().apply {
            timeInMillis = millis
            setToEndOfDay()
        }.timeInMillis

        /**
         * MaterialDatePicker returns UTC midnight timestamps; convert year/month/day cleanly to local midnight.
         */
        fun utcPickerMillisToLocalDay(utcMillis: Long, endOfDay: Boolean = false): Long {
            val utcCal = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
                timeInMillis = utcMillis
            }
            return Calendar.getInstance().apply {
                set(
                    utcCal.get(Calendar.YEAR),
                    utcCal.get(Calendar.MONTH),
                    utcCal.get(Calendar.DAY_OF_MONTH)
                )
                if (endOfDay) setToEndOfDay() else setToStartOfDay()
            }.timeInMillis
        }

        private fun Calendar.setToStartOfDay() {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        private fun Calendar.setToEndOfDay() {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
    }
}
