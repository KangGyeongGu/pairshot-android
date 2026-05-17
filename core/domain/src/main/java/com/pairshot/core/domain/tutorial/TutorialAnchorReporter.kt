package com.pairshot.core.domain.tutorial

data class AnchorBounds(
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float,
)

interface TutorialAnchorReporter {
    fun report(
        key: AnchorKey,
        bounds: AnchorBounds?,
    )
}
