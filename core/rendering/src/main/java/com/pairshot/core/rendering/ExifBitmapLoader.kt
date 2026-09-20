package com.pairshot.core.rendering

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExifBitmapLoader
@Inject
constructor(
    @ApplicationContext private val context: Context,
) {
    fun loadBitmapWithExifCorrection(uri: Uri): Bitmap = loadBitmapWithExifCorrection(uri, inSampleSize = 1)

    fun loadBitmapDownscaled(
        uri: Uri,
        maxLongestSide: Int,
    ): Bitmap {
        val sampleSize = if (maxLongestSide > 0) calculateInSampleSize(uri, maxLongestSide) else 1
        return loadBitmapWithExifCorrection(uri, sampleSize)
    }

    fun loadBitmapWithExifCorrection(
        uri: Uri,
        inSampleSize: Int,
    ): Bitmap {
        val options =
            BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize.coerceAtLeast(1)
            }
        val inputStream =
            context.contentResolver.openInputStream(uri)
                ?: error("Cannot open input stream for $uri")

        val bitmap =
            inputStream.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            } ?: error("Cannot decode bitmap for $uri")

        val rotation = readExifDegrees(uri)
        if (rotation == 0) return bitmap

        val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (rotated !== bitmap) bitmap.recycle()
        return rotated
    }

    private fun calculateInSampleSize(
        uri: Uri,
        maxLongestSide: Int,
    ): Int {
        val bounds = readBitmapBounds(uri) ?: return 1
        val (width, height) = bounds
        val longest = maxOf(width, height)
        if (longest <= maxLongestSide) return 1
        var sample = 1
        while (longest / (sample * 2) >= maxLongestSide) {
            sample *= 2
        }
        return sample
    }

    fun readBitmapBounds(uri: Uri): Pair<Int, Int>? {
        val options =
            BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
        val stream = context.contentResolver.openInputStream(uri) ?: return null
        stream.use { BitmapFactory.decodeStream(it, null, options) }
        if (options.outWidth <= 0 || options.outHeight <= 0) return null
        return options.outWidth to options.outHeight
    }

    fun readExifDegrees(uri: Uri): Int {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return 0
        return inputStream.use { stream ->
            val exif = ExifInterface(stream)
            when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> ROTATE_QUARTER
                ExifInterface.ORIENTATION_ROTATE_180 -> ROTATE_HALF
                ExifInterface.ORIENTATION_ROTATE_270 -> ROTATE_THREE_QUARTERS
                else -> 0
            }
        }
    }

    /**
     * 사진의 표시 회전각(0/90/270)을 판정한다. EXIF 보정 결과가 세로면 0(가이드 없음),
     * 가로면 EXIF 180일 때 270(오른쪽), 그 외 90(왼쪽). 치수 읽기 실패 시 EXIF만으로 폴백.
     */
    fun readOrientationDegrees(uri: Uri): Float {
        val exifDegrees = readExifDegrees(uri)
        val bounds =
            readBitmapBounds(uri)
                ?: return OverlayTransformCalculator.fromExifOnly(exifDegrees)
        return OverlayTransformCalculator.fromImageOrientation(bounds.first, bounds.second, exifDegrees)
    }

    fun readIsPortrait(uri: Uri): Boolean = readOrientationDegrees(uri) == 0f

    companion object {
        private const val ROTATE_QUARTER = 90
        private const val ROTATE_HALF = 180
        private const val ROTATE_THREE_QUARTERS = 270
    }
}
