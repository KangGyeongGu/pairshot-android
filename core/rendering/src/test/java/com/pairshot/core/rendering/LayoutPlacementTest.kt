package com.pairshot.core.rendering

import com.pairshot.core.model.CombineLayout
import com.pairshot.core.rendering.internal.BorderInsets
import com.pairshot.core.rendering.internal.CropRegion
import com.pairshot.core.rendering.internal.LayoutPlacement
import com.pairshot.core.rendering.internal.calculatePlacement
import com.pairshot.core.rendering.internal.centerCropRegion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CalculatePlacementTest {
    private val noInsets = BorderInsets.uniform(0)

    @Test
    fun `HORIZONTAL uses min width and min height as the equal cell, cells sit side by side`() {
        val placement =
            calculatePlacement(
                beforeWidth = 1920,
                beforeHeight = 1080,
                afterWidth = 1080,
                afterHeight = 1920,
                layout = CombineLayout.HORIZONTAL,
                insets = noInsets,
            )
        assertEquals(
            LayoutPlacement(
                canvasWidth = 2160,
                canvasHeight = 1080,
                cellWidth = 1080,
                cellHeight = 1080,
                beforeLeft = 0,
                beforeTop = 0,
                afterLeft = 1080,
                afterTop = 0,
            ),
            placement,
        )
    }

    @Test
    fun `VERTICAL uses min width and min height as the equal cell, cells stack vertically`() {
        val placement =
            calculatePlacement(
                beforeWidth = 1920,
                beforeHeight = 1080,
                afterWidth = 1080,
                afterHeight = 1920,
                layout = CombineLayout.VERTICAL,
                insets = noInsets,
            )
        assertEquals(
            LayoutPlacement(
                canvasWidth = 1080,
                canvasHeight = 2160,
                cellWidth = 1080,
                cellHeight = 1080,
                beforeLeft = 0,
                beforeTop = 0,
                afterLeft = 0,
                afterTop = 1080,
            ),
            placement,
        )
    }

    @Test
    fun `cell width and height can come from different photos (min per axis)`() {
        val placement =
            calculatePlacement(
                beforeWidth = 1600,
                beforeHeight = 1200,
                afterWidth = 1080,
                afterHeight = 1920,
                layout = CombineLayout.HORIZONTAL,
                insets = noInsets,
            )
        // min width = 1080 (after), min height = 1200 (before)
        assertEquals(1080, placement.cellWidth)
        assertEquals(1200, placement.cellHeight)
    }

    @Test
    fun `border insets expand canvas and offset cells without changing cell size`() {
        val insets = BorderInsets(left = 10, top = 20, right = 30, bottom = 40, centerGap = 50)
        val placement =
            calculatePlacement(
                beforeWidth = 1920,
                beforeHeight = 1080,
                afterWidth = 1080,
                afterHeight = 1920,
                layout = CombineLayout.HORIZONTAL,
                insets = insets,
            )
        assertEquals(2250, placement.canvasWidth) // 1080*2 + 10 + 30 + 50
        assertEquals(1140, placement.canvasHeight) // 1080 + 20 + 40
        assertEquals(1080, placement.cellWidth)
        assertEquals(10, placement.beforeLeft)
        assertEquals(20, placement.beforeTop)
        assertEquals(1140, placement.afterLeft) // 10 + 1080 + 50
        assertEquals(20, placement.afterTop)
    }
}

class CenterCropRegionTest {
    @Test
    fun `landscape source into square cell crops width, centered`() {
        val region = centerCropRegion(srcWidth = 1920, srcHeight = 1080, cellWidth = 1080, cellHeight = 1080)
        assertEquals(CropRegion(left = 420, top = 0, right = 1500, bottom = 1080), region)
    }

    @Test
    fun `portrait source into square cell crops height, centered`() {
        val region = centerCropRegion(srcWidth = 1080, srcHeight = 1920, cellWidth = 1080, cellHeight = 1080)
        assertEquals(CropRegion(left = 0, top = 420, right = 1080, bottom = 1500), region)
    }

    @Test
    fun `source matching the cell aspect ratio is taken whole`() {
        val region = centerCropRegion(srcWidth = 1000, srcHeight = 1000, cellWidth = 500, cellHeight = 500)
        assertEquals(CropRegion(left = 0, top = 0, right = 1000, bottom = 1000), region)
    }

    @Test
    fun `crop region never exceeds the source (no upscaling)`() {
        val region = centerCropRegion(srcWidth = 4000, srcHeight = 3000, cellWidth = 1080, cellHeight = 1920)
        assertTrue("region width within source", region.right - region.left <= 4000)
        assertTrue("region height within source", region.bottom - region.top <= 3000)
        assertTrue("left within bounds", region.left >= 0)
        assertTrue("top within bounds", region.top >= 0)
    }
}
