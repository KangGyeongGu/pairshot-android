package com.pairshot.core.promotion.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pairshot.core.promotion.domain.ActivatePromotionUseCase
import com.pairshot.core.promotion.domain.ActivationResult
import com.pairshot.core.promotion.domain.ObservePromotionStateUseCase
import com.pairshot.core.promotion.domain.Promotion
import com.pairshot.core.promotion.domain.PromotionRepository
import com.pairshot.core.promotion.domain.PromotionState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class PromotionViewModel
@Inject
constructor(
    observePromotionStateUseCase: ObservePromotionStateUseCase,
    private val activatePromotionUseCase: ActivatePromotionUseCase,
    private val repository: PromotionRepository,
) : ViewModel() {
    val state: StateFlow<PromotionState> =
        observePromotionStateUseCase().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(WHILE_SUBSCRIBED_TIMEOUT_MS),
            initialValue = PromotionState.Empty,
        )

    val myPromotions: StateFlow<List<Promotion>> =
        kotlinx.coroutines.flow
            .MutableStateFlow(emptyList<Promotion>())
            .also { flow ->
                viewModelScope.launch {
                    state.collect { snapshot -> flow.value = snapshot.promotions }
                }
            }.asStateFlow()

    private val _activationState =
        MutableStateFlow<PromotionActivationUiState>(PromotionActivationUiState.Idle)
    val activationState: StateFlow<PromotionActivationUiState> = _activationState.asStateFlow()

    private val _myPromotionsLoading = MutableStateFlow(false)
    val myPromotionsLoading: StateFlow<Boolean> = _myPromotionsLoading.asStateFlow()

    fun activate(code: String) {
        if (code.isBlank()) {
            _activationState.value =
                PromotionActivationUiState.Failure(ActivationResult.Failure.InvalidFormat)
            return
        }
        viewModelScope.launch {
            _activationState.value = PromotionActivationUiState.Loading
            val result = activatePromotionUseCase(code.trim())
            _activationState.value =
                when (result) {
                    is ActivationResult.Success -> {
                        PromotionActivationUiState.Success(
                            entitlement = result.promotion.entitlement,
                            durationDays = result.promotion.durationDays,
                        )
                    }

                    is ActivationResult.Failure -> {
                        PromotionActivationUiState.Failure(result)
                    }
                }
        }
    }

    fun resetActivationState() {
        _activationState.value = PromotionActivationUiState.Idle
    }

    fun loadMyPromotions() {
        viewModelScope.launch {
            _myPromotionsLoading.value = true
            runCatching { repository.refresh() }
                .onFailure { Timber.w(it, "fetch my promotions failed") }
            _myPromotionsLoading.value = false
        }
    }

    private companion object {
        const val WHILE_SUBSCRIBED_TIMEOUT_MS = 5_000L
    }
}
