package com.pairshot.core.navigation

import kotlinx.serialization.Serializable

@Serializable
data object Home

@Serializable
data class AlbumDetail(
    val albumId: Long,
)

@Serializable
data class PairPicker(
    val albumId: Long,
)

@Serializable
data class PairPreview(
    val pairId: Long,
)

@Serializable
data class ExportSettings(
    val pairIds: String,
)

@Serializable
data class Camera(
    val albumId: Long? = null,
    val replaceBeforeForPairId: Long? = null,
)

@Serializable
data class AfterCamera(
    val initialPairId: Long? = null,
    val albumId: Long? = null,
)

@Serializable
enum class SettingsHighlight {
    WATERMARK,
    COMBINE,
}

@Serializable
data class Settings(
    val highlight: SettingsHighlight? = null,
)

@Serializable
data object WatermarkSettings

@Serializable
data object CombineSettings

@Serializable
data object License

@Serializable
data class Paywall(
    val dismissible: Boolean = true,
    val trigger: PaywallTrigger = PaywallTrigger.NONE,
)

enum class PaywallTrigger { NONE, DAILY_LIMIT, FEATURE_LOCKED }
