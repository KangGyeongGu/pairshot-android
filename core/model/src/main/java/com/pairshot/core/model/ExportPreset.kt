package com.pairshot.core.model

enum class ExportFormat {
    ZIP,
    INDIVIDUAL,
    ;

    companion object {
        val DEFAULT: ExportFormat = INDIVIDUAL

        fun fromName(name: String?): ExportFormat = entries.firstOrNull { it.name == name } ?: DEFAULT
    }
}

data class ExportPreset(
    val format: ExportFormat = ExportFormat.INDIVIDUAL,
    val includeBefore: Boolean = false,
    val includeAfter: Boolean = false,
    val includeCombined: Boolean = true,
    val applyCombineConfig: Boolean = true,
)
