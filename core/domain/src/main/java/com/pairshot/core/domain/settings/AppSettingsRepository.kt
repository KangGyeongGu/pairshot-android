package com.pairshot.core.domain.settings

import com.pairshot.core.model.AppSettings
import com.pairshot.core.model.AspectRatio
import com.pairshot.core.model.ExportPreset
import com.pairshot.core.model.FlashMode
import com.pairshot.core.model.ImageQualityPreset
import com.pairshot.core.model.SortOrder
import kotlinx.coroutines.flow.Flow

interface AppSettingsRepository {
    val settingsFlow: Flow<AppSettings>

    suspend fun getCurrent(): AppSettings

    suspend fun updateImageQuality(preset: ImageQualityPreset)

    suspend fun updateFileNamePrefix(prefix: String)

    suspend fun updateOverlayEnabled(enabled: Boolean)

    suspend fun updateOverlayAlpha(alpha: Float)

    suspend fun updateCameraGridEnabled(enabled: Boolean)

    suspend fun updateCameraLevelEnabled(enabled: Boolean)

    suspend fun updateCameraFlashMode(mode: FlashMode)

    suspend fun updateCameraNightMode(enabled: Boolean)

    suspend fun updateCameraHdr(enabled: Boolean)

    suspend fun updateCameraAspectRatio(ratio: AspectRatio)

    suspend fun getLastExportPreset(): ExportPreset

    suspend fun saveLastExportPreset(preset: ExportPreset)

    val homeSortOrderFlow: Flow<SortOrder>

    val albumSortOrderFlow: Flow<SortOrder>

    suspend fun updateHomeSortOrder(order: SortOrder)

    suspend fun updateAlbumSortOrder(order: SortOrder)

    val appThemeNameFlow: Flow<String>

    suspend fun updateAppThemeName(name: String)

    val appTextScaleNameFlow: Flow<String>

    suspend fun updateAppTextScaleName(name: String)
}
