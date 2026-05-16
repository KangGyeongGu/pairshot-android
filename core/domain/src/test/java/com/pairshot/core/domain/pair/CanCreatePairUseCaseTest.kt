package com.pairshot.core.domain.pair

import com.pairshot.core.domain.membership.Membership
import com.pairshot.core.domain.membership.MembershipProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class CanCreatePairUseCaseTest {
    private val repository: PhotoPairRepository = mockk()
    private val membership: MembershipProvider = mockk()
    private val useCase = CanCreatePairUseCase(repository, membership)

    @Test
    fun `pro user can always create pair and repository is not queried`() =
        runTest {
            coEvery { membership.current() } returns proMembership()
            val result = useCase()
            assertTrue(result is CanCreatePairUseCase.Result.Allowed)
            coVerify(exactly = 0) { repository.countCreatedSince(any()) }
        }

    @Test
    fun `ad-free-only user is still limited (ad_free does not grant unlimited pairs)`() =
        runTest {
            coEvery { membership.current() } returns adFreeOnlyMembership()
            every { repository.countCreatedSince(any()) } returns flowOf(CanCreatePairUseCase.FREE_DAILY_LIMIT)
            val result = useCase() as CanCreatePairUseCase.Result.LimitReached
            assertEquals(CanCreatePairUseCase.FREE_DAILY_LIMIT, result.limit)
        }

    @Test
    fun `free user under daily quota can create pair`() =
        runTest {
            coEvery { membership.current() } returns Membership.Free
            val capturedSince = slot<Long>()
            every { repository.countCreatedSince(capture(capturedSince)) } returns flowOf(3)
            val result = useCase()
            assertTrue(result is CanCreatePairUseCase.Result.Allowed)
            val expectedTodayStart =
                LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            assertEquals(expectedTodayStart, capturedSince.captured)
        }

    @Test
    fun `free user at exact daily limit is rejected with limit value populated`() =
        runTest {
            coEvery { membership.current() } returns Membership.Free
            every { repository.countCreatedSince(any()) } returns flowOf(CanCreatePairUseCase.FREE_DAILY_LIMIT)
            val result = useCase() as CanCreatePairUseCase.Result.LimitReached
            assertEquals(CanCreatePairUseCase.FREE_DAILY_LIMIT, result.current)
            assertEquals(CanCreatePairUseCase.FREE_DAILY_LIMIT, result.limit)
        }

    @Test
    fun `over-quota free user is rejected with current count propagated`() =
        runTest {
            coEvery { membership.current() } returns Membership.Free
            val overshoot = CanCreatePairUseCase.FREE_DAILY_LIMIT + 3
            every { repository.countCreatedSince(any()) } returns flowOf(overshoot)
            val result = useCase() as CanCreatePairUseCase.Result.LimitReached
            assertEquals(overshoot, result.current)
            assertEquals(CanCreatePairUseCase.FREE_DAILY_LIMIT, result.limit)
        }

    @Test
    fun `free user just below limit returns Allowed at boundary`() =
        runTest {
            coEvery { membership.current() } returns Membership.Free
            every { repository.countCreatedSince(any()) } returns flowOf(CanCreatePairUseCase.FREE_DAILY_LIMIT - 1)
            val result = useCase()
            assertTrue(result is CanCreatePairUseCase.Result.Allowed)
        }

    private fun proMembership(): Membership =
        Membership(
            isPro = true,
            isAdFree = true,
            proExpiresAtEpochMillis = null,
            adFreeExpiresAtEpochMillis = null,
        )

    private fun adFreeOnlyMembership(): Membership =
        Membership(
            isPro = false,
            isAdFree = true,
            proExpiresAtEpochMillis = null,
            adFreeExpiresAtEpochMillis = null,
        )
}
