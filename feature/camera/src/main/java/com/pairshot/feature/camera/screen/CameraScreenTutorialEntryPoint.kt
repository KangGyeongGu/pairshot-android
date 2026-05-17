package com.pairshot.feature.camera.screen

import com.pairshot.core.domain.tutorial.TutorialActionDispatcher
import com.pairshot.core.domain.tutorial.TutorialModeProvider
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface CameraScreenTutorialEntryPoint {
    fun tutorialActionDispatcher(): TutorialActionDispatcher

    fun tutorialModeProvider(): TutorialModeProvider
}
