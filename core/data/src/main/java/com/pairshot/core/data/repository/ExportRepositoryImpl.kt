package com.pairshot.core.data.repository

import android.net.Uri
import com.pairshot.core.data.export.ShareImagePreparer
import com.pairshot.core.data.export.WatermarkedBitmapWriter
import com.pairshot.core.database.dao.PhotoPairDao
import com.pairshot.core.database.entity.PhotoPairEntity
import com.pairshot.core.domain.combine.ExportHistoryRepository
import com.pairshot.core.domain.export.ExportAction
import com.pairshot.core.domain.export.ExportRepository
import com.pairshot.core.domain.export.PreparedZip
import com.pairshot.core.domain.settings.AppSettingsRepository
import com.pairshot.core.model.CombineConfig
import com.pairshot.core.model.ExportFormat
import com.pairshot.core.model.ExportHistoryEntry
import com.pairshot.core.model.ExportHistoryKind
import com.pairshot.core.model.ExportPreset
import com.pairshot.core.model.WatermarkConfig
import com.pairshot.core.rendering.PairImageComposer
import com.pairshot.core.storage.MediaStoreManager
import com.pairshot.core.storage.ZipImageEntry
import com.pairshot.core.storage.ZipManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

private const val ZIP_FILENAME_PATTERN = "yyyyMMdd_HHmmss"
private const val ZIP_FILENAME_PREFIX = "PairShot"
private const val ZIP_FILENAME_SUFFIX = ".zip"

