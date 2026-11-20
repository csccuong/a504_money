package com.money.tracker.budget.planner.moneymanager.activity

import android.content.Intent
import android.os.Bundle
import com.money.tracker.budget.planner.moneymanager.R
import com.money.tracker.budget.planner.moneymanager.ads.AdsManager
import com.money.tracker.budget.planner.moneymanager.ads.RemoteConfigs
import com.money.tracker.budget.planner.moneymanager.base.BaseActivity
import com.money.tracker.budget.planner.moneymanager.data.AppPreferences
import com.money.tracker.budget.planner.moneymanager.data.BudgetRepository
import com.money.tracker.budget.planner.moneymanager.databinding.ActivitySetBudgetBinding
import com.money.tracker.budget.planner.moneymanager.ui.common.attachThousandSeparator
import com.money.tracker.budget.planner.moneymanager.ui.common.cleanNumericText

class SetBudgetActivity : BaseActivity<ActivitySetBudgetBinding>(ActivitySetBudgetBinding::inflate) {

    private val preferences by lazy { AppPreferences.getInstance(this) }
    private val budgetRepository by lazy { BudgetRepository(this) }

    override fun setupView(savedInstanceState: Bundle?) {
        binding.tvCurrencyPrefix.text = preferences.getSelectedCurrencyOrDefault()
        binding.inputBudget.attachThousandSeparator()

        binding.btnSkip.setOnClickListener {
            openMainAndFinish()
        }

        binding.btnSave.setOnClickListener {
            val budgetText = binding.inputBudget.cleanNumericText()
            if (budgetText.isEmpty()) {
                binding.inputBudget.error = getString(R.string.msg_enter_budget_amount)
                return@setOnClickListener
            }
            val amount = budgetText.toDoubleOrNull()
            if (amount == null || amount <= 0.0) {
                binding.inputBudget.error = getString(R.string.msg_invalid_amount)
                return@setOnClickListener
            }
            budgetRepository.setTotalBudget(amount)
            openMainAndFinish()
        }

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

    private fun openMainAndFinish() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
