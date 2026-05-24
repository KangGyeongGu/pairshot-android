package com.pairshot.core.rendering

import com.pairshot.core.model.CombineConfig
import com.pairshot.core.model.CombineLayout
import com.pairshot.core.model.LabelAnchor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private const val REF_DIM = 3000

class ResolveLabelFontSizeTest {
    @Test
    fun `font size scales linearly with reference dimension and ratio`() {
        val config = CombineConfig(labelSizeRatio = 0.05f)
        assertEquals(150f, resolveLabelFontSize(config, REF_DIM), 0.001f)
    }

    @Test
    fun `font size never drops below LABEL_MIN_FONT_PX when ratio is tiny`() {
        val config = CombineConfig(labelSizeRatio = 0f)
        val fontSize = resolveLabelFontSize(config, REF_DIM)
        assertTrue("expected >= 10f, got $fontSize", fontSize >= 10f)
    }

    @Test
    fun `font size never drops below LABEL_MIN_FONT_PX when reference dim is tiny`() {
        val config = CombineConfig(labelSizeRatio = 0.05f)
        val fontSize = resolveLabelFontSize(config, 1)
        assertTrue("expected >= 10f, got $fontSize", fontSize >= 10f)
    }
}

class ResolveBorderLabelEdgePxTest {
    @Test
    fun `edge px includes the rect height plus vertical padding on both sides`() {
        val config = CombineConfig(labelSizeRatio = 0.05f)
        val fontSize = resolveLabelFontSize(config, REF_DIM)
        val expected = (fontSize * 1.6f + fontSize * 0.4f * 2).toInt()
        assertEquals(expected, resolveBorderLabelEdgePx(config, REF_DIM))
    }

    @Test
    fun `edge px is positive even with zero ratio (min font kicks in)`() {
        val config = CombineConfig(labelSizeRatio = 0f)
        assertTrue(resolveBorderLabelEdgePx(config, REF_DIM) > 0)
    }
}

class ToBorderVerticalAlignmentTest {
    @Test
    fun `TOP anchors stay TOP`() {
        listOf(LabelAnchor.TOP_LEFT, LabelAnchor.TOP_CENTER, LabelAnchor.TOP_RIGHT).forEach {
            assertEquals(VerticalAlignment.TOP, it.toBorderVerticalAlignment())
        }
    }

    @Test
    fun `BOTTOM anchors stay BOTTOM`() {
        listOf(LabelAnchor.BOTTOM_LEFT, LabelAnchor.BOTTOM_CENTER, LabelAnchor.BOTTOM_RIGHT).forEach {
            assertEquals(VerticalAlignment.BOTTOM, it.toBorderVerticalAlignment())
        }
    }

    @Test
    fun `MIDDLE anchors collapse to BOTTOM (safety net for unexpected data)`() {
        listOf(LabelAnchor.MIDDLE_LEFT, LabelAnchor.MIDDLE_CENTER, LabelAnchor.MIDDLE_RIGHT).forEach {
            assertEquals(VerticalAlignment.BOTTOM, it.toBorderVerticalAlignment())
        }
    }
}

class ComputeBorderRegionTest {
    private val image = LabelRect(left = 100, top = 200, width = 1000, height = 800)
    private val insets = BorderInsets(left = 50, top = 60, right = 50, bottom = 70, centerGap = 80)

    @Test
    fun `HORIZONTAL TOP anchor places region above image, spanning image width`() {
        val region = computeBorderRegion(
            image,
            LabelAnchor.TOP_CENTER,
            CombineLayout.HORIZONTAL,
            insets,
            isBefore = true
        )
        assertEquals(LabelRect(left = 100, top = 0, width = 1000, height = 60), region)
    }

