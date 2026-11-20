package com.money.tracker.budget.planner.moneymanager.ads

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.LayoutRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.cscmobi.libraryads.CSCApplication
import com.cscmobi.libraryads.CSCFOConfigs
import com.cscmobi.libraryads.ads.banner_ads.CSCBanner
import com.cscmobi.libraryads.ads.inter_ads.CSCInter
import com.cscmobi.libraryads.ads.native_ads.CSCNativeManager
import com.cscmobi.libraryads.ads.native_ads.callback.INativeAdCallback
import com.cscmobi.libraryads.ads.native_ads.model.LoadStrategy
import com.cscmobi.libraryads.ads.open_ads.CSCOpenAds
import com.cscmobi.libraryads.ads.utils.EnumAdsNamePosition
import com.cscmobi.libraryads.ads.utils.StatusShowAd
import com.cscmobi.libraryads.commons.remote.ValueRemoteConfigModule
import com.cscmobi.libraryads.commons.sharepreference.CSCSPFHolder
import com.cscmobi.libraryads.commons.utils.TaskCoordinator
import com.money.tracker.budget.planner.moneymanager.R

object AdsManager {
    private const val TAG = "AdsManager"

    private var openAdsInstance: CSCOpenAds? = null

    fun registerOpenAds(openAds: CSCOpenAds?) {
        this.openAdsInstance = openAds
    }

    fun disableAOA() {
        try {
            openAdsInstance?.disableShowAOA()
        } catch (e: Exception) {
            Log.e(TAG, "Error disabling AOA", e)
        }
    }

    fun enableAOA() {
        try {
            openAdsInstance?.enableShowAOA()
        } catch (e: Exception) {
            Log.e(TAG, "Error enabling AOA", e)
        }
    }

    fun isOrganicUser(): Boolean {
        val shouldCheck = runCatching {
            RemoteConfigs.check_organic_user && ValueRemoteConfigModule.check_organic_user
        }.getOrDefault(true)
        if (!shouldCheck) return false

        return runCatching {
            CSCSPFHolder.get().is_organic
        }.getOrDefault(false)
    }

    fun isFullAdsEnabled(): Boolean = !isOrganicUser()

