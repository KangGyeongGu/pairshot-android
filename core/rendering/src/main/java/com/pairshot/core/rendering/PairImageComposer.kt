package com.pairshot.core.rendering

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.net.Uri
import android.os.Trace
import com.pairshot.core.model.CombineConfig
import com.pairshot.core.model.CombineLayout
import com.pairshot.core.model.LabelPlacement
import com.pairshot.core.model.LabelPositionMode
import com.pairshot.core.model.RenderProfile
import com.pairshot.core.model.WatermarkConfig
import com.pairshot.core.rendering.internal.BorderInsets
import com.pairshot.core.rendering.internal.LayoutPlacement
import com.pairshot.core.rendering.internal.VerticalAlignment
import com.pairshot.core.rendering.internal.calculatePlacement
import com.pairshot.core.rendering.internal.centerCropRegion
import com.pairshot.core.rendering.internal.createCanvasBitmap
import com.pairshot.core.rendering.internal.downscaleIfNeeded
import com.pairshot.core.rendering.internal.drawBothLabels
import com.pairshot.core.rendering.internal.drawSingleLabel
import com.pairshot.core.rendering.internal.paintBackground
import com.pairshot.core.rendering.internal.recycleIntermediateBitmaps
import com.pairshot.core.rendering.internal.recycleOriginalsIfReplaced
import com.pairshot.core.rendering.internal.resolveBorderLabelEdgePx
import com.pairshot.core.rendering.internal.toBorderVerticalAlignment
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
private const val REFERENCE_IMAGE_DIM_PX = 3000

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
        val placement =
            calculatePlacement(
                wmBefore.width,
                wmBefore.height,
                wmAfter.width,
                wmAfter.height,
                combineConfig.layout,
                insets,
            )

        val combined = createCanvasBitmap(placement)
        val canvas = Canvas(combined)
        paintBackground(canvas, combineConfig)

        val cropPaint = Paint(Paint.FILTER_BITMAP_FLAG)
        drawIntoCell(canvas, wmBefore, placement.beforeLeft, placement.beforeTop, placement, cropPaint)
        drawIntoCell(canvas, wmAfter, placement.afterLeft, placement.afterTop, placement, cropPaint)

        if (combineConfig.labelEnabled) {
            val isFree = combineConfig.labelPositionMode == LabelPositionMode.FREE
            val cornerPx = resolveCornerPx(combineConfig, referenceDim, isFree)
            drawBothLabels(canvas, combineConfig, referenceDim, placement, insets, cornerPx)
        }

        recycleIntermediateBitmaps(before, wmBefore, after, wmAfter, recycleInputs)

        return downscaleIfNeeded(combined, profile.maxOutputPx)
    }

    private fun drawIntoCell(
        canvas: Canvas,
        bitmap: Bitmap,
        cellLeft: Int,
        cellTop: Int,
        placement: LayoutPlacement,
        paint: Paint,
    ) {
        val region = centerCropRegion(bitmap.width, bitmap.height, placement.cellWidth, placement.cellHeight)
        val src = Rect(region.left, region.top, region.right, region.bottom)
        val dst = Rect(cellLeft, cellTop, cellLeft + placement.cellWidth, cellTop + placement.cellHeight)
        canvas.drawBitmap(bitmap, src, dst, paint)
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
