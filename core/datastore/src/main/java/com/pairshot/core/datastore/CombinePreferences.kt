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
import com.pairshot.core.model.CombineConfig
import com.pairshot.core.model.CombineLayout
import com.pairshot.core.model.LabelAnchor
import com.pairshot.core.model.LabelPosition
import com.pairshot.core.model.LabelPositionMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.combineDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "combine_settings",
)

@Singleton
class CombinePreferences
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        private val defaultConfig = CombineConfig()

        private object Keys {
            val LAYOUT = stringPreferencesKey("combine_layout")
            val BORDER_ENABLED = booleanPreferencesKey("combine_border_enabled")
            val BORDER_THICKNESS = intPreferencesKey("combine_border_thickness_dp")
            val BORDER_COLOR_ARGB = intPreferencesKey("combine_border_color_argb")
            val LABEL_ENABLED = booleanPreferencesKey("combine_label_enabled")
            val BEFORE_LABEL = stringPreferencesKey("combine_before_label")
            val AFTER_LABEL = stringPreferencesKey("combine_after_label")
            val LABEL_POSITION = stringPreferencesKey("combine_label_position")
            val LABEL_SIZE_RATIO = floatPreferencesKey("combine_label_size_ratio")
            val LABEL_TEXT_COLOR_ARGB = intPreferencesKey("combine_label_text_color_argb")
            val LABEL_BG_COLOR_ARGB = intPreferencesKey("combine_label_bg_color_argb")
            val LABEL_BG_ALPHA = floatPreferencesKey("combine_label_bg_alpha")
            val LABEL_BG_ENABLED = booleanPreferencesKey("label_bg_enabled")
            val LABEL_BG_MATCHES_BORDER = booleanPreferencesKey("label_bg_matches_border")
            val LABEL_POSITION_MODE = stringPreferencesKey("label_position_mode")
            val BEFORE_LABEL_ANCHOR = stringPreferencesKey("before_label_anchor")
            val AFTER_LABEL_ANCHOR = stringPreferencesKey("after_label_anchor")
            val LABEL_BG_CORNER = intPreferencesKey("label_bg_corner_dp")
        }

        val configFlow: Flow<CombineConfig> =
            context.combineDataStore.data.map { prefs ->
                CombineConfig(
                    layout = CombineLayout.fromName(prefs[Keys.LAYOUT]),
                    borderEnabled = prefs[Keys.BORDER_ENABLED] ?: defaultConfig.borderEnabled,
                    borderThicknessDp = prefs[Keys.BORDER_THICKNESS] ?: defaultConfig.borderThicknessDp,
                    borderColorArgb = prefs[Keys.BORDER_COLOR_ARGB] ?: defaultConfig.borderColorArgb,
                    labelEnabled = prefs[Keys.LABEL_ENABLED] ?: defaultConfig.labelEnabled,
                    beforeLabel = prefs[Keys.BEFORE_LABEL] ?: defaultConfig.beforeLabel,
                    afterLabel = prefs[Keys.AFTER_LABEL] ?: defaultConfig.afterLabel,
                    labelPosition = LabelPosition.fromName(prefs[Keys.LABEL_POSITION]),
                    labelSizeRatio = prefs[Keys.LABEL_SIZE_RATIO] ?: defaultConfig.labelSizeRatio,
                    labelTextColorArgb = prefs[Keys.LABEL_TEXT_COLOR_ARGB] ?: defaultConfig.labelTextColorArgb,
                    labelBgColorArgb = prefs[Keys.LABEL_BG_COLOR_ARGB] ?: defaultConfig.labelBgColorArgb,
                    labelBgAlpha = prefs[Keys.LABEL_BG_ALPHA] ?: defaultConfig.labelBgAlpha,
                    labelBgEnabled = prefs[Keys.LABEL_BG_ENABLED] ?: defaultConfig.labelBgEnabled,
                    labelBgMatchesBorder = prefs[Keys.LABEL_BG_MATCHES_BORDER] ?: defaultConfig.labelBgMatchesBorder,
                    labelPositionMode = LabelPositionMode.fromName(prefs[Keys.LABEL_POSITION_MODE]),
                    beforeLabelAnchor = LabelAnchor.fromName(prefs[Keys.BEFORE_LABEL_ANCHOR]),
                    afterLabelAnchor = LabelAnchor.fromName(prefs[Keys.AFTER_LABEL_ANCHOR]),
                    labelBgCornerDp = prefs[Keys.LABEL_BG_CORNER] ?: defaultConfig.labelBgCornerDp,
                )
            }

        suspend fun saveConfig(config: CombineConfig) {
            context.combineDataStore.edit { prefs ->
                prefs[Keys.LAYOUT] = config.layout.name
                prefs[Keys.BORDER_ENABLED] = config.borderEnabled
                prefs[Keys.BORDER_THICKNESS] = config.borderThicknessDp
                prefs[Keys.BORDER_COLOR_ARGB] = config.borderColorArgb
                prefs[Keys.LABEL_ENABLED] = config.labelEnabled
                prefs[Keys.BEFORE_LABEL] = config.beforeLabel
                prefs[Keys.AFTER_LABEL] = config.afterLabel
                prefs[Keys.LABEL_POSITION] = config.labelPosition.name
                prefs[Keys.LABEL_SIZE_RATIO] = config.labelSizeRatio
                prefs[Keys.LABEL_TEXT_COLOR_ARGB] = config.labelTextColorArgb
                prefs[Keys.LABEL_BG_COLOR_ARGB] = config.labelBgColorArgb
                prefs[Keys.LABEL_BG_ALPHA] = config.labelBgAlpha
                prefs[Keys.LABEL_BG_ENABLED] = config.labelBgEnabled
                prefs[Keys.LABEL_BG_MATCHES_BORDER] = config.labelBgMatchesBorder
                prefs[Keys.LABEL_POSITION_MODE] = config.labelPositionMode.name
                prefs[Keys.BEFORE_LABEL_ANCHOR] = config.beforeLabelAnchor.name
                prefs[Keys.AFTER_LABEL_ANCHOR] = config.afterLabelAnchor.name
                prefs[Keys.LABEL_BG_CORNER] = config.labelBgCornerDp
            }
        }
    }
