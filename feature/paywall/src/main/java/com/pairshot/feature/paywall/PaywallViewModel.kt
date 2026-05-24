package com.pairshot.feature.paywall

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pairshot.core.billing.BillingProductCatalog
import com.pairshot.core.billing.BillingRepository
import com.pairshot.core.billing.PurchaseLaunchResult
import com.pairshot.core.billing.domain.BillingOffer
import com.pairshot.core.billing.domain.PurchaseError
import com.pairshot.core.domain.membership.MembershipProvider
import com.pairshot.core.domain.settings.OnboardingStateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PaywallUiState(
    val loading: Boolean = true,
    val loadError: Boolean = false,
    val trialOffer: BillingOffer? = null,
    val monthlyOffer: BillingOffer? = null,
    val yearlyOffer: BillingOffer? = null,
)

sealed interface PaywallEvent {
    data object EntitlementGranted : PaywallEvent

    data class PurchaseFailed(
        val reason: PurchaseError,
    ) : PaywallEvent

    data object AlreadyOwned : PaywallEvent

    data object RestoreSuccess : PaywallEvent

    data object RestoreEmpty : PaywallEvent

    data object ContinuedFree : PaywallEvent
}

@HiltViewModel
class PaywallViewModel
@Inject
constructor(
    private val billingRepository: BillingRepository,
    private val membershipProvider: MembershipProvider,
    private val onboardingStateRepository: OnboardingStateRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(PaywallUiState())
    val uiState: StateFlow<PaywallUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<PaywallEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<PaywallEvent> = _events.asSharedFlow()

    init {
        loadOffers()
        observeEntitlement()
    }

    fun loadOffers() {
        _uiState.update { it.copy(loading = true, loadError = false) }
        viewModelScope.launch {
            billingRepository.loadOffers().fold(
                onSuccess = { offers ->
                    val monthlyPlan = BillingProductCatalog.BASE_PLAN_MONTHLY
                    val yearlyPlan = BillingProductCatalog.BASE_PLAN_YEARLY
                    val trial = offers.firstOrNull { it.trialDays != null && it.basePlanId == monthlyPlan }
                    val monthly = offers.firstOrNull { it.basePlanId == monthlyPlan && it.trialDays == null }
                    val yearly = offers.firstOrNull { it.basePlanId == yearlyPlan }
                    _uiState.value =
                        PaywallUiState(
                            loading = false,
                            loadError = false,
                            trialOffer = trial,
                            monthlyOffer = monthly ?: trial,
                            yearlyOffer = yearly,
                        )
                },
                onFailure = {
                    _uiState.value = PaywallUiState(loading = false, loadError = true)
                },
            )
        }
    }

    fun purchase(
        activity: Activity,
        offer: BillingOffer,
    ) {
        viewModelScope.launch {
            when (val result = billingRepository.launchPurchaseFlow(activity, offer)) {
                PurchaseLaunchResult.Launched -> {
                    Unit
                }

                PurchaseLaunchResult.AlreadyOwned -> {
                    _events.tryEmit(PaywallEvent.AlreadyOwned)
                }

                is PurchaseLaunchResult.Failed -> {
                    if (result.error !is PurchaseError.UserCanceled) {
                        _events.tryEmit(PaywallEvent.PurchaseFailed(result.error))
                    }
                }
            }
        }
    }

    fun restore() {
        viewModelScope.launch {
            billingRepository.refresh()
            val isPro = membershipProvider.current().isPro
            _events.tryEmit(
                if (isPro) PaywallEvent.RestoreSuccess else PaywallEvent.RestoreEmpty,
            )
        }
    }

    fun continueFree() {
        viewModelScope.launch {
            onboardingStateRepository.markOnboardingPaywallShown()
            _events.tryEmit(PaywallEvent.ContinuedFree)
        }
    }

    private fun observeEntitlement() {
        viewModelScope.launch {
            membershipProvider
                .observe()
                .drop(1)
                .collect { membership ->
                    if (membership.isPro) {
                        onboardingStateRepository.markOnboardingPaywallShown()
                        _events.tryEmit(PaywallEvent.EntitlementGranted)
                    }
                }
        }
    }
}