class ExportRepositoryImpl
    @Inject
    constructor(
        private val photoPairDao: PhotoPairDao,
        private val zipManager: ZipManager,
        private val mediaStoreManager: MediaStoreManager,
        private val watermarkedBitmapWriter: WatermarkedBitmapWriter,
        private val shareImagePreparer: ShareImagePreparer,
        private val pairImageComposer: PairImageComposer,
        private val appSettingsRepository: AppSettingsRepository,
        private val exportHistoryRepository: ExportHistoryRepository,
    ) : ExportRepository {
        override suspend fun composeCombinedForGallery(
            pairIds: List<Long>,
            combineConfig: CombineConfig,
            watermarkConfig: WatermarkConfig?,
            onProgress: (current: Int, total: Int) -> Unit,
        ): Int =
            withContext(Dispatchers.IO) {
                val jpegQuality = appSettingsRepository.getCurrent().jpegQuality
                val pairs = photoPairDao.getByIds(pairIds)
                val tempDir = shareImagePreparer.prepareTempDir("compose_combined")
                var savedCount = 0
                try {
                    pairs.forEachIndexed { index, pair ->
                        val before = pair.validBeforeUriOrNull()
                        val after = pair.validAfterUriOrNull()
                        if (before != null && after != null) {
                            val displayName = "PAIR_%03d.jpg".format(index + 1)
                            val tempFile = File(tempDir, displayName)
                            composeCombinedFile(
                                beforeUri = before,
                                afterUri = after,
                                destFile = tempFile,
                                combineConfig = combineConfig,
                                watermarkConfig = watermarkConfig,
                                jpegQuality = jpegQuality,
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
                            savedCount++
                        }
                        onProgress(index + 1, pairs.size)
                    }
                } finally {
                    tempDir.deleteRecursively()
                }
                savedCount
            }

        override suspend fun saveDecoratedOriginals(
            pairIds: List<Long>,
            preset: ExportPreset,
            combineConfig: CombineConfig,
            watermarkConfig: WatermarkConfig?,
            onProgress: (current: Int, total: Int) -> Unit,
        ): Int =
            withContext(Dispatchers.IO) {
                val jpegQuality = appSettingsRepository.getCurrent().jpegQuality
                val pairs = photoPairDao.getByIds(pairIds)
                val tempDir = shareImagePreparer.prepareTempDir("save_decorated_originals")
                var savedCount = 0
                try {
                    pairs.forEachIndexed { index, pair ->
                        val seq = index + 1
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
                                    jpegQuality = jpegQuality,
                                )
                                savedCount++
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
                                    jpegQuality = jpegQuality,
                                )
                                savedCount++
                            }
                        }
                        onProgress(index + 1, pairs.size)
                    }
                } finally {
                    tempDir.deleteRecursively()
                }
                savedCount
            }

        override suspend fun prepareZipForSave(
            pairIds: List<Long>,
            preset: ExportPreset,
            combineConfig: CombineConfig,
            watermarkConfig: WatermarkConfig?,
            onProgress: (current: Int, total: Int) -> Unit,
        ): PreparedZip? =
            withContext(Dispatchers.IO) {
                val jpegQuality = appSettingsRepository.getCurrent().jpegQuality
                val pairs = photoPairDao.getByIds(pairIds)
                val outputDir = shareImagePreparer.prepareTempDir("save_zip")
                val staging = shareImagePreparer.prepareTempDir("save_zip_staging")
                val suggestedName = buildZipFileName()
                val zipFile = File(outputDir, suggestedName)
                var success = false
                try {
                    val zipEntries = ArrayList<ZipImageEntry>()
                    pairs.forEachIndexed { index, pair ->
                        materializePairForShare(
                            pair = pair,
                            seq = index + 1,
                            preset = preset,
                            combineConfig = combineConfig,
                            watermarkConfig = watermarkConfig,
                            jpegQuality = jpegQuality,
                            destDir = staging,
                        ) { destFile, subdir ->
                            zipEntries.add(
                                ZipImageEntry(
                                    uri = Uri.fromFile(destFile),
                                    entryPath = "$subdir/${destFile.name}",
                                ),
                            )
                        }
                        onProgress(index + 1, pairs.size)
                    }

                    if (zipEntries.isEmpty()) return@withContext null

                    zipManager.createZipToFile(
                        entries = zipEntries,
                        outputFile = zipFile,
                        onProgress = { _, _ -> },
                    )
                    success = true
                    PreparedZip(
                        filePath = zipFile.absolutePath,
                        suggestedName = suggestedName,
                    )
                } finally {
                    staging.deleteRecursively()
                    if (!success) outputDir.deleteRecursively()
                }
            }

        override suspend fun discardPreparedZip(filePath: String) {
            withContext(Dispatchers.IO) {
                runCatching {
                    val file = File(filePath)
                    if (file.exists()) file.parentFile?.deleteRecursively()
                }.onFailure { error ->
                    Timber.w(error, "failed to discard prepared zip $filePath")
                }
            }
        }

        private fun buildZipFileName(): String {
            val stamp = SimpleDateFormat(ZIP_FILENAME_PATTERN, Locale.US).format(Date())
            return "${ZIP_FILENAME_PREFIX}_$stamp$ZIP_FILENAME_SUFFIX"
        }

        override suspend fun buildShareablePayload(
            pairIds: List<Long>,
            preset: ExportPreset,
            combineConfig: CombineConfig,
            watermarkConfig: WatermarkConfig?,
            onProgress: (current: Int, total: Int) -> Unit,
        ): ExportAction =
            when (preset.format) {
                ExportFormat.INDIVIDUAL -> {
                    ExportAction.ShareImages(
                        buildShareIndividual(pairIds, preset, combineConfig, watermarkConfig, onProgress),
                    )
                }

                ExportFormat.ZIP -> {
                    ExportAction.ShareZip(
                        buildShareZip(pairIds, preset, combineConfig, watermarkConfig, onProgress),
                    )
                }
            }

        private suspend fun buildShareIndividual(
            pairIds: List<Long>,
            preset: ExportPreset,
            combineConfig: CombineConfig,
            watermarkConfig: WatermarkConfig?,
            onProgress: (current: Int, total: Int) -> Unit,
        ): List<String> =
            withContext(Dispatchers.IO) {
                val jpegQuality = appSettingsRepository.getCurrent().jpegQuality
                val pairs = photoPairDao.getByIds(pairIds)
                val shareDir = shareImagePreparer.prepareShareImageDir()
                val results = ArrayList<String>()
                pairs.forEachIndexed { index, pair ->
                    materializePairForShare(
                        pair = pair,
                        seq = index + 1,
                        preset = preset,
                        combineConfig = combineConfig,
                        watermarkConfig = watermarkConfig,
                        jpegQuality = jpegQuality,
                        destDir = shareDir,
                    ) { destFile, _ ->
                        results.add(shareImagePreparer.getFileProviderUri(destFile).toString())
                    }
                    onProgress(index + 1, pairs.size)
                }
                results
            }

        private suspend fun buildShareZip(
            pairIds: List<Long>,
            preset: ExportPreset,
            combineConfig: CombineConfig,
            watermarkConfig: WatermarkConfig?,
            onProgress: (current: Int, total: Int) -> Unit,
        ): String =
            withContext(Dispatchers.IO) {
                val jpegQuality = appSettingsRepository.getCurrent().jpegQuality
                val pairs = photoPairDao.getByIds(pairIds)
                val outputDir = shareImagePreparer.prepareTempDir("share_zip")
                val outputFile = File(outputDir, "PairShot.zip")
                val staging = shareImagePreparer.prepareTempDir("share_zip_staging")
                try {
                    val zipEntries = ArrayList<ZipImageEntry>()
                    pairs.forEachIndexed { index, pair ->
                        materializePairForShare(
                            pair = pair,
                            seq = index + 1,
                            preset = preset,
                            combineConfig = combineConfig,
                            watermarkConfig = watermarkConfig,
                            jpegQuality = jpegQuality,
                            destDir = staging,
                        ) { destFile, subdir ->
                            zipEntries.add(
                                ZipImageEntry(
                                    uri = Uri.fromFile(destFile),
                                    entryPath = "$subdir/${destFile.name}",
                                ),
                            )
                        }
                        onProgress(index + 1, pairs.size)
                    }
                    zipManager.createZipToFile(
                        entries = zipEntries,
                        outputFile = outputFile,
                        onProgress = { _, _ -> },
                    )
                } finally {
                    staging.deleteRecursively()
                }
                outputFile.absolutePath
            }

        private suspend inline fun materializePairForShare(
            pair: PhotoPairEntity,
            seq: Int,
            preset: ExportPreset,
            combineConfig: CombineConfig,
            watermarkConfig: WatermarkConfig?,
            jpegQuality: Int,
            destDir: File,
            onFile: (destFile: File, subdir: String) -> Unit,
        ) {
            if (preset.includeBefore) {
                pair.validBeforeUriOrNull()?.let { uri ->
                    val name = "BEFORE_%03d.jpg".format(seq)
                    val destFile = File(destDir, name)
                    materializeSingle(uri, destFile, isBefore = true, combineConfig, watermarkConfig, jpegQuality)
                    onFile(destFile, "before")
                }
            }
            if (preset.includeAfter) {
                pair.validAfterUriOrNull()?.let { uri ->
                    val name = "AFTER_%03d.jpg".format(seq)
                    val destFile = File(destDir, name)
                    materializeSingle(uri, destFile, isBefore = false, combineConfig, watermarkConfig, jpegQuality)
                    onFile(destFile, "after")
                }
            }
            if (preset.includeCombined) {
                val before = pair.validBeforeUriOrNull()
                val after = pair.validAfterUriOrNull()
                if (before != null && after != null) {
                    val name = "PAIR_%03d.jpg".format(seq)
                    val destFile = File(destDir, name)
                    composeCombinedFile(before, after, destFile, combineConfig, watermarkConfig, jpegQuality)
                    onFile(destFile, "combined")
                }
            }
        }

        private suspend fun materializeSingle(
            sourceUri: String,
            destFile: File,
            isBefore: Boolean,
            combineConfig: CombineConfig,
            watermarkConfig: WatermarkConfig?,
            jpegQuality: Int,
        ) {
            if (com.pairshot.core.domain.export.needsIndividualDecoration(combineConfig, watermarkConfig)) {
                pairImageComposer.composeSingleToFile(
                    sourceUri = Uri.parse(sourceUri),
                    destFile = destFile,
                    isBefore = isBefore,
                    combineConfig = combineConfig,
                    watermarkConfig = watermarkConfig ?: WatermarkConfig(),
                    jpegQuality = jpegQuality,
                )
            } else {
                shareImagePreparer.copyFromContentUri(sourceUri, destFile)
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
            jpegQuality: Int,
        ) {
            val tempFile = File(tempDir, displayName)
            pairImageComposer.composeSingleToFile(
                sourceUri = Uri.parse(sourceUri),
                destFile = tempFile,
                isBefore = isBefore,
                combineConfig = combineConfig,
                watermarkConfig = watermarkConfig ?: WatermarkConfig(),
                jpegQuality = jpegQuality,
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

        private suspend fun composeCombinedFile(
            beforeUri: String,
            afterUri: String,
            destFile: File,
            combineConfig: CombineConfig,
            watermarkConfig: WatermarkConfig?,
            jpegQuality: Int,
        ) {
            if (watermarkConfig != null) {
                watermarkedBitmapWriter.combineWithWatermark(
                    beforeUri = beforeUri,
                    afterUri = afterUri,
                    destFile = destFile,
                    config = watermarkConfig,
                    jpegQuality = jpegQuality,
                    combineConfig = combineConfig,
                )
            } else {
                pairImageComposer.composeToFile(
                    beforeUri = Uri.parse(beforeUri),
                    afterUri = Uri.parse(afterUri),
                    destFile = destFile,
                    combineConfig = combineConfig,
                    watermarkConfig = WatermarkConfig(),
                    jpegQuality = jpegQuality,
                )
            }
        }
    }

private fun PhotoPairEntity.validBeforeUriOrNull(): String? =
    beforePhotoUri?.takeIf {
        it.isNotBlank() && Uri.parse(it).scheme == "content"
    }

private fun PhotoPairEntity.validAfterUriOrNull(): String? = afterPhotoUri?.takeIf { it.isNotBlank() && Uri.parse(it).scheme == "content" }
