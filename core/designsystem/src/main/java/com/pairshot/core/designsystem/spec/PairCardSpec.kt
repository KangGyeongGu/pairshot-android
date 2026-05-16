package com.pairshot.core.designsystem.spec

import com.pairshot.core.designsystem.PairShotSpacing
import com.pairshot.core.designsystem.PairShotStroke

/**
 * Pair card invariants — referenced from feature/home (HomePairGridSection),
 * feature/album (AlbumPairGridSection, PairPickerGridSection), and core/ui (PairCard).
 *
 * These define the visual identity of pair cards across the app.
 */
object PairCardSpec {
    /** Card aspect ratio (width / height) — 3:2 horizontal. Before/After slots split 50:50 inside. */
    const val ASPECT_RATIO: Float = 1.5f

    /** Number of columns in any pair-card grid. Always 2 across home/album/picker. */
    const val GRID_COLUMNS: Int = 2

    /** Spacing between cells in the grid. */
    val cellSpacing = PairShotSpacing.sm

    /** Stroke width when card is in selected state. */
    val selectionBorderWidth = PairShotStroke.thin

    /** Background overlay alpha applied to a selected card. */
    const val SELECTION_OVERLAY_ALPHA: Float = 0.15f

    /** Scrim alpha for cards in a disabled / already-added state (PairPicker). */
    const val DISABLED_SCRIM_ALPHA: Float = 0.45f
}
