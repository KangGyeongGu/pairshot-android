package com.pairshot.feature.settings.component

import com.pairshot.core.model.LabelAnchor

internal val labelAnchorOrder =
    listOf(
        LabelAnchor.TOP_LEFT,
        LabelAnchor.TOP_CENTER,
        LabelAnchor.TOP_RIGHT,
        LabelAnchor.MIDDLE_LEFT,
        LabelAnchor.MIDDLE_CENTER,
        LabelAnchor.MIDDLE_RIGHT,
        LabelAnchor.BOTTOM_LEFT,
        LabelAnchor.BOTTOM_CENTER,
        LabelAnchor.BOTTOM_RIGHT,
    )

internal val borderLabelAnchorOrder =
    labelAnchorOrder.filterNot {
        it == LabelAnchor.MIDDLE_LEFT || it == LabelAnchor.MIDDLE_CENTER || it == LabelAnchor.MIDDLE_RIGHT
    }
