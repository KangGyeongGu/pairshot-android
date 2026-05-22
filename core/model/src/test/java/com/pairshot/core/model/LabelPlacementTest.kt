package com.pairshot.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class LabelPlacementTest {
    @Test
    fun `fromName returns matching enum when name is known`() {
        assertEquals(LabelPlacement.INSIDE_IMAGE, LabelPlacement.fromName("INSIDE_IMAGE"))
        assertEquals(LabelPlacement.INSIDE_BORDER, LabelPlacement.fromName("INSIDE_BORDER"))
    }

    @Test
    fun `fromName falls back to DEFAULT for null or unknown names`() {
        assertEquals(LabelPlacement.DEFAULT, LabelPlacement.fromName(null))
        assertEquals(LabelPlacement.DEFAULT, LabelPlacement.fromName(""))
        assertEquals(LabelPlacement.DEFAULT, LabelPlacement.fromName("UNKNOWN_VALUE"))
    }

    @Test
    fun `DEFAULT is INSIDE_IMAGE so existing users keep legacy behavior`() {
        assertEquals(LabelPlacement.INSIDE_IMAGE, LabelPlacement.DEFAULT)
    }
}

class LabelAnchorCoercionTest {
    @Test
    fun `coercedForBorderPlacement maps every MIDDLE row to BOTTOM preserving horizontal`() {
        assertEquals(LabelAnchor.BOTTOM_LEFT, LabelAnchor.MIDDLE_LEFT.coercedForBorderPlacement())
        assertEquals(LabelAnchor.BOTTOM_CENTER, LabelAnchor.MIDDLE_CENTER.coercedForBorderPlacement())
        assertEquals(LabelAnchor.BOTTOM_RIGHT, LabelAnchor.MIDDLE_RIGHT.coercedForBorderPlacement())
    }

    @Test
    fun `coercedForBorderPlacement is identity for non-MIDDLE anchors`() {
        val nonMiddle =
            LabelAnchor.entries.filterNot {
                it == LabelAnchor.MIDDLE_LEFT || it == LabelAnchor.MIDDLE_CENTER || it == LabelAnchor.MIDDLE_RIGHT
            }
        nonMiddle.forEach { anchor ->
            assertEquals(
                "$anchor should be unchanged",
                anchor,
                anchor.coercedForBorderPlacement(),
            )
        }
    }

    @Test
    fun `coercedForBorderPlacement is idempotent`() {
        LabelAnchor.entries.forEach { anchor ->
            val once = anchor.coercedForBorderPlacement()
            val twice = once.coercedForBorderPlacement()
            assertEquals(once, twice)
        }
    }
}

class CombineConfigWithLabelPlacementTest {
    @Test
    fun `withLabelPlacement to INSIDE_BORDER forces border on`() {
        val base = CombineConfig(borderEnabled = false)
        val result = base.withLabelPlacement(LabelPlacement.INSIDE_BORDER)
        assertTrue(
            "switching to INSIDE_BORDER must enable the border, otherwise the label has no surface to draw on",
            result.borderEnabled,
        )
        assertEquals(LabelPlacement.INSIDE_BORDER, result.labelPlacement)
    }

    @Test
    fun `withLabelPlacement to INSIDE_BORDER coerces MIDDLE anchors to BOTTOM`() {
        val base =
            CombineConfig(
                beforeLabelAnchor = LabelAnchor.MIDDLE_LEFT,
                afterLabelAnchor = LabelAnchor.MIDDLE_RIGHT,
            )
        val result = base.withLabelPlacement(LabelPlacement.INSIDE_BORDER)
        assertEquals(LabelAnchor.BOTTOM_LEFT, result.beforeLabelAnchor)
        assertEquals(LabelAnchor.BOTTOM_RIGHT, result.afterLabelAnchor)
    }

    @Test
    fun `withLabelPlacement to INSIDE_BORDER preserves non-MIDDLE anchors`() {
        val base =
            CombineConfig(
                beforeLabelAnchor = LabelAnchor.TOP_LEFT,
                afterLabelAnchor = LabelAnchor.BOTTOM_RIGHT,
            )
        val result = base.withLabelPlacement(LabelPlacement.INSIDE_BORDER)
        assertEquals(LabelAnchor.TOP_LEFT, result.beforeLabelAnchor)
        assertEquals(LabelAnchor.BOTTOM_RIGHT, result.afterLabelAnchor)
    }

    @Test
    fun `withLabelPlacement to INSIDE_IMAGE is plain copy and does not touch border`() {
        val base =
            CombineConfig(
                borderEnabled = false,
                beforeLabelAnchor = LabelAnchor.MIDDLE_CENTER,
            )
        val result = base.withLabelPlacement(LabelPlacement.INSIDE_IMAGE)
        assertEquals(LabelPlacement.INSIDE_IMAGE, result.labelPlacement)
        assertEquals(
            "switching back to INSIDE_IMAGE must not silently enable the border",
            false,
            result.borderEnabled,
        )
        assertEquals(
            "MIDDLE anchors must survive a round-trip from INSIDE_IMAGE -> INSIDE_BORDER -> INSIDE_IMAGE",
            LabelAnchor.MIDDLE_CENTER,
            result.beforeLabelAnchor,
        )
    }

    @Test
    fun `withLabelPlacement returns a different instance than input but same when no-op equivalent`() {
        val base = CombineConfig()
        val unchanged = base.withLabelPlacement(LabelPlacement.INSIDE_IMAGE)
        assertEquals(base, unchanged)
        assertNotSame("data class copy always returns a new instance", base, unchanged)
    }

    @Test
    fun `enum DEFAULT constants are stable singletons`() {
        assertSame(LabelPlacement.INSIDE_IMAGE, LabelPlacement.DEFAULT)
        assertSame(LabelAnchor.TOP_LEFT, LabelAnchor.DEFAULT)
    }
}
