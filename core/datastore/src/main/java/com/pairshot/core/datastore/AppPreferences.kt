package com.pairshot.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.appDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "app_settings",
)

@Singleton
class AppPreferences
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        private companion object {
            const val DEFAULT_OVERLAY_ALPHA = 0.35f
            const val LEGACY_JPEG_LOW_THRESHOLD = 80
            const val LEGACY_JPEG_BEST_THRESHOLD = 93
        }

        private object Keys {
            val JPEG_QUALITY = intPreferencesKey("jpeg_quality")
            val IMAGE_QUALITY = stringPreferencesKey("image_quality")
            val FILE_NAME_PREFIX = stringPreferencesKey("file_name_prefix")
            val OVERLAY_ENABLED = booleanPreferencesKey("overlay_enabled")
            val OVERLAY_ALPHA = floatPreferencesKey("overlay_alpha")
            val CAMERA_GRID_ENABLED = booleanPreferencesKey("camera_grid_enabled")
            val CAMERA_LEVEL_ENABLED = booleanPreferencesKey("camera_level_enabled")
            val CAMERA_FLASH_MODE = stringPreferencesKey("camera_flash_mode")
            val CAMERA_NIGHT_MODE = booleanPreferencesKey("camera_night_mode")
            val CAMERA_HDR = booleanPreferencesKey("camera_hdr")
            val CAMERA_ASPECT_RATIO = stringPreferencesKey("camera_aspect_ratio")
            val EXPORT_FORMAT = stringPreferencesKey("export_format")
            val EXPORT_INCLUDE_BEFORE = booleanPreferencesKey("export_include_before")
            val EXPORT_INCLUDE_AFTER = booleanPreferencesKey("export_include_after")
            val EXPORT_INCLUDE_COMBINED = booleanPreferencesKey("export_include_combined")
            val EXPORT_APPLY_COMBINE_CONFIG = booleanPreferencesKey("export_apply_combine_config")
            val HOME_SORT_ORDER = stringPreferencesKey("home_sort_order")
            val ALBUM_SORT_ORDER = stringPreferencesKey("album_sort_order")
            val APP_THEME = stringPreferencesKey("app_theme")
            val ONBOARDING_PAYWALL_SHOWN = booleanPreferencesKey("onboarding_paywall_shown")
        }

        val imageQuality: Flow<String> =
            context.appDataStore.data.map { prefs ->
                prefs[Keys.IMAGE_QUALITY]
                    ?: prefs[Keys.JPEG_QUALITY]?.let { legacy ->
                        when {
                            legacy <= LEGACY_JPEG_LOW_THRESHOLD -> "LOW"
                            legacy >= LEGACY_JPEG_BEST_THRESHOLD -> "BEST"
                            else -> "HIGH"
                        }
                    }
                    ?: "HIGH"
            }

        val fileNamePrefix: Flow<String> =
            context.appDataStore.data.map { prefs ->
                prefs[Keys.FILE_NAME_PREFIX] ?: "PAIRSHOT"
            }

        val overlayEnabled: Flow<Boolean> =
            context.appDataStore.data.map { prefs ->
                prefs[Keys.OVERLAY_ENABLED] ?: true
            }

        val overlayAlpha: Flow<Float> =
            context.appDataStore.data.map { prefs ->
                prefs[Keys.OVERLAY_ALPHA] ?: DEFAULT_OVERLAY_ALPHA
            }

        suspend fun setImageQuality(preset: String) {
            context.appDataStore.edit { prefs ->
                prefs[Keys.IMAGE_QUALITY] = preset
                prefs.remove(Keys.JPEG_QUALITY)
            }
        }

        suspend fun setFileNamePrefix(prefix: String) {
            context.appDataStore.edit { prefs ->
                prefs[Keys.FILE_NAME_PREFIX] = prefix
            }
        }

        suspend fun setOverlayEnabled(enabled: Boolean) {
            context.appDataStore.edit { prefs ->
                prefs[Keys.OVERLAY_ENABLED] = enabled
            }
        }

        suspend fun setOverlayAlpha(alpha: Float) {
            context.appDataStore.edit { prefs ->
                prefs[Keys.OVERLAY_ALPHA] = alpha
            }
        }

        val cameraGridEnabled: Flow<Boolean> =
            context.appDataStore.data.map { prefs ->
                prefs[Keys.CAMERA_GRID_ENABLED] ?: false
            }

        val cameraLevelEnabled: Flow<Boolean> =
            context.appDataStore.data.map { prefs ->
                prefs[Keys.CAMERA_LEVEL_ENABLED] ?: false
            }

        val cameraFlashMode: Flow<String> =
            context.appDataStore.data.map { prefs ->
                prefs[Keys.CAMERA_FLASH_MODE] ?: "OFF"
            }

        val cameraNightMode: Flow<Boolean> =
            context.appDataStore.data.map { prefs ->
                prefs[Keys.CAMERA_NIGHT_MODE] ?: false
            }

        val cameraHdr: Flow<Boolean> =
            context.appDataStore.data.map { prefs ->
                prefs[Keys.CAMERA_HDR] ?: false
            }

        val cameraAspectRatio: Flow<String> =
            context.appDataStore.data.map { prefs ->
                prefs[Keys.CAMERA_ASPECT_RATIO] ?: "RATIO_4_3"
            }

        suspend fun setCameraGridEnabled(enabled: Boolean) {
            context.appDataStore.edit { prefs ->
                prefs[Keys.CAMERA_GRID_ENABLED] = enabled
            }
        }

        suspend fun setCameraLevelEnabled(enabled: Boolean) {
            context.appDataStore.edit { prefs ->
                prefs[Keys.CAMERA_LEVEL_ENABLED] = enabled
            }
        }

        suspend fun setCameraFlashMode(mode: String) {
            context.appDataStore.edit { prefs ->
                prefs[Keys.CAMERA_FLASH_MODE] = mode
            }
        }

        suspend fun setCameraNightMode(enabled: Boolean) {
            context.appDataStore.edit { prefs ->
                prefs[Keys.CAMERA_NIGHT_MODE] = enabled
            }
        }

        suspend fun setCameraHdr(enabled: Boolean) {
            context.appDataStore.edit { prefs ->
                prefs[Keys.CAMERA_HDR] = enabled
            }
        }

        suspend fun setCameraAspectRatio(ratio: String) {
            context.appDataStore.edit { prefs ->
                prefs[Keys.CAMERA_ASPECT_RATIO] = ratio
            }
        }

        val exportFormat: Flow<String> =
            context.appDataStore.data.map { prefs ->
                prefs[Keys.EXPORT_FORMAT] ?: "INDIVIDUAL"
            }

        val exportIncludeBefore: Flow<Boolean> =
            context.appDataStore.data.map { prefs ->
                prefs[Keys.EXPORT_INCLUDE_BEFORE] ?: false
            }

        val exportIncludeAfter: Flow<Boolean> =
            context.appDataStore.data.map { prefs ->
                prefs[Keys.EXPORT_INCLUDE_AFTER] ?: false
            }

        val exportIncludeCombined: Flow<Boolean> =
            context.appDataStore.data.map { prefs ->
                prefs[Keys.EXPORT_INCLUDE_COMBINED] ?: true
            }

        val exportApplyCombineConfig: Flow<Boolean> =
            context.appDataStore.data.map { prefs ->
                prefs[Keys.EXPORT_APPLY_COMBINE_CONFIG] ?: true
            }

        suspend fun setExportFormat(format: String) {
            context.appDataStore.edit { prefs ->
                prefs[Keys.EXPORT_FORMAT] = format
            }
        }

        suspend fun setExportIncludeBefore(value: Boolean) {
            context.appDataStore.edit { prefs ->
                prefs[Keys.EXPORT_INCLUDE_BEFORE] = value
            }
        }

        suspend fun setExportIncludeAfter(value: Boolean) {
            context.appDataStore.edit { prefs ->
                prefs[Keys.EXPORT_INCLUDE_AFTER] = value
            }
        }

        suspend fun setExportIncludeCombined(value: Boolean) {
            context.appDataStore.edit { prefs ->
                prefs[Keys.EXPORT_INCLUDE_COMBINED] = value
            }
        }

        suspend fun saveExportPreset(
            format: String,
            includeBefore: Boolean,
            includeAfter: Boolean,
            includeCombined: Boolean,
            applyCombineConfig: Boolean,
        ) {
            context.appDataStore.edit { prefs ->
                prefs[Keys.EXPORT_FORMAT] = format
                prefs[Keys.EXPORT_INCLUDE_BEFORE] = includeBefore
                prefs[Keys.EXPORT_INCLUDE_AFTER] = includeAfter
                prefs[Keys.EXPORT_INCLUDE_COMBINED] = includeCombined
                prefs[Keys.EXPORT_APPLY_COMBINE_CONFIG] = applyCombineConfig
            }
        }

        val homeSortOrder: Flow<String> =
            context.appDataStore.data.map { prefs ->
                prefs[Keys.HOME_SORT_ORDER] ?: "DESC"
            }

        val albumSortOrder: Flow<String> =
            context.appDataStore.data.map { prefs ->
                prefs[Keys.ALBUM_SORT_ORDER] ?: "DESC"
            }

        suspend fun setHomeSortOrder(value: String) {
            context.appDataStore.edit { prefs ->
                prefs[Keys.HOME_SORT_ORDER] = value
            }
        }

        suspend fun setAlbumSortOrder(value: String) {
            context.appDataStore.edit { prefs ->
                prefs[Keys.ALBUM_SORT_ORDER] = value
            }
        }

        val appTheme: Flow<String> =
            context.appDataStore.data.map { prefs ->
                prefs[Keys.APP_THEME] ?: "SYSTEM"
            }

        suspend fun setAppTheme(theme: String) {
            context.appDataStore.edit { prefs ->
                prefs[Keys.APP_THEME] = theme
            }
        }

        val onboardingPaywallShown: Flow<Boolean> =
            context.appDataStore.data.map { prefs ->
                prefs[Keys.ONBOARDING_PAYWALL_SHOWN] ?: false
            }

        suspend fun setOnboardingPaywallShown(shown: Boolean) {
            context.appDataStore.edit { prefs ->
                prefs[Keys.ONBOARDING_PAYWALL_SHOWN] = shown
            }
        }
    }
