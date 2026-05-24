package com.pairshot.feature.pairpreview.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.pairshot.core.domain.combine.CombineSettingsRepository
import com.pairshot.core.domain.combine.DeleteCombinedPhotosUseCase
import com.pairshot.core.domain.combine.ExportHistoryRepository
import com.pairshot.core.domain.pair.PhotoPairRepository
import com.pairshot.core.domain.pair.PrunePairResult
import com.pairshot.core.domain.settings.WatermarkRepository
import com.pairshot.core.model.CombineConfig
import com.pairshot.core.model.ExportHistoryKind
import com.pairshot.core.model.PhotoPair
import com.pairshot.core.model.WatermarkConfig
import com.pairshot.core.navigation.PairPreview
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

private const val WHILE_SUBSCRIBED_TIMEOUT_MS = 5_000L

data class LivePreviewInputs(
    val pair: PhotoPair,
    val config: CombineConfig,
    val watermark: WatermarkConfig,
)

sealed interface PairPreviewUiState {
    data object Loading : PairPreviewUiState

    data class Ready(
        val pair: PhotoPair,
        val hasCombined: Boolean,
        val showDeleteDialog: Boolean,
        val livePreviewInputs: LivePreviewInputs?,
    ) : PairPreviewUiState
}

@HiltViewModel
class PairPreviewViewModel
@Inject
constructor(
    savedStateHandle: SavedStateHandle,
    private val photoPairRepository: PhotoPairRepository,
    private val exportHistoryRepository: ExportHistoryRepository,
    private val deleteCombinedPhotosUseCase: DeleteCombinedPhotosUseCase,
    combineSettingsRepository: CombineSettingsRepository,
    watermarkRepository: WatermarkRepository,
) : ViewModel() {
    private val route = savedStateHandle.toRoute<PairPreview>()
    val pairId: Long = route.pairId

    private val hasCombinedState = MutableStateFlow(false)
    private val deleteDialogVisible = MutableStateFlow(false)

    private val pairFlow: StateFlow<PhotoPair?> =
        photoPairRepository
            .observeById(pairId)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(WHILE_SUBSCRIBED_TIMEOUT_MS),
                initialValue = null,
            )

    private val configFlow: StateFlow<CombineConfig> =
        combineSettingsRepository.configFlow.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(WHILE_SUBSCRIBED_TIMEOUT_MS),
            CombineConfig(),
        )

    private val watermarkFlow: StateFlow<WatermarkConfig> =
        watermarkRepository.watermarkConfigFlow.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(WHILE_SUBSCRIBED_TIMEOUT_MS),
            WatermarkConfig(),
        )

    val uiState: StateFlow<PairPreviewUiState> =
        combine(
            pairFlow,
            hasCombinedState,
            deleteDialogVisible,
            configFlow,
            watermarkFlow,
        ) { pair, hasCombined, showDialog, config, watermark ->
            if (pair == null) {
                PairPreviewUiState.Loading
            } else {
                PairPreviewUiState.Ready(
                    pair = pair,
                    hasCombined = hasCombined,
                    showDeleteDialog = showDialog,
                    livePreviewInputs =
                    if (pair.afterPhotoUri == null) {
                        null
                    } else {
                        LivePreviewInputs(pair, config, watermark)
                    },
                )
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(WHILE_SUBSCRIBED_TIMEOUT_MS),
            initialValue = PairPreviewUiState.Loading,
        )

    private val _deleteComplete =
        MutableSharedFlow<Unit>(
            replay = 0,
            extraBufferCapacity = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )
    val deleteComplete: SharedFlow<Unit> = _deleteComplete.asSharedFlow()

    private val _pruneNotice =
        MutableSharedFlow<PrunePairResult>(
            replay = 0,
            extraBufferCapacity = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )
    val pruneNotice: SharedFlow<PrunePairResult> = _pruneNotice.asSharedFlow()

    init {
        verifyAndLoadPair()
        refreshHasCombined()
    }

    private fun verifyAndLoadPair() {
        viewModelScope.launch {
            val pruneResult =
                runCatching { photoPairRepository.pruneMissingSources(pairId) }
                    .onFailure { Timber.w(it, "pruneMissingSources failed for pair=%d", pairId) }
                    .getOrDefault(PrunePairResult.Healthy)

            when (pruneResult) {
                PrunePairResult.DeletedEntirely, PrunePairResult.NotFound -> {
                    _pruneNotice.emit(pruneResult)
                    _deleteComplete.emit(Unit)
                }

                PrunePairResult.BeforeDropped,
                PrunePairResult.AfterDropped,
                -> {
                    _pruneNotice.emit(pruneResult)
                }

                PrunePairResult.Healthy -> {
                    Unit
                }
            }
        }
    }

    private fun refreshHasCombined() {
        viewModelScope.launch {
            hasCombinedState.value =
                exportHistoryRepository
                    .findByPairIdsAndKind(listOf(pairId), ExportHistoryKind.COMBINED)
                    .isNotEmpty()
        }
    }

    fun showDeleteDialog() {
        deleteDialogVisible.value = true
    }

    fun dismissDeleteDialog() {
        deleteDialogVisible.value = false
    }

    fun deletePair() {
        viewModelScope.launch {
            val currentPair = pairFlow.value ?: return@launch
            runCatching { exportHistoryRepository.deleteByPairIds(listOf(currentPair.id)) }
                .onFailure { error ->
                    Timber.w(error, "failed to clear export history for pair ${currentPair.id}")
                }
            runCatching { photoPairRepository.delete(currentPair) }
                .onFailure { error ->
                    Timber.w(error, "failed to delete pair ${currentPair.id}")
                }
            deleteDialogVisible.value = false
            _deleteComplete.emit(Unit)
        }
    }

    fun deleteCombinedOnly() {
        viewModelScope.launch {
            deleteCombinedPhotosUseCase(listOf(pairId))
            hasCombinedState.value = false
            deleteDialogVisible.value = false
        }
    }
}
