package com.pairshot.core.model

enum class CombineLayout {
    HORIZONTAL,
    VERTICAL,
    ;

    companion object {
        val DEFAULT: CombineLayout = HORIZONTAL

        fun fromName(name: String?): CombineLayout = entries.firstOrNull { it.name == name } ?: DEFAULT
    }
}
