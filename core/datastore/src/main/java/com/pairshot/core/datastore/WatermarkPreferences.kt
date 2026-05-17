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
import com.pairshot.core.model.LogoPosition
import com.pairshot.core.model.WatermarkConfig
import com.pairshot.core.model.WatermarkType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.watermarkDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "watermark_settings",
)

@Singleton
class WatermarkPreferences
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        private val defaultConfig = WatermarkConfig()

        private object Keys {
            val ENABLED = booleanPreferencesKey("enabled")
            val TYPE = stringPreferencesKey("type")
            val TEXT = stringPreferencesKey("text")
            val ALPHA = floatPreferencesKey("alpha")
            val DIAGONAL_COUNT = intPreferencesKey("diagonal_count")
            val REPEAT_DENSITY = floatPreferencesKey("repeat_density")
            val TEXT_SIZE_RATIO = floatPreferencesKey("text_size_ratio")
            val LOGO_PATH = stringPreferencesKey("logo_path")
            val LOGO_POSITION = stringPreferencesKey("logo_position")
            val LOGO_SIZE_RATIO = floatPreferencesKey("logo_size_ratio")
            val LOGO_ALPHA = floatPreferencesKey("logo_alpha")
        }

        val watermarkConfigFlow: Flow<WatermarkConfig> =
            context.watermarkDataStore.data.map { prefs ->
                WatermarkConfig(
                    enabled = prefs[Keys.ENABLED] ?: defaultConfig.enabled,
                    type = WatermarkType.fromName(prefs[Keys.TYPE]),
                    text = prefs[Keys.TEXT] ?: defaultConfig.text,
                    alpha = prefs[Keys.ALPHA] ?: defaultConfig.alpha,
                    diagonalCount = prefs[Keys.DIAGONAL_COUNT] ?: defaultConfig.diagonalCount,
                    repeatDensity = prefs[Keys.REPEAT_DENSITY] ?: defaultConfig.repeatDensity,
                    textSizeRatio = prefs[Keys.TEXT_SIZE_RATIO] ?: defaultConfig.textSizeRatio,
                    logoPath = prefs[Keys.LOGO_PATH] ?: defaultConfig.logoPath,
                    logoPosition = LogoPosition.fromName(prefs[Keys.LOGO_POSITION]),
                    logoSizeRatio = prefs[Keys.LOGO_SIZE_RATIO] ?: defaultConfig.logoSizeRatio,
                    logoAlpha = prefs[Keys.LOGO_ALPHA] ?: defaultConfig.logoAlpha,
                )
            }

        suspend fun saveConfig(config: WatermarkConfig) {
            context.watermarkDataStore.edit { prefs ->
                prefs[Keys.ENABLED] = config.enabled
                prefs[Keys.TYPE] = config.type.name
                prefs[Keys.TEXT] = config.text
                prefs[Keys.ALPHA] = config.alpha
                prefs[Keys.DIAGONAL_COUNT] = config.diagonalCount
                prefs[Keys.REPEAT_DENSITY] = config.repeatDensity
                prefs[Keys.TEXT_SIZE_RATIO] = config.textSizeRatio
                prefs[Keys.LOGO_PATH] = config.logoPath
                prefs[Keys.LOGO_POSITION] = config.logoPosition.name
                prefs[Keys.LOGO_SIZE_RATIO] = config.logoSizeRatio
                prefs[Keys.LOGO_ALPHA] = config.logoAlpha
            }
        }
    }
