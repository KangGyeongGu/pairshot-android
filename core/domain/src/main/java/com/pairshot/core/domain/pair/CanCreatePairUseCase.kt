package com.pairshot.core.domain.pair

import com.pairshot.core.domain.membership.MembershipProvider
import com.pairshot.core.domain.tutorial.TutorialModeProvider
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

class CanCreatePairUseCase
@Inject
constructor(
    private val photoPairRepository: PhotoPairRepository,
    private val membershipProvider: MembershipProvider,
    private val tutorialModeProvider: TutorialModeProvider,
) {
    suspend operator fun invoke(): Result {
        if (tutorialModeProvider.isActive.value) return Result.Allowed
        val membership = membershipProvider.current()
        if (membership.isPro) return Result.Allowed
        val sinceEpochMs = todayStartEpochMs()
        val createdToday = photoPairRepository.countCreatedSince(sinceEpochMs).first()
        return if (createdToday < FREE_DAILY_LIMIT) {
            Result.Allowed
        } else {
            Result.LimitReached(createdToday, FREE_DAILY_LIMIT)
        }
    }

    sealed interface Result {
        data object Allowed : Result

        data class LimitReached(
            val current: Int,
            val limit: Int,
        ) : Result
    }

    companion object {
        const val FREE_DAILY_LIMIT = 5

        private fun todayStartEpochMs(): Long =
            LocalDate
                .now()
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
    }
}
