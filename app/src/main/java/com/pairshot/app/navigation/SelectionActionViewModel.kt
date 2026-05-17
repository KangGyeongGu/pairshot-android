package com.pairshot.app.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pairshot.core.domain.combine.CombineSettingsRepository
import com.pairshot.core.domain.export.ExportAction
import com.pairshot.core.domain.export.ExportRepository
import com.pairshot.core.domain.export.HasSavableSelectionUseCase
import com.pairshot.core.domain.export.SaveSelectionToDeviceUseCase
import com.pairshot.core.domain.export.SaveToDeviceResult
import com.pairshot.core.domain.export.ShareSelectionUseCase
import com.pairshot.core.domain.pair.SyncMissingSourcesUseCase
import com.pairshot.core.domain.settings.AppSettingsRepository
import com.pairshot.core.domain.settings.WatermarkRepository
import com.pairshot.core.model.CombineConfig
import com.pairshot.core.model.ExportPreset
import com.pairshot.core.model.WatermarkConfig
import com.pairshot.core.model.isContentMissing
import com.pairshot.core.ui.R
import com.pairshot.core.ui.text.UiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

sealed interface SelectionMessage {
    val text: UiText

    data class Success(
        override val text: UiText,
    ) : SelectionMessage

    data class Warning(
        override val text: UiText,
    ) : SelectionMessage

    data class Error(
        override val text: UiText,
    ) : SelectionMessage
}

data class SaveDocumentRequest(
    val sourceFilePath: String,
    val suggestedName: String,
    val mimeType: String,
)

sealed interface SaveDocumentResult {
    val sourceFilePath: String

    data class Saved(
        override val sourceFilePath: String,
        val displayName: String,
    ) : SaveDocumentResult

    data class Cancelled(
        override val sourceFilePath: String,
    ) : SaveDocumentResult

    data class Failed(
        override val sourceFilePath: String,
        val error: Throwable,
    ) : SaveDocumentResult
}

