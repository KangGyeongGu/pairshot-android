package com.pairshot.core.data.repository

import android.net.Uri
import com.pairshot.core.data.export.ExportPipeline
import com.pairshot.core.data.export.ShareImagePreparer
import com.pairshot.core.data.export.internal.ExportItemRenderer
import com.pairshot.core.data.export.internal.RenderedItem
import com.pairshot.core.domain.combine.ExportHistoryRepository
import com.pairshot.core.domain.export.ExportAction
import com.pairshot.core.domain.export.ExportDestination
import com.pairshot.core.domain.export.ExportItem
import com.pairshot.core.domain.export.ExportPlan
import com.pairshot.core.domain.export.ExportProgress
import com.pairshot.core.domain.export.ExportRepository
import com.pairshot.core.domain.export.SaveToDeviceResult
import com.pairshot.core.domain.settings.AppSettingsRepository
import com.pairshot.core.model.ExportFormat
import com.pairshot.core.model.ExportHistoryEntry
import com.pairshot.core.model.ExportHistoryKind
import com.pairshot.core.storage.MediaStoreManager
import com.pairshot.core.storage.ZipImageEntry
import com.pairshot.core.storage.ZipManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject

private const val ZIP_FILENAME_PATTERN = "yyyyMMdd_HHmmss"
private const val ZIP_FILENAME_PREFIX = "PairShot"
private const val ZIP_FILENAME_SUFFIX = ".zip"

