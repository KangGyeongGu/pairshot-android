package com.pairshot.core.model

enum class AspectRatio {
    RATIO_4_3,
    RATIO_16_9,
    RATIO_1_1,
    ;

    companion object {
        val DEFAULT: AspectRatio = RATIO_4_3

        fun fromName(name: String?): AspectRatio = entries.firstOrNull { it.name == name } ?: DEFAULT
    }
}
