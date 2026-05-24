package com.pairshot.arch

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.verify.assertTrue
import org.junit.jupiter.api.Test

private const val MAX_FILE_LINES = 600

class KonsistFileSizeTest {
    @Test
    fun `K-01 production kt files must not exceed max line count`() {
        Konsist
            .scopeFromProduction()
            .files
            .assertTrue { file -> file.text.lines().size <= MAX_FILE_LINES }
    }
}
