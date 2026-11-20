package com.money.tracker.budget.planner.moneymanager.utils

import android.content.Context
import com.money.tracker.budget.planner.moneymanager.R
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

data class YearMonthKey(
    val year: Int,
    val monthIndex: Int // 0..11 (Calendar.MONTH)
) {
    fun displayLabel(context: Context): String {
        val months = context.resources.getStringArray(R.array.months)
        val monthName = months.getOrElse(monthIndex) { "" }
        return "$monthName $year"
    }

    fun shortDisplayLabel(context: Context): String {
        val months = context.resources.getStringArray(R.array.months_short)
        val monthName = months.getOrElse(monthIndex) { "${monthIndex + 1}" }
        return "$monthName $year"
    }

    fun previous(): YearMonthKey =
        if (monthIndex == 0) YearMonthKey(year - 1, 11) else YearMonthKey(year, monthIndex - 1)

    fun next(): YearMonthKey =
        if (monthIndex == 11) YearMonthKey(year + 1, 0) else YearMonthKey(year, monthIndex + 1)

    fun daysInMonth(): Int {
        val cal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, monthIndex)
        }
        return cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    /**
     * Remaining days in this month inclusive of today (for current month),
     * full month days for future months, or 0 for past months.
     */
    fun remainingDaysInclusive(): Int {
        val now = Calendar.getInstance()
        val currentYear = now.get(Calendar.YEAR)
        val currentMonth = now.get(Calendar.MONTH)
        return when {
            year == currentYear && monthIndex == currentMonth -> {
                (daysInMonth() - now.get(Calendar.DAY_OF_MONTH) + 1).coerceAtLeast(1)
            }
            year > currentYear || (year == currentYear && monthIndex > currentMonth) -> daysInMonth()
            else -> 0
        }
    }

    /**
     * Number of elapsed days for daily-average calculation:
     * current day of month for current month, full month days for past months, 1 for future months.
     */
    fun elapsedDaysForAverage(): Int {
        val now = Calendar.getInstance()
        val currentYear = now.get(Calendar.YEAR)
        val currentMonth = now.get(Calendar.MONTH)
        return when {
            year == currentYear && monthIndex == currentMonth -> now.get(Calendar.DAY_OF_MONTH).coerceAtLeast(1)
            year < currentYear || (year == currentYear && monthIndex < currentMonth) -> daysInMonth()
            else -> 1
        }
    }

    companion object {
        fun current(): YearMonthKey {
            val cal = Calendar.getInstance()
            return YearMonthKey(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH))
        }

        fun fromDate(date: Date): YearMonthKey {
            val cal = Calendar.getInstance().apply { time = date }
            return YearMonthKey(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH))
        }
    }
}

object FormatUtils {

    private val INPUT_DATE_PATTERNS = listOf(
        "MMMM, d yyyy" to Locale.US,
        "dd/M/yyyy" to Locale.US,
        "dd/MM/yyyy" to Locale.US,
        "yyyy-MM-dd" to Locale.US,
        "EEE, dd MMMM" to Locale.US
    )

    fun formatAmount(amount: Double): String = NumberFormat.getInstance(Locale.US).apply {
        maximumFractionDigits = 2
    }.format(amount)

    fun formatAmountWithDecimals(amount: Double): String =
        NumberFormat.getInstance(Locale.US).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }.format(amount)

    fun formatCompactAmount(amount: Double): String {
        val sign = if (amount < 0) "-" else ""
        val value = abs(amount)
        return when {
            value >= 1_000_000_000.0 -> "${sign}${trimDecimal(value / 1_000_000_000.0)}B"
            value >= 1_000_000.0 -> "${sign}${trimDecimal(value / 1_000_000.0)}M"
            value >= 10_000.0 -> "${sign}${trimDecimal(value / 1_000.0)}K"
            else -> "$sign${formatAmount(value)}"
        }
    }

    private fun trimDecimal(value: Double): String {
        val formatted = String.format(Locale.US, "%.1f", value)
        return if (formatted.endsWith(".0")) formatted.dropLast(2) else formatted
    }

    fun parseTransactionDate(dateString: String?): Date? {
        if (dateString.isNullOrBlank()) return null
        val trimmed = dateString.trim()

        for ((pattern, locale) in INPUT_DATE_PATTERNS) {
            try {
                val parsed = SimpleDateFormat(pattern, locale).apply { isLenient = true }.parse(trimmed)
                if (parsed != null) return parsed
            } catch (_: Exception) {
                // Try next pattern
            }
        }
        return null
    }

    fun formatMonthYear(date: Date, locale: Locale = Locale.US): String =
        SimpleDateFormat("MMMM yyyy", locale).format(date)

    fun formatDayGroupKey(date: Date): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(date)

    fun formatDayNumber(date: Date): String = SimpleDateFormat("dd", Locale.US).format(date)

    fun formatDayOfWeek(date: Date, locale: Locale = Locale.US): String =
        SimpleDateFormat("EEEE", locale).format(date)

    fun formatInputDate(date: Date = Date()): String =
        SimpleDateFormat("dd/MM/yyyy", Locale.US).format(date)

    fun formatInputTime(date: Date = Date()): String =
        SimpleDateFormat("HH:mm", Locale.US).format(date)
}
