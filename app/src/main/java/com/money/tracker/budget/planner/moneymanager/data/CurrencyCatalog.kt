package com.money.tracker.budget.planner.moneymanager.data

import com.money.tracker.budget.planner.moneymanager.model.CurrencyUnitModel
import com.mynameismidori.currencypicker.ExtendedCurrency

object CurrencyCatalog {

    fun getAllCurrencies(): List<CurrencyUnitModel> =
        ExtendedCurrency.getAllCurrencies().map { currency ->
            CurrencyUnitModel(
                symbol = currency.symbol,
                languageName = currency.name,
                code = currency.code,
                isChecked = false,
                image = currency.flag
            )
        }

    fun findCurrency(code: String, symbol: String): CurrencyUnitModel? {
        val all = getAllCurrencies()
        if (code.isNotBlank()) {
            all.firstOrNull { it.code.equals(code, ignoreCase = true) }?.let { return it }
        }
        if (symbol.isNotBlank()) {
            // Prefer USD when legacy preference only stored "$"
            if (symbol == "$") {
                all.firstOrNull { it.code == "USD" }?.let { return it }
            }
            all.firstOrNull { it.symbol == symbol || it.code.equals(symbol, ignoreCase = true) }?.let { return it }
        }
        return all.firstOrNull { it.code == "USD" }
    }

    fun formatDisplayBadge(code: String, symbol: String): String {
        val matched = findCurrency(code, symbol)
        return when {
            matched != null && matched.symbol.equals(matched.code, ignoreCase = true) -> matched.code
            matched != null -> "${matched.code} (${matched.symbol})"
            code.isNotBlank() && symbol.isNotBlank() && !code.equals(symbol, ignoreCase = true) -> "$code ($symbol)"
            code.isNotBlank() -> code
            symbol.isNotBlank() -> symbol
            else -> "USD ($)"
        }
    }
}
