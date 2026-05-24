package com.pairshot.core.rendering.internal

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import com.pairshot.core.model.CombineConfig
import com.pairshot.core.model.LabelAnchor
import com.pairshot.core.model.LabelPlacement
import com.pairshot.core.model.LabelPositionMode
import com.pairshot.core.model.effectiveLabelBgColor

private const val OPAQUE_ALPHA = 255
private const val ALPHA_MAX_FLOAT = 255f

internal fun drawBothLabels(
    canvas: Canvas,
    combineConfig: CombineConfig,
    referenceImageDim: Int,
    wmBefore: Bitmap,
    wmAfter: Bitmap,
    placement: LayoutPlacement,
    insets: BorderInsets,
    cornerPx: Float,
) {
    val fontSize = resolveLabelFontSize(combineConfig, referenceImageDim)
    val beforeRect = LabelRect(placement.beforeLeft, placement.beforeTop, wmBefore.width, wmBefore.height)
    val afterRect = LabelRect(placement.afterLeft, placement.afterTop, wmAfter.width, wmAfter.height)

    if (combineConfig.labelPlacement == LabelPlacement.INSIDE_BORDER) {
        drawBorderLabelInRegion(
            canvas = canvas,
            text = combineConfig.beforeLabel,
            anchor = combineConfig.beforeLabelAnchor,
            region = computeBorderRegion(
                beforeRect,
                combineConfig.beforeLabelAnchor,
                combineConfig.layout,
                insets,
                isBefore = true,
            ),
            config = combineConfig,
            fontSize = fontSize,
        )
        drawBorderLabelInRegion(
            canvas = canvas,
            text = combineConfig.afterLabel,
            anchor = combineConfig.afterLabelAnchor,
            region = computeBorderRegion(
                afterRect,
                combineConfig.afterLabelAnchor,
                combineConfig.layout,
                insets,
                isBefore = false,
            ),
            config = combineConfig,
            fontSize = fontSize,
        )
        return
    }
    val isFree = combineConfig.labelPositionMode == LabelPositionMode.FREE
    drawLabel(
        canvas = canvas,
        text = combineConfig.beforeLabel,
        imageRect = beforeRect,
        config = combineConfig,
        fontSize = fontSize,
        anchor = if (isFree) combineConfig.beforeLabelAnchor else null,
        cornerPx = cornerPx,
    )
    drawLabel(
        canvas = canvas,
        text = combineConfig.afterLabel,
        imageRect = afterRect,
        config = combineConfig,
        fontSize = fontSize,
        anchor = if (isFree) combineConfig.afterLabelAnchor else null,
        cornerPx = cornerPx,
    )
}

internal fun drawSingleLabel(
    canvas: Canvas,
    isBefore: Boolean,
    combineConfig: CombineConfig,
    image: Bitmap,
    insets: BorderInsets,
    referenceImageDim: Int,
    cornerPx: Float,
) {
    val text = if (isBefore) combineConfig.beforeLabel else combineConfig.afterLabel
    val fontSize = resolveLabelFontSize(combineConfig, referenceImageDim)
    val imageRect = LabelRect(insets.left, insets.top, image.width, image.height)

    if (combineConfig.labelPlacement == LabelPlacement.INSIDE_BORDER) {
        val anchor = if (isBefore) combineConfig.beforeLabelAnchor else combineConfig.afterLabelAnchor
        drawBorderLabelInRegion(
            canvas = canvas,
            text = text,
            anchor = anchor,
            region = computeSingleBorderRegion(imageRect, anchor, insets),
            config = combineConfig,
            fontSize = fontSize,
        )
        return
    }
    val isFree = combineConfig.labelPositionMode == LabelPositionMode.FREE
    val anchor =
        if (isFree) {
            if (isBefore) combineConfig.beforeLabelAnchor else combineConfig.afterLabelAnchor
        } else {
            null
        }
    drawLabel(
        canvas = canvas,
        text = text,
        imageRect = imageRect,
        config = combineConfig,
        fontSize = fontSize,
        anchor = anchor,
        cornerPx = cornerPx,
    )
}

private fun drawBorderLabelInRegion(
    canvas: Canvas,
    text: String,
    anchor: LabelAnchor,
    region: LabelRect,
    config: CombineConfig,
    fontSize: Float,
) {
    if (region.width <= 0 || region.height <= 0) return
    val textPaint = buildLabelTextPaint(config, fontSize)

    val textBounds = Rect()
    textPaint.getTextBounds(text, 0, text.length, textBounds)
    val margin = (fontSize * LABEL_MARGIN_FACTOR).toInt()

    val textX =
        when (anchor.horizontalAlignment()) {
            HorizontalAlignment.LEFT -> region.left + margin + textBounds.width() / 2f
            HorizontalAlignment.CENTER -> region.left + region.width / 2f
            HorizontalAlignment.RIGHT -> region.left + region.width - margin - textBounds.width() / 2f
        }
    val textY = region.top + region.height / 2f - (textPaint.descent() + textPaint.ascent()) / 2f
    canvas.drawText(text, textX, textY, textPaint)
}

private fun drawLabel(
    canvas: Canvas,
    text: String,
    imageRect: LabelRect,
    config: CombineConfig,
    fontSize: Float,
    anchor: LabelAnchor? = null,
    cornerPx: Float = 0f,
) {
    val rectHeight = (fontSize * LABEL_RECT_HEIGHT_FACTOR).toInt()
    val bgPaint = buildLabelBgPaint(config)
    val textPaint = buildLabelTextPaint(config, fontSize)

    val labelRect =
        if (anchor == null) {
            fixedLabelRect(config.labelPosition, imageRect, rectHeight)
        } else {
            anchoredLabelRect(anchor, text, textPaint, fontSize, imageRect, rectHeight)
        }

    drawLabelBackgroundIfEnabled(canvas, config, labelRect, anchor, cornerPx, bgPaint)
    drawLabelText(canvas, text, labelRect, textPaint)
}

private fun buildLabelBgPaint(config: CombineConfig): Paint {
    val bgAlpha = (config.labelBgAlpha * ALPHA_MAX_FLOAT).toInt().coerceIn(0, OPAQUE_ALPHA)
    return Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = config.effectiveLabelBgColor()
        alpha = bgAlpha
        style = Paint.Style.FILL
    }
}

private fun buildLabelTextPaint(
    config: CombineConfig,
    fontSize: Float,
): Paint =
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = config.labelTextColorArgb
        alpha = OPAQUE_ALPHA
        textSize = fontSize
        textAlign = Paint.Align.CENTER
    }

private fun drawLabelBackgroundIfEnabled(
    canvas: Canvas,
    config: CombineConfig,
    labelRect: LabelRect,
    anchor: LabelAnchor?,
    cornerPx: Float,
    bgPaint: Paint,
) {
    if (!config.labelBgEnabled) return
    val rf =
        RectF(
            labelRect.left.toFloat(),
            labelRect.top.toFloat(),
            (labelRect.left + labelRect.width).toFloat(),
            (labelRect.top + labelRect.height).toFloat(),
        )
    if (anchor != null && cornerPx > 0f) {
        canvas.drawRoundRect(rf, cornerPx, cornerPx, bgPaint)
    } else {
        canvas.drawRect(rf, bgPaint)
    }
}

private fun drawLabelText(
    canvas: Canvas,
    text: String,
    labelRect: LabelRect,
    textPaint: Paint,
) {
    val textX = labelRect.left + labelRect.width / 2f
    val textY = labelRect.top + labelRect.height / 2f - (textPaint.descent() + textPaint.ascent()) / 2f
    canvas.drawText(text, textX, textY, textPaint)
}
