package com.pairshot.core.rendering.internal

import android.graphics.Bitmap
import com.pairshot.core.model.CombineLayout

internal data class LayoutPlacement(
    val canvasWidth: Int,
    val canvasHeight: Int,
    val beforeLeft: Int,
    val beforeTop: Int,
    val afterLeft: Int,
    val afterTop: Int,
)

internal fun calculatePlacement(
    wmBefore: Bitmap,
    wmAfter: Bitmap,
    layout: CombineLayout,
    insets: BorderInsets,
): LayoutPlacement =
    when (layout) {
        CombineLayout.HORIZONTAL -> {
            LayoutPlacement(
                canvasWidth = wmBefore.width + wmAfter.width + insets.left + insets.right + insets.centerGap,
                canvasHeight = maxOf(wmBefore.height, wmAfter.height) + insets.top + insets.bottom,
                beforeLeft = insets.left,
                beforeTop = insets.top,
                afterLeft = insets.left + wmBefore.width + insets.centerGap,
                afterTop = insets.top,
            )
        }

        CombineLayout.VERTICAL -> {
            LayoutPlacement(
                canvasWidth = maxOf(wmBefore.width, wmAfter.width) + insets.left + insets.right,
                canvasHeight = wmBefore.height + wmAfter.height + insets.top + insets.bottom + insets.centerGap,
                beforeLeft = insets.left,
                beforeTop = insets.top,
                afterLeft = insets.left,
                afterTop = insets.top + wmBefore.height + insets.centerGap,
            )
        }
    }

internal fun createCanvasBitmap(placement: LayoutPlacement): Bitmap =
    Bitmap.createBitmap(
        placement.canvasWidth.coerceAtLeast(1),
        placement.canvasHeight.coerceAtLeast(1),
        Bitmap.Config.ARGB_8888,
    )
