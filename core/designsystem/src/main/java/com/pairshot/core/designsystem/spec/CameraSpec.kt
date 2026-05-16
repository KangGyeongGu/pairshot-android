package com.pairshot.core.designsystem.spec

import androidx.compose.ui.unit.dp
import com.pairshot.core.designsystem.PairShotSpacing
import com.pairshot.core.designsystem.PairShotStroke
import com.pairshot.core.designsystem.PairShotTouchTarget

/**
 * Camera screen invariants. Referenced from multiple files in feature/camera
 * (ShutterButton, CameraBottomBar, CameraScreenContent, BeforePreviewStrip, ...).
 *
 * These values are structural — changing them affects multiple components in tandem.
 */
object CameraSpec {
    // Preview ratios (width/height) — match sensor configuration
    /** Square preview (1:1). */
    const val PREVIEW_RATIO_SQUARE: Float = 1.0f

    /** Standard 4:3 preview. */
    const val PREVIEW_RATIO_STANDARD: Float = 0.75f

    /** Wide 16:9 preview. */
    const val PREVIEW_RATIO_WIDE: Float = 0.5625f

    // Shutter
    val shutterOuterSize = PairShotTouchTarget.large    // 56
    val shutterInnerSize = PairShotTouchTarget.standard // 48
    val shutterBorderWidth = PairShotStroke.thick       // 3

    // Bottom layout (fixed-height anchored areas below the flexible preview)
    val shutterSectionHeight = 116.dp
    val bottomSpacer = PairShotSpacing.xxl              // 32
    val bottomBarHorizontalPadding = PairShotSpacing.xxl // 32

    // Before strip (Before photos thumbnail strip above shutter)
    val beforeStripHeight = 168.dp
    val beforeCardWidth = 100.dp
    val beforeCardHeight = 134.dp
    val beforeCardSpacing = PairShotSpacing.sm          // 8
    val beforeCardFallbackHPadding = 20.dp
    const val BEFORE_CARD_INACTIVE_SCALE: Float = 0.85f

    // Bottom bar thumbnail
    val thumbnailSize = PairShotTouchTarget.large       // 56
    val thumbnailCornerRadius = PairShotSpacing.sm      // 8

    // Zoom controls (lens picker + zoom dial)
    val lensButtonSize = 36.dp
    val lensButtonIconSize = 18.dp
    val zoomChipHorizontalPadding = 10.dp
    val zoomIndicatorHeight = 18.dp

    // Focus + exposure overlay (drag-EV bar)
    val evBarWidth = 1.5.dp
    val evBarHeight = 100.dp
    val evHitAreaLeftExpansion = 20.dp
    val evHitAreaRightExpansion = 30.dp
    val evHitAreaVerticalExpansion = 20.dp

    // Before strip card corner radius (rounded thumbnail cards)
    val beforeCardCornerRadius = 10.dp

    // Camera settings sheet max width
    val settingsSheetMaxWidth = 520.dp
}
