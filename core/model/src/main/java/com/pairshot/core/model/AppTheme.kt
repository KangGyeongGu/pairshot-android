package com.pairshot.core.model

enum class AppTheme {
    SYSTEM,
    LIGHT,
    DARK,
    ;

    companion object {
        val DEFAULT: AppTheme = SYSTEM

        fun fromName(name: String?): AppTheme = entries.firstOrNull { it.name == name } ?: DEFAULT
    }
}
