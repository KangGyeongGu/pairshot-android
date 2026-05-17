package com.pairshot.core.model

enum class LabelPositionMode {
    FULL_WIDTH,
    FREE,
    ;

    companion object {
        val DEFAULT: LabelPositionMode = FREE

        fun fromName(name: String?): LabelPositionMode = entries.firstOrNull { it.name == name } ?: DEFAULT
    }
}
