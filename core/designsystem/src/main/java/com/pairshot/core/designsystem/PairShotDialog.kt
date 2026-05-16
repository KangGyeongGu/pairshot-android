package com.pairshot.core.designsystem

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/** Dialog sizing and elevation. */
object PairShotDialogTokens {
    /** Default fixed width for content-modal dialogs (PairPreview etc.). */
    val width = 340.dp

    /** Default fixed height for content-modal dialogs. */
    val height = 420.dp

    /** Max width for confirm-type dialogs (short message + 1-2 buttons). */
    val confirmMaxWidth = 360.dp

    /** Max width for option / list dialogs (no height ceiling — content driven). */
    val optionMaxWidth = 480.dp

    /** Outer margin between dialog and screen edge. */
    val screenMargin = 32.dp

    /** Shared modal corner shape. */
    val shape = RoundedCornerShape(24.dp)

    /** Base elevation for the dialog surface. */
    val elevation = 8.dp

    /** Elevation used while the dialog is in an active/pressed state. */
    val elevationActive = 14.dp
}
