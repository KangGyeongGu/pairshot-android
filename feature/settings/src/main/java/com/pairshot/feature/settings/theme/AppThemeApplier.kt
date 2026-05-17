package com.pairshot.feature.settings.theme

import androidx.appcompat.app.AppCompatDelegate
import com.pairshot.core.model.AppTheme

fun AppTheme.apply() {
    AppCompatDelegate.setDefaultNightMode(nightMode)
}

private val AppTheme.nightMode: Int
    get() =
        when (this) {
            AppTheme.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            AppTheme.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            AppTheme.DARK -> AppCompatDelegate.MODE_NIGHT_YES
        }
