package com.pairshot.feature.tutorial.domain

import com.pairshot.core.domain.tutorial.AnchorKey
import com.pairshot.core.domain.tutorial.TutorialActionIds
import com.pairshot.feature.tutorial.R

object TutorialStepDefinitions {
    private val mainOrdered: List<TutorialStepDef> =
        listOf(
            TutorialStepDef(
                id = TutorialStepId.HOME_SHOOT_HIGHLIGHT,
                anchor = AnchorKey.HOME_SHOOT_BUTTON,
                messageResId = R.string.tutorial_msg_home_shoot,
                advance = AdvanceCondition.UserAction(TutorialActionIds.HOME_SHOOT_CLICKED),
            ),
            TutorialStepDef(
                id = TutorialStepId.CAMERA_PORTRAIT,
                anchor = AnchorKey.CAMERA_PREVIEW,
                messageResId = R.string.tutorial_msg_camera_portrait,
                messageKeywordResId = R.string.tutorial_msg_camera_portrait_keyword,
                advance = AdvanceCondition.UserAction(TutorialActionIds.CAMERA_SHUTTER_PORTRAIT),
                dimAlpha = TutorialStepDef.CAMERA_DIM_ALPHA,
                rotationHint = RotationHint.PORTRAIT,
                actionAnchor = AnchorKey.CAMERA_SHUTTER_BUTTON,
                strokeAnchor = false,
            ),
            TutorialStepDef(
                id = TutorialStepId.CAMERA_LANDSCAPE_LEFT,
                anchor = AnchorKey.CAMERA_PREVIEW,
                messageResId = R.string.tutorial_msg_camera_landscape_left,
                messageKeywordResId = R.string.tutorial_msg_camera_landscape_left_keyword,
                advance = AdvanceCondition.UserAction(TutorialActionIds.CAMERA_SHUTTER_LANDSCAPE_LEFT),
                dimAlpha = TutorialStepDef.CAMERA_DIM_ALPHA,
                rotationHint = RotationHint.LEFT,
                actionAnchor = AnchorKey.CAMERA_SHUTTER_BUTTON,
                strokeAnchor = false,
            ),
            TutorialStepDef(
                id = TutorialStepId.CAMERA_LANDSCAPE_RIGHT,
                anchor = AnchorKey.CAMERA_PREVIEW,
                messageResId = R.string.tutorial_msg_camera_landscape_right,
                messageKeywordResId = R.string.tutorial_msg_camera_landscape_right_keyword,
                advance = AdvanceCondition.UserAction(TutorialActionIds.CAMERA_SHUTTER_LANDSCAPE_RIGHT),
                dimAlpha = TutorialStepDef.CAMERA_DIM_ALPHA,
                rotationHint = RotationHint.RIGHT,
                actionAnchor = AnchorKey.CAMERA_SHUTTER_BUTTON,
                strokeAnchor = false,
            ),
            TutorialStepDef(
                id = TutorialStepId.CAMERA_BACK_GUIDE,
                anchor = AnchorKey.CAMERA_BACK_BUTTON,
                messageResId = R.string.tutorial_msg_camera_back,
                advance = AdvanceCondition.UserAction(TutorialActionIds.CAMERA_BACK_TO_HOME),
            ),
            TutorialStepDef(
                id = TutorialStepId.HOME_PAIR_CARD_GUIDE,
                anchor = AnchorKey.HOME_PAIR_CARD_FIRST,
                messageResId = R.string.tutorial_msg_home_pair_card,
                advance = AdvanceCondition.UserAction(TutorialActionIds.HOME_PAIR_CARD_TAPPED),
            ),
            TutorialStepDef(
                id = TutorialStepId.AFTER_CAMERA_STRIP_INFO,
                anchor = AnchorKey.AFTER_CAMERA_STRIP,
                messageResId = R.string.tutorial_msg_after_strip,
                advance = AdvanceCondition.TapAnywhere,
                dimAlpha = TutorialStepDef.CAMERA_DIM_ALPHA,
                nextButtonLabelResId = R.string.tutorial_button_next,
            ),
            TutorialStepDef(
                id = TutorialStepId.AFTER_CAMERA_SELECTED_CARD_GUIDE,
                anchor = AnchorKey.AFTER_CAMERA_SELECTED_CARD,
                messageResId = R.string.tutorial_msg_after_selected_card,
                advance = AdvanceCondition.UserAction(TutorialActionIds.AFTER_CAMERA_BEFORE_PREVIEW_OPENED),
                dimAlpha = TutorialStepDef.CAMERA_DIM_ALPHA,
            ),
            TutorialStepDef(
                id = TutorialStepId.AFTER_CAMERA_BEFORE_PREVIEW_DISMISS_GUIDE,
                anchor = null,
                messageResId = R.string.tutorial_msg_after_preview_dismiss,
                advance = AdvanceCondition.UserAction(TutorialActionIds.AFTER_CAMERA_BEFORE_PREVIEW_DISMISSED),
                dimAlpha = 0f,
                centerMessage = true,
                strokeAnchor = false,
            ),
            TutorialStepDef(
                id = TutorialStepId.AFTER_CAMERA_SHUTTER_GUIDE,
                anchor = AnchorKey.AFTER_CAMERA_OVERLAY,
                messageResId = R.string.tutorial_msg_after_overlay,
                advance = AdvanceCondition.UserAction(TutorialActionIds.AFTER_CAMERA_SHUTTER),
                dimAlpha = TutorialStepDef.CAMERA_DIM_ALPHA,
                actionAnchor = AnchorKey.AFTER_CAMERA_SHUTTER,
                strokeAnchor = false,
            ),
            TutorialStepDef(
                id = TutorialStepId.AFTER_CAMERA_AWAIT_COMPLETION,
                anchor = null,
                messageResId = null,
                advance = AdvanceCondition.UserAction(TutorialActionIds.AFTER_CAMERA_ALL_COMPLETED),
                dimAlpha = 0f,
                actionAnchor = AnchorKey.AFTER_CAMERA_SHUTTER,
            ),
            TutorialStepDef(
                id = TutorialStepId.AFTER_CAMERA_BACK_GUIDE,
                anchor = AnchorKey.CAMERA_BACK_BUTTON,
                messageResId = R.string.tutorial_msg_after_back,
                advance = AdvanceCondition.UserAction(TutorialActionIds.AFTER_CAMERA_BACK_TO_HOME),
            ),
            TutorialStepDef(
                id = TutorialStepId.HOME_SELECTION_MODE_GUIDE,
                anchor = AnchorKey.HOME_PAIR_CARD_FIRST,
                messageResId = R.string.tutorial_msg_home_selection_mode,
                advance = AdvanceCondition.UserAction(TutorialActionIds.HOME_SELECTION_MODE_ENTERED),
            ),
            TutorialStepDef(
                id = TutorialStepId.SELECTION_BAR_SHARE,
                anchor = AnchorKey.HOME_SELECTION_BAR_SHARE,
                messageResId = R.string.tutorial_msg_selection_share,
                advance = AdvanceCondition.TapAnywhere,
                nextButtonLabelResId = R.string.tutorial_button_next,
            ),
            TutorialStepDef(
                id = TutorialStepId.SELECTION_BAR_SAVE,
                anchor = AnchorKey.HOME_SELECTION_BAR_SAVE,
                messageResId = R.string.tutorial_msg_selection_save,
                advance = AdvanceCondition.TapAnywhere,
                nextButtonLabelResId = R.string.tutorial_button_next,
            ),
            TutorialStepDef(
                id = TutorialStepId.SELECTION_BAR_DELETE,
                anchor = AnchorKey.HOME_SELECTION_BAR_DELETE,
                messageResId = R.string.tutorial_msg_selection_delete,
                advance = AdvanceCondition.TapAnywhere,
                nextButtonLabelResId = R.string.tutorial_button_next,
            ),
            TutorialStepDef(
                id = TutorialStepId.SELECTION_BAR_EXPORT_SETTINGS,
                anchor = AnchorKey.HOME_SELECTION_BAR_EXPORT_SETTINGS,
                messageResId = R.string.tutorial_msg_selection_export_settings,
                advance = AdvanceCondition.TapAnywhere,
                nextButtonLabelResId = R.string.tutorial_button_next,
            ),
            TutorialStepDef(
                id = TutorialStepId.SELECTION_BAR_EXIT_GUIDE,
                anchor = AnchorKey.HOME_SELECTION_EXIT_BUTTON,
                messageResId = R.string.tutorial_msg_selection_exit,
                advance = AdvanceCondition.UserAction(TutorialActionIds.HOME_SELECTION_EXITED),
            ),
            TutorialStepDef(
                id = TutorialStepId.SETTINGS_NAVIGATE,
                anchor = AnchorKey.HOME_SETTINGS_BUTTON,
                messageResId = R.string.tutorial_msg_settings_navigate,
                advance = AdvanceCondition.UserAction(TutorialActionIds.HOME_SETTINGS_OPENED),
            ),
            TutorialStepDef(
                id = TutorialStepId.SETTINGS_SCREEN_INFO,
                anchor = null,
                messageResId = R.string.tutorial_msg_settings_screen,
                advance = AdvanceCondition.Manual,
                showSkip = false,
                nextButtonLabelResId = R.string.tutorial_button_finish,
                centerMessage = true,
            ),
        )

