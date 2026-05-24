package com.pairshot.feature.settings.section

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.pairshot.core.ui.component.SettingsCard
import com.pairshot.core.ui.component.SettingsDivider
import com.pairshot.core.ui.component.SettingsItem
import com.pairshot.feature.settings.R
import com.pairshot.feature.settings.viewmodel.formatBytes

@Composable
internal fun SettingsStorageInfoSection(
    usedStorageBytes: Long,
    cacheBytes: Long,
    appVersion: String,
    onClearCacheClick: () -> Unit,
    onLicenseClick: () -> Unit,
    onPrivacyPolicyClick: () -> Unit,
) {
    SettingsCard {
        SettingsItem(
            label = stringResource(R.string.settings_item_photo_storage),
            trailing = formatBytes(usedStorageBytes),
        )
        SettingsDivider()
        SettingsItem(
            label = stringResource(R.string.settings_item_cache),
            trailing = formatBytes(cacheBytes),
            onClick = onClearCacheClick,
        )
        SettingsDivider()
        SettingsItem(
            label = stringResource(R.string.settings_item_app_version),
            trailing = appVersion,
        )
        SettingsDivider()
        SettingsItem(
            label = stringResource(R.string.settings_item_license),
            onClick = onLicenseClick,
        )
        SettingsDivider()
        SettingsItem(
            label = stringResource(R.string.settings_item_privacy_policy),
            onClick = onPrivacyPolicyClick,
        )
    }
}