    fun showBanner(
        activity: Activity,
        container: ViewGroup?,
        adUnitId: String = activity.getString(R.string.banner_splash),
        isEnabled: Boolean = true,
        isCollapsible: Boolean = false,
        onLoaded: (() -> Unit)? = null,
        onFailed: (() -> Unit)? = null
    ) {
        if (container == null) return
        if (!isEnabled || adUnitId.isBlank() || activity.isFinishing || activity.isDestroyed) {
            container.visibility = View.GONE
            onFailed?.invoke()
            return
        }

        try {
            val frameLayout = resolveFrameLayout(activity, container, R.id.flBannerContainer)
            container.visibility = View.VISIBLE
            val bannerType = if (isCollapsible) {
                CSCBanner.TypeAds.BANNER_COLLAPSIBLE_BOTTOM
            } else {
                CSCBanner.TypeAds.BANNER_ADAPTIVE
            }
            CSCBanner.requestBanner(
                activity = activity,
                id = adUnitId,
                typeAds = bannerType,
                adFrame = frameLayout,
                canShowAd = isEnabled,
                onResult = { adView ->
                    activity.runOnUiThread {
                        if (activity.isFinishing || activity.isDestroyed) return@runOnUiThread
                        if (adView != null) {
                            container.visibility = View.VISIBLE
                            onLoaded?.invoke()
                        } else {
                            container.visibility = View.GONE
                            onFailed?.invoke()
                        }
                    }
                },
                onShown = {
                    Log.d(TAG, "Banner impression recorded")
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error requesting banner", e)
            container.visibility = View.GONE
            onFailed?.invoke()
        }
    }

    fun destroyBanner(activity: Activity) {
        try {
            CSCBanner.destroyBannerAds(activity)
        } catch (e: Exception) {
            Log.e(TAG, "Error destroying banner ad", e)
        }
    }

    /**
     * Show Home bottom collapsible native (`native_collap`) as an overlay covering the bottom nav.
     * When the user closes it via `btn_close_ad`, stop collapsible refresh, hide `collapContainer`,
     * and show `native_banner` (no media) inside `bannerFallbackContainer`.
     */
    fun showCollapsibleNativeHome(
        activity: AppCompatActivity,
        collapContainer: ViewGroup?,
        bannerFallbackContainer: ViewGroup?,
        isCollapEnabled: Boolean = RemoteConfigs.native_collap,
        isBannerFallbackEnabled: Boolean = RemoteConfigs.native_banner,
        requireFullAds: Boolean = true,
        refreshIntervalMs: Long = 30_000L,
        onClosedToBanner: (() -> Unit)? = null,
        onShown: (() -> Unit)? = null,
        onFailed: (() -> Unit)? = null
    ) {
        if (collapContainer == null && bannerFallbackContainer == null) return
        val canShowFullAds = !requireFullAds || isFullAdsEnabled()
        if (!canShowFullAds || activity.isFinishing || activity.isDestroyed) {
            stopCollapsibleNative(activity)
            collapContainer?.visibility = View.GONE
            bannerFallbackContainer?.visibility = View.GONE
            onFailed?.invoke()
            return
        }

        if (!isCollapEnabled || collapContainer == null) {
            stopCollapsibleNative(activity)
            collapContainer?.visibility = View.GONE
            if (isBannerFallbackEnabled && bannerFallbackContainer != null) {
                showNativeBannerHome(
                    activity = activity,
                    container = bannerFallbackContainer,
                    isEnabled = true,
                    requireFullAds = requireFullAds,
                    onLoaded = onShown,
                    onFailed = onFailed
                )
            } else {
                bannerFallbackContainer?.visibility = View.GONE
                onFailed?.invoke()
            }
            return
        }

        val adUnitId = activity.getString(R.string.native_collap)
        if (adUnitId.isBlank()) {
            stopCollapsibleNative(activity)
            collapContainer.visibility = View.GONE
            bannerFallbackContainer?.visibility = View.GONE
            onFailed?.invoke()
            return
        }

        try {
            bannerFallbackContainer?.visibility = View.GONE
            val frameLayout = resolveFrameLayout(activity, collapContainer, R.id.flNativeContainer)
            collapContainer.visibility = View.VISIBLE

            val attachCloseListener = {
                val closeBtn = frameLayout.findViewById<View>(R.id.btn_close_ad)
                    ?: frameLayout.findViewById<View>(com.cscmobi.libraryads.R.id.btn_close_ad)
                closeBtn?.setOnClickListener {
                    stopCollapsibleNative(activity)
                    collapContainer.visibility = View.GONE
                    onClosedToBanner?.invoke()
                    if (isBannerFallbackEnabled && bannerFallbackContainer != null && (!requireFullAds || isFullAdsEnabled())) {
                        showNativeBannerHome(
                            activity = activity,
                            container = bannerFallbackContainer,
                            isEnabled = true,
                            requireFullAds = requireFullAds
                        )
                    } else {
                        bannerFallbackContainer?.visibility = View.GONE
                    }
                }
            }

            CSCNativeManager.showCollapsibleNative(
                activity = activity,
                adFrame = frameLayout,
                adName = "native_collap",
                adId = adUnitId,
                adLayout = R.layout.layout_native_collapsible,
                refreshTime = refreshIntervalMs,
                isPreload = true,
                canShowAd = true,
                callback = object : INativeAdCallback {
                    override fun onAdShown(adName: String) {
                        activity.runOnUiThread {
                            if (activity.isFinishing || activity.isDestroyed) return@runOnUiThread
                            collapContainer.visibility = View.VISIBLE
                            attachCloseListener()
                            onShown?.invoke()
                        }
                    }

                    override fun onAdLoaded(adName: String) {
                        activity.runOnUiThread {
                            if (activity.isFinishing || activity.isDestroyed) return@runOnUiThread
                            collapContainer.visibility = View.VISIBLE
                            frameLayout.post { attachCloseListener() }
                            onShown?.invoke()
                        }
                    }

                    override fun onAdFailedToLoad(adName: String, errorMessage: String) {
                        Log.e(TAG, "Collapsible native onAdFailedToLoad: $adName, error=$errorMessage")
                        activity.runOnUiThread {
                            if (activity.isFinishing || activity.isDestroyed) return@runOnUiThread
                            collapContainer.visibility = View.GONE
                            onFailed?.invoke()
                        }
                    }

                    override fun onAdShowFailed(adName: String, reason: String) {
                        Log.e(TAG, "Collapsible native onAdShowFailed: $adName, reason=$reason")
                        activity.runOnUiThread {
                            if (activity.isFinishing || activity.isDestroyed) return@runOnUiThread
                            collapContainer.visibility = View.GONE
                            onFailed?.invoke()
                        }
                    }

                    override fun onAdDismissed(adName: String) {
                        activity.runOnUiThread {
                            if (activity.isFinishing || activity.isDestroyed) return@runOnUiThread
                            collapContainer.visibility = View.GONE
                        }
                    }
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error showing collapsible native", e)
            collapContainer.visibility = View.GONE
            onFailed?.invoke()
        }
    }

    /**
     * Show compact no-media native banner (`native_banner`) at Home bottom when `native_collap` is closed.
     */
    fun showNativeBannerHome(
        activity: Activity,
        container: ViewGroup?,
        isEnabled: Boolean = RemoteConfigs.native_banner,
        requireFullAds: Boolean = true,
        onLoaded: (() -> Unit)? = null,
        onFailed: (() -> Unit)? = null
    ) {
        loadAndShowNative(
            activity = activity,
            container = container,
            placementKey = "native_banner",
            adUnitIdRes = R.string.native_banner,
            layoutRes = R.layout.layout_native_banner,
            isEnabled = isEnabled,
            requireFullAds = requireFullAds,
            onLoaded = onLoaded,
            onFailed = onFailed
        )
    }

    fun stopCollapsibleNative(activity: Activity) {
        try {
            CSCNativeManager.stopCollapsible(activity)
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping collapsible native", e)
        }
    }

    fun loadAndShowNative(
        activity: Activity,
        container: ViewGroup?,
        placementKey: String = "native_all",
        @StringRes adUnitIdRes: Int = R.string.native_all,
        @LayoutRes layoutRes: Int = R.layout.layout_native_small,
        isEnabled: Boolean = RemoteConfigs.native_all,
        requireFullAds: Boolean = false,
        onLoaded: (() -> Unit)? = null,
        onFailed: (() -> Unit)? = null
    ) {
        val effectiveEnabled = isEnabled && (!requireFullAds || isFullAdsEnabled())
        loadAndShowNative(
            activity = activity,
            container = container,
            placementKey = placementKey,
            adUnitId = activity.getString(adUnitIdRes),
            layoutRes = layoutRes,
            isEnabled = effectiveEnabled,
            onLoaded = onLoaded,
            onFailed = onFailed
        )
    }

    fun loadAndShowNative(
        activity: Activity,
        container: ViewGroup?,
        placementKey: String,
        adUnitId: String,
        @LayoutRes layoutRes: Int = R.layout.layout_native_small,
        isEnabled: Boolean = true,
        onLoaded: (() -> Unit)? = null,
        onFailed: (() -> Unit)? = null
    ) {
        if (container == null) return
        if (!isEnabled || adUnitId.isBlank() || activity.isFinishing || activity.isDestroyed) {
            container.visibility = View.GONE
            onFailed?.invoke()
            return
        }

        try {
            val frameLayout = resolveFrameLayout(activity, container, R.id.flNativeContainer)

            val effectiveKey = if (placementKey.isNotBlank()) {
                if (placementKey.contains(activity.javaClass.simpleName)) placementKey else "${placementKey}_${activity.javaClass.simpleName}"
            } else {
                "native_${activity.javaClass.simpleName}"
            }

            container.visibility = View.VISIBLE
            CSCNativeManager.loadAndShowNative(
                adFrame = frameLayout,
                adName = effectiveKey,
                adId = adUnitId,
                adLayout = layoutRes,
                isPreload = false,
                canShowAd = isEnabled,
                callback = object : INativeAdCallback {
                    override fun onAdLoaded(adName: String) {
                        activity.runOnUiThread {
                            if (activity.isFinishing || activity.isDestroyed) return@runOnUiThread
                            container.visibility = View.VISIBLE
                            onLoaded?.invoke()
                        }
                    }

                    override fun onAdFailedToLoad(adName: String, errorMessage: String) {
                        Log.e(TAG, "Native onAdFailedToLoad: $adName, error=$errorMessage")
                        activity.runOnUiThread {
                            if (activity.isFinishing || activity.isDestroyed) return@runOnUiThread
                            container.visibility = View.GONE
                            onFailed?.invoke()
                        }
                    }

                    override fun onAdShowFailed(adName: String, reason: String) {
                        Log.e(TAG, "Native onAdShowFailed: $adName, reason=$reason")
                        activity.runOnUiThread {
                            if (activity.isFinishing || activity.isDestroyed) return@runOnUiThread
                            container.visibility = View.GONE
                            onFailed?.invoke()
                        }
                    }

                    override fun onAdDismissed(adName: String) {
                        activity.runOnUiThread {
                            if (activity.isFinishing || activity.isDestroyed) return@runOnUiThread
                            container.visibility = View.GONE
                        }
                    }
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error loading native ad: $placementKey", e)
            container.visibility = View.GONE
            onFailed?.invoke()
        }
    }

    fun showInter(
        activity: Activity,
        adUnitId: String,
        isEnabled: Boolean = true,
        onNext: () -> Unit
    ) {
        if (!isEnabled || adUnitId.isBlank() || activity.isFinishing || activity.isDestroyed) {
            onNext()
            return
        }

        var executed = false
        val safeNext: () -> Unit = {
            if (!executed) {
                executed = true
                runCatching { StatusShowAd.isInterstitialShown = false }
                activity.runOnUiThread { onNext() }
            }
        }

        try {
            CSCInter.showInter(
                activity = activity,
                adName = adUnitId,
                nextAction = { _: Boolean -> safeNext() },
                onShown = null,
                preload = true,
                canShowAd = isEnabled
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error showing inter ad", e)
            safeNext()
        }
    }

    fun loadAndShowInter(
        activity: Activity,
        @StringRes adUnitIdRes: Int = R.string.inter_all,
        isEnabled: Boolean = RemoteConfigs.inter_all,
        requireFullAds: Boolean = true,
        timeout: Long = 6_000L,
        onNext: () -> Unit
    ) {
        val effectiveEnabled = isEnabled && (!requireFullAds || isFullAdsEnabled())
        loadAndShowInter(
            activity = activity,
            adUnitId = activity.getString(adUnitIdRes),
            isEnabled = effectiveEnabled,
            timeout = timeout,
            onNext = onNext
        )
    }

    fun loadAndShowInter(
        activity: Activity,
        adUnitId: String,
        isEnabled: Boolean = true,
        timeout: Long = 6_000L,
        onNext: () -> Unit
    ) {
        if (!isEnabled || adUnitId.isBlank() || activity.isFinishing || activity.isDestroyed) {
            onNext()
            return
        }

        var executed = false
        val safeNext: () -> Unit = {
            if (!executed) {
                executed = true
                AdLoadingDialog.dismiss()
                runCatching { StatusShowAd.isInterstitialShown = false }
                activity.runOnUiThread { onNext() }
            }
        }

        try {
            AdLoadingDialog.show(activity)

            CSCInter.loadAndShowInter(
                activity = activity,
                adId = adUnitId,
                timeDelay = 0L,
                timeOut = timeout,
                nextAction = { _: Boolean ->
                    AdLoadingDialog.dismiss()
                    safeNext()
                },
                onShown = {
                    AdLoadingDialog.dismiss()
                },
                canShowId = isEnabled,
                onLoadFailed = {
                    AdLoadingDialog.dismiss()
                    safeNext()
                },
                showLoading = false
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error loadAndShowInter", e)
            AdLoadingDialog.dismiss()
            safeNext()
        }
    }

    /**
     * Load and display Splash Inter (`inter_splash_high` -> `inter_splash`) followed by
     * Splash Native Full (`native_full_high_inter_splash` -> `native_full_inter_splash`) for non-organic users.
     */
    fun loadAndShowInterSplashWithNativeFull(
        activity: AppCompatActivity,
        onCompleted: () -> Unit
    ) {
        if (activity.isFinishing || activity.isDestroyed) {
            onCompleted()
            return
        }

        val splashConfig = runCatching { CSCFOConfigs.splashConfig }.getOrNull()
        val adsSplashConfig = splashConfig?.adsSplashConfig
        val uiSplashConfig = splashConfig?.uiSplashConfig
        val isFirstOpen = runCatching { CSCApplication.isFirstOpen }.getOrElse { false }

        val interHighId = if (isFirstOpen) {
            adsSplashConfig?.interHighId.orEmpty()
        } else {
            adsSplashConfig?.interHighIdS2.orEmpty()
        }.ifBlank { activity.getString(R.string.inter_splash_high) }

        val interAllId = if (isFirstOpen) {
            adsSplashConfig?.interAllId.orEmpty()
        } else {
            adsSplashConfig?.interAllIdS2.orEmpty()
        }.ifBlank { activity.getString(R.string.inter_splash) }

        val nativeFullHighId = if (isFirstOpen) {
            adsSplashConfig?.nativeFullHighId.orEmpty()
        } else {
            adsSplashConfig?.nativeFullHighIdS2.orEmpty()
        }.ifBlank { activity.getString(R.string.native_full_high_inter_splash) }

        val nativeFullAllId = if (isFirstOpen) {
            adsSplashConfig?.nativeFullId.orEmpty()
        } else {
            adsSplashConfig?.nativeFullIdS2.orEmpty()
        }.ifBlank { activity.getString(R.string.native_full_inter_splash) }

        val loadInterStrategy = adsSplashConfig?.loadInterStrategy ?: LoadStrategy.SEQUENTIAL
        val loadNativeFullStrategy = adsSplashConfig?.loadNativeFullStrategy ?: LoadStrategy.SEQUENTIAL
        val nativeFullLayout = adsSplashConfig?.nativeFullLayout ?: R.layout.layout_native_full
        val dismissOnAdClick = uiSplashConfig?.dismissNativeFullOnAdClick ?: true

        val mainHandler = Handler(Looper.getMainLooper())
        val lifecycleObserver = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_DESTROY) {
                mainHandler.removeCallbacksAndMessages(null)
            }
        }
        activity.lifecycle.addObserver(lifecycleObserver)

        val coordinator = TaskCoordinator {
            activity.lifecycle.removeObserver(lifecycleObserver)
            mainHandler.removeCallbacksAndMessages(null)
            if (!activity.isFinishing && !activity.isDestroyed) {
                onCompleted()
            }
        }

        val canPreloadNativeFull = isFullAdsEnabled() &&
            (ValueRemoteConfigModule.inter_splash || ValueRemoteConfigModule.inter_splash_high)
        if (canPreloadNativeFull) {
            runCatching {
                CSCNativeManager.preloadNativeWithHigh(
                    context = activity.applicationContext,
                    adName = EnumAdsNamePosition.NATIVE_FULL_AFTER_INTER_SPLASH.position,
                    adId = nativeFullAllId,
                    idHigh = nativeFullHighId,
                    strategy = loadNativeFullStrategy,
                    canShowIdAll = ValueRemoteConfigModule.native_full_inter_splash,
                    canShowIdHigh = ValueRemoteConfigModule.native_full_high_inter_splash
                )
            }
        }

        try {
            CSCInter.loadAndShowInterSplash(
                activity = activity,
                idHigh = interHighId,
                idAllPrice = interAllId,
                timeOut = 0L,
                nextAction = { _ ->
                    coordinator.onTaskADone()
                    CSCNativeManager.startFullscreenCountdown(
                        EnumAdsNamePosition.NATIVE_FULL_AFTER_INTER_SPLASH.position
                    )
                },
                onShown = {
                    coordinator.requireTaskB()
                    if (isFullAdsEnabled()) {
                        mainHandler.postDelayed({
                            showNativeFullAfterInterSplash(
                                activity = activity,
                                nativeFullLayout = nativeFullLayout,
                                dismissOnAdClick = dismissOnAdClick,
                                onDismissed = { coordinator.onTaskBDone() }
                            )
                        }, 1000L)
                    } else {
                        coordinator.onTaskBDone()
                    }
                },
                strategy = loadInterStrategy
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error loadAndShowInterSplashWithNativeFull", e)
            coordinator.onTaskADone()
            coordinator.onTaskBDone()
        }
    }

    private fun showNativeFullAfterInterSplash(
        activity: AppCompatActivity,
        @LayoutRes nativeFullLayout: Int,
        dismissOnAdClick: Boolean,
        onDismissed: () -> Unit
    ) {
        if (activity.isFinishing || activity.isDestroyed) {
            onDismissed()
            return
        }
        try {
            CSCNativeManager.showFullscreenNativeWithCountdownWithHigh(
                activity = activity,
                adName = EnumAdsNamePosition.NATIVE_FULL_AFTER_INTER_SPLASH.position,
                adLayoutRes = nativeFullLayout,
                countdownSeconds = ValueRemoteConfigModule.countdown_native_full_inter,
                canShowIdAll = ValueRemoteConfigModule.native_full_inter_splash,
                canShowIdHigh = ValueRemoteConfigModule.native_full_high_inter_splash,
                startCountdownImmediately = false,
                dismissOnAdClick = dismissOnAdClick,
                callback = object : INativeAdCallback {},
                onDismissed = onDismissed
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error showing native full after inter splash", e)
            onDismissed()
        }
    }

    private fun resolveFrameLayout(
        activity: Activity,
        container: ViewGroup,
        wrapperId: Int
    ): FrameLayout {
        if (container is FrameLayout) return container
        val existing = container.findViewById<FrameLayout>(wrapperId)
        if (existing != null) return existing
        return FrameLayout(activity).apply { id = wrapperId }.also { fl ->
            container.removeAllViews()
            container.addView(
                fl,
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
    }
}
