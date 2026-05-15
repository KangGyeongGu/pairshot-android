package com.pairshot.core.coupon.domain

sealed interface CouponStatus {
    data object None : CouponStatus

    data class Active(
        val coupon: Coupon,
        val expiresAtEpochMillis: Long?,
    ) : CouponStatus

    data class Expired(
        val coupon: Coupon,
        val expiresAtEpochMillis: Long,
    ) : CouponStatus
}
