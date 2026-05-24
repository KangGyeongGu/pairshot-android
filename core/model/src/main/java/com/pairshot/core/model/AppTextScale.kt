package com.pairshot.core.model

enum class AppTextScale(
    val factor: Float,
) {
    SMALL(factor = 0.9f),
    NORMAL(factor = 1.0f),
    LARGE(factor = 1.15f),
    EXTRA_LARGE(factor = 1.3f),
    ;

    companion object {
        val DEFAULT: AppTextScale = NORMAL

        fun fromName(name: String?): AppTextScale = entries.firstOrNull { it.name == name } ?: DEFAULT
    }
}
