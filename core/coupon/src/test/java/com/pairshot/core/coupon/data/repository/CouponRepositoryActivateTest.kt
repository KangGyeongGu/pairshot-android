package com.pairshot.core.coupon.data.repository

import com.pairshot.core.coupon.domain.ActivationResult
import com.pairshot.core.coupon.local.CouponPreferencesSource
import com.pairshot.core.coupon.local.DeviceHashProvider
import com.pairshot.core.coupon.remote.ActivationApiResult
import com.pairshot.core.coupon.remote.CouponActivationApi
import com.pairshot.core.coupon.remote.dto.ActivateResponseDto
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CouponRepositoryActivateTest {
    private val api: CouponActivationApi = mockk()
    private val preferences: CouponPreferencesSource = mockk(relaxed = true)
    private val deviceHashProvider: DeviceHashProvider = mockk()

    private lateinit var repository: CouponRepositoryImpl

    @Before
    fun setUp() {
        repository = CouponRepositoryImpl(api, preferences, deviceHashProvider)
        every { deviceHashProvider.deviceHash() } returns "device-hash"
    }

    @Test
    fun `empty code returns InvalidFormat without hitting api`() =
        runTest {
            val result = repository.activate("   ")
            assertEquals(ActivationResult.Failure.InvalidFormat, result)
            coVerify(exactly = 0) { api.activate(any()) }
            coVerify(exactly = 0) { preferences.savePending(any(), any()) }
        }

    @Test
    fun `InvalidCodeFormat clears pending and returns InvalidFormat`() =
        runTest {
            coEvery { api.activate(any()) } returns ActivationApiResult.InvalidCodeFormat
            val result = repository.activate("PS-30D-ABC")
            assertEquals(ActivationResult.Failure.InvalidFormat, result)
            coVerify(exactly = 1) { preferences.clearPending() }
        }

    @Test
    fun `InvalidSignature clears pending and returns InvalidSignature`() =
        runTest {
            coEvery { api.activate(any()) } returns ActivationApiResult.InvalidSignature
            val result = repository.activate("PS-30D-ABC")
            assertEquals(ActivationResult.Failure.InvalidSignature, result)
            coVerify(exactly = 1) { preferences.clearPending() }
        }

    @Test
    fun `NotFound clears pending and returns NotFound`() =
        runTest {
            coEvery { api.activate(any()) } returns ActivationApiResult.NotFound
            val result = repository.activate("PS-30D-ABC")
            assertEquals(ActivationResult.Failure.NotFound, result)
            coVerify(exactly = 1) { preferences.clearPending() }
        }

    @Test
    fun `AlreadyUsedOnAnotherDevice clears pending and returns AlreadyUsedOnAnotherDevice`() =
        runTest {
            coEvery { api.activate(any()) } returns ActivationApiResult.AlreadyUsedOnAnotherDevice
            val result = repository.activate("PS-30D-ABC")
            assertEquals(ActivationResult.Failure.AlreadyUsedOnAnotherDevice, result)
            coVerify(exactly = 1) { preferences.clearPending() }
        }

    @Test
    fun `Revoked clears pending and returns Revoked`() =
        runTest {
            coEvery { api.activate(any()) } returns ActivationApiResult.Revoked
            val result = repository.activate("PS-30D-ABC")
            assertEquals(ActivationResult.Failure.Revoked, result)
            coVerify(exactly = 1) { preferences.clearPending() }
        }

    @Test
    fun `NetworkError keeps pending so retry can succeed later`() =
        runTest {
            coEvery { api.activate(any()) } returns ActivationApiResult.NetworkError
            val result = repository.activate("PS-30D-ABC")
            assertEquals(ActivationResult.Failure.NetworkError, result)
            coVerify(exactly = 0) { preferences.clearPending() }
        }

    @Test
    fun `ServerError returns UnknownError and keeps pending`() =
        runTest {
            coEvery { api.activate(any()) } returns ActivationApiResult.ServerError
            val result = repository.activate("PS-30D-ABC")
            assertEquals(ActivationResult.Failure.UnknownError, result)
            coVerify(exactly = 0) { preferences.clearPending() }
        }

    @Test
    fun `Success saves pending then clears it after success and persists state`() =
        runTest {
            val response =
                ActivateResponseDto(
                    couponId = "coupon-123",
                    durationDays = 30L,
                    activatedAt = "2026-01-01T00:00:00Z",
                )
            coEvery { api.activate(any()) } returns ActivationApiResult.Success(response)
            every { preferences.state } returns flowOf(null)

            val result = repository.activate("PS-30D-ABC")

            assertTrue(result is ActivationResult.Success)
            coVerify(exactly = 1) { preferences.savePending("PS-30D-ABC", any()) }
            coVerify(exactly = 1) { preferences.clearPending() }
            coVerify(exactly = 1) { preferences.save(any()) }
        }

    @Test
    fun `code is trimmed before being sent to api`() =
        runTest {
            coEvery { api.activate(any()) } returns ActivationApiResult.NotFound
            repository.activate("  PS-30D-ABC  ")
            coVerify {
                api.activate(
                    match { it.code == "PS-30D-ABC" && it.deviceHash == "device-hash" },
                )
            }
        }
}
