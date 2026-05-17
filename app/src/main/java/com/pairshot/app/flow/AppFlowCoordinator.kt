package com.pairshot.app.flow

import com.pairshot.core.domain.membership.MembershipProvider
import com.pairshot.core.domain.pair.PhotoPairRepository
import com.pairshot.core.domain.settings.OnboardingStateRepository
import com.pairshot.feature.tutorial.domain.TutorialFinishReason
import com.pairshot.feature.tutorial.domain.TutorialSection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppFlowCoordinator
    @Inject
    constructor(
        onboardingStateRepository: OnboardingStateRepository,
        membershipProvider: MembershipProvider,
        photoPairRepository: PhotoPairRepository,
        private val decider: AppFlowDecider,
    ) {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        val state: StateFlow<AppFlowState?> =
            combine(
                onboardingStateRepository.tutorialCompletedFlow,
                onboardingStateRepository.onboardingPaywallShownFlow,
                membershipProvider.observe().map { it.isPro },
                photoPairRepository.countAll().map { it > 0 },
            ) { tutorialCompleted, paywallShown, isPro, hasPairs ->
                AppFlowState(
                    tutorialCompleted = tutorialCompleted,
                    onboardingPaywallShown = paywallShown,
                    isPro = isPro,
                    hasPairs = hasPairs,
                )
            }.stateIn(scope, SharingStarted.Eagerly, null)

        suspend fun current(): AppFlowState = state.filterNotNull().first()

        suspend fun decideStartup(): StartupDecision = decider.decideStartup(current())

        suspend fun decidePostTutorial(
            section: TutorialSection,
            reason: TutorialFinishReason,
        ): PostTutorialDecision = decider.decidePostTutorial(current(), section, reason)
    }
