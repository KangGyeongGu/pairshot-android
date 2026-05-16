package com.pairshot.core.designsystem

import androidx.compose.ui.unit.dp

/**
 * Primitive touch-target sizes.
 * - standard: Material minimum (48dp) for typical interactive controls
 * - large: 56dp for primary actions (FAB, shutter, list row)
 * - compact: 40dp only in dense contexts where standard cannot fit
 */
object PairShotTouchTarget {
    val compact = 40.dp
    val standard = 48.dp
    val large = 56.dp
}
