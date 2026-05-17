package com.pairshot.core.domain.membership

data class Membership(
    val isPro: Boolean,
    val isAdFree: Boolean,
    val proExpiresAtEpochMillis: Long?,
    val adFreeExpiresAtEpochMillis: Long?,
) {
    companion object {
        val Free: Membership =
            Membership(
                isPro = false,
                isAdFree = false,
                proExpiresAtEpochMillis = null,
                adFreeExpiresAtEpochMillis = null,
            )
    }
}
