package com.money.tracker.budget.planner.moneymanager.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import com.money.tracker.budget.planner.moneymanager.Application
import com.money.tracker.budget.planner.moneymanager.R
import com.money.tracker.budget.planner.moneymanager.base.BaseActivity
import com.money.tracker.budget.planner.moneymanager.data.AppPreferences
import com.money.tracker.budget.planner.moneymanager.databinding.ActivityLanguageSettingBinding
import com.money.tracker.budget.planner.moneymanager.utils.LocaleHelper
import com.money.tracker.budget.planner.moneymanager.utils.language.LanguageCatalog

class LanguageSettingActivity :
    BaseActivity<ActivityLanguageSettingBinding>(ActivityLanguageSettingBinding::inflate) {

    override val applySavedLocale: Boolean = true

    private var selectedLangCode: String = "en"

    override fun setupView(savedInstanceState: Bundle?) {
        selectedLangCode = LocaleHelper.getSavedLanguage(this).ifEmpty { "en" }

        refreshLanguageData()
        binding.uiLanguage.selectLanguageCode(selectedLangCode)
        binding.uiLanguage.setOnLanguageInteractionListener { _, _, codeLang ->
            if (codeLang.isNotEmpty()) {
                selectedLangCode = codeLang
                LocaleHelper.changeLanguage(this, selectedLangCode)
                binding.tvTitle.setText(R.string.languages)
                binding.tvPleaseLanguage.setText(R.string.please_select_language_to_continue)
                binding.ivSelect.setText(R.string.save)
                refreshLanguageData()
                binding.uiLanguage.selectLanguageCode(selectedLangCode)
            }
        }

        binding.ivSelect.setOnClickListener {
            LocaleHelper.saveLanguage(this, selectedLangCode)
            AppPreferences.getInstance(this).setLanguageSelectedFlag(true)
            Application.refreshDynamicShortcuts(this)
            restartMainActivity()
        }

        binding.ivBack.setOnClickListener { finish() }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })
    }

    private fun refreshLanguageData() {
        binding.uiLanguage.updateData(
            englishList = LanguageCatalog.getEnglishVariants(this),
            indianList = LanguageCatalog.getIndianLanguages(this),
            portugueseList = LanguageCatalog.getPortugueseVariants(this),
            otherList = LanguageCatalog.getOtherLanguages(this)
        )
    }

    private fun restartMainActivity() {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        startActivity(intent)
        finish()
    }
}
