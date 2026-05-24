package com.pairshot.core.rendering

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.pairshot.core.model.LogoPosition
import com.pairshot.core.model.WatermarkConfig
import com.pairshot.core.model.WatermarkType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

private const val DIAGONAL_WATERMARK_ANGLE_DEG = -45f
private const val ALPHA_OPAQUE_BYTE = 255
private const val ALPHA_TRANSPARENT_BYTE = 0
private const val DIAGONAL_TEXT_MIN_GAP_PX = 16f
private const val LOGO_PADDING_RATIO = 0.02f

@Singleton
class WatermarkRenderer
@Inject
constructor() {
    suspend fun applyTextWatermark(
        source: Bitmap,
        config: WatermarkConfig,
    ): Bitmap =
        withContext(Dispatchers.Default) {
            val result = source.copy(Bitmap.Config.ARGB_8888, true)
            if (config.text.isBlank()) return@withContext result
            val canvas = Canvas(result)

            val paint =
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.WHITE
                    alpha =
                        (config.alpha * ALPHA_OPAQUE_BYTE)
                            .toInt()
                            .coerceIn(ALPHA_TRANSPARENT_BYTE, ALPHA_OPAQUE_BYTE)
                    textSize = source.width * config.textSizeRatio
                }

            val width = result.width.toFloat()
            val height = result.height.toFloat()
            val centerX = width / 2f
            val centerY = height / 2f

            val diagonal = sqrt(width * width + height * height)

            val lineSpacing = diagonal / config.diagonalCount

            val textWidth = paint.measureText(config.text)
            val textSpacing =
                (textWidth * (2f / config.repeatDensity))
                    .coerceAtLeast(textWidth + DIAGONAL_TEXT_MIN_GAP_PX)

            canvas.save()
            canvas.rotate(DIAGONAL_WATERMARK_ANGLE_DEG, centerX, centerY)

            val startY = centerY - diagonal
            val endY = centerY + diagonal
            val startX = centerX - diagonal
            val endX = centerX + diagonal

            var y = startY
            while (y <= endY) {
                var x = startX
                while (x <= endX) {
                    canvas.drawText(config.text, x, y, paint)
                    x += textSpacing
                }
                y += lineSpacing
            }

            canvas.restore()
            result
        }

    suspend fun applyLogoWatermark(
        source: Bitmap,
        config: WatermarkConfig,
    ): Bitmap =
        withContext(Dispatchers.Default) {
            val result = source.copy(Bitmap.Config.ARGB_8888, true)
            if (config.logoPath.isBlank()) return@withContext result

            val rawLogo =
                runCatching { BitmapFactory.decodeFile(config.logoPath) }.getOrNull()
                    ?: return@withContext result
            val canvas = Canvas(result)

            val targetSize = (source.width * config.logoSizeRatio).toInt().coerceAtLeast(1)
            val logoAspect = rawLogo.height.toFloat() / rawLogo.width.toFloat()
            val logoWidth = targetSize
            val logoHeight = (targetSize * logoAspect).toInt().coerceAtLeast(1)

            val resizedLogo =
                runCatching {
                    Bitmap.createScaledBitmap(rawLogo, logoWidth, logoHeight, true)
                }.getOrNull()

            rawLogo.recycle()

            if (resizedLogo == null) {
                result.recycle()
                return@withContext source
            }

            val padding = (source.width * LOGO_PADDING_RATIO).toInt()
            val imgWidth = result.width
            val imgHeight = result.height

            val (x, y) =
                when (config.logoPosition) {
                    LogoPosition.TOP_LEFT -> {
                        padding.toFloat() to padding.toFloat()
                    }

                    LogoPosition.TOP_CENTER -> {
                        ((imgWidth - logoWidth) / 2f) to padding.toFloat()
                    }

                    LogoPosition.TOP_RIGHT -> {
                        (imgWidth - logoWidth - padding).toFloat() to padding.toFloat()
                    }

                    LogoPosition.CENTER_LEFT -> {
                        padding.toFloat() to ((imgHeight - logoHeight) / 2f)
                    }

                    LogoPosition.CENTER -> {
                        ((imgWidth - logoWidth) / 2f) to ((imgHeight - logoHeight) / 2f)
                    }

                    LogoPosition.CENTER_RIGHT -> {
                        (imgWidth - logoWidth - padding).toFloat() to ((imgHeight - logoHeight) / 2f)
                    }

                    LogoPosition.BOTTOM_LEFT -> {
                        padding.toFloat() to (imgHeight - logoHeight - padding).toFloat()
                    }

                    LogoPosition.BOTTOM_CENTER -> {
                        ((imgWidth - logoWidth) / 2f) to (imgHeight - logoHeight - padding).toFloat()
                    }

                    LogoPosition.BOTTOM_RIGHT -> {
                        (imgWidth - logoWidth - padding).toFloat() to (imgHeight - logoHeight - padding).toFloat()
                    }
                }

            val paint =
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    alpha =
                        (config.logoAlpha * ALPHA_OPAQUE_BYTE)
                            .toInt()
                            .coerceIn(ALPHA_TRANSPARENT_BYTE, ALPHA_OPAQUE_BYTE)
                }

            canvas.drawBitmap(resizedLogo, x, y, paint)
            resizedLogo.recycle()

            result
        }

    suspend fun apply(
        source: Bitmap,
        config: WatermarkConfig,
    ): Bitmap {
        if (!config.enabled) return source
        return when (config.type) {
            WatermarkType.TEXT -> applyTextWatermark(source, config)
            WatermarkType.LOGO -> applyLogoWatermark(source, config)
        }
    }
}
