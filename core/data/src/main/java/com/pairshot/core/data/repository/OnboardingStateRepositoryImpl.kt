package com.pairshot.core.data.repository

import com.pairshot.core.datastore.AppPreferences
import com.pairshot.core.domain.settings.OnboardingStateRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class OnboardingStateRepositoryImpl
    @Inject
    constructor(
        private val appPreferences: AppPreferences,
    ) : OnboardingStateRepository {
        override val onboardingPaywallShownFlow: Flow<Boolean> = appPreferences.onboardingPaywallShown

        override suspend fun isOnboardingPaywallShown(): Boolean = appPreferences.onboardingPaywallShown.first()

        override suspend fun markOnboardingPaywallShown() = appPreferences.setOnboardingPaywallShown(true)

        override val tutorialCompletedFlow: Flow<Boolean> = appPreferences.tutorialCompleted

        override suspend fun isTutorialCompleted(): Boolean = appPreferences.tutorialCompleted.first()

        override suspend fun isExportSettingsTutorialCompleted(): Boolean = appPreferences.exportSettingsTutorialCompleted.first()
    }
