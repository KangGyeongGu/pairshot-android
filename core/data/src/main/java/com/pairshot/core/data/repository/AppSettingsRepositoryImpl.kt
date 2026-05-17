package com.pairshot.core.data.repository

import com.pairshot.core.datastore.AppPreferences
import com.pairshot.core.domain.settings.AppSettingsRepository
import com.pairshot.core.model.AppSettings
import com.pairshot.core.model.AspectRatio
import com.pairshot.core.model.ExportFormat
import com.pairshot.core.model.ExportPreset
import com.pairshot.core.model.ImageQualityPreset
import com.pairshot.core.model.SortOrder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class AppSettingsRepositoryImpl
    @Inject
    constructor(
        private val appPreferences: AppPreferences,
    ) : AppSettingsRepository {
        private val baseSettingsFlow: Flow<AppSettings> =
            combine(
                appPreferences.imageQuality,
                appPreferences.fileNamePrefix,
                appPreferences.overlayEnabled,
                appPreferences.overlayAlpha,
            ) { qualityName, prefix, enabled, alpha ->
                AppSettings(
                    imageQuality = qualityName.toImageQualityPreset(),
                    fileNamePrefix = prefix,
                    overlayEnabled = enabled,
                    defaultOverlayAlpha = alpha,
                )
            }

        private val cameraSettingsFlow: Flow<AppSettings> =
            combine(
                appPreferences.cameraGridEnabled,
                appPreferences.cameraLevelEnabled,
                appPreferences.cameraFlashMode,
                appPreferences.cameraNightMode,
                appPreferences.cameraHdr,
            ) { grid, level, flash, night, hdr ->
                AppSettings(
                    cameraGridEnabled = grid,
                    cameraLevelEnabled = level,
                    cameraFlashMode = flash,
                    cameraNightModeEnabled = night,
                    cameraHdrEnabled = hdr,
                )
            }.combine(appPreferences.cameraAspectRatio) { partial, ratioName ->
                partial.copy(cameraAspectRatio = ratioName.toAspectRatio())
            }

        override val settingsFlow: Flow<AppSettings> =
            combine(baseSettingsFlow, cameraSettingsFlow) { base, camera ->
                base.copy(
                    cameraGridEnabled = camera.cameraGridEnabled,
                    cameraLevelEnabled = camera.cameraLevelEnabled,
                    cameraFlashMode = camera.cameraFlashMode,
                    cameraNightModeEnabled = camera.cameraNightModeEnabled,
                    cameraHdrEnabled = camera.cameraHdrEnabled,
                    cameraAspectRatio = camera.cameraAspectRatio,
                )
            }

        override suspend fun getCurrent(): AppSettings = settingsFlow.first()

        override suspend fun updateImageQuality(preset: ImageQualityPreset) = appPreferences.setImageQuality(preset.name)

        override suspend fun updateFileNamePrefix(prefix: String) = appPreferences.setFileNamePrefix(prefix)

        override suspend fun updateOverlayEnabled(enabled: Boolean) = appPreferences.setOverlayEnabled(enabled)

        override suspend fun updateOverlayAlpha(alpha: Float) = appPreferences.setOverlayAlpha(alpha)

        override suspend fun updateCameraGridEnabled(enabled: Boolean) = appPreferences.setCameraGridEnabled(enabled)

        override suspend fun updateCameraLevelEnabled(enabled: Boolean) = appPreferences.setCameraLevelEnabled(enabled)

        override suspend fun updateCameraFlashMode(mode: String) = appPreferences.setCameraFlashMode(mode)

        override suspend fun updateCameraNightMode(enabled: Boolean) = appPreferences.setCameraNightMode(enabled)

        override suspend fun updateCameraHdr(enabled: Boolean) = appPreferences.setCameraHdr(enabled)

        override suspend fun updateCameraAspectRatio(ratio: AspectRatio) = appPreferences.setCameraAspectRatio(ratio.name)

        override suspend fun getLastExportPreset(): ExportPreset {
            val format = appPreferences.exportFormat.first()
            val includeBefore = appPreferences.exportIncludeBefore.first()
            val includeAfter = appPreferences.exportIncludeAfter.first()
            val includeCombined = appPreferences.exportIncludeCombined.first()
            val applyCombineConfig = appPreferences.exportApplyCombineConfig.first()
            return ExportPreset(
                format = runCatching { ExportFormat.valueOf(format) }.getOrDefault(ExportFormat.INDIVIDUAL),
                includeBefore = includeBefore,
                includeAfter = includeAfter,
                includeCombined = includeCombined,
                applyCombineConfig = applyCombineConfig,
            )
        }

        override suspend fun saveLastExportPreset(preset: ExportPreset) {
            appPreferences.saveExportPreset(
                format = preset.format.name,
                includeBefore = preset.includeBefore,
                includeAfter = preset.includeAfter,
                includeCombined = preset.includeCombined,
                applyCombineConfig = preset.applyCombineConfig,
            )
        }

        override val homeSortOrderFlow: Flow<SortOrder> = appPreferences.homeSortOrder.map { it.toSortOrder() }

        override val albumSortOrderFlow: Flow<SortOrder> = appPreferences.albumSortOrder.map { it.toSortOrder() }

        override suspend fun updateHomeSortOrder(order: SortOrder) = appPreferences.setHomeSortOrder(order.name)

        override suspend fun updateAlbumSortOrder(order: SortOrder) = appPreferences.setAlbumSortOrder(order.name)

        override val appThemeNameFlow: Flow<String> = appPreferences.appTheme

        override suspend fun updateAppThemeName(name: String) = appPreferences.setAppTheme(name)
    }

private fun String.toSortOrder(): SortOrder = runCatching { SortOrder.valueOf(this) }.getOrDefault(SortOrder.DESC)

private fun String.toAspectRatio(): AspectRatio = runCatching { AspectRatio.valueOf(this) }.getOrDefault(AspectRatio.RATIO_4_3)

private fun String.toImageQualityPreset(): ImageQualityPreset =
    runCatching { ImageQualityPreset.valueOf(this) }.getOrDefault(ImageQualityPreset.DEFAULT)
