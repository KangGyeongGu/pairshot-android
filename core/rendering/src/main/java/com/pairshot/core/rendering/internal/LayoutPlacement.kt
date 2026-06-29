package com.pairshot.core.rendering.internal

import android.graphics.Bitmap
import com.pairshot.core.model.CombineLayout

internal data class LayoutPlacement(
    val canvasWidth: Int,
    val canvasHeight: Int,
    val cellWidth: Int,
    val cellHeight: Int,
    val beforeLeft: Int,
    val beforeTop: Int,
    val afterLeft: Int,
    val afterTop: Int,
)

internal data class CropRegion(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
)

/**
 * before/after 두 사진을 동일한 크기의 칸 두 개에 배치한다.
 * 칸 크기는 (두 사진 중 최소 너비) × (두 사진 중 최소 높이)로 고정되며,
 * 각 사진은 이 칸을 center-crop 으로 꽉 채운다(레터박스 없음, 정확히 50:50).
 * 칸 크기가 두 사진의 너비/높이보다 항상 작거나 같으므로 확대(업스케일)는 일어나지 않는다.
 */
internal fun calculatePlacement(
    beforeWidth: Int,
    beforeHeight: Int,
    afterWidth: Int,
    afterHeight: Int,
    layout: CombineLayout,
    insets: BorderInsets,
): LayoutPlacement {
    val cellWidth = minOf(beforeWidth, afterWidth)
    val cellHeight = minOf(beforeHeight, afterHeight)
    return when (layout) {
        CombineLayout.HORIZONTAL ->
            LayoutPlacement(
                canvasWidth = cellWidth * 2 + insets.left + insets.right + insets.centerGap,
                canvasHeight = cellHeight + insets.top + insets.bottom,
                cellWidth = cellWidth,
                cellHeight = cellHeight,
                beforeLeft = insets.left,
                beforeTop = insets.top,
                afterLeft = insets.left + cellWidth + insets.centerGap,
                afterTop = insets.top,
            )

        CombineLayout.VERTICAL ->
            LayoutPlacement(
                canvasWidth = cellWidth + insets.left + insets.right,
                canvasHeight = cellHeight * 2 + insets.top + insets.bottom + insets.centerGap,
                cellWidth = cellWidth,
                cellHeight = cellHeight,
                beforeLeft = insets.left,
                beforeTop = insets.top,
                afterLeft = insets.left,
                afterTop = insets.top + cellHeight + insets.centerGap,
            )
    }
}

/**
 * 원본(srcWidth×srcHeight)에서 칸 비율(cellWidth:cellHeight)과 동일한 최대 영역을 중앙 기준으로 잘라낸 영역.
 * 이 영역을 칸 dst 로 그리면 비율 유지 + 중앙 크롭(cover)이 된다.
 */
internal fun centerCropRegion(
    srcWidth: Int,
    srcHeight: Int,
    cellWidth: Int,
    cellHeight: Int,
): CropRegion {
    val cropWidth: Int
    val cropHeight: Int
    // src 가 칸보다 가로로 넓으면 너비를, 세로로 길면 높이를 자른다 (정수 교차곱으로 비교).
    if (srcWidth.toLong() * cellHeight >= cellWidth.toLong() * srcHeight) {
        cropHeight = srcHeight
        cropWidth = (cellWidth.toLong() * srcHeight / cellHeight).toInt().coerceIn(1, srcWidth)
    } else {
        cropWidth = srcWidth
        cropHeight = (cellHeight.toLong() * srcWidth / cellWidth).toInt().coerceIn(1, srcHeight)
    }
    val left = (srcWidth - cropWidth) / 2
    val top = (srcHeight - cropHeight) / 2
    return CropRegion(left = left, top = top, right = left + cropWidth, bottom = top + cropHeight)
}

internal fun createCanvasBitmap(placement: LayoutPlacement): Bitmap =
    Bitmap.createBitmap(
        placement.canvasWidth.coerceAtLeast(1),
        placement.canvasHeight.coerceAtLeast(1),
        Bitmap.Config.ARGB_8888,
    )
