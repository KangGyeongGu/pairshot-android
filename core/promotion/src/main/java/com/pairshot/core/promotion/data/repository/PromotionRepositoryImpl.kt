package com.pairshot.core.promotion.data.repository

import com.pairshot.core.promotion.domain.ActivationResult
import com.pairshot.core.promotion.domain.Promotion
import com.pairshot.core.promotion.domain.PromotionEntitlement
import com.pairshot.core.promotion.domain.PromotionRepository
import com.pairshot.core.promotion.domain.PromotionState
import com.pairshot.core.promotion.domain.PromotionStatus
import com.pairshot.core.promotion.local.DeviceHashProvider
import com.pairshot.core.promotion.local.PromotionPreferencesSource
import com.pairshot.core.promotion.remote.ActivationApiResult
import com.pairshot.core.promotion.remote.MembershipApiResult
import com.pairshot.core.promotion.remote.PromotionApi
import com.pairshot.core.promotion.remote.dto.ActivateRequestDto
import com.pairshot.core.promotion.remote.dto.EntitlementInfoDto
import com.pairshot.core.promotion.remote.dto.MembershipDto
import com.pairshot.core.promotion.remote.dto.MembershipPromotionDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import timber.log.Timber
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PromotionRepositoryImpl
    @Inject
    constructor(
        private val api: PromotionApi,
        private val preferences: PromotionPreferencesSource,
        private val deviceHashProvider: DeviceHashProvider,
    ) : PromotionRepository {
        override fun observe(): Flow<PromotionState> = preferences.state

        override suspend fun refresh() {
            val deviceHash = deviceHashProvider.deviceHash()
            when (val result = api.fetchMembership(deviceHash)) {
                is MembershipApiResult.Success -> {
                    preferences.save(result.membership.toDomain())
                }

                MembershipApiResult.NetworkError,
                MembershipApiResult.ServerError,
                -> Unit
            }
        }

        override suspend fun activate(code: String): ActivationResult {
            val trimmed = code.trim()
            if (trimmed.isEmpty()) return ActivationResult.Failure.InvalidFormat

            preferences.savePending(trimmed, System.currentTimeMillis())

            val deviceHash = deviceHashProvider.deviceHash()
            val apiResult = api.activate(ActivateRequestDto(code = trimmed, device = deviceHash))

            return when (apiResult) {
                is ActivationApiResult.Success -> {
                    preferences.clearPending()
                    preferences.save(apiResult.response.membership.toDomain())
                    val promotion = apiResult.response.promotion.toDomainOrNull()
                    if (promotion == null) {
                        Timber.tag(TAG).w(
                            "activate response promotion malformed — entitlement=%s status=%s",
                            apiResult.response.promotion.entitlement,
                            apiResult.response.promotion.status,
                        )
                        ActivationResult.Failure.UnknownError
                    } else {
                        ActivationResult.Success(promotion)
                    }
                }

                ActivationApiResult.InvalidCodeFormat -> {
                    preferences.clearPending()
                    ActivationResult.Failure.InvalidFormat
                }

                ActivationApiResult.InvalidSignature -> {
                    preferences.clearPending()
                    ActivationResult.Failure.InvalidSignature
                }

                ActivationApiResult.NotFound -> {
                    preferences.clearPending()
                    ActivationResult.Failure.NotFound
                }

                ActivationApiResult.AlreadyUsedOnAnotherDevice -> {
                    preferences.clearPending()
                    ActivationResult.Failure.AlreadyUsedOnAnotherDevice
                }

                ActivationApiResult.Revoked -> {
                    preferences.clearPending()
                    ActivationResult.Failure.Revoked
                }

                ActivationApiResult.NetworkError -> ActivationResult.Failure.NetworkError

                ActivationApiResult.ServerError -> ActivationResult.Failure.UnknownError
            }
        }

        override suspend fun retryPendingIfAny() {
            val pending = preferences.pending.first() ?: return
            if (System.currentTimeMillis() - pending.sinceEpochMillis > PENDING_EXPIRY_MS) {
                preferences.clearPending()
                return
            }
            activate(pending.code)
        }

        override suspend fun clear() {
            preferences.clear()
        }

        private companion object {
            const val PENDING_EXPIRY_MS: Long = 7L * 24L * 60L * 60L * 1000L
            const val TAG = "PromotionRepo"
        }
    }

internal fun MembershipDto.toDomain(): PromotionState =
    PromotionState(
        proActive = pro.active,
        proExpiresAtEpochMillis = pro.parseExpiry(),
        adFreeActive = adFree.active,
        adFreeExpiresAtEpochMillis = adFree.parseExpiry(),
        promotions = promotions.mapNotNull { it.toDomainOrNull() },
    )

internal fun EntitlementInfoDto.parseExpiry(): Long? = expiresAt?.let { parseIsoMillisOrNull(it) }

internal fun MembershipPromotionDto.toDomainOrNull(): Promotion? {
    val parsedEntitlement = parseEntitlement(entitlement)
    val parsedStatus = parseStatus(status)
    val activatedMillis = parseIsoMillisOrNull(activatedAt)
    if (parsedEntitlement == null || parsedStatus == null || activatedMillis == null) return null
    return Promotion(
        id = id,
        entitlement = parsedEntitlement,
        durationDays = durationDays,
        activatedAtEpochMillis = activatedMillis,
        expiresAtEpochMillis = expiresAt?.let { parseIsoMillisOrNull(it) },
        status = parsedStatus,
        shortCode = shortCode,
        batchLabel = batchLabel,
    )
}

private fun parseEntitlement(raw: String): PromotionEntitlement? =
    when (raw) {
        "pro" -> PromotionEntitlement.PRO
        "ad_free" -> PromotionEntitlement.AD_FREE
        else -> null
    }

private fun parseStatus(raw: String): PromotionStatus? =
    when (raw) {
        "unused" -> PromotionStatus.UNUSED
        "activated" -> PromotionStatus.ACTIVATED
        "expired" -> PromotionStatus.EXPIRED
        "revoked" -> PromotionStatus.REVOKED
        else -> null
    }

private fun parseIsoMillisOrNull(iso: String): Long? =
    runCatching { Instant.parse(iso).toEpochMilli() }.getOrNull()
