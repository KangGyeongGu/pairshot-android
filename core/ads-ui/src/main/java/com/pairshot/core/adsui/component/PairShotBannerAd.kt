package com.pairshot.core.adsui.component

import android.app.Activity
import android.content.Context
import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.pairshot.core.ads.config.AdsConfig
import com.pairshot.core.ads.di.AdsEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.map
import timber.log.Timber

val DefaultAdaptiveBannerFallbackHeight: Dp = 62.dp

@Composable
fun PairShotBannerAd(
    modifier: Modifier = Modifier,
    height: Dp? = null,
) {
    if (LocalInspectionMode.current) {
        Box(modifier = modifier.fillMaxWidth().height(height ?: DefaultAdaptiveBannerFallbackHeight))
        return
    }
    val context = LocalContext.current
    val entryPoint =
        remember(context) {
            EntryPointAccessors.fromApplication(
                context.applicationContext,
                AdsEntryPoint::class.java,
            )
        }
    val adsConfig = remember(entryPoint) { entryPoint.adsConfig() }
    val entitlementProvider = remember(entryPoint) { entryPoint.proEntitlementProvider() }
    val isActiveFlow = remember(entitlementProvider) { entitlementProvider.observe().map { it.isActive } }
    val isAdFree: Boolean? by isActiveFlow.collectAsStateWithLifecycle(initialValue = null)

    val windowInfo = LocalWindowInfo.current
    val density = LocalDensity.current
    val adWidth =
        with(density) {
            windowInfo.containerSize.width
                .toDp()
                .value
                .toInt()
        }
    val activity = remember(context) { context.asActivityOrNull() }

    val slotHeight =
        remember(height, activity, adWidth, density) {
            height ?: resolveAdaptiveBannerHeight(activity, adWidth, density)
        }

    if (isAdFree == null) {
        Box(modifier = modifier.fillMaxWidth().height(slotHeight))
        return
    }

    if (isAdFree == true) return

    val adView =
        remember(context, adsConfig, adWidth) {
            buildAdView(context = context, adsConfig = adsConfig, widthDp = adWidth)
        }

    DisposableEffect(adView) {
        adView.adListener =
            object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Timber.tag(TAG).w("banner load failed: %s", error.message)
                }

                override fun onAdLoaded() {
                    Timber.tag(TAG).d("banner loaded")
                }
            }
        onDispose { }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, adView) {
        val observer =
            LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_RESUME -> adView.resume()
                    Lifecycle.Event.ON_PAUSE -> adView.pause()
                    Lifecycle.Event.ON_DESTROY -> adView.destroy()
                    else -> Unit
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            adView.destroy()
        }
    }

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(slotHeight),
        contentAlignment = Alignment.Center,
    ) {
        AndroidView(
            factory = { adView },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private fun resolveAdaptiveBannerHeight(
    activity: Activity?,
    widthDp: Int,
    density: Density,
): Dp {
    if (activity == null) return DefaultAdaptiveBannerFallbackHeight
    val adSize = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, widthDp)
    val heightPx = adSize.getHeightInPixels(activity)
    if (heightPx <= 0) return DefaultAdaptiveBannerFallbackHeight
    return with(density) { heightPx.toDp() }
}

private fun buildAdView(
    context: Context,
    adsConfig: AdsConfig,
    widthDp: Int,
): AdView {
    val activity = context.asActivityOrNull()
    val adSize =
        if (activity != null) {
            AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, widthDp)
        } else {
            AdSize.BANNER
        }
    return AdView(context).apply {
        layoutParams =
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
        setAdSize(adSize)
        adUnitId = adsConfig.bannerAdUnitId
        loadAd(AdRequest.Builder().build())
    }
}

private tailrec fun Context.asActivityOrNull(): Activity? =
    when (this) {
        is Activity -> this
        is android.content.ContextWrapper -> baseContext.asActivityOrNull()
        else -> null
    }

private const val TAG = "PairShotBannerAd"
