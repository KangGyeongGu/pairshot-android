package com.pairshot.app.flow

data class AppFlowState(
    val tutorialCompleted: Boolean,
    val onboardingPaywallShown: Boolean,
    val isPro: Boolean,
    val hasPairs: Boolean,
)
