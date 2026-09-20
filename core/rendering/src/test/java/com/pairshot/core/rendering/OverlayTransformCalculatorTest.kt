package com.pairshot.core.rendering

import org.junit.Assert.assertEquals
import org.junit.Test

class OverlayTransformCalculatorTest {
    @Test
    fun `portrait pixels with exif normal needs no rotation`() {
        assertEquals(0f, OverlayTransformCalculator.fromImageOrientation(3024, 4032, 0))
    }

    @Test
    fun `landscape pixels with exif normal rotates left`() {
        assertEquals(90f, OverlayTransformCalculator.fromImageOrientation(4032, 3024, 0))
    }

    @Test
    fun `landscape buffer with exif quarter turns is portrait shot`() {
        assertEquals(0f, OverlayTransformCalculator.fromImageOrientation(4032, 3024, 90))
        assertEquals(0f, OverlayTransformCalculator.fromImageOrientation(4032, 3024, 270))
    }

    @Test
    fun `landscape buffer with exif half turn rotates right`() {
        assertEquals(270f, OverlayTransformCalculator.fromImageOrientation(4032, 3024, 180))
    }

    @Test
    fun `portrait pixels with exif half turn needs no rotation`() {
        assertEquals(0f, OverlayTransformCalculator.fromImageOrientation(3024, 4032, 180))
    }

    @Test
    fun `portrait buffer with exif quarter turns is landscape shot rotating left`() {
        assertEquals(90f, OverlayTransformCalculator.fromImageOrientation(3024, 4032, 90))
        assertEquals(90f, OverlayTransformCalculator.fromImageOrientation(3024, 4032, 270))
    }

    @Test
    fun `square image needs no rotation`() {
        assertEquals(0f, OverlayTransformCalculator.fromImageOrientation(2000, 2000, 0))
    }

    @Test
    fun `exif only fallback maps quarter to portrait and half to right`() {
        assertEquals(0f, OverlayTransformCalculator.fromExifOnly(90))
        assertEquals(0f, OverlayTransformCalculator.fromExifOnly(270))
        assertEquals(270f, OverlayTransformCalculator.fromExifOnly(180))
        assertEquals(90f, OverlayTransformCalculator.fromExifOnly(0))
    }
}
