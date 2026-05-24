package com.pairshot.core.rendering.internal

import com.pairshot.core.model.LabelAnchor

internal enum class HorizontalAlignment { LEFT, CENTER, RIGHT }

internal enum class VerticalAlignment { TOP, MIDDLE, BOTTOM }

internal fun LabelAnchor.horizontalAlignment(): HorizontalAlignment =
    when (this) {
        LabelAnchor.TOP_LEFT, LabelAnchor.MIDDLE_LEFT, LabelAnchor.BOTTOM_LEFT -> HorizontalAlignment.LEFT
        LabelAnchor.TOP_CENTER, LabelAnchor.MIDDLE_CENTER, LabelAnchor.BOTTOM_CENTER -> HorizontalAlignment.CENTER
        LabelAnchor.TOP_RIGHT, LabelAnchor.MIDDLE_RIGHT, LabelAnchor.BOTTOM_RIGHT -> HorizontalAlignment.RIGHT
    }

internal fun LabelAnchor.verticalAlignment(): VerticalAlignment =
    when (this) {
        LabelAnchor.TOP_LEFT, LabelAnchor.TOP_CENTER, LabelAnchor.TOP_RIGHT -> VerticalAlignment.TOP
        LabelAnchor.MIDDLE_LEFT, LabelAnchor.MIDDLE_CENTER, LabelAnchor.MIDDLE_RIGHT -> VerticalAlignment.MIDDLE
        LabelAnchor.BOTTOM_LEFT, LabelAnchor.BOTTOM_CENTER, LabelAnchor.BOTTOM_RIGHT -> VerticalAlignment.BOTTOM
    }

internal fun LabelAnchor.toBorderVerticalAlignment(): VerticalAlignment =
    when (verticalAlignment()) {
        VerticalAlignment.TOP -> VerticalAlignment.TOP
        VerticalAlignment.MIDDLE -> VerticalAlignment.BOTTOM
        VerticalAlignment.BOTTOM -> VerticalAlignment.BOTTOM
    }
