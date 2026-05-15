package com.pairshot.core.ads.controller

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.pairshot.core.ads.config.AdsConfig
import com.pairshot.core.ads.initializer.AdsInitializer
import com.pairshot.core.ads.premium.SettingsPremiumGate
import com.pairshot.core.domain.entitlement.ProEntitlementProvider
import com.pairshot.core.domain.premium.PremiumFeature
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RewardedAdController
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val adsConfig: AdsConfig,
        private val adsInitializer: AdsInitializer,
        private val entitlementProvider: ProEntitlementProvider,
        private val gate: SettingsPremiumGate,
        private val fullscreenAdState: FullscreenAdState,
    ) {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        private val loading = AtomicBoolean(false)
        private val showing = AtomicBoolean(false)

        @Volatile
        private var currentAd: RewardedAd? = null

        fun preload() {
            scope.launch { loadInternal() }
        }

        fun showIfAvailable(
            activity: Activity,
            feature: PremiumFeature,
            onReward: () -> Unit,
            onSkip: () -> Unit,
        ) {
            scope.launch {
                if (gate.isUnlocked(feature)) {
                    onReward()
                    return@launch
                }

                if (entitlementProvider.current().isActive) {
                    gate.unlock(feature)
                    onReward()
                    return@launch
                }

                if (!showing.compareAndSet(false, true)) {
                    onSkip()
                    return@launch
                }

                val ad = currentAd
                if (ad == null) {
                    showing.set(false)
                    onSkip()
                    loadInternal()
                    return@launch
                }

                if (activity.isFinishing || activity.isDestroyed) {
                    showing.set(false)
                    onSkip()
                    return@launch
                }

                if (!fullscreenAdState.markShown()) {
                    showing.set(false)
                    onSkip()
                    return@launch
                }

                var rewarded = false
                ad.fullScreenContentCallback =
                    fullscreenCallback(
                        tag = TAG,
                        fullscreenAdState = fullscreenAdState,
                        onDismissed = {
                            currentAd = null
                            showing.set(false)
                            if (rewarded) onReward() else onSkip()
                            scope.launch { loadInternal() }
                        },
                        onShowFailed = {
                            currentAd = null
                            showing.set(false)
                            onSkip()
                            scope.launch { loadInternal() }
                        },
                        onShown = { currentAd = null },
                    )
                runCatching {
                    ad.show(activity) {
                        rewarded = true
                        gate.unlock(feature)
                    }
                }.onFailure { error ->
                    Timber.tag(TAG).e(error, "show threw")
                    fullscreenAdState.markDismissed()
                    currentAd = null
                    showing.set(false)
                    onSkip()
                    scope.launch { loadInternal() }
                }
            }
        }

        private suspend fun loadInternal() {
            if (currentAd != null) return
            if (!loading.compareAndSet(false, true)) return
            adsInitializer.awaitReady()
            val request = AdRequest.Builder().build()
            RewardedAd.load(
                context,
                adsConfig.rewardedAdUnitId,
                request,
                object : RewardedAdLoadCallback() {
                    override fun onAdLoaded(ad: RewardedAd) {
                        currentAd = ad
                        loading.set(false)
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        Timber.tag(TAG).w("load failed: %s", error.message)
                        currentAd = null
                        loading.set(false)
                    }
                },
            )
        }

        private companion object {
            const val TAG = "RewardedAdCtrl"
        }
    }
