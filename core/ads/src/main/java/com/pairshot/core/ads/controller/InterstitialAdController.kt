package com.pairshot.core.ads.controller

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.pairshot.core.ads.config.AdsConfig
import com.pairshot.core.ads.initializer.AdsInitializer
import com.pairshot.core.domain.entitlement.ProEntitlementProvider
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
class InterstitialAdController
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val adsConfig: AdsConfig,
        private val adsInitializer: AdsInitializer,
        private val entitlementProvider: ProEntitlementProvider,
        private val fullscreenAdState: FullscreenAdState,
    ) {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        private val loading = AtomicBoolean(false)

        @Volatile
        private var currentAd: InterstitialAd? = null

        @Volatile
        private var lastShownAt: Long = 0L

        fun preload() {
            scope.launch { loadInternal() }
        }

        fun showIfAvailable(
            activity: Activity,
            onFinished: () -> Unit,
        ) {
            scope.launch {
                if (entitlementProvider.current().isActive) {
                    onFinished()
                    return@launch
                }

                val now = System.currentTimeMillis()
                if (lastShownAt > 0L && now - lastShownAt < COOLDOWN_MS) {
                    onFinished()
                    loadInternal()
                    return@launch
                }

                val ad = currentAd
                if (ad == null) {
                    onFinished()
                    loadInternal()
                    return@launch
                }

                if (activity.isFinishing || activity.isDestroyed) {
                    onFinished()
                    return@launch
                }

                if (!fullscreenAdState.markShown()) {
                    onFinished()
                    return@launch
                }

                ad.fullScreenContentCallback =
                    fullscreenCallback(
                        tag = TAG,
                        fullscreenAdState = fullscreenAdState,
                        onDismissed = {
                            currentAd = null
                            lastShownAt = System.currentTimeMillis()
                            onFinished()
                            scope.launch { loadInternal() }
                        },
                        onShowFailed = {
                            currentAd = null
                            onFinished()
                            scope.launch { loadInternal() }
                        },
                        onShown = { currentAd = null },
                    )
                runCatching { ad.show(activity) }
                    .onFailure { error ->
                        Timber.tag(TAG).e(error, "show threw")
                        fullscreenAdState.markDismissed()
                        currentAd = null
                        onFinished()
                        scope.launch { loadInternal() }
                    }
            }
        }

        private suspend fun loadInternal() {
            if (currentAd != null) return
            if (!loading.compareAndSet(false, true)) return
            adsInitializer.awaitReady()
            val request = AdRequest.Builder().build()
            InterstitialAd.load(
                context,
                adsConfig.interstitialExportCompleteAdUnitId,
                request,
                object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(ad: InterstitialAd) {
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
            const val TAG = "InterstitialAdCtrl"
            const val COOLDOWN_MS = 5_000L
        }
    }