    private val exportSettingsOrdered: List<TutorialStepDef> =
        listOf(
            TutorialStepDef(
                id = TutorialStepId.EXPORT_INCLUDE_INFO,
                anchor = AnchorKey.EXPORT_SECTION_INCLUDE,
                messageResId = R.string.tutorial_msg_export_include,
                advance = AdvanceCondition.TapAnywhere,
                showSkip = false,
                nextButtonLabelResId = R.string.tutorial_button_next,
            ),
            TutorialStepDef(
                id = TutorialStepId.EXPORT_FORMAT_INFO,
                anchor = AnchorKey.EXPORT_SECTION_FORMAT,
                messageResId = R.string.tutorial_msg_export_format,
                advance = AdvanceCondition.TapAnywhere,
                showSkip = false,
                nextButtonLabelResId = R.string.tutorial_button_next,
            ),
            TutorialStepDef(
                id = TutorialStepId.EXPORT_WATERMARK_INFO,
                anchor = AnchorKey.EXPORT_SECTION_WATERMARK,
                messageResId = R.string.tutorial_msg_export_watermark,
                advance = AdvanceCondition.TapAnywhere,
                showSkip = false,
                nextButtonLabelResId = R.string.tutorial_button_next,
            ),
            TutorialStepDef(
                id = TutorialStepId.EXPORT_COMBINE_INFO,
                anchor = AnchorKey.EXPORT_SECTION_COMBINE,
                messageResId = R.string.tutorial_msg_export_combine,
                advance = AdvanceCondition.TapAnywhere,
                showSkip = false,
                nextButtonLabelResId = R.string.tutorial_button_next,
            ),
            TutorialStepDef(
                id = TutorialStepId.EXPORT_PRESETS_INFO,
                anchor = AnchorKey.EXPORT_SECTION_PRESETS,
                messageResId = R.string.tutorial_msg_export_presets,
                advance = AdvanceCondition.TapAnywhere,
                showSkip = false,
                nextButtonLabelResId = R.string.tutorial_button_next,
            ),
            TutorialStepDef(
                id = TutorialStepId.EXPORT_PRESET_DEFAULT_CARD_INFO,
                anchor = AnchorKey.EXPORT_PRESET_DEFAULT_CARD,
                messageResId = R.string.tutorial_msg_export_preset_default_card,
                advance = AdvanceCondition.Manual,
                showSkip = false,
                nextButtonLabelResId = R.string.tutorial_button_finish,
            ),
        )

