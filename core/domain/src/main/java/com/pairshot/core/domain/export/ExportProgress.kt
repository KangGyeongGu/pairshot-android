package com.pairshot.core.domain.export

sealed interface ExportProgress {
    val current: Int
    val total: Int

    data class Rendering(
        override val current: Int,
        override val total: Int,
    ) : ExportProgress

    data class Compressing(
        override val current: Int,
        override val total: Int,
    ) : ExportProgress
}
