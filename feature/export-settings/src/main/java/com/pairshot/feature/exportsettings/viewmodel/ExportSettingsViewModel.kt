package com.pairshot.feature.exportsettings.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.pairshot.core.domain.export.ExportPresetRepository
import com.pairshot.core.domain.membership.MembershipProvider
import com.pairshot.core.domain.settings.AppSettingsRepository
import com.pairshot.core.domain.settings.WatermarkRepository
import com.pairshot.core.model.ExportFormat
import com.pairshot.core.model.ExportPreset
import com.pairshot.core.model.ExportPresetSlot
import com.pairshot.core.model.WatermarkConfig
import com.pairshot.core.navigation.ExportSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExportSettingsViewModel
@Inject
constructor(
    savedStateHandle: SavedStateHandle,
    private val appSettingsRepository: AppSettingsRepository,
    private val watermarkRepository: WatermarkRepository,
    private val exportPresetRepository: ExportPresetRepository,
    membershipProvider: MembershipProvider,
) : ViewModel() {
    val pairIds: String = savedStateHandle.toRoute<ExportSettings>().pairIds

    private val _preset = MutableStateFlow(ExportPreset())
    val preset: StateFlow<ExportPreset> = _preset.asStateFlow()

    val slots: StateFlow<ImmutableList<ExportPresetSlot>> =
        exportPresetRepository.slotsFlow
            .map { it.toImmutableList() }
            .stateIn(
                viewModelScope,
                SharingStarted.Eagerly,
                persistentListOf(),
            )

    val activeSlotId: StateFlow<String> =
        exportPresetRepository.activeSlotIdFlow.stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            ExportPresetSlot.DEFAULT_ID,
        )

    val isProSubscriber: StateFlow<Boolean> =
        membershipProvider
            .observe()
            .map { it.isPro }
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val watermarkConfig: StateFlow<WatermarkConfig> =
        watermarkRepository.watermarkConfigFlow.stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            WatermarkConfig(),
        )

    val applyWatermark: StateFlow<Boolean> =
        watermarkConfig
            .map { it.enabled }
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val _slotError = MutableSharedFlow<SlotError>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val slotError: SharedFlow<SlotError> = _slotError.asSharedFlow()

    init {
        viewModelScope.launch {
            _preset.value = appSettingsRepository.getLastExportPreset()
            exportPresetRepository.getActiveSlot()
        }
    }

    val effectivePreset: StateFlow<ExportPreset> =
        combine(preset, isProSubscriber) { raw, isPro -> raw.toEffective(isPro) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, ExportPreset())

    val effectiveActiveSlotId: StateFlow<String> =
        combine(activeSlotId, slots, isProSubscriber) { id, list, isPro ->
            val index = list.indexOfFirst { it.id == id }
            if (!isPro && index >= ExportPresetSlot.FREE_SLOT_LIMIT) {
                ExportPresetSlot.DEFAULT_ID
            } else {
                id
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, ExportPresetSlot.DEFAULT_ID)

    val effectiveWatermarkConfig: StateFlow<WatermarkConfig> =
        combine(watermarkConfig, isProSubscriber) { raw, isPro ->
            if (isPro) raw else WatermarkConfig(enabled = raw.enabled)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, WatermarkConfig())

    fun setIncludeBefore(value: Boolean) {
        updatePreset { it.copy(includeBefore = value) }
    }

    fun setIncludeAfter(value: Boolean) {
        updatePreset { it.copy(includeAfter = value) }
    }

    fun setIncludeCombined(value: Boolean) {
        updatePreset { it.copy(includeCombined = value) }
    }

    fun setFormat(newFormat: ExportFormat) {
        updatePreset { it.copy(format = newFormat) }
    }

    fun setApplyWatermark(value: Boolean) {
        viewModelScope.launch {
            val current = watermarkRepository.getConfig()
            val updated = current.copy(enabled = value)
            watermarkRepository.saveConfig(updated)
            exportPresetRepository.syncActiveSlotWatermark(updated)
        }
    }

    fun setApplyCombineConfig(value: Boolean) {
        updatePreset { it.copy(applyCombineConfig = value) }
    }

    fun selectSlot(id: String) {
        viewModelScope.launch {
            val result = exportPresetRepository.selectSlot(id)
            if (result.isFailure) {
                _slotError.emit(SlotError.SelectFailed)
                return@launch
            }
            _preset.value = appSettingsRepository.getLastExportPreset()
        }
    }

    fun createSlot(name: String) {
        viewModelScope.launch {
            val result = exportPresetRepository.createSlot(name)
            result.fold(
                onSuccess = {
                    _preset.value = appSettingsRepository.getLastExportPreset()
                },
                onFailure = { _slotError.emit(it.toSlotError()) },
            )
        }
    }

    fun renameSlot(
        id: String,
        name: String,
    ) {
        viewModelScope.launch {
            val result = exportPresetRepository.renameSlot(id, name)
            if (result.isFailure) {
                val error = result.exceptionOrNull()?.toSlotError() ?: SlotError.Unknown
                _slotError.emit(error)
            }
        }
    }

    fun deleteSlot(id: String) {
        viewModelScope.launch {
            val result = exportPresetRepository.deleteSlot(id)
            result.fold(
                onSuccess = {
                    _preset.value = appSettingsRepository.getLastExportPreset()
                },
                onFailure = { _slotError.emit(it.toSlotError()) },
            )
        }
    }

    private fun updatePreset(transform: (ExportPreset) -> ExportPreset) {
        val updated = transform(_preset.value)
        _preset.value = updated
        viewModelScope.launch {
            appSettingsRepository.saveLastExportPreset(updated)
            exportPresetRepository.syncActiveSlotExport(updated)
        }
    }

    private fun Throwable.toSlotError(): SlotError =
        when (this) {
            is IllegalArgumentException ->
                if (message?.contains("blank") == true) {
                    SlotError.NameBlank
                } else if (message?.contains("too long") == true) {
                    SlotError.NameTooLong
                } else if (message?.contains("default") == true) {
                    SlotError.CannotDeleteDefault
                } else {
                    SlotError.Unknown
                }

            is IllegalStateException ->
                if (message?.contains("duplicate") == true) {
                    SlotError.NameDuplicate
                } else if (message?.contains("limit") == true) {
                    SlotError.LimitReached
                } else {
                    SlotError.Unknown
                }

            is NoSuchElementException -> SlotError.NotFound
            else -> SlotError.Unknown
        }
}

enum class SlotError {
    NameBlank,
    NameTooLong,
    NameDuplicate,
    LimitReached,
    CannotDeleteDefault,
    NotFound,
    SelectFailed,
    Unknown,
}

private fun ExportPreset.toEffective(isPro: Boolean): ExportPreset =
    if (isPro) this else copy(format = ExportFormat.INDIVIDUAL)
