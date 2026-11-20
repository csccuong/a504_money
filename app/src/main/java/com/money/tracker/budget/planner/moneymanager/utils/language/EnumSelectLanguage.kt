package com.money.tracker.budget.planner.moneymanager.utils.language

import com.cscmobi.libraryads.data.LanguageModel
import com.money.tracker.budget.planner.moneymanager.R
import java.util.Locale

enum class EnumSelectLanguage(
    val id: Int,
    val nameLanguage: Int,
    val flag: Int,
    val code: String
) {
    ENGLISH(1, R.string.english, R.drawable.flag_us, "en"),
    VIETNAMESE(2, R.string.vietnamese, R.drawable.flag_vi, "vi"),
    SPANISH(3, R.string.spanish, R.drawable.flag_es, "es"),
    PORTUGUESE(4, R.string.portuguese, R.drawable.flag_portugese, "pt"),
    HINDI(5, R.string.hindi, R.drawable.flag_hi, "hi"),
    FRENCH(6, R.string.french, R.drawable.flag_fr, "fr"),
    GERMAN(7, R.string.germany, R.drawable.flag_de, "de"),
    INDONESIAN(8, R.string.indonesia, R.drawable.flag_indonesia, "in"),
    ARABIC(9, R.string.arabic, R.drawable.flag_arabic, "ar"),
    RUSSIAN(10, R.string.russia, R.drawable.flag_russian, "ru"),
    JAPANESE(11, R.string.japan, R.drawable.flag_japanese, "ja"),
    KOREAN(12, R.string.korea, R.drawable.flag_korea, "ko"),
    PHILIPPINES(13, R.string.philippines, R.drawable.flag_philippines, "tl"),
    THAI(14, R.string.thailand, R.drawable.flag_thailand, "th"),
    TURKISH(15, R.string.turkey, R.drawable.flag_turkish, "tr"),
    POLAND(16, R.string.poland, R.drawable.flag_poland, "pl"),
    UKRAINE(17, R.string.ukraine, R.drawable.flag_ukraine, "uk"),
    KAZAKHSTAN(18, R.string.kazakhstan, R.drawable.flag_kazakhstan, "kk");

    companion object {
        fun toLanguageModelList(): ArrayList<LanguageModel> {
            val systemLang = Locale.getDefault().language
            val normalizedSystem = if (systemLang.equals("id", ignoreCase = true)) "in" else systemLang
            val hasSystemMatch = entries.any { it.code.equals(normalizedSystem, ignoreCase = true) }
            val defaultSelectedCode = if (hasSystemMatch) normalizedSystem else "en"

            val list = arrayListOf<LanguageModel>()
            for (lang in entries) {
                list.add(
                    LanguageModel(
                        txtLanguage = lang.nameLanguage,
                        icFlag = lang.flag,
                        code = lang.code,
                        isSelect = lang.code.equals(defaultSelectedCode, ignoreCase = true)
                    )
                )
            }
            return list
        }
    }
}
