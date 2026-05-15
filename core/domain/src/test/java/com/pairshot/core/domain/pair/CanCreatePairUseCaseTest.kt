package com.pairshot.core.domain.pair

import com.pairshot.core.domain.entitlement.EntitlementSource
import com.pairshot.core.domain.entitlement.ProEntitlement
import com.pairshot.core.domain.entitlement.ProEntitlementProvider
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CanCreatePairUseCaseTest {
    private val repository: PhotoPairRepository = mockk()
    private val entitlement: ProEntitlementProvider = mockk()
    private val useCase = CanCreatePairUseCase(repository, entitlement)

    @Test
    fun `pro entitled user can always create pair`() =
        runTest {
            coEvery { entitlement.current() } returns ProEntitlement(true, EntitlementSource.SUBSCRIPTION)
            val result = useCase()
            assertTrue(result is CanCreatePairUseCase.Result.Allowed)
        }

    @Test
    fun `free user under daily quota can create pair`() =
        runTest {
            coEvery { entitlement.current() } returns ProEntitlement(false, EntitlementSource.NONE)
            val capturedSince = slot<Long>()
            every { repository.countCreatedSince(capture(capturedSince)) } returns flowOf(3)
            val result = useCase()
            assertTrue(result is CanCreatePairUseCase.Result.Allowed)
            assertTrue("query timestamp should be epoch ms", capturedSince.captured > 0)
        }

    @Test
    fun `free user at daily limit cannot create pair`() =
        runTest {
            coEvery { entitlement.current() } returns ProEntitlement(false, EntitlementSource.NONE)
            every { repository.countCreatedSince(any()) } returns flowOf(CanCreatePairUseCase.FREE_DAILY_LIMIT)
            val result = useCase()
            assertTrue(result is CanCreatePairUseCase.Result.LimitReached)
            assertEquals(
                CanCreatePairUseCase.FREE_DAILY_LIMIT,
                (result as CanCreatePairUseCase.Result.LimitReached).current,
            )
        }

    @Test
    fun `coupon grandfather user can always create pair`() =
        runTest {
            coEvery { entitlement.current() } returns ProEntitlement(true, EntitlementSource.COUPON)
            val result = useCase()
            assertTrue(result is CanCreatePairUseCase.Result.Allowed)
        }

    @Test
    fun `over-quota free user is rejected`() =
        runTest {
            coEvery { entitlement.current() } returns ProEntitlement(false, EntitlementSource.NONE)
            every { repository.countCreatedSince(any()) } returns flowOf(CanCreatePairUseCase.FREE_DAILY_LIMIT + 3)
            val result = useCase()
            assertTrue(result is CanCreatePairUseCase.Result.LimitReached)
        }
}
