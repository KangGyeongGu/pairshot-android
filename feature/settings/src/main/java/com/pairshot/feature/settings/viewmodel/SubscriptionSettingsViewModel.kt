package com.pairshot.feature.settings.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pairshot.core.billing.BillingRepository
import com.pairshot.core.billing.domain.SubscriptionStatus
import com.pairshot.core.domain.membership.Membership
import com.pairshot.core.domain.membership.MembershipProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SubscriptionSettingsState(
    val membership: Membership = Membership.Free,
    val subscriptionStatus: SubscriptionStatus = SubscriptionStatus.Inactive,
)

sealed interface SubscriptionSettingsEvent {
    data object RestoreSuccess : SubscriptionSettingsEvent

    data object RestoreEmpty : SubscriptionSettingsEvent
}

@HiltViewModel
class SubscriptionSettingsViewModel
    @Inject
    constructor(
        private val membershipProvider: MembershipProvider,
        private val billingRepository: BillingRepository,
    ) : ViewModel() {
        val state: StateFlow<SubscriptionSettingsState> =
            combine(
                membershipProvider.observe(),
                billingRepository.subscriptionStatus,
            ) { membership, status ->
                SubscriptionSettingsState(membership, status)
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS),
                initialValue = SubscriptionSettingsState(),
            )

        private val _events = MutableSharedFlow<SubscriptionSettingsEvent>(extraBufferCapacity = 1)
        val events: SharedFlow<SubscriptionSettingsEvent> = _events.asSharedFlow()

        fun restore() {
            viewModelScope.launch {
                billingRepository.refresh()
                val isPro = membershipProvider.current().isPro
                _events.tryEmit(
                    if (isPro) SubscriptionSettingsEvent.RestoreSuccess else SubscriptionSettingsEvent.RestoreEmpty,
                )
            }
        }

        fun manageSubscriptionsIntent(productId: String?) = billingRepository.manageSubscriptionsIntent(productId)

        private companion object {
            const val SUBSCRIPTION_TIMEOUT_MS = 5_000L
        }
    }
