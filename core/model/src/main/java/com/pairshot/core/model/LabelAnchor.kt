package com.pairshot.core.model

enum class LabelAnchor {
    TOP_LEFT,
    TOP_CENTER,
    TOP_RIGHT,
    MIDDLE_LEFT,
    MIDDLE_CENTER,
    MIDDLE_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_CENTER,
    BOTTOM_RIGHT,
    ;

    companion object {
        val DEFAULT: LabelAnchor = TOP_LEFT

        fun fromName(name: String?): LabelAnchor = entries.firstOrNull { it.name == name } ?: DEFAULT
    }
}
