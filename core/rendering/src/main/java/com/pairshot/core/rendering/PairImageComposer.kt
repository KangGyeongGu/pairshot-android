package com.pairshot.core.rendering

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.net.Uri
import android.os.Trace
import com.pairshot.core.model.CombineConfig
import com.pairshot.core.model.CombineLayout
import com.pairshot.core.model.LabelAnchor
import com.pairshot.core.model.LabelPlacement
import com.pairshot.core.model.LabelPosition
import com.pairshot.core.model.LabelPositionMode
import com.pairshot.core.model.RenderProfile
import com.pairshot.core.model.WatermarkConfig
import com.pairshot.core.model.effectiveLabelBgColor
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

private const val TRACE_COMPOSE = "ps.compose"
private const val TRACE_LOAD_BEFORE = "ps.load-before"
private const val TRACE_LOAD_AFTER = "ps.load-after"
private const val TRACE_COMPOSE_INTERNAL = "ps.compose-internal"
private const val INPUT_DOWNSAMPLE_DIVISOR = 2

private const val OPAQUE_ALPHA = 255
private const val ALPHA_MAX_FLOAT = 255f
private const val LABEL_RECT_HEIGHT_FACTOR = 1.6f
private const val LABEL_HPAD_FACTOR = 0.75f
private const val LABEL_MARGIN_FACTOR = 0.4f
private const val LABEL_MIN_FONT_PX = 10f
private const val REFERENCE_IMAGE_DIM_PX = 3000

private data class LayoutPlacement(
    val canvasWidth: Int,
    val canvasHeight: Int,
    val beforeLeft: Int,
    val beforeTop: Int,
    val afterLeft: Int,
    val afterTop: Int,
)

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

