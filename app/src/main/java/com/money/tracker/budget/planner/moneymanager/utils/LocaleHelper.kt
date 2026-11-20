package com.money.tracker.budget.planner.moneymanager.utils

import android.content.Context
import android.content.res.Configuration
import com.cscmobi.libraryads.commons.sharepreference.CSCSPFHolder
import com.money.tracker.budget.planner.moneymanager.data.AppPreferences
import java.util.Locale

object LocaleHelper {

    fun getSavedLanguage(context: Context): String {
        val cscLang = runCatching { CSCSPFHolder.get().language_code_selected }.getOrNull()
        if (!cscLang.isNullOrBlank()) {
            return cscLang
        }
        return AppPreferences.readSavedLanguageCode(context)
    }

    fun saveLanguage(context: Context, languageCode: String) {
        if (languageCode.isNotEmpty()) {
            AppPreferences.getInstance(context).setLanguageCode(languageCode)
            runCatching { CSCSPFHolder.get().language_code_selected = languageCode }
        }
    }

    fun resolveLocale(languageCode: String): Locale {
        if (languageCode.isBlank()) return Locale.getDefault()
        return when {
            languageCode.equals("in", ignoreCase = true) || languageCode.equals("id", ignoreCase = true) ->
                @Suppress("DEPRECATION") Locale("in")
            languageCode.equals("tl", ignoreCase = true) || languageCode.equals("fil", ignoreCase = true) ->
                @Suppress("DEPRECATION") Locale("tl")
            languageCode.contains("-") -> Locale.forLanguageTag(languageCode)
            else -> Locale.forLanguageTag(languageCode)
        }
    }

    fun currentLocale(context: Context): Locale = resolveLocale(getSavedLanguage(context))

    fun wrapContext(base: Context): Context {
        val language = getSavedLanguage(base)
        if (language.isBlank()) return base
        val locale = resolveLocale(language)
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration).apply {
            setLocale(locale)
            setLayoutDirection(locale)
        }
        return base.createConfigurationContext(config)
    }

    @Suppress("DEPRECATION")
    fun applySavedLocale(context: Context) {
        val language = getSavedLanguage(context)
        val locale = if (language.isEmpty()) Locale.getDefault() else resolveLocale(language)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration).apply {
            setLocale(locale)
            setLayoutDirection(locale)
        }
        context.resources.updateConfiguration(config, context.resources.displayMetrics)
    }

    @Suppress("DEPRECATION")
    fun changeLanguage(context: Context, languageCode: String) {
        if (languageCode.isBlank()) return
        saveLanguage(context, languageCode)
        val locale = resolveLocale(languageCode)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration).apply {
            setLocale(locale)
            setLayoutDirection(locale)
        }
        context.resources.updateConfiguration(config, context.resources.displayMetrics)
    }

    fun localizedContext(context: Context): Context = wrapContext(context)
}
