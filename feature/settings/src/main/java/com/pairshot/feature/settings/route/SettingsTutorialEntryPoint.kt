package com.pairshot.feature.settings.route

import com.pairshot.core.domain.tutorial.TutorialReplayController
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface SettingsTutorialEntryPoint {
    fun tutorialReplayController(): TutorialReplayController
}
