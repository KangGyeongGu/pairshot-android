package com.pairshot.core.model

enum class SortOrder {
    ASC,
    DESC,
    ;

    companion object {
        val DEFAULT: SortOrder = DESC

        fun fromName(name: String?): SortOrder = entries.firstOrNull { it.name == name } ?: DEFAULT
    }
}
