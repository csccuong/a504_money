package com.money.tracker.budget.planner.moneymanager.activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.OnBackPressedCallback
import com.money.tracker.budget.planner.moneymanager.R
import com.money.tracker.budget.planner.moneymanager.ads.AdsManager
import com.money.tracker.budget.planner.moneymanager.ads.RemoteConfigs
import com.money.tracker.budget.planner.moneymanager.base.BaseActivity
import com.money.tracker.budget.planner.moneymanager.databinding.ActivityUninstallBinding
import com.money.tracker.budget.planner.moneymanager.ui.common.SystemBarsHelper

class UninstallActivity : BaseActivity<ActivityUninstallBinding>(ActivityUninstallBinding::inflate) {

    override val statusBarColorRes: Int = R.color.surface_card
    override val statusBarIconStyle: SystemBarsHelper.StatusBarIconStyle =
        SystemBarsHelper.StatusBarIconStyle.DARK

    override fun setupView(savedInstanceState: Bundle?) {
        val returnToMainClick = { navigateToMain() }

        binding.btnBack.setOnClickListener { returnToMainClick() }
        binding.TryAgain.setOnClickListener { returnToMainClick() }
        binding.Explore.setOnClickListener { returnToMainClick() }
        binding.dont.setOnClickListener { returnToMainClick() }

        binding.still.setOnClickListener {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
            }
            startActivity(intent)
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                navigateToMain()
            }
        })

        AdsManager.loadAndShowNative(
            activity = this,
            container = binding.frAds,
            placementKey = "native_all",
            adUnitIdRes = R.string.native_all,
            layoutRes = R.layout.layout_native_small_home,
            isEnabled = RemoteConfigs.native_all,
            requireFullAds = false
        )
    }

    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
