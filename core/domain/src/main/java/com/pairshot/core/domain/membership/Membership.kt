package com.pairshot.core.domain.membership

data class Membership(
    val isPro: Boolean,
    val proExpiresAtEpochMillis: Long?,
) {
    companion object {
        val Free: Membership =
            Membership(
                isPro = false,
                proExpiresAtEpochMillis = null,
            )
    }
}