    private val orderedBySection: Map<TutorialSection, List<TutorialStepDef>> =
        mapOf(
            TutorialSection.MAIN_ONBOARDING to mainOrdered,
            TutorialSection.EXPORT_SETTINGS_INTRO to exportSettingsOrdered,
        )

    private val sectionByStepId: Map<TutorialStepId, TutorialSection> =
        orderedBySection
            .flatMap { (section, steps) ->
                steps.map { it.id to section }
            }.toMap()

    private val byId: Map<TutorialStepId, TutorialStepDef> =
        orderedBySection.values.flatten().associateBy { it.id }

    fun get(id: TutorialStepId): TutorialStepDef? = byId[id]

    fun sectionOf(id: TutorialStepId): TutorialSection? = sectionByStepId[id]

    fun firstStepOf(section: TutorialSection): TutorialStepId = orderedBySection.getValue(section).first().id

    fun next(id: TutorialStepId): TutorialStepId {
        val section = sectionByStepId[id] ?: return TutorialStepId.DONE
        val steps = orderedBySection.getValue(section)
        val index = steps.indexOfFirst { it.id == id }
        if (index < 0) return TutorialStepId.DONE
        val nextIndex = index + 1
        return if (nextIndex < steps.size) steps[nextIndex].id else TutorialStepId.DONE
    }

    fun indexOf(id: TutorialStepId): Int {
        val section = sectionByStepId[id] ?: return -1
        return orderedBySection.getValue(section).filter { it.isVisible }.indexOfFirst { it.id == id }
    }

    fun totalOf(section: TutorialSection): Int = orderedBySection.getValue(section).count { it.isVisible }
}
