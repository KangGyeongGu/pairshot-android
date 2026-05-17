package com.pairshot.core.model

enum class LabelPosition {
    TOP,
    BOTTOM,
    ;

    companion object {
        val DEFAULT: LabelPosition = BOTTOM

        fun fromName(name: String?): LabelPosition = entries.firstOrNull { it.name == name } ?: DEFAULT
    }
}
