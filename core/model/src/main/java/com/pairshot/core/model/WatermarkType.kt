package com.pairshot.core.model

enum class WatermarkType {
    TEXT,
    LOGO,
    ;

    companion object {
        val DEFAULT: WatermarkType = TEXT

        fun fromName(name: String?): WatermarkType = entries.firstOrNull { it.name == name } ?: DEFAULT
    }
}
