package com.money.tracker.budget.planner.moneymanager.utils.language

import android.content.Context
import com.money.tracker.budget.planner.moneymanager.R
import com.money.tracker.budget.planner.moneymanager.model.LanguageModel

object LanguageCatalog {

    fun getEnglishVariants(context: Context): List<LanguageModel> = listOf(
        LanguageModel(context.getString(R.string.english_us), "en-US", false, R.drawable.flag_us),
        LanguageModel(context.getString(R.string.english_uk), "en-GB", false, R.drawable.flag_en),
        LanguageModel(context.getString(R.string.english_canada), "en-CA", false, R.drawable.flag_ca),
        LanguageModel(context.getString(R.string.english_south_africa), "en-ZA", false, R.drawable.flag_sou)
    )

    fun getIndianLanguages(context: Context): List<LanguageModel> = listOf(
        LanguageModel(context.getString(R.string.hindi), "hi"),
        LanguageModel(context.getString(R.string.bengali), "bn"),
        LanguageModel(context.getString(R.string.marathi), "mr"),
        LanguageModel(context.getString(R.string.telugu), "te"),
        LanguageModel(context.getString(R.string.tamil), "ta"),
        LanguageModel(context.getString(R.string.urdu), "ur"),
        LanguageModel(context.getString(R.string.kannada), "kn"),
        LanguageModel(context.getString(R.string.odia), "or"),
        LanguageModel(context.getString(R.string.malayalam), "ml")
    )

    fun getPortugueseVariants(context: Context): List<LanguageModel> = listOf(
        LanguageModel(context.getString(R.string.portuguese_brazil), "pt", false, R.drawable.flag_bra),
        LanguageModel(context.getString(R.string.portuguese_europeu), "pt", false, R.drawable.flag_euro),
        LanguageModel(context.getString(R.string.portuguese_angola), "pt", false, R.drawable.flag_angola),
        LanguageModel(context.getString(R.string.portuguese_mozambique), "pt", false, R.drawable.flag_mozam)
    )

    fun getOtherLanguages(context: Context): List<LanguageModel> = listOf(
        LanguageModel(context.getString(R.string.english), "en", false, R.drawable.flag_en),
        LanguageModel(context.getString(R.string.vietnamese), "vi", false, R.drawable.flag_vi),
        LanguageModel(context.getString(R.string.germany), "de", false, R.drawable.flag_de),
        LanguageModel(context.getString(R.string.mexico), "es", false, R.drawable.flag_mexico),
        LanguageModel(context.getString(R.string.korea), "ko", false, R.drawable.flag_korea),
        LanguageModel(context.getString(R.string.russia), "ru", false, R.drawable.flag_russian),
        LanguageModel(context.getString(R.string.poland), "pl", false, R.drawable.flag_poland),
        LanguageModel(context.getString(R.string.japan), "ja", false, R.drawable.flag_japanese),
        LanguageModel(context.getString(R.string.turkey), "tr", false, R.drawable.flag_turkish),
        LanguageModel(context.getString(R.string.portuguese_brazil), "pt", false, R.drawable.flag_bra),
        LanguageModel(context.getString(R.string.portuguese_europeu), "pt", false, R.drawable.flag_euro),
        LanguageModel(context.getString(R.string.portuguese_angola), "pt", false, R.drawable.flag_angola),
        LanguageModel(context.getString(R.string.portuguese_mozambique), "pt", false, R.drawable.flag_mozam),
        LanguageModel(context.getString(R.string.hindi), "hi", false, R.drawable.flag_hi),
        LanguageModel(context.getString(R.string.indonesia), "in", false, R.drawable.flag_indonesia),
        LanguageModel(context.getString(R.string.thailand), "th", false, R.drawable.flag_thailand),
        LanguageModel(context.getString(R.string.philippines), "tl", false, R.drawable.flag_philippines),
        LanguageModel(context.getString(R.string.nigeria), "en", false, R.drawable.flag_nigeria),
        LanguageModel(context.getString(R.string.iraq), "ar", false, R.drawable.flag_iraq),
        LanguageModel(context.getString(R.string.ukraine), "uk", false, R.drawable.flag_ukraine),
        LanguageModel(context.getString(R.string.argentina), "es", false, R.drawable.flag_argentina),
        LanguageModel(context.getString(R.string.kazakhstan), "kk", false, R.drawable.flag_kazakhstan),
        LanguageModel(context.getString(R.string.arabic), "ar", false, R.drawable.flag_arabic)
    )

    fun displayNameForCode(context: Context, rawCode: String): String {
        val normalized = rawCode.trim().lowercase()
        val base = normalized.substringBefore("-").substringBefore("_")
        return when (normalized) {
            "en-us" -> "English (US)"
            "en-gb" -> "English (UK)"
            "en-ca" -> "English (CA)"
            "en-za" -> "English (ZA)"
            else -> when (base) {
                "en", "" -> "English"
                "vi" -> "Tiếng Việt"
                "es" -> "Español"
                "fr" -> "Français"
                "de" -> "Deutsch"
                "pt" -> "Português"
                "ru" -> "Русский"
                "pl" -> "Polski"
                "tr" -> "Türkçe"
                "uk" -> "Українська"
                "kk" -> "Қазақша"
                "ar" -> "العربية"
                "ja" -> "日本語"
                "ko" -> "한국어"
                "th" -> "ภาษาไทย"
                "in", "id" -> "Indonesia"
                "tl", "fil" -> "Filipino"
                "hi" -> "हिन्दी"
                "bn" -> "বাংলা"
                "mr" -> "मराठी"
                "te" -> "తెలుగు"
                "ta" -> "தமிழ்"
                "kn" -> "ಕನ್ನಡ"
                "ml" -> "മലയാളം"
                "or" -> "ଓଡ଼ିଆ"
                "ur" -> "اردو"
                else -> rawCode.uppercase().ifEmpty { context.getString(R.string.english) }
            }
        }
    }
}
