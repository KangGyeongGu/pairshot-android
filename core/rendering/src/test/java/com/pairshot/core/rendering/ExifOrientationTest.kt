package com.pairshot.core.rendering

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExifOrientationTest {
    @Test
    fun `90 and 270 degrees are portrait`() {
        assertTrue(ExifBitmapLoader.isPortrait(90))
        assertTrue(ExifBitmapLoader.isPortrait(270))
    }

    @Test
    fun `0 and 180 degrees are landscape`() {
        assertFalse(ExifBitmapLoader.isPortrait(0))
        assertFalse(ExifBitmapLoader.isPortrait(180))
    }
}
