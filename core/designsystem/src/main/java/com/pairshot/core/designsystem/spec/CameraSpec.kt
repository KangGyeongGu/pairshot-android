package com.pairshot.core.designsystem.spec

import androidx.compose.ui.unit.dp
import com.pairshot.core.designsystem.PairShotSpacing
import com.pairshot.core.designsystem.PairShotStroke
import com.pairshot.core.designsystem.PairShotTouchTarget


object CameraSpec {
    
    
    const val PREVIEW_RATIO_SQUARE: Float = 1.0f

    
    const val PREVIEW_RATIO_STANDARD: Float = 0.75f

    
    const val PREVIEW_RATIO_WIDE: Float = 0.5625f

    
    val shutterOuterSize = PairShotTouchTarget.large    
    val shutterInnerSize = PairShotTouchTarget.standard 
    val shutterBorderWidth = PairShotStroke.thick       

    
    val shutterSectionHeight = 116.dp
    val bottomSpacer = PairShotSpacing.xxl              
    val bottomBarHorizontalPadding = PairShotSpacing.xxl 

    
    val beforeStripHeight = 168.dp
    val beforeCardWidth = 100.dp
    val beforeCardHeight = 134.dp
    val beforeCardSpacing = PairShotSpacing.sm          
    val beforeCardFallbackHPadding = 20.dp
    const val BEFORE_CARD_INACTIVE_SCALE: Float = 0.85f

    
    val thumbnailSize = PairShotTouchTarget.large       
    val thumbnailCornerRadius = PairShotSpacing.sm      

    
    val lensButtonSize = 36.dp
    val lensButtonIconSize = 18.dp
    val zoomChipHorizontalPadding = 10.dp
    val zoomIndicatorHeight = 18.dp

    
    val evBarWidth = 1.5.dp
    val evBarHeight = 100.dp
    val evHitAreaLeftExpansion = 20.dp
    val evHitAreaRightExpansion = 30.dp
    val evHitAreaVerticalExpansion = 20.dp

    
    val beforeCardCornerRadius = 10.dp

    
    val settingsSheetMaxWidth = 520.dp
}
