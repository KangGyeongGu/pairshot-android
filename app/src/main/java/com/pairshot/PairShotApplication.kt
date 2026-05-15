package com.pairshot

import android.app.Application
import android.os.StrictMode
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.pairshot.core.ads.controller.AppOpenAdController
import com.pairshot.core.ads.controller.InterstitialAdController
import com.pairshot.core.ads.controller.RewardedAdController
import com.pairshot.core.ads.initializer.AdsInitializer
import com.pairshot.core.ads.lifecycle.AppOpenAdLifecycleObserver
import com.pairshot.core.billing.BillingRepository
import com.pairshot.core.coupon.domain.CouponRepository
import com.pairshot.core.domain.pair.SyncMissingSourcesUseCase
import com.pairshot.core.domain.settings.AppSettingsRepository
import com.pairshot.feature.settings.theme.AppTheme
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class PairShotApplication : Application() {
    @Inject
    lateinit var appSettingsRepository: AppSettingsRepository

    @Inject
    lateinit var adsInitializer: AdsInitializer

    @Inject
    lateinit var interstitialAdController: InterstitialAdController

    @Inject
    lateinit var rewardedAdController: RewardedAdController

    @Inject
    lateinit var appOpenAdController: AppOpenAdController

    @Inject
    lateinit var appOpenAdLifecycleObserver: AppOpenAdLifecycleObserver

    @Inject
    lateinit var couponRepository: CouponRepository

    @Inject
    lateinit var billingRepository: BillingRepository

    @Inject
    lateinit var syncMissingSourcesUseCase: SyncMissingSourcesUseCase

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
            installStrictMode()
        }
        applicationScope.launch {
            val name = appSettingsRepository.appThemeNameFlow.first()
            AppTheme.fromName(name).apply()
        }
        adsInitializer.initialize(this)
        interstitialAdController.preload()
        rewardedAdController.preload()
        appOpenAdController.preload()
        appOpenAdLifecycleObserver.register(this)
        billingRepository.start()
        applicationScope.launch {
            runCatching { withContext(Dispatchers.IO) { couponRepository.retryPendingIfAny() } }
        }

        registerForegroundObservers()
    }

    private fun installStrictMode() {
        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy
                .Builder()
                .detectDiskReads()
                .detectDiskWrites()
                .detectNetwork()
                .detectCustomSlowCalls()
                .penaltyLog()
                .build(),
        )
        StrictMode.setVmPolicy(
            StrictMode.VmPolicy
                .Builder()
                .detectLeakedClosableObjects()
                .detectLeakedRegistrationObjects()
                .detectActivityLeaks()
                .detectFileUriExposure()
                .penaltyLog()
                .build(),
        )
    }

    private fun registerForegroundObservers() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onStart(owner: LifecycleOwner) {
                    applicationScope.launch {
                        runCatching { withContext(Dispatchers.IO) { couponRepository.syncStatus() } }
                    }
                    applicationScope.launch {
                        runCatching { withContext(Dispatchers.IO) { syncMissingSourcesUseCase() } }
                            .onFailure { Timber.w(it, "syncMissingSources failed on app foreground") }
                    }
                    applicationScope.launch {
                        runCatching { billingRepository.refresh() }
                            .onFailure { Timber.w(it, "billing refresh failed on app foreground") }
                    }
                }
            },
        )
    }
}