@Singleton
class PairImageComposer
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val exifBitmapLoader: ExifBitmapLoader,
        private val watermarkRenderer: WatermarkRenderer,
    ) {
        suspend fun compose(
            beforeUri: Uri,
            afterUri: Uri,
            combineConfig: CombineConfig,
            watermarkConfig: WatermarkConfig = WatermarkConfig(),
            profile: RenderProfile = RenderProfile.FULL,
        ): Bitmap =
            withContext(Dispatchers.Default) {
                Trace.beginSection(TRACE_COMPOSE)
                try {
                    val inputMaxPx =
                        if (profile.maxOutputPx > 0) profile.maxOutputPx / INPUT_DOWNSAMPLE_DIVISOR else 0
                    val before =
                        withContext(Dispatchers.IO) {
                            Trace.beginSection(TRACE_LOAD_BEFORE)
                            try {
                                exifBitmapLoader.loadBitmapDownscaled(beforeUri, inputMaxPx)
                            } finally {
                                Trace.endSection()
                            }
                        }
                    val after =
                        withContext(Dispatchers.IO) {
                            Trace.beginSection(TRACE_LOAD_AFTER)
                            try {
                                exifBitmapLoader.loadBitmapDownscaled(afterUri, inputMaxPx)
                            } finally {
                                Trace.endSection()
                            }
                        }
                    try {
                        Trace.beginSection(TRACE_COMPOSE_INTERNAL)
                        try {
                            composeInternal(before, after, combineConfig, watermarkConfig, profile)
                        } finally {
                            Trace.endSection()
                        }
                    } finally {
                        if (!before.isRecycled) before.recycle()
                        if (!after.isRecycled) after.recycle()
                    }
                } finally {
                    Trace.endSection()
                }
            }

        suspend fun composeFromBitmaps(
            before: Bitmap,
            after: Bitmap,
            combineConfig: CombineConfig,
            watermarkConfig: WatermarkConfig = WatermarkConfig(),
            profile: RenderProfile = RenderProfile.FULL,
        ): Bitmap =
            withContext(Dispatchers.Default) {
                composeInternal(before, after, combineConfig, watermarkConfig, profile, recycleInputs = false)
            }

        suspend fun composeToFile(
            beforeUri: Uri,
            afterUri: Uri,
            destFile: File,
            combineConfig: CombineConfig,
            watermarkConfig: WatermarkConfig,
            jpegQuality: Int,
            profile: RenderProfile = RenderProfile.FULL,
        ) {
            val combined = compose(beforeUri, afterUri, combineConfig, watermarkConfig, profile)
            try {
                withContext(Dispatchers.IO) {
                    FileOutputStream(destFile).use { out ->
                        combined.compress(Bitmap.CompressFormat.JPEG, jpegQuality, out)
                    }
                }
            } finally {
                combined.recycle()
            }
        }

        suspend fun composeSingleToFile(
            sourceUri: Uri,
            destFile: File,
            isBefore: Boolean,
            combineConfig: CombineConfig,
            watermarkConfig: WatermarkConfig,
            jpegQuality: Int,
            profile: RenderProfile = RenderProfile.FULL,
        ) = withContext(Dispatchers.Default) {
            val inputMaxPx =
                if (profile.maxOutputPx > 0) profile.maxOutputPx / INPUT_DOWNSAMPLE_DIVISOR else 0
            val source =
                withContext(Dispatchers.IO) {
                    if (inputMaxPx > 0) {
                        exifBitmapLoader.loadBitmapDownscaled(sourceUri, inputMaxPx)
                    } else {
                        exifBitmapLoader.loadBitmapWithExifCorrection(sourceUri)
                    }
                }
            val composed = composeSingleInternal(source, isBefore, combineConfig, watermarkConfig, profile)
            try {
                withContext(Dispatchers.IO) {
                    FileOutputStream(destFile).use { out ->
                        composed.compress(Bitmap.CompressFormat.JPEG, jpegQuality, out)
                    }
                }
            } finally {
                if (!composed.isRecycled) composed.recycle()
            }
        }

        suspend fun combineSideBySide(
            beforeUri: Uri,
            afterUri: Uri,
        ): Bitmap = combineSideBySide(beforeUri, afterUri, CombineConfig())

        suspend fun combineSideBySide(
            beforeUri: Uri,
            afterUri: Uri,
            config: CombineConfig,
        ): Bitmap = compose(beforeUri, afterUri, config, WatermarkConfig(), RenderProfile.FULL)

        suspend fun combineSideBySideWithWatermark(
            beforeUri: Uri,
            afterUri: Uri,
            config: CombineConfig,
            watermarkConfig: WatermarkConfig,
        ): Bitmap = compose(beforeUri, afterUri, config, watermarkConfig.copy(enabled = true), RenderProfile.FULL)

        private suspend fun composeSingleInternal(
            source: Bitmap,
            isBefore: Boolean,
            combineConfig: CombineConfig,
            watermarkConfig: WatermarkConfig,
            profile: RenderProfile,
        ): Bitmap {
            val wmSource = applyWatermarkIfEnabled(source, watermarkConfig)
            if (wmSource !== source && !source.isRecycled) source.recycle()

            val referenceDim = minOf(wmSource.width, wmSource.height)
            val insets = resolveSingleBorderInsets(combineConfig, isBefore, referenceDim)
            val canvasWidth = wmSource.width + insets.left + insets.right
            val canvasHeight = wmSource.height + insets.top + insets.bottom
            val canvasBitmap =
                Bitmap.createBitmap(
                    canvasWidth.coerceAtLeast(1),
                    canvasHeight.coerceAtLeast(1),
                    Bitmap.Config.ARGB_8888,
                )
            val canvas = Canvas(canvasBitmap)
            paintBackground(canvas, combineConfig)
            canvas.drawBitmap(wmSource, insets.left.toFloat(), insets.top.toFloat(), null)

            if (combineConfig.labelEnabled) {
                val isFree = combineConfig.labelPositionMode == LabelPositionMode.FREE
                val cornerPx = resolveCornerPx(combineConfig, referenceDim, isFree)
                drawSingleLabel(canvas, isBefore, combineConfig, wmSource, insets, referenceDim, cornerPx)
            }

            if (!wmSource.isRecycled) wmSource.recycle()

            return downscaleIfNeeded(canvasBitmap, profile.maxOutputPx)
        }

        private suspend fun composeInternal(
            before: Bitmap,
            after: Bitmap,
            combineConfig: CombineConfig,
            watermarkConfig: WatermarkConfig,
            profile: RenderProfile,
            recycleInputs: Boolean = true,
        ): Bitmap {
            val wmBefore = applyWatermarkIfEnabled(before, watermarkConfig)
            val wmAfter = applyWatermarkIfEnabled(after, watermarkConfig)

            recycleOriginalsIfReplaced(before, wmBefore, after, wmAfter, recycleInputs)

            val referenceDim =
                minOf(
                    minOf(wmBefore.width, wmBefore.height),
                    minOf(wmAfter.width, wmAfter.height),
                )
            val insets = resolveBorderInsets(combineConfig, combineConfig.layout, referenceDim)
            val placement = calculatePlacement(wmBefore, wmAfter, combineConfig.layout, insets)

            val combined = createCanvasBitmap(placement)
            val canvas = Canvas(combined)
            paintBackground(canvas, combineConfig)

            canvas.drawBitmap(wmBefore, placement.beforeLeft.toFloat(), placement.beforeTop.toFloat(), null)
            canvas.drawBitmap(wmAfter, placement.afterLeft.toFloat(), placement.afterTop.toFloat(), null)

            if (combineConfig.labelEnabled) {
                val isFree = combineConfig.labelPositionMode == LabelPositionMode.FREE
                val cornerPx = resolveCornerPx(combineConfig, referenceDim, isFree)
                drawBothLabels(canvas, combineConfig, referenceDim, wmBefore, wmAfter, placement, insets, cornerPx)
            }

            recycleIntermediateBitmaps(before, wmBefore, after, wmAfter, recycleInputs)

            return downscaleIfNeeded(combined, profile.maxOutputPx)
        }

        private suspend fun applyWatermarkIfEnabled(
            source: Bitmap,
            watermarkConfig: WatermarkConfig,
        ): Bitmap = if (watermarkConfig.enabled) watermarkRenderer.apply(source, watermarkConfig) else source

        private fun resolveBorderPx(
            combineConfig: CombineConfig,
            referenceImageDim: Int,
        ): Int {
            if (!combineConfig.borderEnabled) return 0
            val density = context.resources.displayMetrics.density
            val scale = referenceImageDim.toFloat() / REFERENCE_IMAGE_DIM_PX
            return (combineConfig.borderThicknessDp * density * scale).toInt()
        }

        private fun resolveCornerPx(
            combineConfig: CombineConfig,
            referenceImageDim: Int,
            isFreeMode: Boolean,
        ): Float {
            if (!isFreeMode) return 0f
            val density = context.resources.displayMetrics.density
            val scale = referenceImageDim.toFloat() / REFERENCE_IMAGE_DIM_PX
            return combineConfig.labelBgCornerDp * density * scale
        }

        private fun resolveBorderInsets(
            combineConfig: CombineConfig,
            layout: CombineLayout,
            referenceImageDim: Int,
        ): BorderInsets {
            val userBorder = resolveBorderPx(combineConfig, referenceImageDim)
            if (!combineConfig.labelEnabled || combineConfig.labelPlacement != LabelPlacement.INSIDE_BORDER) {
                return BorderInsets.uniform(userBorder)
            }
            val labelNeed = resolveBorderLabelEdgePx(combineConfig, referenceImageDim)
            val beforeV = combineConfig.beforeLabelAnchor.toBorderVerticalAlignment()
            val afterV = combineConfig.afterLabelAnchor.toBorderVerticalAlignment()
            return when (layout) {
                CombineLayout.HORIZONTAL -> {
                    val topNeeded = beforeV == VerticalAlignment.TOP || afterV == VerticalAlignment.TOP
                    val bottomNeeded = beforeV == VerticalAlignment.BOTTOM || afterV == VerticalAlignment.BOTTOM
                    BorderInsets(
                        left = userBorder,
                        top = if (topNeeded) maxOf(userBorder, labelNeed) else userBorder,
                        right = userBorder,
                        bottom = if (bottomNeeded) maxOf(userBorder, labelNeed) else userBorder,
                        centerGap = userBorder,
                    )
                }

                CombineLayout.VERTICAL -> {
                    val topNeeded = beforeV == VerticalAlignment.TOP
                    val bottomNeeded = afterV == VerticalAlignment.BOTTOM
                    val centerNeeded = beforeV == VerticalAlignment.BOTTOM || afterV == VerticalAlignment.TOP
                    BorderInsets(
                        left = userBorder,
                        top = if (topNeeded) maxOf(userBorder, labelNeed) else userBorder,
                        right = userBorder,
                        bottom = if (bottomNeeded) maxOf(userBorder, labelNeed) else userBorder,
                        centerGap = if (centerNeeded) maxOf(userBorder, labelNeed) else userBorder,
                    )
                }
            }
        }

        private fun resolveSingleBorderInsets(
            combineConfig: CombineConfig,
            isBefore: Boolean,
            referenceImageDim: Int,
        ): BorderInsets {
            val userBorder = resolveBorderPx(combineConfig, referenceImageDim)
            if (!combineConfig.labelEnabled || combineConfig.labelPlacement != LabelPlacement.INSIDE_BORDER) {
                return BorderInsets.uniform(userBorder)
            }
            val labelNeed = resolveBorderLabelEdgePx(combineConfig, referenceImageDim)
            val anchor = if (isBefore) combineConfig.beforeLabelAnchor else combineConfig.afterLabelAnchor
            val v = anchor.toBorderVerticalAlignment()
            return BorderInsets(
                left = userBorder,
                top = if (v == VerticalAlignment.TOP) maxOf(userBorder, labelNeed) else userBorder,
                right = userBorder,
                bottom = if (v == VerticalAlignment.BOTTOM) maxOf(userBorder, labelNeed) else userBorder,
                centerGap = userBorder,
            )
        }
    }

