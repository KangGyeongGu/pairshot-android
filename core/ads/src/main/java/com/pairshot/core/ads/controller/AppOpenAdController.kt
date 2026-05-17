package com.pairshot.core.ads.controller

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import com.pairshot.core.ads.config.AdsConfig
import com.pairshot.core.ads.initializer.AdsInitializer
import com.pairshot.core.domain.membership.MembershipProvider
import com.pairshot.core.domain.settings.OnboardingStateRepository
import com.pairshot.core.domain.tutorial.TutorialModeProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppOpenAdController
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val adsConfig: AdsConfig,
        private val adsInitializer: AdsInitializer,
        private val membershipProvider: MembershipProvider,
        private val onboardingStateRepository: OnboardingStateRepository,
        private val tutorialMode: TutorialModeProvider,
        private val fullscreenAdState: FullscreenAdState,
    ) {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        private val loading = AtomicBoolean(false)

        @Volatile
        private var currentAd: AppOpenAd? = null

        @Volatile
        private var loadTimestamp: Long = 0L

        @Volatile
        private var lastShownAt: Long = 0L

        @Volatile
        private var firstForegroundFired: Boolean = false

        fun preload() {
            scope.launch { loadInternal() }
        }

        fun onForeground(activity: Activity) {
            scope.launch {
                if (tutorialMode.isActive.value) return@launch
                if (membershipProvider.current().isAdFree) return@launch
                if (fullscreenAdState.isShowing()) return@launch
                if (isWithinCooldown()) return@launch

                val isColdStart = consumeColdStartFlag()
                if (isColdStart && !onboardingStateRepository.isOnboardingPaywallShown()) return@launch

                val ad = ensureAdLoaded(isColdStart) ?: return@launch

                if (isAdExpired()) {
                    currentAd = null
                    loadTimestamp = 0L
                    loadInternal()
                    return@launch
                }
                if (activity.isFinishing || activity.isDestroyed) return@launch
                if (!fullscreenAdState.markShown()) return@launch

                showAd(ad, activity)
            }
        }

        private fun isWithinCooldown(): Boolean {
            val last = lastShownAt
            if (last <= 0L) return false
            return System.currentTimeMillis() - last < COOLDOWN_MS
        }

        private fun consumeColdStartFlag(): Boolean {
            val isColdStart = !firstForegroundFired
            firstForegroundFired = true
            return isColdStart
        }

        private fun isAdExpired(): Boolean {
            val ts = loadTimestamp
            if (ts <= 0L) return false
            return System.currentTimeMillis() - ts >= AD_EXPIRATION_MS
        }

        private suspend fun ensureAdLoaded(isColdStart: Boolean): AppOpenAd? {
            if (currentAd != null) return currentAd
            loadInternal()
            if (!isColdStart) return null
            var waited = 0L
            while (currentAd == null && waited < COLD_START_WAIT_MS) {
                delay(POLL_INTERVAL_MS)
                waited += POLL_INTERVAL_MS
            }
            return currentAd
        }

        private fun showAd(
            ad: AppOpenAd,
            activity: Activity,
        ) {
            ad.fullScreenContentCallback =
                fullscreenCallback(
                    tag = TAG,
                    fullscreenAdState = fullscreenAdState,
                    onDismissed = { onAdFinished(markDismiss = false) },
                    onShowFailed = { onAdFinished(markDismiss = false) },
                    onShown = { currentAd = null },
                )
            runCatching { ad.show(activity) }
                .onFailure { error ->
                    Timber.tag(TAG).e(error, "show threw")
                    onAdFinished(markDismiss = true)
                }
        }

        private fun onAdFinished(markDismiss: Boolean) {
            if (markDismiss) fullscreenAdState.markDismissed()
            lastShownAt = System.currentTimeMillis()
            currentAd = null
            loadTimestamp = 0L
            scope.launch { loadInternal() }
        }

        private suspend fun loadInternal() {
            if (currentAd != null) return
            if (!loading.compareAndSet(false, true)) return
            adsInitializer.awaitReady()
            val request = AdRequest.Builder().build()
            AppOpenAd.load(
                context,
                adsConfig.appOpenAdUnitId,
                request,
                object : AppOpenAd.AppOpenAdLoadCallback() {
                    override fun onAdLoaded(ad: AppOpenAd) {
                        currentAd = ad
                        loadTimestamp = System.currentTimeMillis()
                        loading.set(false)
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        Timber.tag(TAG).w("load failed: code=%d %s", error.code, error.message)
                        currentAd = null
                        loadTimestamp = 0L
                        loading.set(false)
                    }
                },
            )
        }

        private companion object {
            const val TAG = "AppOpenAdCtrl"
            const val COOLDOWN_MS = 60_000L
            const val AD_EXPIRATION_MS = 4 * 60 * 60 * 1000L
            const val COLD_START_WAIT_MS = 6000L
            const val POLL_INTERVAL_MS = 100L
        }
    }
