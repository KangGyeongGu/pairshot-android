package com.pairshot.core.model

enum class FlashMode {
    OFF,
    AUTO,
    ON,
    TORCH,
    ;

    companion object {
        val DEFAULT: FlashMode = OFF

        fun fromName(name: String?): FlashMode = entries.firstOrNull { it.name == name } ?: DEFAULT
    }
}
