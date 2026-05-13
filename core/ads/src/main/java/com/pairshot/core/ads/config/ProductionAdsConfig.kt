package com.pairshot.core.ads.config

import com.pairshot.core.ads.BuildConfig

object ProductionAdsConfig : AdsConfig {
    override val bannerAdUnitId: String = BuildConfig.ADMOB_BANNER_AD_UNIT_ID
    override val interstitialExportCompleteAdUnitId: String = BuildConfig.ADMOB_INTERSTITIAL_EXPORT_COMPLETE_AD_UNIT_ID
    override val appOpenAdUnitId: String = BuildConfig.ADMOB_APP_OPEN_AD_UNIT_ID
    override val nativeAdUnitId: String = BuildConfig.ADMOB_NATIVE_AD_UNIT_ID
    override val rewardedAdUnitId: String = BuildConfig.ADMOB_REWARDED_AD_UNIT_ID
}
