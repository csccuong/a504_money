package com.money.tracker.budget.planner.moneymanager.activity

import android.content.Intent
import android.os.Bundle
import androidx.core.widget.doOnTextChanged
import com.money.tracker.budget.planner.moneymanager.R
import com.money.tracker.budget.planner.moneymanager.adapter.CurrencyUnitAdapter
import com.money.tracker.budget.planner.moneymanager.ads.AdsManager
import com.money.tracker.budget.planner.moneymanager.ads.RemoteConfigs
import com.money.tracker.budget.planner.moneymanager.base.BaseActivity
import com.money.tracker.budget.planner.moneymanager.data.BudgetRepository
import com.money.tracker.budget.planner.moneymanager.data.CurrencyCatalog
import com.money.tracker.budget.planner.moneymanager.databinding.ActivityCurrencyUnitBinding
import com.money.tracker.budget.planner.moneymanager.ui.common.show

class CurrencyUnitActivity :
    BaseActivity<ActivityCurrencyUnitBinding>(ActivityCurrencyUnitBinding::inflate) {

    private lateinit var currencyAdapter: CurrencyUnitAdapter

    override fun setupView(savedInstanceState: Bundle?) {
        val fromSettings = intent.getBooleanExtra(EXTRA_FROM_SETTINGS, false)
        if (fromSettings) {
            binding.ivBack.show()
            binding.ivBack.setOnClickListener { finish() }
            binding.ivSelect.setText(R.string.save)
        }

        currencyAdapter = CurrencyUnitAdapter(
            context = this,
            allItems = CurrencyCatalog.getAllCurrencies()
        ) {
            setConfirmButtonEnabled(true)
        }

        binding.rvCurrencyUnit.adapter = currencyAdapter
        val initialSelected = currencyAdapter.getSelectedPosition()
        if (initialSelected >= 0) {
            binding.rvCurrencyUnit.scrollToPosition(initialSelected)
        }
        binding.etSearch.doOnTextChanged { text, _, _, _ ->
            currencyAdapter.filter(text?.toString().orEmpty())
        }

        setConfirmButtonEnabled(fromSettings)

        binding.ivSelect.setOnClickListener {
            if (fromSettings) {
                setResult(RESULT_OK, Intent())
                finish()
            } else {
                val budgetRepository = BudgetRepository(this)
                if (budgetRepository.getTotalBudget() == 0.0) {
                    startActivity(Intent(this, SetBudgetActivity::class.java))
                } else {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
            }
        }

        loadNativeAd()
    }

    private fun setConfirmButtonEnabled(enabled: Boolean) {
        binding.ivSelect.isEnabled = enabled
        binding.ivSelect.alpha = if (enabled) 1.0f else 0.3f
    }

    private fun loadNativeAd() {
        AdsManager.loadAndShowNative(
            activity = this,
            container = binding.frAds,
            placementKey = "native_all",
            adUnitIdRes = R.string.native_all,
            layoutRes = R.layout.layout_native_media,
            isEnabled = RemoteConfigs.native_all,
            requireFullAds = true
        )
    }

    companion object {
        const val EXTRA_FROM_SETTINGS = "extra_from_settings"
    }
}
