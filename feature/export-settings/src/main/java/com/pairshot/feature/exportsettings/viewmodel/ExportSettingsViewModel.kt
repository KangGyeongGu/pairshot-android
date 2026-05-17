package com.pairshot.feature.exportsettings.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.pairshot.core.domain.membership.MembershipProvider
import com.pairshot.core.domain.settings.AppSettingsRepository
import com.pairshot.core.domain.settings.WatermarkRepository
import com.pairshot.core.model.ExportFormat
import com.pairshot.core.model.ExportPreset
import com.pairshot.core.model.WatermarkConfig
import com.pairshot.core.navigation.ExportSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
        membershipProvider: MembershipProvider,
    ) : ViewModel() {
        val pairIds: String = savedStateHandle.toRoute<ExportSettings>().pairIds

        private val _preset = MutableStateFlow(ExportPreset())
        val preset: StateFlow<ExportPreset> = _preset.asStateFlow()

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

        init {
            viewModelScope.launch {
                _preset.value = appSettingsRepository.getLastExportPreset()
            }
        }

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
                watermarkRepository.saveConfig(current.copy(enabled = value))
            }
        }

        fun setApplyCombineConfig(value: Boolean) {
            updatePreset { it.copy(applyCombineConfig = value) }
        }

        private fun updatePreset(transform: (ExportPreset) -> ExportPreset) {
            val updated = transform(_preset.value)
            _preset.value = updated
            viewModelScope.launch {
                appSettingsRepository.saveLastExportPreset(updated)
            }
        }
    }
