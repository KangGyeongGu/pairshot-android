package com.pairshot.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.pairshot.core.model.AppSettings
import com.pairshot.core.model.AppTextScale
import com.pairshot.core.model.AppTheme
import com.pairshot.core.model.ExportPreset
import com.pairshot.core.model.ImageQualityPreset
import com.pairshot.core.model.SortOrder
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
    private val defaultSettings = AppSettings()
    private val defaultExportPreset = ExportPreset()

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
        val APP_TEXT_SCALE = stringPreferencesKey("app_text_scale")
        val ONBOARDING_PAYWALL_SHOWN = booleanPreferencesKey("onboarding_paywall_shown")
        val TUTORIAL_COMPLETED = booleanPreferencesKey("tutorial_completed")
        val EXPORT_SETTINGS_TUTORIAL_COMPLETED = booleanPreferencesKey("export_settings_tutorial_completed")
        val CURRENT_TUTORIAL_STEP = stringPreferencesKey("current_tutorial_step")
        val CURRENT_TUTORIAL_SECTION = stringPreferencesKey("current_tutorial_section")
        val TUTORIAL_SANDBOX_PAIR_IDS = stringSetPreferencesKey("tutorial_sandbox_pair_ids")
        val TUTORIAL_SANDBOX_TEMP_FILES = stringSetPreferencesKey("tutorial_sandbox_temp_files")
        val EXPORT_PRESET_SLOTS_JSON = stringPreferencesKey("export_preset_slots_json")
        val ACTIVE_EXPORT_PRESET_ID = stringPreferencesKey("active_export_preset_id")
        val FIRST_SAVE_REVIEW_REQUESTED = booleanPreferencesKey("first_save_review_requested")
    }

    val imageQuality: Flow<String> =
        context.appDataStore.data.map { prefs ->
            prefs[Keys.IMAGE_QUALITY]
                ?: prefs[Keys.JPEG_QUALITY]?.let { legacy ->
                    ImageQualityPreset.fromLegacyJpegQuality(legacy).name
                }
                ?: ImageQualityPreset.DEFAULT.name
        }

    val fileNamePrefix: Flow<String> =
        context.appDataStore.data.map { prefs ->
            prefs[Keys.FILE_NAME_PREFIX] ?: defaultSettings.fileNamePrefix
        }

    val overlayEnabled: Flow<Boolean> =
        context.appDataStore.data.map { prefs ->
            prefs[Keys.OVERLAY_ENABLED] ?: defaultSettings.overlayEnabled
        }

    val overlayAlpha: Flow<Float> =
        context.appDataStore.data.map { prefs ->
            prefs[Keys.OVERLAY_ALPHA] ?: defaultSettings.defaultOverlayAlpha
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
            prefs[Keys.CAMERA_GRID_ENABLED] ?: defaultSettings.cameraGridEnabled
        }

    val cameraLevelEnabled: Flow<Boolean> =
        context.appDataStore.data.map { prefs ->
            prefs[Keys.CAMERA_LEVEL_ENABLED] ?: defaultSettings.cameraLevelEnabled
        }

    val cameraFlashMode: Flow<String> =
        context.appDataStore.data.map { prefs ->
            prefs[Keys.CAMERA_FLASH_MODE] ?: defaultSettings.cameraFlashMode.name
        }

    val cameraNightMode: Flow<Boolean> =
        context.appDataStore.data.map { prefs ->
            prefs[Keys.CAMERA_NIGHT_MODE] ?: defaultSettings.cameraNightModeEnabled
        }

    val cameraHdr: Flow<Boolean> =
        context.appDataStore.data.map { prefs ->
            prefs[Keys.CAMERA_HDR] ?: defaultSettings.cameraHdrEnabled
        }

    val cameraAspectRatio: Flow<String> =
        context.appDataStore.data.map { prefs ->
            prefs[Keys.CAMERA_ASPECT_RATIO] ?: defaultSettings.cameraAspectRatio.name
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
            prefs[Keys.EXPORT_FORMAT] ?: defaultExportPreset.format.name
        }

    val exportIncludeBefore: Flow<Boolean> =
        context.appDataStore.data.map { prefs ->
            prefs[Keys.EXPORT_INCLUDE_BEFORE] ?: defaultExportPreset.includeBefore
        }

    val exportIncludeAfter: Flow<Boolean> =
        context.appDataStore.data.map { prefs ->
            prefs[Keys.EXPORT_INCLUDE_AFTER] ?: defaultExportPreset.includeAfter
        }

    val exportIncludeCombined: Flow<Boolean> =
        context.appDataStore.data.map { prefs ->
            prefs[Keys.EXPORT_INCLUDE_COMBINED] ?: defaultExportPreset.includeCombined
        }

    val exportApplyCombineConfig: Flow<Boolean> =
        context.appDataStore.data.map { prefs ->
            prefs[Keys.EXPORT_APPLY_COMBINE_CONFIG] ?: defaultExportPreset.applyCombineConfig
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
            prefs[Keys.HOME_SORT_ORDER] ?: SortOrder.DEFAULT.name
        }

    val albumSortOrder: Flow<String> =
        context.appDataStore.data.map { prefs ->
            prefs[Keys.ALBUM_SORT_ORDER] ?: SortOrder.DEFAULT.name
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
            prefs[Keys.APP_THEME] ?: AppTheme.DEFAULT.name
        }

    suspend fun setAppTheme(theme: String) {
        context.appDataStore.edit { prefs ->
            prefs[Keys.APP_THEME] = theme
        }
    }

    val appTextScale: Flow<String> =
        context.appDataStore.data.map { prefs ->
            prefs[Keys.APP_TEXT_SCALE] ?: AppTextScale.DEFAULT.name
        }

    suspend fun setAppTextScale(scale: String) {
        context.appDataStore.edit { prefs ->
            prefs[Keys.APP_TEXT_SCALE] = scale
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

    val tutorialCompleted: Flow<Boolean> =
        context.appDataStore.data.map { prefs ->
            prefs[Keys.TUTORIAL_COMPLETED] ?: false
        }

    suspend fun setTutorialCompleted(completed: Boolean) {
        context.appDataStore.edit { prefs ->
            prefs[Keys.TUTORIAL_COMPLETED] = completed
        }
    }

    val exportSettingsTutorialCompleted: Flow<Boolean> =
        context.appDataStore.data.map { prefs ->
            prefs[Keys.EXPORT_SETTINGS_TUTORIAL_COMPLETED] ?: false
        }

    suspend fun setExportSettingsTutorialCompleted(completed: Boolean) {
        context.appDataStore.edit { prefs ->
            prefs[Keys.EXPORT_SETTINGS_TUTORIAL_COMPLETED] = completed
        }
    }

    val currentTutorialStep: Flow<String?> =
        context.appDataStore.data.map { prefs -> prefs[Keys.CURRENT_TUTORIAL_STEP] }

    val currentTutorialSection: Flow<String?> =
        context.appDataStore.data.map { prefs -> prefs[Keys.CURRENT_TUTORIAL_SECTION] }

    suspend fun setCurrentTutorialStep(stepName: String?) {
        context.appDataStore.edit { prefs ->
            if (stepName == null) {
                prefs.remove(
                    Keys.CURRENT_TUTORIAL_STEP
                )
            } else {
                prefs[Keys.CURRENT_TUTORIAL_STEP] = stepName
            }
        }
    }

    suspend fun setCurrentTutorialSection(sectionName: String?) {
        context.appDataStore.edit { prefs ->
            if (sectionName == null) {
                prefs.remove(Keys.CURRENT_TUTORIAL_SECTION)
            } else {
                prefs[Keys.CURRENT_TUTORIAL_SECTION] = sectionName
            }
        }
    }

    val tutorialSandboxPairIds: Flow<Set<Long>> =
        context.appDataStore.data.map { prefs ->
            prefs[Keys.TUTORIAL_SANDBOX_PAIR_IDS]?.mapNotNull { it.toLongOrNull() }?.toSet().orEmpty()
        }

    suspend fun setTutorialSandboxPairIds(ids: Set<Long>) {
        context.appDataStore.edit { prefs ->
            prefs[Keys.TUTORIAL_SANDBOX_PAIR_IDS] = ids.map { it.toString() }.toSet()
        }
    }

    val tutorialSandboxTempFiles: Flow<Set<String>> =
        context.appDataStore.data.map { prefs ->
            prefs[Keys.TUTORIAL_SANDBOX_TEMP_FILES].orEmpty()
        }

    suspend fun setTutorialSandboxTempFiles(paths: Set<String>) {
        context.appDataStore.edit { prefs ->
            prefs[Keys.TUTORIAL_SANDBOX_TEMP_FILES] = paths
        }
    }

    val exportPresetSlotsJson: Flow<String?> =
        context.appDataStore.data.map { prefs -> prefs[Keys.EXPORT_PRESET_SLOTS_JSON] }

    suspend fun setExportPresetSlotsJson(json: String) {
        context.appDataStore.edit { prefs ->
            prefs[Keys.EXPORT_PRESET_SLOTS_JSON] = json
        }
    }

    val activeExportPresetId: Flow<String?> =
        context.appDataStore.data.map { prefs -> prefs[Keys.ACTIVE_EXPORT_PRESET_ID] }

    suspend fun setActiveExportPresetId(id: String) {
        context.appDataStore.edit { prefs ->
            prefs[Keys.ACTIVE_EXPORT_PRESET_ID] = id
        }
    }

    val firstSaveReviewRequested: Flow<Boolean> =
        context.appDataStore.data.map { prefs -> prefs[Keys.FIRST_SAVE_REVIEW_REQUESTED] ?: false }

    suspend fun setFirstSaveReviewRequested(requested: Boolean) {
        context.appDataStore.edit { prefs ->
            prefs[Keys.FIRST_SAVE_REVIEW_REQUESTED] = requested
        }
    }
}
