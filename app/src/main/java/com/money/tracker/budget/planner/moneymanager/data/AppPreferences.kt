package com.money.tracker.budget.planner.moneymanager.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.google.gson.Gson

/**
 * Centralized SharedPreferences facade preserving exact legacy preference file names and keys
 * so existing user data upgrades seamlessly.
 */
class AppPreferences private constructor(context: Context) {

    private val appContext: Context = context.applicationContext ?: context

    val gson: Gson = Gson()

    private val mainPrefs: SharedPreferences =
        appContext.getSharedPreferences(PREF_AI_MONEY, Context.MODE_PRIVATE)
    private val currencyPrefs: SharedPreferences =
        appContext.getSharedPreferences(PREF_CURRENCY, Context.MODE_PRIVATE)
    private val dataPrefs: SharedPreferences =
        appContext.getSharedPreferences(PREF_DATA, Context.MODE_PRIVATE)
    private val budgetPrefs: SharedPreferences =
        appContext.getSharedPreferences(PREF_BUDGET, Context.MODE_PRIVATE)
    private val notificationPrefs: SharedPreferences =
        appContext.getSharedPreferences(PREF_NOTI, Context.MODE_PRIVATE)
    private val permissionPrefs: SharedPreferences =
        appContext.getSharedPreferences(PREF_PERMISSION, Context.MODE_PRIVATE)
    private val languageFlagPrefs: SharedPreferences =
        appContext.getSharedPreferences(PREF_LANGUAGE_FLAG, Context.MODE_PRIVATE)

    // Transactions & counter
    fun getTransactionListJson(): String = mainPrefs.getString(KEY_TRANSACTION_LIST, "").orEmpty()

    fun setTransactionListJson(json: String) {
        mainPrefs.edit { putString(KEY_TRANSACTION_LIST, json) }
    }

    fun getLastTransactionJson(): String = mainPrefs.getString(KEY_TRANSACTION_DATA, "").orEmpty()

    fun getCounterValue(): Int = mainPrefs.getInt(KEY_COUNTER, 0)

    fun incrementCounter() {
        mainPrefs.edit { putInt(KEY_COUNTER, getCounterValue() + 1) }
    }

    // Currency
    fun getSelectedCurrencyRaw(): String = currencyPrefs.getString(KEY_SELECTED_CURRENCY, "").orEmpty()

    fun getSelectedCurrencyOrDefault(defaultSymbol: String = "$"): String =
        getSelectedCurrencyRaw().ifEmpty { defaultSymbol }

    fun setSelectedCurrency(symbol: String) {
        currencyPrefs.edit { putString(KEY_SELECTED_CURRENCY, symbol) }
    }

    fun getSelectedCurrencyCode(): String =
        currencyPrefs.getString(KEY_SELECTED_CURRENCY_CODE, "").orEmpty()

    fun setSelectedCurrencyCode(code: String) {
        currencyPrefs.edit { putString(KEY_SELECTED_CURRENCY_CODE, code) }
    }

    // Language & app metadata
    fun getLanguageCode(): String = dataPrefs.getString(KEY_LANGUAGE, "en").orEmpty()

    fun setLanguageCode(code: String) {
        dataPrefs.edit { putString(KEY_LANGUAGE, code) }
    }

    fun setLanguageSelectedFlag(selected: Boolean) {
        languageFlagPrefs.edit { putBoolean(KEY_LANGUAGE_FLAG, selected) }
    }

    fun getAppLaunchCount(): Int = dataPrefs.getInt(KEY_APP_LAUNCH_COUNT, 0)

    fun setAppLaunchCount(count: Int) {
        dataPrefs.edit { putInt(KEY_APP_LAUNCH_COUNT, count) }
    }

    fun isOrganic(): Boolean = dataPrefs.getBoolean(KEY_ORGANIC, true)

    fun setOrganic(value: Boolean) {
        dataPrefs.edit { putBoolean(KEY_ORGANIC, value) }
    }

    // Budget
    fun getBudgetListJson(): String = budgetPrefs.getString(KEY_BUDGETS, "").orEmpty()

    fun setBudgetListJson(json: String) {
        budgetPrefs.edit { putString(KEY_BUDGETS, json) }
    }

    fun getTotalBudget(): Double {
        val exact = budgetPrefs.getString(KEY_TOTAL_BUDGET_EXACT, null)?.toDoubleOrNull()
        return exact ?: budgetPrefs.getFloat(KEY_TOTAL_BUDGET, 0f).toDouble()
    }

