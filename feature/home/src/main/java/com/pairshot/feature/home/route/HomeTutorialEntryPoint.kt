package com.pairshot.feature.home.route

import com.pairshot.core.domain.tutorial.TutorialActionDispatcher
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface HomeTutorialEntryPoint {
    fun tutorialActionDispatcher(): TutorialActionDispatcher
}
