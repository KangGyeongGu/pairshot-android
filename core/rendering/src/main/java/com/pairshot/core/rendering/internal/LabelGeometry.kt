package com.pairshot.core.rendering.internal

import android.graphics.Paint
import android.graphics.Rect
import com.pairshot.core.model.CombineConfig
import com.pairshot.core.model.CombineLayout
import com.pairshot.core.model.LabelAnchor
import com.pairshot.core.model.LabelPosition

internal const val LABEL_RECT_HEIGHT_FACTOR = 1.6f
internal const val LABEL_HPAD_FACTOR = 0.75f
internal const val LABEL_MARGIN_FACTOR = 0.4f
internal const val LABEL_MIN_FONT_PX = 10f

internal data class LabelRect(
    val left: Int,
    val top: Int,
    val width: Int,
    val height: Int,
)

internal data class BorderInsets(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
    val centerGap: Int,
) {
    companion object {
        fun uniform(px: Int): BorderInsets = BorderInsets(px, px, px, px, px)
    }
}

internal fun resolveLabelFontSize(
    combineConfig: CombineConfig,
    referenceImageDim: Int,
): Float = (referenceImageDim * combineConfig.labelSizeRatio).coerceAtLeast(LABEL_MIN_FONT_PX)

internal fun resolveBorderLabelEdgePx(
    combineConfig: CombineConfig,
    referenceImageDim: Int,
): Int {
    val fontSize = resolveLabelFontSize(combineConfig, referenceImageDim)
    val rectHeight = fontSize * LABEL_RECT_HEIGHT_FACTOR
    val verticalPadding = fontSize * LABEL_MARGIN_FACTOR * 2
    return (rectHeight + verticalPadding).toInt()
}

internal fun computeBorderRegion(
    imageRect: LabelRect,
    anchor: LabelAnchor,
    layout: CombineLayout,
    insets: BorderInsets,
    isBefore: Boolean,
): LabelRect {
    val v = anchor.toBorderVerticalAlignment()
    val imageBottom = imageRect.top + imageRect.height
    return when (layout) {
        CombineLayout.HORIZONTAL -> {
            if (v == VerticalAlignment.TOP) {
                LabelRect(left = imageRect.left, top = 0, width = imageRect.width, height = insets.top)
            } else {
                LabelRect(left = imageRect.left, top = imageBottom, width = imageRect.width, height = insets.bottom)
            }
        }

        CombineLayout.VERTICAL -> {
            when {
                isBefore && v == VerticalAlignment.TOP -> {
                    LabelRect(left = imageRect.left, top = 0, width = imageRect.width, height = insets.top)
                }

                isBefore -> {
                    LabelRect(
                        left = imageRect.left,
                        top = imageBottom,
                        width = imageRect.width,
                        height = insets.centerGap,
                    )
                }

                v == VerticalAlignment.TOP -> {
                    LabelRect(
                        left = imageRect.left,
                        top = imageRect.top - insets.centerGap,
                        width = imageRect.width,
                        height = insets.centerGap,
                    )
                }

                else -> {
                    LabelRect(left = imageRect.left, top = imageBottom, width = imageRect.width, height = insets.bottom)
                }
            }
        }
    }
}

internal fun computeSingleBorderRegion(
    imageRect: LabelRect,
    anchor: LabelAnchor,
    insets: BorderInsets,
): LabelRect {
    val imageBottom = imageRect.top + imageRect.height
    return if (anchor.toBorderVerticalAlignment() == VerticalAlignment.TOP) {
        LabelRect(left = imageRect.left, top = 0, width = imageRect.width, height = insets.top)
    } else {
        LabelRect(left = imageRect.left, top = imageBottom, width = imageRect.width, height = insets.bottom)
    }
}

internal fun fixedLabelRect(
    labelPosition: LabelPosition,
    imageRect: LabelRect,
    rectHeight: Int,
): LabelRect {
    val rectTop =
        when (labelPosition) {
            LabelPosition.TOP -> imageRect.top
            LabelPosition.BOTTOM -> imageRect.top + imageRect.height - rectHeight
        }
    return LabelRect(left = imageRect.left, top = rectTop, width = imageRect.width, height = rectHeight)
}

internal fun anchoredLabelRect(
    anchor: LabelAnchor,
    text: String,
    textPaint: Paint,
    fontSize: Float,
    imageRect: LabelRect,
    rectHeight: Int,
): LabelRect {
    val textBounds = Rect()
    textPaint.getTextBounds(text, 0, text.length, textBounds)
    val hPad = (fontSize * LABEL_HPAD_FACTOR).toInt()
    val rectWidth = (textBounds.width() + hPad * 2).coerceAtLeast(rectHeight)
    val margin = (fontSize * LABEL_MARGIN_FACTOR).toInt()

    val rectLeft = anchoredLabelLeft(anchor, imageRect.left, imageRect.width, rectWidth, margin)
    val rectTop = anchoredLabelTop(anchor, imageRect.top, imageRect.height, rectHeight, margin)
    return LabelRect(left = rectLeft, top = rectTop, width = rectWidth, height = rectHeight)
}

private fun anchoredLabelLeft(
    anchor: LabelAnchor,
    imageLeft: Int,
    imageWidth: Int,
    rectWidth: Int,
    margin: Int,
): Int =
    when (anchor.horizontalAlignment()) {
        HorizontalAlignment.LEFT -> imageLeft + margin
        HorizontalAlignment.CENTER -> imageLeft + (imageWidth - rectWidth) / 2
        HorizontalAlignment.RIGHT -> imageLeft + imageWidth - rectWidth - margin
    }

private fun anchoredLabelTop(
    anchor: LabelAnchor,
    imageTop: Int,
    imageHeight: Int,
    rectHeight: Int,
    margin: Int,
): Int =
    when (anchor.verticalAlignment()) {
        VerticalAlignment.TOP -> imageTop + margin
        VerticalAlignment.MIDDLE -> imageTop + (imageHeight - rectHeight) / 2
        VerticalAlignment.BOTTOM -> imageTop + imageHeight - rectHeight - margin
    }
