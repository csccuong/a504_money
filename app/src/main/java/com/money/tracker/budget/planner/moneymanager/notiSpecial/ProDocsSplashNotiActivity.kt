package com.money.tracker.budget.planner.moneymanager.notiSpecial

import android.content.Intent
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.money.tracker.budget.planner.moneymanager.R
import com.money.tracker.budget.planner.moneymanager.activity.MainActivity
import com.money.tracker.budget.planner.moneymanager.ads.AdsManager
import com.money.tracker.budget.planner.moneymanager.ads.RemoteConfigs
import com.money.tracker.budget.planner.moneymanager.base.BaseActivity
import com.money.tracker.budget.planner.moneymanager.databinding.ActivitySplashBinding

class ProDocsSplashNotiActivity : BaseActivity<ActivitySplashBinding>(ActivitySplashBinding::inflate) {

    override fun setupView(savedInstanceState: Bundle?) {
        FirebaseAnalytics.getInstance(this).logEvent("event_click_noti_new", null)
        AdsManager.loadAndShowInter(
            activity = this,
            adUnitIdRes = R.string.inter_all,
            isEnabled = RemoteConfigs.inter_all,
            requireFullAds = false
        ) {
            if (!isFinishing && !isDestroyed) {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
        }
    }
}
