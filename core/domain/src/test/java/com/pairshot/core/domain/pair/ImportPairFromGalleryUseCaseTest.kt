package com.pairshot.core.domain.pair

import com.pairshot.core.model.AspectRatio
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ImportPairFromGalleryUseCaseTest {
    private val repository: PhotoPairRepository = mockk()
    private val metadataReader: GalleryImageMetadataReader = mockk()
    private val useCase = ImportPairFromGalleryUseCase(repository, metadataReader)

    @Test
    fun `before only import saves before photo with taken timestamp and inferred aspect`() =
        runTest {
            coEvery { metadataReader.takenAtMs("content://before") } returns 1_000L
            coEvery { metadataReader.dimensions("content://before") } returns ImageDimensions(4032, 3024)
            coEvery {
                repository.saveBeforePhoto(any(), any(), any(), any(), any())
            } returns 7L

            val pairId = useCase(beforeUri = "content://before")

            assertEquals(7L, pairId)
            coVerify(exactly = 1) {
                repository.saveBeforePhoto(
                    tempFileUri = "content://before",
                    zoomLevel = null,
                    albumId = null,
                    aspectRatio = AspectRatio.RATIO_4_3,
                    capturedAtMs = 1_000L,
                )
            }
            coVerify(exactly = 0) { repository.saveAfterPhoto(any(), any(), any()) }
        }

    @Test
    fun `before and after import saves both with each taken timestamp`() =
        runTest {
            coEvery { metadataReader.takenAtMs("content://before") } returns 1_000L
            coEvery { metadataReader.takenAtMs("content://after") } returns 2_000L
            coEvery { metadataReader.dimensions("content://before") } returns ImageDimensions(3840, 2160)
            coEvery {
                repository.saveBeforePhoto(any(), any(), any(), any(), any())
            } returns 3L
            coEvery { repository.saveAfterPhoto(any(), any(), any()) } returns Unit

            useCase(beforeUri = "content://before", afterUri = "content://after", albumId = 11L)

            coVerify(exactly = 1) {
                repository.saveBeforePhoto(
                    tempFileUri = "content://before",
                    zoomLevel = null,
                    albumId = 11L,
                    aspectRatio = AspectRatio.RATIO_16_9,
                    capturedAtMs = 1_000L,
                )
            }
            coVerify(exactly = 1) {
                repository.saveAfterPhoto(
                    pairId = 3L,
                    tempFileUri = "content://after",
                    capturedAtMs = 2_000L,
                )
            }
        }

    @Test
    fun `missing metadata falls back to null timestamp and aspect`() =
        runTest {
            coEvery { metadataReader.takenAtMs(any()) } returns null
            coEvery { metadataReader.dimensions(any()) } returns null
            coEvery {
                repository.saveBeforePhoto(any(), any(), any(), any(), any())
            } returns 1L

            useCase(beforeUri = "content://before")

            coVerify(exactly = 1) {
                repository.saveBeforePhoto(
                    tempFileUri = "content://before",
                    zoomLevel = null,
                    albumId = null,
                    aspectRatio = null,
                    capturedAtMs = null,
                )
            }
        }

    @Test
    fun `nearest aspect ratio maps camera and square and ultra wide dimensions`() {
        assertEquals(AspectRatio.RATIO_4_3, ImportPairFromGalleryUseCase.nearestAspectRatio(4032, 3024))
        assertEquals(AspectRatio.RATIO_4_3, ImportPairFromGalleryUseCase.nearestAspectRatio(3024, 4032))
        assertEquals(AspectRatio.RATIO_16_9, ImportPairFromGalleryUseCase.nearestAspectRatio(3840, 2160))
        assertEquals(AspectRatio.RATIO_1_1, ImportPairFromGalleryUseCase.nearestAspectRatio(2000, 2000))
        assertEquals(AspectRatio.RATIO_16_9, ImportPairFromGalleryUseCase.nearestAspectRatio(1170, 2532))
        assertNull(ImportPairFromGalleryUseCase.nearestAspectRatio(0, 0))
    }
}
