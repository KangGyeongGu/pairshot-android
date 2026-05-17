package com.pairshot.feature.exportsettings.route

import com.pairshot.core.domain.settings.OnboardingStateRepository
import com.pairshot.feature.tutorial.domain.TutorialCoordinator
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface ExportSettingsTutorialEntryPoint {
    fun tutorialCoordinator(): TutorialCoordinator

    fun onboardingStateRepository(): OnboardingStateRepository
}
