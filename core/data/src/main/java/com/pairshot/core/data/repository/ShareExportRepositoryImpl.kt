package com.pairshot.core.data.repository

import android.net.Uri
import com.pairshot.core.data.export.ExportPipeline
import com.pairshot.core.data.export.ShareImagePreparer
import com.pairshot.core.data.export.internal.DecoratedPairMaterializer
import com.pairshot.core.database.dao.PhotoPairDao
import com.pairshot.core.domain.export.ExportAction
import com.pairshot.core.domain.export.ShareExportRepository
import com.pairshot.core.domain.settings.AppSettingsRepository
import com.pairshot.core.model.CombineConfig
import com.pairshot.core.model.ExportFormat
import com.pairshot.core.model.ExportPreset
import com.pairshot.core.model.WatermarkConfig
import com.pairshot.core.storage.ZipImageEntry
import com.pairshot.core.storage.ZipManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

class ShareExportRepositoryImpl
@Inject
constructor(
    private val photoPairDao: PhotoPairDao,
    private val zipManager: ZipManager,
    private val shareImagePreparer: ShareImagePreparer,
    private val appSettingsRepository: AppSettingsRepository,
    private val exportPipeline: ExportPipeline,
    private val decoratedPairMaterializer: DecoratedPairMaterializer,
) : ShareExportRepository {
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
            val imageQuality = appSettingsRepository.getCurrent().imageQuality
            val pairs = photoPairDao.getByIds(pairIds)
            val shareDir = shareImagePreparer.prepareShareImageDir()
            val urlLists =
                exportPipeline.processInParallel(
                    total = pairs.size,
                    maxOutputPx = imageQuality.maxOutputPx,
                    onProgress = onProgress,
                ) { index ->
                    val pair = pairs[index]
                    val urls = mutableListOf<String>()
                    decoratedPairMaterializer.materializePair(
                        pair = pair,
                        seq = index + 1,
                        preset = preset,
                        combineConfig = combineConfig,
                        watermarkConfig = watermarkConfig,
                        imageQuality = imageQuality,
                        destDir = shareDir,
                    ) { destFile, _ ->
                        urls.add(shareImagePreparer.getFileProviderUri(destFile).toString())
                    }
                    urls
                }
            urlLists.filterNotNull().flatten()
        }

    private suspend fun buildShareZip(
        pairIds: List<Long>,
        preset: ExportPreset,
        combineConfig: CombineConfig,
        watermarkConfig: WatermarkConfig?,
        onProgress: (current: Int, total: Int) -> Unit,
    ): String =
        withContext(Dispatchers.IO) {
            val imageQuality = appSettingsRepository.getCurrent().imageQuality
            val pairs = photoPairDao.getByIds(pairIds)
            val outputDir = shareImagePreparer.prepareTempDir("share_zip")
            val outputFile = File(outputDir, "PairShot.zip")
            val staging = shareImagePreparer.prepareTempDir("share_zip_staging")
            try {
                val entryLists =
                    exportPipeline.processInParallel(
                        total = pairs.size,
                        maxOutputPx = imageQuality.maxOutputPx,
                        onProgress = onProgress,
                    ) { index ->
                        val pair = pairs[index]
                        val entries = mutableListOf<ZipImageEntry>()
                        decoratedPairMaterializer.materializePair(
                            pair = pair,
                            seq = index + 1,
                            preset = preset,
                            combineConfig = combineConfig,
                            watermarkConfig = watermarkConfig,
                            imageQuality = imageQuality,
                            destDir = staging,
                        ) { destFile, subdir ->
                            entries.add(
                                ZipImageEntry(
                                    uri = Uri.fromFile(destFile),
                                    entryPath = "$subdir/${destFile.name}",
                                ),
                            )
                        }
                        entries
                    }
                val zipEntries = entryLists.filterNotNull().flatten()
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
}
