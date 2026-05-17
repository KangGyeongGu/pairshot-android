package com.pairshot.core.model

enum class LogoPosition {
    TOP_LEFT,
    TOP_CENTER,
    TOP_RIGHT,
    CENTER_LEFT,
    CENTER,
    CENTER_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_CENTER,
    BOTTOM_RIGHT,
    ;

    companion object {
        val DEFAULT: LogoPosition = CENTER

        fun fromName(name: String?): LogoPosition = entries.firstOrNull { it.name == name } ?: DEFAULT
    }
}
