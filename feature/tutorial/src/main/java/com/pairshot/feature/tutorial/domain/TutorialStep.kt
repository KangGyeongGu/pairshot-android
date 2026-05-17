package com.pairshot.feature.tutorial.domain

import com.pairshot.core.domain.tutorial.AnchorKey

enum class TutorialSection { MAIN_ONBOARDING, EXPORT_SETTINGS_INTRO }

enum class TutorialStepId {
    HOME_SHOOT_HIGHLIGHT,
    CAMERA_PORTRAIT,
    CAMERA_LANDSCAPE_LEFT,
    CAMERA_LANDSCAPE_RIGHT,
    CAMERA_BACK_GUIDE,
    HOME_PAIR_CARD_GUIDE,
    AFTER_CAMERA_OVERLAY_HINT,
    AFTER_CAMERA_STRIP_INFO,
    AFTER_CAMERA_SHUTTER_GUIDE,
    AFTER_CAMERA_AWAIT_COMPLETION,
    AFTER_CAMERA_BACK_GUIDE,
    HOME_SELECTION_MODE_GUIDE,
    SELECTION_BAR_SHARE,
    SELECTION_BAR_SAVE,
    SELECTION_BAR_DELETE,
    SELECTION_BAR_EXPORT_SETTINGS,
    SELECTION_BAR_EXIT_GUIDE,
    SETTINGS_NAVIGATE,
    SETTINGS_SCREEN_INFO,
    EXPORT_INCLUDE_INFO,
    EXPORT_FORMAT_INFO,
    EXPORT_WATERMARK_INFO,
    EXPORT_COMBINE_INFO,
    DONE,
}

sealed interface AdvanceCondition {
    data object TapAnywhere : AdvanceCondition

    data object Manual : AdvanceCondition

    data class UserAction(
        val actionId: String,
    ) : AdvanceCondition
}

enum class RotationHint { NONE, LEFT, RIGHT, PORTRAIT }

data class TutorialStepDef(
    val id: TutorialStepId,
    val anchor: AnchorKey?,
    val messageResId: Int?,
    val advance: AdvanceCondition,
    val dimAlpha: Float = DEFAULT_DIM_ALPHA,
    val rotationHint: RotationHint = RotationHint.NONE,
    val showSkip: Boolean = true,
    val nextButtonLabelResId: Int? = null,
    val actionAnchor: AnchorKey? = null,
    val strokeAnchor: Boolean = true,
    val centerMessage: Boolean = false,
) {
    val isVisible: Boolean get() = messageResId != null

    companion object {
        const val DEFAULT_DIM_ALPHA = 0.78f
        const val CAMERA_DIM_ALPHA = 0.35f
    }
}
