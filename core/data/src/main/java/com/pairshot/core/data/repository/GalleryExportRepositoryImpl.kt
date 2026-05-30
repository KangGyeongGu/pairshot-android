package com.pairshot.core.data.repository

import android.net.Uri
import com.pairshot.core.data.export.ExportPipeline
import com.pairshot.core.data.export.ShareImagePreparer
import com.pairshot.core.data.export.internal.CombinedImageComposer
import com.pairshot.core.data.export.internal.validAfterUriOrNull
import com.pairshot.core.data.export.internal.validBeforeUriOrNull
import com.pairshot.core.database.dao.PhotoPairDao
import com.pairshot.core.domain.combine.ExportHistoryRepository
import com.pairshot.core.domain.export.GalleryExportRepository
import com.pairshot.core.domain.settings.AppSettingsRepository
import com.pairshot.core.model.CombineConfig
import com.pairshot.core.model.ExportHistoryEntry
import com.pairshot.core.model.ExportHistoryKind
import com.pairshot.core.model.ExportPreset
import com.pairshot.core.model.ImageQualityPreset
import com.pairshot.core.model.RenderProfile
import com.pairshot.core.model.WatermarkConfig
import com.pairshot.core.rendering.PairImageComposer
import com.pairshot.core.storage.MediaStoreManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject

class GalleryExportRepositoryImpl
@Inject
constructor(
    private val photoPairDao: PhotoPairDao,
    private val mediaStoreManager: MediaStoreManager,
    private val shareImagePreparer: ShareImagePreparer,
    private val appSettingsRepository: AppSettingsRepository,
    private val exportHistoryRepository: ExportHistoryRepository,
    private val exportPipeline: ExportPipeline,
    private val pairImageComposer: PairImageComposer,
    private val combinedImageComposer: CombinedImageComposer,
) : GalleryExportRepository {
    override suspend fun composeCombinedForGallery(
        pairIds: List<Long>,
        combineConfig: CombineConfig,
        watermarkConfig: WatermarkConfig?,
        progressBase: Int,
        progressTotal: Int,
        onProgress: (current: Int, total: Int) -> Unit,
    ): Int =
        withContext(Dispatchers.IO) {
            val imageQuality = appSettingsRepository.getCurrent().imageQuality
            val pairs = photoPairDao.getByIds(pairIds)
            val tempDir = shareImagePreparer.prepareTempDir("compose_combined")
            try {
                val results =
                    exportPipeline.processInParallel(
                        total = pairs.size,
                        maxOutputPx = imageQuality.maxOutputPx,
                        onProgress = { current, _ ->
                            onProgress(progressBase + current, progressTotal)
                        },
                    ) { index ->
                        val pair = pairs[index]
                        val before = pair.validBeforeUriOrNull()
                        val after = pair.validAfterUriOrNull()
                        if (before == null || after == null) {
                            return@processInParallel false
                        }
                        val displayName = "PAIR_%03d.jpg".format(index + 1)
                        val tempFile = File(tempDir, displayName)
                        combinedImageComposer.composeCombinedFile(
                            beforeUri = before,
                            afterUri = after,
                            destFile = tempFile,
                            combineConfig = combineConfig,
                            watermarkConfig = watermarkConfig,
                            imageQuality = imageQuality,
                        )
                        val savedUri =
                            mediaStoreManager.saveToGallery(
                                tempFileUri = Uri.fromFile(tempFile),
                                subfolder = "",
                                displayName = displayName,
                            )
                        exportHistoryRepository.insert(
                            ExportHistoryEntry(
                                pairId = pair.id,
                                mediaStoreUri = savedUri.toString(),
                                kind = ExportHistoryKind.COMBINED,
                            ),
                        )
                        true
                    }
                results.count { it == true }
            } finally {
                tempDir.deleteRecursively()
            }
        }

    override suspend fun saveDecoratedOriginals(
        pairIds: List<Long>,
        preset: ExportPreset,
        combineConfig: CombineConfig,
        watermarkConfig: WatermarkConfig?,
        progressBase: Int,
        progressTotal: Int,
        onProgress: (current: Int, total: Int) -> Unit,
    ): Int =
        withContext(Dispatchers.IO) {
            val imageQuality = appSettingsRepository.getCurrent().imageQuality
            val pairs = photoPairDao.getByIds(pairIds)
            val tempDir = shareImagePreparer.prepareTempDir("save_decorated_originals")
            val processedCount = AtomicInteger(0)
            try {
                val perPairCounts =
                    exportPipeline.processInParallel(
                        total = pairs.size,
                        maxOutputPx = imageQuality.maxOutputPx,
                        onProgress = { _, _ -> },
                    ) { index ->
                        val pair = pairs[index]
                        val seq = index + 1
                        var localCount = 0
                        if (preset.includeBefore) {
                            pair.validBeforeUriOrNull()?.let { uri ->
                                saveDecoratedToGallery(
                                    pairId = pair.id,
                                    sourceUri = uri,
                                    tempDir = tempDir,
                                    displayName = "BEFORE_%03d.jpg".format(seq),
                                    kind = ExportHistoryKind.WATERMARKED_BEFORE,
                                    isBefore = true,
                                    combineConfig = combineConfig,
                                    watermarkConfig = watermarkConfig,
                                    imageQuality = imageQuality,
                                )
                                onProgress(progressBase + processedCount.incrementAndGet(), progressTotal)
                                localCount++
                            }
                        }
                        if (preset.includeAfter) {
                            pair.validAfterUriOrNull()?.let { uri ->
                                saveDecoratedToGallery(
                                    pairId = pair.id,
                                    sourceUri = uri,
                                    tempDir = tempDir,
                                    displayName = "AFTER_%03d.jpg".format(seq),
                                    kind = ExportHistoryKind.WATERMARKED_AFTER,
                                    isBefore = false,
                                    combineConfig = combineConfig,
                                    watermarkConfig = watermarkConfig,
                                    imageQuality = imageQuality,
                                )
                                onProgress(progressBase + processedCount.incrementAndGet(), progressTotal)
                                localCount++
                            }
                        }
                        localCount
                    }
                perPairCounts.filterNotNull().sum()
            } finally {
                tempDir.deleteRecursively()
            }
        }

    private suspend fun saveDecoratedToGallery(
        pairId: Long,
        sourceUri: String,
        tempDir: File,
        displayName: String,
        kind: ExportHistoryKind,
        isBefore: Boolean,
        combineConfig: CombineConfig,
        watermarkConfig: WatermarkConfig?,
        imageQuality: ImageQualityPreset,
    ) {
        val tempFile = File(tempDir, displayName)
        pairImageComposer.composeSingleToFile(
            sourceUri = Uri.parse(sourceUri),
            destFile = tempFile,
            isBefore = isBefore,
            combineConfig = combineConfig,
            watermarkConfig = watermarkConfig ?: WatermarkConfig(),
            jpegQuality = imageQuality.jpegQuality,
            profile = RenderProfile.forExport(imageQuality),
        )
        val savedUri =
            mediaStoreManager.saveToGallery(
                tempFileUri = Uri.fromFile(tempFile),
                subfolder = "",
                displayName = displayName,
            )
        exportHistoryRepository.insert(
            ExportHistoryEntry(
                pairId = pairId,
                mediaStoreUri = savedUri.toString(),
                kind = kind,
            ),
        )
    }
}
