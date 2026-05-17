package com.pairshot.feature.tutorial.di

import com.pairshot.core.domain.tutorial.TutorialActionDispatcher
import com.pairshot.core.domain.tutorial.TutorialAnchorReporter
import com.pairshot.core.domain.tutorial.TutorialModeProvider
import com.pairshot.core.domain.tutorial.TutorialPairTracker
import com.pairshot.core.domain.tutorial.TutorialReplayController
import com.pairshot.feature.tutorial.domain.TutorialAnchorRegistry
import com.pairshot.feature.tutorial.domain.TutorialCoordinator
import com.pairshot.feature.tutorial.internal.TutorialSandbox
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class TutorialModule {
    @Binds
    abstract fun bindTutorialModeProvider(impl: TutorialCoordinator): TutorialModeProvider

    @Binds
    abstract fun bindTutorialActionDispatcher(impl: TutorialCoordinator): TutorialActionDispatcher

    @Binds
    abstract fun bindTutorialAnchorReporter(impl: TutorialAnchorRegistry): TutorialAnchorReporter

    @Binds
    abstract fun bindTutorialPairTracker(impl: TutorialSandbox): TutorialPairTracker

    @Binds
    abstract fun bindTutorialReplayController(impl: TutorialCoordinator): TutorialReplayController
}
