package com.pairshot.core.domain.settings

import kotlinx.coroutines.flow.Flow

interface OnboardingStateRepository {
    val onboardingPaywallShownFlow: Flow<Boolean>

    suspend fun isOnboardingPaywallShown(): Boolean

    suspend fun markOnboardingPaywallShown()

    val tutorialCompletedFlow: Flow<Boolean>

    suspend fun isTutorialCompleted(): Boolean

    suspend fun isExportSettingsTutorialCompleted(): Boolean
}
