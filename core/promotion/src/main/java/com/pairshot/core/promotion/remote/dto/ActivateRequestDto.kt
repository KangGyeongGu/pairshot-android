package com.pairshot.core.promotion.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ActivateRequestDto(
    val code: String,
    val device: String,
)