    @Test
    fun `HORIZONTAL BOTTOM anchor places region just below image bottom`() {
        val region = computeBorderRegion(
            image,
            LabelAnchor.BOTTOM_LEFT,
            CombineLayout.HORIZONTAL,
            insets,
            isBefore = false
        )
        assertEquals(LabelRect(left = 100, top = 1000, width = 1000, height = 70), region)
    }

    @Test
    fun `VERTICAL BEFORE TOP attaches to canvas top, height equals top inset`() {
        val region = computeBorderRegion(image, LabelAnchor.TOP_CENTER, CombineLayout.VERTICAL, insets, isBefore = true)
        assertEquals(LabelRect(left = 100, top = 0, width = 1000, height = 60), region)
    }

    @Test
    fun `VERTICAL BEFORE BOTTOM lands in center gap (between the two images)`() {
        val region = computeBorderRegion(
            image,
            LabelAnchor.BOTTOM_LEFT,
            CombineLayout.VERTICAL,
            insets,
            isBefore = true
        )
        assertEquals(LabelRect(left = 100, top = 1000, width = 1000, height = 80), region)
    }

    @Test
    fun `VERTICAL AFTER TOP also lands in center gap, just above the after image`() {
        val region = computeBorderRegion(image, LabelAnchor.TOP_RIGHT, CombineLayout.VERTICAL, insets, isBefore = false)
        assertEquals(LabelRect(left = 100, top = 120, width = 1000, height = 80), region)
    }

    @Test
    fun `VERTICAL AFTER BOTTOM attaches to canvas bottom, height equals bottom inset`() {
        val region = computeBorderRegion(
            image,
            LabelAnchor.BOTTOM_CENTER,
            CombineLayout.VERTICAL,
            insets,
            isBefore = false
        )
        assertEquals(LabelRect(left = 100, top = 1000, width = 1000, height = 70), region)
    }

    @Test
    fun `MIDDLE anchor falls back to BOTTOM behavior (asymmetric safety with toBorderVerticalAlignment)`() {
        val asTopAnchor = computeBorderRegion(
            image,
            LabelAnchor.TOP_LEFT,
            CombineLayout.HORIZONTAL,
            insets,
            isBefore = true
        )
        val asMiddleAnchor = computeBorderRegion(
            image,
            LabelAnchor.MIDDLE_LEFT,
            CombineLayout.HORIZONTAL,
            insets,
            isBefore = true
        )
        val asBottomAnchor = computeBorderRegion(
            image,
            LabelAnchor.BOTTOM_LEFT,
            CombineLayout.HORIZONTAL,
            insets,
            isBefore = true
        )
        assertEquals(
            "MIDDLE should collapse to BOTTOM, not TOP",
            asBottomAnchor,
            asMiddleAnchor,
        )
        assertTrue("sanity: TOP and BOTTOM should differ", asTopAnchor != asBottomAnchor)
    }
}

class ComputeSingleBorderRegionTest {
    private val image = LabelRect(left = 40, top = 50, width = 600, height = 400)
    private val insets = BorderInsets(left = 40, top = 50, right = 40, bottom = 90, centerGap = 40)

    @Test
    fun `TOP anchor lands above the image`() {
        val region = computeSingleBorderRegion(image, LabelAnchor.TOP_CENTER, insets)
        assertEquals(LabelRect(left = 40, top = 0, width = 600, height = 50), region)
    }

    @Test
    fun `BOTTOM anchor lands just below the image`() {
        val region = computeSingleBorderRegion(image, LabelAnchor.BOTTOM_RIGHT, insets)
        assertEquals(LabelRect(left = 40, top = 450, width = 600, height = 90), region)
    }

    @Test
    fun `MIDDLE anchor collapses to BOTTOM (consistent with toBorderVerticalAlignment)`() {
        val middle = computeSingleBorderRegion(image, LabelAnchor.MIDDLE_LEFT, insets)
        val bottom = computeSingleBorderRegion(image, LabelAnchor.BOTTOM_LEFT, insets)
        assertEquals(bottom, middle)
    }
}
