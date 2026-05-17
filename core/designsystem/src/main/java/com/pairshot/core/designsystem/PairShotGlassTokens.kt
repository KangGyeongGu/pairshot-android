package com.pairshot.core.designsystem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private const val GLASS_SURFACE_ARGB = 0x991C1C1EL
private const val GLASS_DESTRUCTIVE_ARGB = 0xFFFF3B30L

object PairShotGlassTokens {
    val surfaceColor = Color(GLASS_SURFACE_ARGB)

    val contentColor = Color.White
    val border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
    val shape = RoundedCornerShape(20.dp)
    val shadowElevation = 20.dp

    val destructiveColor = Color(GLASS_DESTRUCTIVE_ARGB)
    val destructiveContentColor = Color.White
}
