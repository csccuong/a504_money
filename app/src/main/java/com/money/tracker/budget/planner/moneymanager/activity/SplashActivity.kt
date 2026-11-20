package com.money.tracker.budget.planner.moneymanager.activity

import android.content.Intent
import android.os.Bundle
import android.os.Process
import androidx.activity.OnBackPressedCallback
import androidx.lifecycle.lifecycleScope
import com.cscmobi.libraryads.commons.remote.ValueRemoteConfigModule
import com.money.tracker.budget.planner.moneymanager.R
import com.money.tracker.budget.planner.moneymanager.ads.AdsManager
import com.money.tracker.budget.planner.moneymanager.ads.RemoteConfigs
import com.money.tracker.budget.planner.moneymanager.base.BaseActivity
import com.money.tracker.budget.planner.moneymanager.databinding.ActivitySplashBinding
import com.money.tracker.budget.planner.moneymanager.ui.common.SystemBarsHelper
import com.money.tracker.budget.planner.moneymanager.ui.common.show
import com.money.tracker.budget.planner.moneymanager.utils.NotificationPermissionHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashActivity : BaseActivity<ActivitySplashBinding>(ActivitySplashBinding::inflate) {

    override val applySavedLocale: Boolean = true
    override val statusBarColorRes: Int = R.color.surface_canvas
    override val statusBarIconStyle: SystemBarsHelper.StatusBarIconStyle =
        SystemBarsHelper.StatusBarIconStyle.DARK

    private var isProgressDone = false
    private var isAdDone = false

    override fun setupView(savedInstanceState: Bundle?) {
        if (NotificationPermissionHelper.isNotificationPermissionGranted(this)) {
            NotificationPermissionHelper.startWakeServiceSafely(this)
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                exitApp()
            }
        })

        val shortcut = intent.getStringExtra(EXTRA_SHORTCUT)
        when (shortcut) {
            SHORTCUT_UNINSTALL_FAKE -> startUninstallFlow()
            SHORTCUT_ADD_BUDGET -> startAddBudgetFlow()
            else -> {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
        }
    }

    private fun startUninstallFlow() {
        startProgressCountdown {
            navigateIfReady { goToUninstallActivity() }
        }

        AdsManager.loadAndShowInter(
            activity = this,
            adUnitIdRes = R.string.inter_all,
            isEnabled = RemoteConfigs.inter_all,
            requireFullAds = false
        ) {
            isAdDone = true
            navigateIfReady { goToUninstallActivity() }
        }
    }

    private fun startAddBudgetFlow() {
        AdsManager.showBanner(
            activity = this,
            container = binding.layoutAds,
            adUnitId = getString(R.string.banner_splash),
            isEnabled = ValueRemoteConfigModule.banner_splash
        )

        startProgressCountdown {
            navigateIfReady { goToAddBudgetScreen() }
        }

        AdsManager.loadAndShowInterSplashWithNativeFull(this) {
            isAdDone = true
            navigateIfReady { goToAddBudgetScreen() }
        }
    }

    private fun startProgressCountdown(onProgressComplete: () -> Unit) {
        binding.tvLoading.show()
        lifecycleScope.launch {
            for (progress in 0..100) {
                binding.progressBar.progress = progress
                binding.tvLoading.text = "$progress%"
                delay(20L)
            }
            isProgressDone = true
            onProgressComplete()
        }
    }

    private fun navigateIfReady(navigate: () -> Unit) {
        if (isProgressDone && isAdDone) {
            navigate()
        }
    }

    override fun onDestroy() {
        AdsManager.destroyBanner(this)
        super.onDestroy()
    }

    private fun goToUninstallActivity() {
        if (isFinishing || isDestroyed) return
        startActivity(Intent(this, UninstallActivity::class.java))
        finish()
    }

    private fun goToAddBudgetScreen() {
        if (isFinishing || isDestroyed) return
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_OPEN_TAB, MainActivity.TAB_BUDGET)
            putExtra(MainActivity.EXTRA_OPEN_ADD_BUDGET_SHEET, true)
        }
        startActivity(intent)
        finish()
    }

    private fun exitApp() {
        moveTaskToBack(true)
        finish()
        Process.killProcess(Process.myPid())
        System.exit(0)
    }

    companion object {
        const val EXTRA_SHORTCUT = "shortcut"
        const val SHORTCUT_UNINSTALL_FAKE = "uninstall_fake"
        const val SHORTCUT_ADD_BUDGET = "add_budget"
    }
}
