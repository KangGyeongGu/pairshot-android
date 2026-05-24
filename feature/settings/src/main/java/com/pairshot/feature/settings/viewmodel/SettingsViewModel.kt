package com.pairshot.feature.settings.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pairshot.core.domain.membership.MembershipProvider
import com.pairshot.core.domain.settings.AppInfo
import com.pairshot.core.domain.settings.AppSettingsRepository
import com.pairshot.core.domain.settings.ClearCacheUseCase
import com.pairshot.core.domain.settings.GetStorageInfoUseCase
import com.pairshot.core.domain.settings.WatermarkRepository
import com.pairshot.core.model.AppSettings
import com.pairshot.core.model.AppTextScale
import com.pairshot.core.model.AppTheme
import com.pairshot.core.model.ImageQualityPreset
import com.pairshot.core.model.WatermarkConfig
import com.pairshot.core.ui.component.SnackbarEvent
import com.pairshot.core.ui.component.SnackbarVariant
import com.pairshot.core.ui.text.UiText
import com.pairshot.feature.settings.R
import com.pairshot.feature.settings.theme.apply
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.pairshot.core.ui.R as CoreR

private const val WHILE_SUBSCRIBED_TIMEOUT_MS = 5_000L

sealed interface SettingsUiState {
    data object Loading : SettingsUiState

    data class Success(
        val usedStorageBytes: Long,
        val cacheBytes: Long,
        val appVersion: String,
        val imageQuality: ImageQualityPreset = ImageQualityPreset.DEFAULT,
        val fileNamePrefix: String = AppSettings.DEFAULT_FILE_NAME_PREFIX,
        val overlayEnabled: Boolean = true,
        val overlayAlpha: Float = AppSettings.DEFAULT_OVERLAY_ALPHA,
    ) : SettingsUiState

    data class Error(
        val message: UiText,
    ) : SettingsUiState
}

