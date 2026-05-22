package com.pairshot.core.model

enum class LabelPlacement {
    INSIDE_IMAGE,
    INSIDE_BORDER,
    ;

    companion object {
        val DEFAULT: LabelPlacement = INSIDE_IMAGE

        fun fromName(name: String?): LabelPlacement = entries.firstOrNull { it.name == name } ?: DEFAULT
    }
}

fun LabelAnchor.coercedForBorderPlacement(): LabelAnchor =
    when (this) {
        LabelAnchor.MIDDLE_LEFT -> LabelAnchor.BOTTOM_LEFT
        LabelAnchor.MIDDLE_CENTER -> LabelAnchor.BOTTOM_CENTER
        LabelAnchor.MIDDLE_RIGHT -> LabelAnchor.BOTTOM_RIGHT
        else -> this
    }

fun CombineConfig.withLabelPlacement(placement: LabelPlacement): CombineConfig =
    when (placement) {
        LabelPlacement.INSIDE_BORDER -> {
            copy(
                labelPlacement = placement,
                borderEnabled = true,
                beforeLabelAnchor = beforeLabelAnchor.coercedForBorderPlacement(),
                afterLabelAnchor = afterLabelAnchor.coercedForBorderPlacement(),
            )
        }

        LabelPlacement.INSIDE_IMAGE -> {
            copy(labelPlacement = placement)
        }
    }
