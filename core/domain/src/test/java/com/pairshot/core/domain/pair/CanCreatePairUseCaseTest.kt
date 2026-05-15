package com.pairshot.core.domain.pair

import com.pairshot.core.domain.entitlement.EntitlementSource
import com.pairshot.core.domain.entitlement.ProEntitlement
import com.pairshot.core.domain.entitlement.ProEntitlementProvider
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
    private val entitlement: ProEntitlementProvider = mockk()
    private val useCase = CanCreatePairUseCase(repository, entitlement)

    @Test
    fun `pro entitled user can always create pair and repository is not queried`() =
        runTest {
            coEvery { entitlement.current() } returns ProEntitlement(true, EntitlementSource.SUBSCRIPTION)
            val result = useCase()
            assertTrue(result is CanCreatePairUseCase.Result.Allowed)
            coVerify(exactly = 0) { repository.countCreatedSince(any()) }
        }

    @Test
    fun `free user under daily quota can create pair`() =
        runTest {
            coEvery { entitlement.current() } returns ProEntitlement(false, EntitlementSource.NONE)
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
            coEvery { entitlement.current() } returns ProEntitlement(false, EntitlementSource.NONE)
            every { repository.countCreatedSince(any()) } returns flowOf(CanCreatePairUseCase.FREE_DAILY_LIMIT)
            val result = useCase() as CanCreatePairUseCase.Result.LimitReached
            assertEquals(CanCreatePairUseCase.FREE_DAILY_LIMIT, result.current)
            assertEquals(CanCreatePairUseCase.FREE_DAILY_LIMIT, result.limit)
        }

    @Test
    fun `coupon grandfather user can always create pair and repository is not queried`() =
        runTest {
            coEvery { entitlement.current() } returns ProEntitlement(true, EntitlementSource.COUPON)
            val result = useCase()
            assertTrue(result is CanCreatePairUseCase.Result.Allowed)
            coVerify(exactly = 0) { repository.countCreatedSince(any()) }
        }

    @Test
    fun `over-quota free user is rejected with current count propagated`() =
        runTest {
            coEvery { entitlement.current() } returns ProEntitlement(false, EntitlementSource.NONE)
            val overshoot = CanCreatePairUseCase.FREE_DAILY_LIMIT + 3
            every { repository.countCreatedSince(any()) } returns flowOf(overshoot)
            val result = useCase() as CanCreatePairUseCase.Result.LimitReached
            assertEquals(overshoot, result.current)
            assertEquals(CanCreatePairUseCase.FREE_DAILY_LIMIT, result.limit)
        }

    @Test
    fun `free user just below limit returns Allowed at boundary`() =
        runTest {
            coEvery { entitlement.current() } returns ProEntitlement(false, EntitlementSource.NONE)
            every { repository.countCreatedSince(any()) } returns flowOf(CanCreatePairUseCase.FREE_DAILY_LIMIT - 1)
            val result = useCase()
            assertTrue(result is CanCreatePairUseCase.Result.Allowed)
        }
}