    fun setTotalBudget(amount: Double) {
        budgetPrefs.edit {
            putFloat(KEY_TOTAL_BUDGET, amount.toFloat())
            putString(KEY_TOTAL_BUDGET_EXACT, amount.toString())
        }
    }

    fun getTotalExpenses(): Double {
        val exact = budgetPrefs.getString(KEY_TOTAL_EXPENSES_EXACT, null)?.toDoubleOrNull()
        return exact ?: budgetPrefs.getFloat(KEY_TOTAL_EXPENSES, 0f).toDouble()
    }

    fun setTotalExpenses(amount: Double) {
        budgetPrefs.edit {
            putFloat(KEY_TOTAL_EXPENSES, amount.toFloat())
            putString(KEY_TOTAL_EXPENSES_EXACT, amount.toString())
        }
    }

    // Notifications & Permissions
    fun getLastUnlockNotificationTime(): Long =
        notificationPrefs.getLong(KEY_LAST_NOTIFICATION_TIME, 0L)

    fun setLastUnlockNotificationTime(timestamp: Long) {
        notificationPrefs.edit { putLong(KEY_LAST_NOTIFICATION_TIME, timestamp) }
    }

    fun getLastTaskRemovedNotificationTime(): Long =
        notificationPrefs.getLong(KEY_LAST_REMOVED_TIME, 0L)

    fun setLastTaskRemovedNotificationTime(timestamp: Long) {
        notificationPrefs.edit { putLong(KEY_LAST_REMOVED_TIME, timestamp) }
    }

    fun setShownNotificationSheet(shown: Boolean) {
        permissionPrefs.edit { putBoolean(KEY_SHOWN_NOTI_SHEET, shown) }
    }

    companion object {
        private const val PREF_AI_MONEY = "AI_MONEY_NAME"
        private const val PREF_CURRENCY = "CurrencyPrefs"
        private const val PREF_DATA = "data"
        private const val PREF_BUDGET = "BudgetManagerPrefs"
        private const val PREF_NOTI = "noti_prefs"
        private const val PREF_PERMISSION = "permission_flags"
        private const val PREF_LANGUAGE_FLAG = "LANGUAGE"

        private const val KEY_TRANSACTION_LIST = "transactionList"
        private const val KEY_TRANSACTION_DATA = "transaction_data"
        private const val KEY_COUNTER = "counter_value"
        private const val KEY_SELECTED_CURRENCY = "SelectedCurrency"
        private const val KEY_SELECTED_CURRENCY_CODE = "SelectedCurrencyCode"
        private const val KEY_LANGUAGE = "KEY_LANGUAGE"
        private const val KEY_LANGUAGE_FLAG = "language"
        private const val KEY_APP_LAUNCH_COUNT = "appLaunchCount"
        private const val KEY_ORGANIC = "organic"
        private const val KEY_BUDGETS = "budgets"
        private const val KEY_TOTAL_BUDGET = "total_budget"
        private const val KEY_TOTAL_BUDGET_EXACT = "total_budget_exact"
        private const val KEY_TOTAL_EXPENSES = "total_expenses"
        private const val KEY_TOTAL_EXPENSES_EXACT = "total_expenses_exact"
        private const val KEY_LAST_NOTIFICATION_TIME = "last_notification_time"
        private const val KEY_LAST_REMOVED_TIME = "last_removed_time"
        private const val KEY_SHOWN_NOTI_SHEET = "shown_noti_sheet"

        @Volatile
        private var instance: AppPreferences? = null

        fun getInstance(context: Context): AppPreferences {
            val appCtx = context.applicationContext
                ?: return AppPreferences(context)
            return instance ?: synchronized(this) {
                instance ?: AppPreferences(appCtx).also { instance = it }
            }
        }

        /**
         * Safe to call inside Application.attachBaseContext() before applicationContext is attached.
         */
        fun readSavedLanguageCode(context: Context): String {
            instance?.let { return it.getLanguageCode() }
            return runCatching {
                val safeCtx = context.applicationContext ?: context
                safeCtx.getSharedPreferences(PREF_DATA, Context.MODE_PRIVATE)
                    .getString(KEY_LANGUAGE, "en")
                    .orEmpty()
            }.getOrDefault("en")
        }
    }
}
