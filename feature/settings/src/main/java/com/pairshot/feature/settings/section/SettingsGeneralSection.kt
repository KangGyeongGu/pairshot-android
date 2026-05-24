package com.pairshot.feature.settings.section

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.pairshot.core.model.AppTextScale
import com.pairshot.core.model.AppTheme
import com.pairshot.core.ui.component.SettingsCard
import com.pairshot.core.ui.component.SettingsDivider
import com.pairshot.core.ui.component.SettingsItem
import com.pairshot.feature.settings.R
import com.pairshot.feature.settings.locale.AppLocale

@Composable
internal fun SettingsGeneralSection(
    currentLocale: AppLocale,
    currentTheme: AppTheme,
    currentTextScale: AppTextScale,
    showAdsConsent: Boolean,
    onLanguageClick: () -> Unit,
    onThemeClick: () -> Unit,
    onTextScaleClick: () -> Unit,
    onAdsConsentClick: () -> Unit,
) {
    SettingsCard {
        val languageLabel =
            when (currentLocale) {
                AppLocale.SYSTEM -> stringResource(R.string.settings_language_system)
                AppLocale.KOREAN -> stringResource(R.string.settings_language_korean)
                AppLocale.ENGLISH -> stringResource(R.string.settings_language_english)
            }
        SettingsItem(
            label = stringResource(R.string.settings_item_language),
            trailing = languageLabel,
            onClick = onLanguageClick,
        )
        SettingsDivider()
        val themeLabel =
            when (currentTheme) {
                AppTheme.SYSTEM -> stringResource(R.string.settings_theme_system)
                AppTheme.LIGHT -> stringResource(R.string.settings_theme_light)
                AppTheme.DARK -> stringResource(R.string.settings_theme_dark)
            }
        SettingsItem(
            label = stringResource(R.string.settings_item_theme),
            trailing = themeLabel,
            onClick = onThemeClick,
        )
        SettingsDivider()
        val textScaleLabel =
            when (currentTextScale) {
                AppTextScale.SMALL -> stringResource(R.string.settings_text_scale_small)
                AppTextScale.NORMAL -> stringResource(R.string.settings_text_scale_normal)
                AppTextScale.LARGE -> stringResource(R.string.settings_text_scale_large)
                AppTextScale.EXTRA_LARGE -> stringResource(R.string.settings_text_scale_extra_large)
            }
        SettingsItem(
            label = stringResource(R.string.settings_item_text_scale),
            trailing = textScaleLabel,
            onClick = onTextScaleClick,
        )
        if (showAdsConsent) {
            SettingsDivider()
            SettingsItem(
                label = stringResource(R.string.settings_item_ads_consent),
                onClick = onAdsConsentClick,
            )
        }
    }
}
