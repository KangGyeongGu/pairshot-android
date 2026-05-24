package com.pairshot.app.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pairshot.app.flow.AppFlowCoordinator
import com.pairshot.app.flow.StartupDecision
import com.pairshot.core.domain.settings.OnboardingStateRepository
import com.pairshot.core.navigation.Camera
import com.pairshot.core.navigation.Home
import com.pairshot.core.navigation.Paywall
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StartupPlan(
    val initialRoute: Any,
    val startMainTutorial: Boolean,
)

@HiltViewModel
class StartupDecisionViewModel
@Inject
constructor(
    private val appFlowCoordinator: AppFlowCoordinator,
    private val onboardingStateRepository: OnboardingStateRepository,
) : ViewModel() {
    private val _plan = MutableStateFlow<StartupPlan?>(null)
    val plan: StateFlow<StartupPlan?> = _plan.asStateFlow()

    init {
        viewModelScope.launch {
            _plan.value = resolvePlan()
        }
    }

    private suspend fun resolvePlan(): StartupPlan =
        when (appFlowCoordinator.decideStartup()) {
            StartupDecision.FirstLaunchTutorial -> {
                StartupPlan(initialRoute = Home, startMainTutorial = true)
            }

            StartupDecision.OnboardingPaywall -> {
                StartupPlan(initialRoute = Paywall(dismissible = false), startMainTutorial = false)
            }

            StartupDecision.Camera -> {
                onboardingStateRepository.markOnboardingPaywallShown()
                StartupPlan(initialRoute = Camera(), startMainTutorial = false)
            }
        }
}
