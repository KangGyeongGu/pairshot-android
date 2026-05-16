package com.pairshot.app.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pairshot.core.domain.membership.MembershipProvider
import com.pairshot.core.domain.pair.PhotoPairRepository
import com.pairshot.core.domain.settings.AppSettingsRepository
import com.pairshot.core.navigation.Camera
import com.pairshot.core.navigation.Paywall
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StartupDecisionViewModel
    @Inject
    constructor(
        private val appSettingsRepository: AppSettingsRepository,
        private val membershipProvider: MembershipProvider,
        private val photoPairRepository: PhotoPairRepository,
    ) : ViewModel() {
        private val _initialRoute = MutableStateFlow<Any?>(null)
        val initialRoute: StateFlow<Any?> = _initialRoute.asStateFlow()

        init {
            viewModelScope.launch {
                _initialRoute.value = decide()
            }
        }

        private suspend fun decide(): Any {
            if (appSettingsRepository.isOnboardingPaywallShown()) return Camera()
            val canSkipPaywall =
                membershipProvider.current().isPro ||
                    photoPairRepository.countAll().first() > 0
            return if (canSkipPaywall) {
                appSettingsRepository.markOnboardingPaywallShown()
                Camera()
            } else {
                Paywall(dismissible = false)
            }
        }
    }