class ExportRepositoryImpl
@Inject
constructor(
    private val appSettingsRepository: AppSettingsRepository,
    private val exportPipeline: ExportPipeline,
    private val exportItemRenderer: ExportItemRenderer,
    private val shareImagePreparer: ShareImagePreparer,
    private val mediaStoreManager: MediaStoreManager,
    private val exportHistoryRepository: ExportHistoryRepository,
    private val zipManager: ZipManager,
) : ExportRepository {
    override suspend fun executeShare(
        plan: ExportPlan,
        onProgress: (ExportProgress) -> Unit,
    ): ExportAction =
        withContext(Dispatchers.IO) {
            require(!plan.isEmpty) { "plan is empty" }
            require(plan.destination == ExportDestination.SHARE) { "destination must be SHARE" }
            onProgress(ExportProgress.Rendering(current = 0, total = plan.totalUnits))

            val shareDir = shareImagePreparer.prepareShareImageDir()
            val rendered = renderImageItems(plan, shareDir, onProgress)
            when (plan.format) {
                ExportFormat.INDIVIDUAL -> {
                    val uris = rendered.map { shareImagePreparer.getFileProviderUri(it.file).toString() }
                    ExportAction.ShareImages(uris)
                }

                ExportFormat.ZIP -> {
                    val outputDir = shareImagePreparer.prepareTempDir("share_zip")
                    val outputFile = File(outputDir, "PairShot.zip")
                    onProgress(ExportProgress.Compressing(current = 0, total = rendered.size))
                    zipManager.createZipToFile(
                        entries = rendered.toZipEntries(),
                        outputFile = outputFile,
                        onProgress = { current, total ->
                            onProgress(ExportProgress.Compressing(current = current, total = total))
                        },
                    )
                    ExportAction.ShareZip(outputFile.absolutePath)
                }
            }
        }

    override suspend fun executeSaveToDevice(
        plan: ExportPlan,
        onProgress: (ExportProgress) -> Unit,
    ): SaveToDeviceResult =
        withContext(Dispatchers.IO) {
            if (plan.isEmpty) return@withContext SaveToDeviceResult.Nothing
            require(plan.destination == ExportDestination.SAVE_TO_DEVICE) { "destination must be SAVE_TO_DEVICE" }
            onProgress(ExportProgress.Rendering(current = 0, total = plan.totalUnits))

            when (plan.format) {
                ExportFormat.INDIVIDUAL -> saveIndividuals(plan, onProgress)
                ExportFormat.ZIP -> prepareZipForSaf(plan, onProgress)
            }
        }

    private suspend fun saveIndividuals(
        plan: ExportPlan,
        onProgress: (ExportProgress) -> Unit,
    ): SaveToDeviceResult {
        val tempDir = shareImagePreparer.prepareTempDir("save_to_gallery")
        try {
            val imageQuality = appSettingsRepository.getCurrent().imageQuality
            val pairsById = plan.pairs.associateBy { it.id }
            val saved = AtomicInteger(0)
            val results =
                exportPipeline.processInParallel(
                    total = plan.items.size,
                    maxOutputPx = imageQuality.maxOutputPx,
                    onProgress = { _, _ -> },
                ) { index ->
                    val item = plan.items[index]
                    val pair = pairsById[item.pairId] ?: return@processInParallel null
                    val rendered =
                        exportItemRenderer.render(
                            item = item,
                            pair = pair,
                            seq = index + 1,
                            destDir = tempDir,
                            combineConfig = plan.combineConfig,
                            watermarkConfig = plan.watermarkConfig,
                            imageQuality = imageQuality,
                        ) ?: return@processInParallel null
                    val savedUri =
                        mediaStoreManager.saveToGallery(
                            tempFileUri = Uri.fromFile(rendered.file),
                            subfolder = "",
                            displayName = rendered.file.name,
                        )
                    exportHistoryRepository.insert(
                        ExportHistoryEntry(
                            pairId = pair.id,
                            mediaStoreUri = savedUri.toString(),
                            kind = item.toHistoryKind(),
                        ),
                    )
                    onProgress(
                        ExportProgress.Rendering(
                            current = saved.incrementAndGet(),
                            total = plan.totalUnits,
                        ),
                    )
                    rendered
                }
            val count = results.count { it != null }
            return if (count > 0) SaveToDeviceResult.SavedImagesToGallery(count) else SaveToDeviceResult.Nothing
        } finally {
            tempDir.deleteRecursively()
        }
    }

    private suspend fun prepareZipForSaf(
        plan: ExportPlan,
        onProgress: (ExportProgress) -> Unit,
    ): SaveToDeviceResult {
        val staging = shareImagePreparer.prepareTempDir("save_zip_staging")
        val outputDir = shareImagePreparer.prepareTempDir("save_zip")
        val suggestedName = buildZipFileName()
        val zipFile = File(outputDir, suggestedName)
        var success = false
        try {
            val rendered = renderImageItems(plan, staging, onProgress)
            onProgress(ExportProgress.Compressing(current = 0, total = rendered.size))
            zipManager.createZipToFile(
                entries = rendered.toZipEntries(),
                outputFile = zipFile,
                onProgress = { current, total ->
                    onProgress(ExportProgress.Compressing(current = current, total = total))
                },
            )
            success = true
            return SaveToDeviceResult.ZipReadyForSave(
                filePath = zipFile.absolutePath,
                suggestedName = suggestedName,
            )
        } finally {
            staging.deleteRecursively()
            if (!success) outputDir.deleteRecursively()
        }
    }

    private suspend fun renderImageItems(
        plan: ExportPlan,
        destDir: File,
        onProgress: (ExportProgress) -> Unit,
    ): List<RenderedItem> {
        val imageQuality = appSettingsRepository.getCurrent().imageQuality
        val pairsById = plan.pairs.associateBy { it.id }
        val produced = AtomicInteger(0)
        val results =
            exportPipeline.processInParallel(
                total = plan.items.size,
                maxOutputPx = imageQuality.maxOutputPx,
                onProgress = { _, _ -> },
            ) { index ->
                val item = plan.items[index]
                val pair = pairsById[item.pairId] ?: return@processInParallel null
                val rendered =
                    exportItemRenderer.render(
                        item = item,
                        pair = pair,
                        seq = index + 1,
                        destDir = destDir,
                        combineConfig = plan.combineConfig,
                        watermarkConfig = plan.watermarkConfig,
                        imageQuality = imageQuality,
                    )
                if (rendered != null) {
                    onProgress(
                        ExportProgress.Rendering(
                            current = produced.incrementAndGet(),
                            total = plan.totalUnits,
                        ),
                    )
                }
                rendered
            }
        return results.filterNotNull()
    }

    private fun List<RenderedItem>.toZipEntries(): List<ZipImageEntry> =
        map { rendered ->
            ZipImageEntry(
                uri = Uri.fromFile(rendered.file),
                entryPath = "${rendered.subdir}/${rendered.file.name}",
            )
        }

    private fun ExportItem.toHistoryKind(): ExportHistoryKind =
        when (this) {
            is ExportItem.CombinedImage -> ExportHistoryKind.COMBINED
            is ExportItem.BeforeImage -> ExportHistoryKind.WATERMARKED_BEFORE
            is ExportItem.AfterImage -> ExportHistoryKind.WATERMARKED_AFTER
        }

    private fun buildZipFileName(): String {
        val ts = SimpleDateFormat(ZIP_FILENAME_PATTERN, Locale.US).format(Date())
        return "${ZIP_FILENAME_PREFIX}_$ts$ZIP_FILENAME_SUFFIX"
    }

    override suspend fun discardPreparedZip(filePath: String) {
        withContext(Dispatchers.IO) {
            File(filePath).parentFile?.deleteRecursively()
        }
    }
}
