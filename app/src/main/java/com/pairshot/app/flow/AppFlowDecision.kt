package com.pairshot.app.flow

sealed interface StartupDecision {
    data object FirstLaunchTutorial : StartupDecision

    data object OnboardingPaywall : StartupDecision

    data object Camera : StartupDecision
}

sealed interface PostTutorialDecision {
    data object NavigateToOnboardingPaywall : PostTutorialDecision

    data object Stay : PostTutorialDecision
}
