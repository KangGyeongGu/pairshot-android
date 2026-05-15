package com.pairshot.core.coupon.data

import com.pairshot.core.coupon.domain.Coupon
import com.pairshot.core.coupon.domain.CouponDuration
import com.pairshot.core.coupon.domain.CouponStatus
import com.pairshot.core.coupon.local.StoredCouponState

object CouponStatusCalculator {
    private const val MILLIS_PER_DAY: Long = 24L * 60L * 60L * 1000L

    fun toStatus(
        stored: StoredCouponState?,
        nowMillis: Long,
    ): CouponStatus {
        if (stored == null) return CouponStatus.None

        val coupon =
            Coupon(
                id = stored.latestCouponId,
                duration =
                    if (stored.expiresAtEpochMillis == null) {
                        CouponDuration.Preset.Unlimited
                    } else {
                        CouponDuration.fromDays(
                            ((stored.expiresAtEpochMillis - nowMillis) / MILLIS_PER_DAY).coerceAtLeast(0L),
                        )
                    },
                activatedAtEpochMillis = stored.firstActivatedAtEpochMillis,
            )

        if (stored.expiresAtEpochMillis == null) {
            return CouponStatus.Active(coupon = coupon, expiresAtEpochMillis = null)
        }
        return if (nowMillis < stored.expiresAtEpochMillis) {
            CouponStatus.Active(coupon = coupon, expiresAtEpochMillis = stored.expiresAtEpochMillis)
        } else {
            CouponStatus.Expired(coupon = coupon)
        }
    }

    fun isActive(
        stored: StoredCouponState?,
        nowMillis: Long,
    ): Boolean = toStatus(stored, nowMillis) is CouponStatus.Active

    fun expiresAt(
        stored: StoredCouponState?,
        nowMillis: Long,
    ): Long? = (toStatus(stored, nowMillis) as? CouponStatus.Active)?.expiresAtEpochMillis
}