@HiltViewModel
class SettingsViewModel
@Inject
constructor(
    private val getStorageInfoUseCase: GetStorageInfoUseCase,
    private val clearCacheUseCase: ClearCacheUseCase,
    private val watermarkRepository: WatermarkRepository,
    private val appSettingsRepository: AppSettingsRepository,
    private val appInfo: AppInfo,
    membershipProvider: MembershipProvider,
) : ViewModel() {
    val isProSubscriber: StateFlow<Boolean> =
        membershipProvider
            .observe()
            .map { it.isPro }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(WHILE_SUBSCRIBED_TIMEOUT_MS),
                initialValue = false,
            )

    private val _uiState = MutableStateFlow<SettingsUiState>(SettingsUiState.Loading)

    val uiState: StateFlow<SettingsUiState> =
        combine(
            _uiState,
            appSettingsRepository.settingsFlow,
        ) { storageState, appSettings ->
            when (storageState) {
                is SettingsUiState.Success -> {
                    storageState.copy(
                        imageQuality = appSettings.imageQuality,
                        fileNamePrefix = appSettings.fileNamePrefix,
                        overlayEnabled = appSettings.overlayEnabled,
                        overlayAlpha = appSettings.defaultOverlayAlpha,
                    )
                }

                SettingsUiState.Loading -> {
                    storageState
                }

                is SettingsUiState.Error -> {
                    storageState
                }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(WHILE_SUBSCRIBED_TIMEOUT_MS),
            initialValue = SettingsUiState.Loading,
        )

    private val _snackbarMessage = MutableSharedFlow<SnackbarEvent>()
    val snackbarMessage: SharedFlow<SnackbarEvent> = _snackbarMessage.asSharedFlow()

    val watermarkConfig: StateFlow<WatermarkConfig> =
        watermarkRepository.watermarkConfigFlow.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = WatermarkConfig(),
        )

    val appTheme: StateFlow<AppTheme> =
        appSettingsRepository.appThemeNameFlow
            .map { AppTheme.fromName(it) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = AppTheme.SYSTEM,
            )

    fun updateAppTheme(theme: AppTheme) {
        viewModelScope.launch {
            appSettingsRepository.updateAppThemeName(theme.name)
            theme.apply()
        }
    }

    val appTextScale: StateFlow<AppTextScale> =
        appSettingsRepository.appTextScaleNameFlow
            .map { AppTextScale.fromName(it) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = AppTextScale.DEFAULT,
            )

    fun updateAppTextScale(scale: AppTextScale) {
        viewModelScope.launch {
            appSettingsRepository.updateAppTextScaleName(scale.name)
        }
    }

    init {
        loadStorageInfo()
    }

    fun refresh() {
        loadStorageInfo()
    }

    private fun loadStorageInfo() {
        viewModelScope.launch {
            try {
                val info = getStorageInfoUseCase()
                _uiState.update {
                    SettingsUiState.Success(
                        usedStorageBytes = info.usedBytes,
                        cacheBytes = info.cacheBytes,
                        appVersion = appInfo.versionName,
                    )
                }
            } catch (_: Exception) {
                _uiState.value =
                    SettingsUiState.Error(UiText.Resource(R.string.settings_error_load_failed))
            }
        }
    }

    fun showMessage(
        message: String,
        variant: SnackbarVariant = SnackbarVariant.INFO,
    ) {
        viewModelScope.launch {
            _snackbarMessage.emit(SnackbarEvent(message, variant))
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            clearCacheUseCase()
            _snackbarMessage.emit(
                SnackbarEvent(
                    UiText.Resource(CoreR.string.snackbar_success_cache_cleared),
                    SnackbarVariant.SUCCESS,
                ),
            )
            loadStorageInfo()
        }
    }

    fun updateWatermarkConfig(config: WatermarkConfig) {
        viewModelScope.launch {
            watermarkRepository.saveConfig(config)
        }
    }

    fun saveLogoFile(uri: String) {
        viewModelScope.launch {
            try {
                val path = watermarkRepository.saveLogoFile(uri)
                val current = watermarkConfig.value
                watermarkRepository.saveConfig(current.copy(logoPath = path))
                watermarkRepository.pruneOldLogoFiles(keepPath = path)
            } catch (_: Exception) {
                _snackbarMessage.emit(
                    SnackbarEvent(
                        UiText.Resource(CoreR.string.snackbar_error_file_load_failed),
                        SnackbarVariant.ERROR,
                    ),
                )
            }
        }
    }

    fun removeLogo() {
        viewModelScope.launch {
            watermarkRepository.removeLogoFile()
            val current = watermarkConfig.value
            watermarkRepository.saveConfig(current.copy(logoPath = ""))
        }
    }

    fun updateImageQuality(preset: ImageQualityPreset) {
        viewModelScope.launch {
            appSettingsRepository.updateImageQuality(preset)
        }
    }

    fun updateFileNamePrefix(prefix: String) {
        viewModelScope.launch {
            appSettingsRepository.updateFileNamePrefix(prefix)
        }
    }

    fun updateOverlayEnabled(enabled: Boolean) {
        viewModelScope.launch {
            appSettingsRepository.updateOverlayEnabled(enabled)
        }
    }

    fun updateOverlayAlpha(alpha: Float) {
        viewModelScope.launch {
            appSettingsRepository.updateOverlayAlpha(alpha)
        }
    }
}

private const val BYTES_PER_KB = 1_024L
private const val BYTES_PER_MB = 1_048_576L
private const val BYTES_PER_GB = 1_073_741_824L

internal fun formatBytes(bytes: Long): String =
    when {
        bytes >= BYTES_PER_GB -> "%.1f GB".format(bytes / BYTES_PER_GB.toDouble())
        bytes >= BYTES_PER_MB -> "%.1f MB".format(bytes / BYTES_PER_MB.toDouble())
        bytes >= BYTES_PER_KB -> "%.1f KB".format(bytes / BYTES_PER_KB.toDouble())
        else -> "$bytes B"
    }
