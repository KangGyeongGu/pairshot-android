package com.pairshot.app.flow

import com.pairshot.feature.tutorial.domain.TutorialFinishReason
import com.pairshot.feature.tutorial.domain.TutorialSection
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppFlowDecider
    @Inject
    constructor() {
        fun decideStartup(state: AppFlowState): StartupDecision =
            when {
                !state.tutorialCompleted -> StartupDecision.FirstLaunchTutorial
                state.onboardingPaywallShown -> StartupDecision.Camera
                state.isPro || state.hasPairs -> StartupDecision.Camera
                else -> StartupDecision.OnboardingPaywall
            }

        fun decidePostTutorial(
            state: AppFlowState,
            section: TutorialSection,
            reason: TutorialFinishReason,
        ): PostTutorialDecision =
            when {
                section != TutorialSection.MAIN_ONBOARDING -> PostTutorialDecision.Stay
                reason == TutorialFinishReason.REPLAY -> PostTutorialDecision.Stay
                state.onboardingPaywallShown -> PostTutorialDecision.Stay
                state.isPro || state.hasPairs -> PostTutorialDecision.Stay
                else -> PostTutorialDecision.NavigateToOnboardingPaywall
            }
    }
