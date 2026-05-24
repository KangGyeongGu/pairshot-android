package com.pairshot.core.rendering.internal

import android.graphics.Bitmap
import android.graphics.Canvas
import com.pairshot.core.model.CombineConfig

internal fun paintBackground(
    canvas: Canvas,
    combineConfig: CombineConfig,
) {
    if (combineConfig.borderEnabled) {
        canvas.drawColor(combineConfig.borderColorArgb)
    } else {
        canvas.drawColor(android.graphics.Color.BLACK)
    }
}

internal fun downscaleIfNeeded(
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

internal fun recycleOriginalsIfReplaced(
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

internal fun recycleIntermediateBitmaps(
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
