package com.money.tracker.budget.planner.moneymanager.activity

import android.content.Intent
import android.os.Bundle
import android.widget.CheckBox
import com.money.tracker.budget.planner.moneymanager.R
import com.money.tracker.budget.planner.moneymanager.ads.AdsManager
import com.money.tracker.budget.planner.moneymanager.ads.RemoteConfigs
import com.money.tracker.budget.planner.moneymanager.base.BaseActivity
import com.money.tracker.budget.planner.moneymanager.databinding.ActivityInterestBinding
import com.money.tracker.budget.planner.moneymanager.ui.common.gone
import com.money.tracker.budget.planner.moneymanager.ui.common.show

class InterestActivity : BaseActivity<ActivityInterestBinding>(ActivityInterestBinding::inflate) {

    override val applySavedLocale: Boolean = true
    override val useFullScreenFlag: Boolean = true

    private lateinit var checkBoxes: List<CheckBox>
    private var isNativeSelectLoaded = false

    override fun setupView(savedInstanceState: Bundle?) {
        checkBoxes = listOf(
            binding.cbTrackExpenses,
            binding.cbMonitorSavings,
            binding.cbAnalyzeSpending,
            binding.cbOptimizeSpending,
            binding.cbPlanInvestments
        )

        setupListeners()
        loadInitialNativeAd()
    }

    private fun setupListeners() {
        checkBoxes.forEach { checkBox ->
            checkBox.setOnCheckedChangeListener { _, _ ->
                if (!isNativeSelectLoaded) {
                    loadSelectionNativeAd()
                }
                val hasSelection = checkBoxes.any { it.isChecked }
                binding.btnContinue.isEnabled = hasSelection
                binding.btnContinue.alpha = if (hasSelection) 1.0f else 0.45f
            }
        }

        binding.btnContinue.setOnClickListener {
            startActivity(Intent(this, CurrencyUnitActivity::class.java))
        }
    }

    private fun loadInitialNativeAd() {
        setNextButtonReady(false)
        AdsManager.loadAndShowNative(
            activity = this,
            container = binding.frAds,
            placementKey = "native_all",
            adUnitIdRes = R.string.native_all,
            layoutRes = R.layout.layout_native_media,
            isEnabled = RemoteConfigs.native_all,
            requireFullAds = false,
            onLoaded = { setNextButtonReady(true) },
            onFailed = { setNextButtonReady(true) }
        )
    }

    private fun loadSelectionNativeAd() {
        setNextButtonReady(false)
        AdsManager.loadAndShowNative(
            activity = this,
            container = binding.frAds,
            placementKey = "native_all",
            adUnitIdRes = R.string.native_all,
            layoutRes = R.layout.layout_native_media_click,
            isEnabled = RemoteConfigs.native_all,
            requireFullAds = false,
            onLoaded = {
                isNativeSelectLoaded = true
                setNextButtonReady(true)
            },
            onFailed = { setNextButtonReady(true) }
        )
    }

    private fun setNextButtonReady(isReady: Boolean) {
        if (isReady) {
            binding.btnContinue.show()
            binding.btnNextLoading.gone()
        } else {
            binding.btnContinue.gone()
            binding.btnNextLoading.show()
        }
    }
}