@HiltViewModel
class SelectionActionViewModel
    @Inject
    constructor(
        private val shareSelectionUseCase: ShareSelectionUseCase,
        private val saveSelectionToDeviceUseCase: SaveSelectionToDeviceUseCase,
        private val hasSavableSelectionUseCase: HasSavableSelectionUseCase,
        private val syncMissingSourcesUseCase: SyncMissingSourcesUseCase,
        private val exportRepository: ExportRepository,
        private val combineSettingsRepository: CombineSettingsRepository,
        private val watermarkRepository: WatermarkRepository,
        private val appSettingsRepository: AppSettingsRepository,
    ) : ViewModel() {
        private val _exportAction = MutableSharedFlow<ExportAction>()
        val exportAction: SharedFlow<ExportAction> = _exportAction.asSharedFlow()

        private val _saveDocumentRequests = MutableSharedFlow<SaveDocumentRequest>()
        val saveDocumentRequests: SharedFlow<SaveDocumentRequest> = _saveDocumentRequests.asSharedFlow()

        private val _messages = MutableSharedFlow<SelectionMessage>()
        val messages: SharedFlow<SelectionMessage> = _messages.asSharedFlow()

        private val _progress = MutableStateFlow<Progress?>(null)
        val progress: StateFlow<Progress?> = _progress.asStateFlow()

        fun shareSelection(ids: Set<Long>) {
            if (ids.isEmpty()) return
            viewModelScope.launch {
                runCatching { syncMissingSourcesUseCase() }
                    .onFailure { Timber.w(it, "syncMissingSources before share failed") }
                val (preset, combine, watermark) = loadConfig()
                if (watermark?.isContentMissing() == true) {
                    _messages.emit(
                        SelectionMessage.Warning(
                            UiText.Resource(R.string.snackbar_warning_watermark_setup_required),
                        ),
                    )
                    return@launch
                }
                val shareLabel = UiText.Resource(com.pairshot.R.string.progress_sharing)
                _progress.value = Progress(shareLabel, 0, ids.size)
                runCatching {
                    val action =
                        shareSelectionUseCase(
                            pairIds = ids.toList(),
                            preset = preset,
                            watermarkConfig = watermark,
                            combineConfig = combine,
                        ) { current, total ->
                            _progress.value = Progress(shareLabel, current, total)
                        }
                    _exportAction.emit(action)
                }.onFailure { error ->
                    Timber.e(error, "share failed")
                    _messages.emit(SelectionMessage.Error(UiText.Resource(R.string.snackbar_error_share_failed)))
                }
                _progress.value = null
            }
        }

        fun saveSelectionToDevice(ids: Set<Long>) {
            if (ids.isEmpty()) return
            viewModelScope.launch {
                runCatching { syncMissingSourcesUseCase() }
                    .onFailure { Timber.w(it, "syncMissingSources before save failed") }
                val (preset, combine, watermark) = loadConfig()
                if (watermark?.isContentMissing() == true) {
                    _messages.emit(
                        SelectionMessage.Warning(
                            UiText.Resource(R.string.snackbar_warning_watermark_setup_required),
                        ),
                    )
                    return@launch
                }
                val hasWork =
                    runCatching {
                        hasSavableSelectionUseCase(ids.toList(), preset, watermark)
                    }.getOrDefault(false)
                if (!hasWork) {
                    _messages.emit(
                        SelectionMessage.Warning(UiText.Resource(R.string.snackbar_warning_nothing_to_save)),
                    )
                    return@launch
                }
                val saveLabel = UiText.Resource(com.pairshot.R.string.progress_saving)
                _progress.value = Progress(saveLabel, 0, ids.size)
                runCatching {
                    saveSelectionToDeviceUseCase(
                        pairIds = ids.toList(),
                        preset = preset,
                        watermarkConfig = watermark,
                        combineConfig = combine,
                    ) { current, total ->
                        _progress.value = Progress(saveLabel, current, total)
                    }
                }.onSuccess { result ->
                    handleSaveResult(result)
                }.onFailure { error ->
                    Timber.e(error, "save to device failed")
                    _messages.emit(SelectionMessage.Error(UiText.Resource(R.string.snackbar_error_save_failed)))
                }
                _progress.value = null
            }
        }

        private suspend fun handleSaveResult(result: SaveToDeviceResult) {
            when (result) {
                is SaveToDeviceResult.SavedImagesToGallery -> {
                    _messages.emit(
                        SelectionMessage.Success(UiText.Resource(R.string.snackbar_success_saved_to_device)),
                    )
                }

                is SaveToDeviceResult.ZipReadyForSave -> {
                    _saveDocumentRequests.emit(
                        SaveDocumentRequest(
                            sourceFilePath = result.filePath,
                            suggestedName = result.suggestedName,
                            mimeType = "application/zip",
                        ),
                    )
                }

                SaveToDeviceResult.Nothing -> {
                    _messages.emit(
                        SelectionMessage.Warning(UiText.Resource(R.string.snackbar_warning_nothing_to_save)),
                    )
                }
            }
        }

        fun onSaveDocumentResult(result: SaveDocumentResult) {
            viewModelScope.launch {
                when (result) {
                    is SaveDocumentResult.Saved -> {
                        _messages.emit(
                            SelectionMessage.Success(
                                UiText.Resource(
                                    R.string.snackbar_success_saved_zip,
                                    listOf(result.displayName),
                                ),
                            ),
                        )
                    }

                    is SaveDocumentResult.Cancelled -> {
                        _messages.emit(
                            SelectionMessage.Warning(UiText.Resource(R.string.snackbar_info_save_cancelled)),
                        )
                    }

                    is SaveDocumentResult.Failed -> {
                        Timber.e(result.error, "zip save failed")
                        _messages.emit(
                            SelectionMessage.Error(UiText.Resource(R.string.snackbar_error_save_failed)),
                        )
                    }
                }
                exportRepository.discardPreparedZip(result.sourceFilePath)
            }
        }

        private suspend fun loadConfig(): Triple<ExportPreset, CombineConfig, WatermarkConfig?> {
            val preset = appSettingsRepository.getLastExportPreset()
            val combine = combineSettingsRepository.configFlow.first()
            val watermark =
                runCatching { watermarkRepository.watermarkConfigFlow.first() }
                    .getOrNull()
                    ?.takeIf { it.enabled }
            return Triple(preset, combine, watermark)
        }
    }

data class Progress(
    val label: UiText,
    val current: Int,
    val total: Int,
)
