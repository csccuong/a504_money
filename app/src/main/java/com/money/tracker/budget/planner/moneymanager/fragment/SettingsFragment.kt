package com.money.tracker.budget.planner.moneymanager.fragment

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.cscmobi.libraryads.commons.utils.Constants
import com.cscmobi.libraryads.views.language.CSCLanguageActivity
import com.money.tracker.budget.planner.moneymanager.R
import com.money.tracker.budget.planner.moneymanager.activity.CurrencyUnitActivity
import com.money.tracker.budget.planner.moneymanager.ads.AdsManager
import com.money.tracker.budget.planner.moneymanager.base.BaseFragment
import com.money.tracker.budget.planner.moneymanager.data.AppPreferences
import com.money.tracker.budget.planner.moneymanager.data.CurrencyCatalog
import com.money.tracker.budget.planner.moneymanager.databinding.FragmentSettingsBinding
import com.money.tracker.budget.planner.moneymanager.utils.LocaleHelper
import com.money.tracker.budget.planner.moneymanager.utils.language.LanguageCatalog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SettingsFragment : BaseFragment<FragmentSettingsBinding>(FragmentSettingsBinding::inflate) {

    private val preferences by lazy { AppPreferences.getInstance(requireContext()) }
    private var isShareProcessing = false

    private val currencySelectLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            updateSelectionBadges()
        }
    }

    override fun setupView(savedInstanceState: Bundle?) {
        updateSelectionBadges()
        setupClickListeners()
    }

    override fun onResume() {
        super.onResume()
        AdsManager.enableAOA()
        updateSelectionBadges()
    }

    private fun setupClickListeners() {
        binding.btnShare.setOnClickListener { shareApp() }

        binding.btnLanguage.setOnClickListener {
            val intent = Intent(requireContext(), CSCLanguageActivity::class.java).apply {
                putExtra(Constants.FROM_SETTING, true)
            }
            startActivity(intent)
        }

        binding.btnRateUs.setOnClickListener { openPlayStore() }

        binding.btnPrivacyPolicy.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_POLICY_URL)))
        }

        binding.llCurrency.setOnClickListener {
            val intent = Intent(requireContext(), CurrencyUnitActivity::class.java).apply {
                putExtra(CurrencyUnitActivity.EXTRA_FROM_SETTINGS, true)
            }
            currencySelectLauncher.launch(intent)
        }
    }

    private fun shareApp() {
        if (isShareProcessing) return
        isShareProcessing = true

        val appPackage = requireContext().packageName
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name))
            putExtra(Intent.EXTRA_TEXT, "https://play.google.com/store/apps/details?id=$appPackage")
        }
        AdsManager.disableAOA()
        startActivity(Intent.createChooser(shareIntent, getString(R.string.share)))

        viewLifecycleOwner.lifecycleScope.launch {
            delay(SHARE_DEBOUNCE_MS)
            isShareProcessing = false
        }
    }

    private fun openPlayStore() {
        val appPackage = requireContext().packageName
        val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$appPackage")).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NO_HISTORY or
                    Intent.FLAG_ACTIVITY_NEW_DOCUMENT or
                    Intent.FLAG_ACTIVITY_MULTIPLE_TASK
            )
        }
        try {
            startActivity(marketIntent)
        } catch (_: ActivityNotFoundException) {
            startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$appPackage"))
            )
        }
    }

    private fun updateSelectionBadges() {
        val ctx = requireContext()
        val currencyCode = preferences.getSelectedCurrencyCode()
        val currencySymbol = preferences.getSelectedCurrencyRaw()

        val langCode = LocaleHelper.getSavedLanguage(ctx)
        binding.tvLanguage.text = LanguageCatalog.displayNameForCode(ctx, langCode)
    }

    private companion object {
        const val PRIVACY_POLICY_URL = "https://docs.google.com/document/d/1JUD-uBWf7Nd-aGjyydJVwrg4nJOwW5XT"
        const val SHARE_DEBOUNCE_MS = 1_000L
    }
}
