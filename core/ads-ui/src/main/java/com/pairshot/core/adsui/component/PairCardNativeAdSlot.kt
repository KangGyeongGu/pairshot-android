package com.pairshot.core.adsui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pairshot.core.ads.controller.NativeAdPool
import com.pairshot.core.ads.di.AdsEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class PairCardNativeAdSlot internal constructor(
    val isAdFree: Boolean,
    private val pool: NativeAdPool?,
    private val nativeAds: List<com.google.android.gms.ads.nativead.NativeAd>,
    private val onSlotCountChangeAction: (Int) -> Unit,
) {
    fun onAdSlotCountChange(total: Int) {
        onSlotCountChangeAction(total)
    }

    @Composable
    fun Content(slotIndex: Int) {
        val nativeAd = nativeAds.getOrNull(slotIndex)
        if (nativeAd != null) {
            PairShotNativeAdCard(nativeAd = nativeAd)
        }
    }
}

@Composable
fun rememberPairCardNativeAdSlot(): PairCardNativeAdSlot {
    val isInspection = LocalInspectionMode.current
    if (isInspection) {
        return remember {
            PairCardNativeAdSlot(isAdFree = true, pool = null, nativeAds = emptyList(), onSlotCountChangeAction = {})
        }
    }

    val context = LocalContext.current
    val entryPoint =
        remember(context) {
            EntryPointAccessors.fromApplication(
                context.applicationContext,
                AdsEntryPoint::class.java,
            )
        }
    val membershipProvider = remember(entryPoint) { entryPoint.membershipProvider() }
    val poolProvider = remember(entryPoint) { entryPoint.nativeAdPoolProvider() }
    val adFreeFlow = remember(membershipProvider) { membershipProvider.observe().map { it.isAdFree } }
    val isAdFree: Boolean? by adFreeFlow.collectAsStateWithLifecycle(initialValue = null)

    val nativeAdPool = remember(poolProvider) { poolProvider.get() }
    DisposableEffect(nativeAdPool) {
        onDispose { nativeAdPool.close() }
    }
    val nativeAds by nativeAdPool.observeAds().collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    return remember(isAdFree, nativeAdPool, nativeAds) {
        PairCardNativeAdSlot(
            isAdFree = isAdFree == true,
            pool = nativeAdPool,
            nativeAds = nativeAds,
            onSlotCountChangeAction = { total ->
                if (isAdFree == false && total > 0) {
                    scope.launch { nativeAdPool.ensurePreloaded(total) }
                }
            },
        )
    }
}
