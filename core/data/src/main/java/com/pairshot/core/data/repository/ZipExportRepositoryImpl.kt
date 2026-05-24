package com.pairshot.core.data.repository

import android.net.Uri
import com.pairshot.core.data.export.ExportPipeline
import com.pairshot.core.data.export.ShareImagePreparer
import com.pairshot.core.data.export.internal.DecoratedPairMaterializer
import com.pairshot.core.database.dao.PhotoPairDao
import com.pairshot.core.domain.export.PreparedZip
import com.pairshot.core.domain.export.ZipExportRepository
import com.pairshot.core.domain.settings.AppSettingsRepository
import com.pairshot.core.model.CombineConfig
import com.pairshot.core.model.ExportPreset
import com.pairshot.core.model.WatermarkConfig
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

class ZipExportRepositoryImpl
@Inject
constructor(
    private val photoPairDao: PhotoPairDao,
    private val zipManager: ZipManager,
    private val shareImagePreparer: ShareImagePreparer,
    private val appSettingsRepository: AppSettingsRepository,
    private val exportPipeline: ExportPipeline,
    private val decoratedPairMaterializer: DecoratedPairMaterializer,
) : ZipExportRepository {
    override suspend fun prepareZipForSave(
        pairIds: List<Long>,
        preset: ExportPreset,
        combineConfig: CombineConfig,
        watermarkConfig: WatermarkConfig?,
        onProgress: (current: Int, total: Int) -> Unit,
    ): PreparedZip? =
        withContext(Dispatchers.IO) {
            val imageQuality = appSettingsRepository.getCurrent().imageQuality
            val pairs = photoPairDao.getByIds(pairIds)
            val outputDir = shareImagePreparer.prepareTempDir("save_zip")
            val staging = shareImagePreparer.prepareTempDir("save_zip_staging")
            val suggestedName = buildZipFileName()
            val zipFile = File(outputDir, suggestedName)
            var success = false
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
}
