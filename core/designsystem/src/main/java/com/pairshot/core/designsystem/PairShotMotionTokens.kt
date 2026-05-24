package com.pairshot.core.designsystem

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween

object PairShotMotionTokens {
    val EasingEnter: Easing = LinearOutSlowInEasing

    val EasingExit: Easing = FastOutLinearInEasing

    const val DURATION_ENTER = 220

    const val DELAY_ENTER = 130

    const val DURATION_EXIT = 180

    const val DURATION_POP_ENTER = 210

    const val DELAY_POP_ENTER = 120

    const val DURATION_POP_EXIT = 170

    const val DURATION_PANEL_ENTER = 200

    const val DURATION_PANEL_EXIT = 160

    fun <T> enterTween() = tween<T>(durationMillis = DURATION_ENTER, delayMillis = DELAY_ENTER, easing = EasingEnter)

    fun <T> exitTween() = tween<T>(durationMillis = DURATION_EXIT, easing = EasingExit)

    fun <T> popEnterTween() = tween<T>(
        durationMillis = DURATION_POP_ENTER,
        delayMillis = DELAY_POP_ENTER,
        easing = EasingEnter
    )

    fun <T> popExitTween() = tween<T>(durationMillis = DURATION_POP_EXIT, easing = EasingExit)

    fun <T> panelEnterTween() = tween<T>(durationMillis = DURATION_PANEL_ENTER, easing = EasingEnter)

    fun <T> panelExitTween() = tween<T>(durationMillis = DURATION_PANEL_EXIT, easing = EasingExit)
}
