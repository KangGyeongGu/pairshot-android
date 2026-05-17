package com.pairshot.core.ads.di

import com.pairshot.core.ads.config.AdsConfig
import com.pairshot.core.ads.controller.FullscreenAdState
import com.pairshot.core.ads.controller.InterstitialAdController
import com.pairshot.core.ads.controller.NativeAdPool
import com.pairshot.core.ads.controller.RewardedAdController
import com.pairshot.core.ads.initializer.AdsInitializer
import com.pairshot.core.ads.premium.SettingsPremiumGate
import com.pairshot.core.domain.membership.MembershipProvider
import com.pairshot.core.domain.tutorial.TutorialModeProvider
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Provider

@EntryPoint
@InstallIn(SingletonComponent::class)
interface AdsEntryPoint {
    fun adsConfig(): AdsConfig

    fun adsInitializer(): AdsInitializer

    fun membershipProvider(): MembershipProvider

    fun fullscreenAdState(): FullscreenAdState

    fun interstitialAdController(): InterstitialAdController

    fun rewardedAdController(): RewardedAdController

    fun settingsPremiumGate(): SettingsPremiumGate

    fun nativeAdPoolProvider(): Provider<NativeAdPool>

    fun tutorialModeProvider(): TutorialModeProvider
}
