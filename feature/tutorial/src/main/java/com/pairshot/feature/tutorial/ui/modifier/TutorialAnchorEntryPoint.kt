package com.pairshot.feature.tutorial.ui.modifier

import com.pairshot.core.domain.tutorial.TutorialAnchorReporter
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface TutorialAnchorEntryPoint {
    fun tutorialAnchorReporter(): TutorialAnchorReporter
}
