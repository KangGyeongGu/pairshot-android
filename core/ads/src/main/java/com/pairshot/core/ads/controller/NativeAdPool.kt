package com.pairshot.core.ads.controller

import android.content.Context
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.pairshot.core.ads.config.AdsConfig
import com.pairshot.core.domain.membership.MembershipProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject

class NativeAdPool
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val adsConfig: AdsConfig,
    private val membershipProvider: MembershipProvider,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val adsFlow = MutableStateFlow<List<NativeAd>>(emptyList())
    private val loadingCount = AtomicInteger(0)

    fun observeAds(): StateFlow<List<NativeAd>> = adsFlow.asStateFlow()

    fun ensurePreloaded(targetCount: Int) {
        if (targetCount <= 0) return
        scope.launch {
            if (membershipProvider.current().isPro) return@launch

            val desired = targetCount.coerceAtMost(MAX_POOL_SIZE)
            val currentSize = adsFlow.value.size
            val inFlight = loadingCount.get()
            val deficit = desired - currentSize - inFlight
            if (deficit <= 0) return@launch

            repeat(deficit) { loadOne() }
        }
    }

    fun close() {
        val snapshot = adsFlow.getAndUpdate { emptyList() }
        snapshot.forEach { runCatching { it.destroy() } }
        scope.cancel()
    }

    private fun loadOne() {
        loadingCount.incrementAndGet()
        val loader =
            AdLoader
                .Builder(context, adsConfig.nativeAdUnitId)
                .forNativeAd { ad ->
                    loadingCount.decrementAndGet()
                    var accepted = true
                    adsFlow.update { current ->
                        if (current.size >= MAX_POOL_SIZE) {
                            accepted = false
                            current
                        } else {
                            current + ad
                        }
                    }
                    if (!accepted) runCatching { ad.destroy() }
                }.withAdListener(
                    object : AdListener() {
                        override fun onAdFailedToLoad(error: LoadAdError) {
                            loadingCount.decrementAndGet()
                            Timber.tag(TAG).w("native load failed: %s", error.message)
                        }
                    },
                ).withNativeAdOptions(NativeAdOptions.Builder().build())
                .build()
        runCatching { loader.loadAd(AdRequest.Builder().build()) }
            .onFailure {
                loadingCount.decrementAndGet()
                Timber.tag(TAG).w(it, "native loadAd threw")
            }
    }

    private companion object {
        const val TAG = "NativeAdPool"
        const val MAX_POOL_SIZE = 5
    }
}
