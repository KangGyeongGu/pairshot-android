package com.pairshot.core.designsystem

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val PretendardFamily =
    FontFamily(
        Font(R.font.pretendard_regular, FontWeight.Normal),
        Font(R.font.pretendard_medium, FontWeight.Medium),
        Font(R.font.pretendard_semibold, FontWeight.SemiBold),
        Font(R.font.pretendard_bold, FontWeight.Bold),
        Font(R.font.pretendard_extrabold, FontWeight.ExtraBold),
    )

val PairShotTypography =
    Typography(
        displayLarge =
        TextStyle(
            fontFamily = PretendardFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 36.sp,
            lineHeight = 44.sp,
            letterSpacing = (-1.0).sp,
        ),
        headlineMedium =
        TextStyle(
            fontFamily = PretendardFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            lineHeight = 32.sp,
            letterSpacing = (-0.5).sp,
        ),
        titleLarge =
        TextStyle(
            fontFamily = PretendardFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 20.sp,
            lineHeight = 28.sp,
            letterSpacing = (-0.3).sp,
        ),
        titleMedium =
        TextStyle(
            fontFamily = PretendardFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.sp,
        ),
        bodyLarge =
        TextStyle(
            fontFamily = PretendardFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.sp,
        ),
        bodyMedium =
        TextStyle(
            fontFamily = PretendardFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.sp,
        ),
        labelLarge =
        TextStyle(
            fontFamily = PretendardFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.sp,
        ),
        labelSmall =
        TextStyle(
            fontFamily = PretendardFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.2.sp,
        ),
    )

object PairShotTypographyTokens {
    val labelExtraSmall =
        TextStyle(
            fontFamily = PretendardFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 10.sp,
            lineHeight = 14.sp,
            letterSpacing = 0.sp,
        )
}