private fun recycleOriginalsIfReplaced(
    before: Bitmap,
    wmBefore: Bitmap,
    after: Bitmap,
    wmAfter: Bitmap,
    recycleInputs: Boolean,
) {
    if (!recycleInputs) return
    if (wmBefore !== before && !before.isRecycled) before.recycle()
    if (wmAfter !== after && !after.isRecycled) after.recycle()
}

private fun recycleIntermediateBitmaps(
    before: Bitmap,
    wmBefore: Bitmap,
    after: Bitmap,
    wmAfter: Bitmap,
    recycleInputs: Boolean,
) {
    if (recycleInputs) {
        if (!wmBefore.isRecycled) wmBefore.recycle()
        if (!wmAfter.isRecycled) wmAfter.recycle()
    } else {
        if (wmBefore !== before && !wmBefore.isRecycled) wmBefore.recycle()
        if (wmAfter !== after && !wmAfter.isRecycled) wmAfter.recycle()
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

private fun calculatePlacement(
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

private fun createCanvasBitmap(placement: LayoutPlacement): Bitmap =
    Bitmap.createBitmap(
        placement.canvasWidth.coerceAtLeast(1),
        placement.canvasHeight.coerceAtLeast(1),
        Bitmap.Config.ARGB_8888,
    )

private fun paintBackground(
    canvas: Canvas,
    combineConfig: CombineConfig,
) {
    if (combineConfig.borderEnabled) {
        canvas.drawColor(combineConfig.borderColorArgb)
    } else {
        canvas.drawColor(android.graphics.Color.BLACK)
    }
}

private fun downscaleIfNeeded(
    source: Bitmap,
    maxOutputPx: Int,
): Bitmap {
    if (maxOutputPx <= 0) return source
    val longestSide = maxOf(source.width, source.height)
    if (longestSide <= maxOutputPx) return source
    val ratio = maxOutputPx.toFloat() / longestSide
    val targetW = (source.width * ratio).toInt().coerceAtLeast(1)
    val targetH = (source.height * ratio).toInt().coerceAtLeast(1)
    val scaled = Bitmap.createScaledBitmap(source, targetW, targetH, true)
    if (scaled !== source && !source.isRecycled) source.recycle()
    return scaled
}

private fun drawBothLabels(
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
            region = computeBorderRegion(beforeRect, combineConfig.beforeLabelAnchor, combineConfig.layout, insets, isBefore = true),
            config = combineConfig,
            fontSize = fontSize,
        )
        drawBorderLabelInRegion(
            canvas = canvas,
            text = combineConfig.afterLabel,
            anchor = combineConfig.afterLabelAnchor,
            region = computeBorderRegion(afterRect, combineConfig.afterLabelAnchor, combineConfig.layout, insets, isBefore = false),
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

private fun drawSingleLabel(
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
                    LabelRect(left = imageRect.left, top = imageBottom, width = imageRect.width, height = insets.centerGap)
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

private fun fixedLabelRect(
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

private fun anchoredLabelRect(
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

internal enum class HorizontalAlignment { LEFT, CENTER, RIGHT }

internal enum class VerticalAlignment { TOP, MIDDLE, BOTTOM }

private fun LabelAnchor.horizontalAlignment(): HorizontalAlignment =
    when (this) {
        LabelAnchor.TOP_LEFT, LabelAnchor.MIDDLE_LEFT, LabelAnchor.BOTTOM_LEFT -> HorizontalAlignment.LEFT
        LabelAnchor.TOP_CENTER, LabelAnchor.MIDDLE_CENTER, LabelAnchor.BOTTOM_CENTER -> HorizontalAlignment.CENTER
        LabelAnchor.TOP_RIGHT, LabelAnchor.MIDDLE_RIGHT, LabelAnchor.BOTTOM_RIGHT -> HorizontalAlignment.RIGHT
    }

private fun LabelAnchor.verticalAlignment(): VerticalAlignment =
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
