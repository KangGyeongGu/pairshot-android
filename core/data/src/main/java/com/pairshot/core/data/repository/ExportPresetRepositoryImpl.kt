package com.pairshot.core.data.repository

import android.content.Context
import com.pairshot.core.datastore.AppPreferences
import com.pairshot.core.datastore.CombinePreferences
import com.pairshot.core.datastore.WatermarkPreferences
import com.pairshot.core.domain.export.ExportPresetRepository
import com.pairshot.core.model.CombineConfig
import com.pairshot.core.model.ExportFormat
import com.pairshot.core.model.ExportPreset
import com.pairshot.core.model.ExportPresetSlot
import com.pairshot.core.model.WatermarkConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExportPresetRepositoryImpl
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val appPreferences: AppPreferences,
    private val watermarkPreferences: WatermarkPreferences,
    private val combinePreferences: CombinePreferences,
) : ExportPresetRepository {
    private val mutex = Mutex()
    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

    override val slotsFlow: Flow<List<ExportPresetSlot>> =
        appPreferences.exportPresetSlotsJson.map { raw -> decodeSlotsOrNull(raw).orEmpty() }

    override val activeSlotIdFlow: Flow<String> =
        appPreferences.activeExportPresetId.map { id -> id ?: ExportPresetSlot.DEFAULT_ID }

    override val activeSlotFlow: Flow<ExportPresetSlot> =
        combine(slotsFlow, activeSlotIdFlow) { slots, activeId ->
            slots.firstOrNull { it.id == activeId } ?: slots.firstOrNull() ?: blankDefaultSlot()
        }

    override suspend fun getSlots(): List<ExportPresetSlot> {
        ensureMigratedIfEmpty()
        return slotsFlow.first()
    }

    override suspend fun getActiveSlot(): ExportPresetSlot {
        ensureMigratedIfEmpty()
        return activeSlotFlow.first()
    }

    override suspend fun createSlot(name: String): Result<String> =
        mutex.withLock {
            ensureMigratedIfEmpty()
            val trimmed = name.trim()
            val validation = validateName(trimmed, currentSlots = decodeSlotsOrNull(currentJson()).orEmpty())
            if (validation != null) return@withLock Result.failure(validation)
            val current = decodeSlotsOrNull(currentJson()).orEmpty()
            if (current.size >= ExportPresetSlot.MAX_SLOTS) {
                return@withLock Result.failure(IllegalStateException("slot limit reached"))
            }
            val activeId = appPreferences.activeExportPresetId.first() ?: ExportPresetSlot.DEFAULT_ID
            val source =
                current.firstOrNull { it.id == activeId } ?: current.firstOrNull() ?: blankDefaultSlot()
            val newId = UUID.randomUUID().toString()
            val copiedLogoPath = copyLogoFileForNewSlot(source.watermarkConfig.logoPath)
            val newSlot =
                source.copy(
                    id = newId,
                    name = trimmed,
                    watermarkConfig = source.watermarkConfig.copy(logoPath = copiedLogoPath),
                )
            persistSlots(current + newSlot)
            appPreferences.setActiveExportPresetId(newId)
            applySlotToLegacyStores(newSlot)
            Result.success(newId)
        }

    override suspend fun renameSlot(
        id: String,
        name: String,
    ): Result<Unit> =
        mutex.withLock {
            ensureMigratedIfEmpty()
            val trimmed = name.trim()
            val current = decodeSlotsOrNull(currentJson()).orEmpty()
            val target = current.firstOrNull { it.id == id }
                ?: return@withLock Result.failure(NoSuchElementException("slot not found: $id"))
            val others = current.filter { it.id != id }
            val validation = validateName(trimmed, currentSlots = others)
            if (validation != null) return@withLock Result.failure(validation)
            val updated = current.map { if (it.id == id) target.copy(name = trimmed) else it }
            persistSlots(updated)
            Result.success(Unit)
        }

    override suspend fun deleteSlot(id: String): Result<Unit> =
        mutex.withLock {
            ensureMigratedIfEmpty()
            if (id == ExportPresetSlot.DEFAULT_ID) {
                return@withLock Result.failure(IllegalArgumentException("cannot delete default slot"))
            }
            val current = decodeSlotsOrNull(currentJson()).orEmpty()
            val target = current.firstOrNull { it.id == id }
                ?: return@withLock Result.failure(NoSuchElementException("slot not found: $id"))
            val activeId = appPreferences.activeExportPresetId.first() ?: ExportPresetSlot.DEFAULT_ID
            val remaining = current.filter { it.id != id }
            persistSlots(remaining)
            deleteLogoFileIfUnused(target.watermarkConfig.logoPath, remaining)
            if (activeId == id) {
                val fallback = remaining.firstOrNull { it.id == ExportPresetSlot.DEFAULT_ID }
                    ?: remaining.firstOrNull()
                    ?: blankDefaultSlot()
                appPreferences.setActiveExportPresetId(fallback.id)
                applySlotToLegacyStores(fallback)
            }
            Result.success(Unit)
        }

    override suspend fun selectSlot(id: String): Result<Unit> =
        mutex.withLock {
            ensureMigratedIfEmpty()
            val current = decodeSlotsOrNull(currentJson()).orEmpty()
            val target = current.firstOrNull { it.id == id }
                ?: return@withLock Result.failure(NoSuchElementException("slot not found: $id"))
            appPreferences.setActiveExportPresetId(id)
            applySlotToLegacyStores(target)
            Result.success(Unit)
        }

    override suspend fun syncActiveSlotExport(preset: ExportPreset) {
        mutex.withLock {
            ensureMigratedIfEmpty()
            updateActiveSlotInPlace { it.copy(exportPreset = preset) }
        }
    }

    override suspend fun syncActiveSlotWatermark(config: WatermarkConfig) {
        mutex.withLock {
            ensureMigratedIfEmpty()
            updateActiveSlotInPlace { it.copy(watermarkConfig = config) }
        }
    }

    override suspend fun syncActiveSlotCombine(config: CombineConfig) {
        mutex.withLock {
            ensureMigratedIfEmpty()
            updateActiveSlotInPlace { it.copy(combineConfig = config) }
        }
    }

    private suspend fun ensureMigratedIfEmpty() {
        val raw = currentJson()
        if (!raw.isNullOrBlank() && decodeSlotsOrNull(raw)?.isNotEmpty() == true) return
        val defaultSlot = buildDefaultSlotFromLegacyStores()
        persistSlots(listOf(defaultSlot))
        if (appPreferences.activeExportPresetId.first().isNullOrBlank()) {
            appPreferences.setActiveExportPresetId(ExportPresetSlot.DEFAULT_ID)
        }
    }

    private suspend fun updateActiveSlotInPlace(transform: (ExportPresetSlot) -> ExportPresetSlot) {
        val current = decodeSlotsOrNull(currentJson()).orEmpty().ifEmpty { listOf(blankDefaultSlot()) }
        val activeId = appPreferences.activeExportPresetId.first() ?: ExportPresetSlot.DEFAULT_ID
        val updated =
            current.map { slot ->
                if (slot.id == activeId) transform(slot) else slot
            }
        if (updated != current) persistSlots(updated)
    }

    private suspend fun buildDefaultSlotFromLegacyStores(): ExportPresetSlot {
        val exportPreset = readExportPresetFromLegacy()
        val watermark = watermarkPreferences.watermarkConfigFlow.first()
        val combine = combinePreferences.configFlow.first()
        return ExportPresetSlot(
            id = ExportPresetSlot.DEFAULT_ID,
            name = DEFAULT_SLOT_NAME,
            exportPreset = exportPreset,
            watermarkConfig = watermark,
            combineConfig = combine,
        )
    }

    private suspend fun readExportPresetFromLegacy(): ExportPreset =
        ExportPreset(
            format = ExportFormat.fromName(appPreferences.exportFormat.first()),
            includeBefore = appPreferences.exportIncludeBefore.first(),
            includeAfter = appPreferences.exportIncludeAfter.first(),
            includeCombined = appPreferences.exportIncludeCombined.first(),
            applyCombineConfig = appPreferences.exportApplyCombineConfig.first(),
        )

    private suspend fun applySlotToLegacyStores(slot: ExportPresetSlot) {
        appPreferences.saveExportPreset(
            format = slot.exportPreset.format.name,
            includeBefore = slot.exportPreset.includeBefore,
            includeAfter = slot.exportPreset.includeAfter,
            includeCombined = slot.exportPreset.includeCombined,
            applyCombineConfig = slot.exportPreset.applyCombineConfig,
        )
        watermarkPreferences.saveConfig(slot.watermarkConfig)
        combinePreferences.saveConfig(slot.combineConfig)
    }

    private suspend fun currentJson(): String? = appPreferences.exportPresetSlotsJson.first()

    private suspend fun persistSlots(slots: List<ExportPresetSlot>) {
        appPreferences.setExportPresetSlotsJson(
            json.encodeToString(ListSerializer(ExportPresetSlot.serializer()), slots),
        )
    }

    private fun decodeSlotsOrNull(raw: String?): List<ExportPresetSlot>? {
        if (raw.isNullOrBlank()) return null
        return runCatching {
            json.decodeFromString(ListSerializer(ExportPresetSlot.serializer()), raw)
        }.onFailure { Timber.w(it, "failed to decode export preset slots") }
            .getOrNull()
    }

    private fun blankDefaultSlot(): ExportPresetSlot =
        ExportPresetSlot(
            id = ExportPresetSlot.DEFAULT_ID,
            name = DEFAULT_SLOT_NAME,
            exportPreset = ExportPreset(),
            watermarkConfig = WatermarkConfig(),
            combineConfig = CombineConfig(),
        )

    private fun validateName(
        name: String,
        currentSlots: List<ExportPresetSlot>,
    ): Throwable? =
        when {
            name.isEmpty() -> IllegalArgumentException("name must not be blank")
            name.length > ExportPresetSlot.MAX_NAME_LENGTH ->
                IllegalArgumentException("name too long")

            currentSlots.any { it.name == name } -> IllegalStateException("duplicate name")
            else -> null
        }

    private suspend fun copyLogoFileForNewSlot(sourcePath: String): String =
        withContext(Dispatchers.IO) {
            if (sourcePath.isBlank()) return@withContext ""
            val source = File(sourcePath)
            if (!source.exists() || !source.isFile) return@withContext ""
            val targetDir = File(context.filesDir, WATERMARK_LOGO_DIR)
            if (!targetDir.exists()) targetDir.mkdirs()
            val targetFile = File(targetDir, "logo_${System.currentTimeMillis()}.png")
            runCatching { source.copyTo(targetFile, overwrite = false) }
                .onFailure { Timber.w(it, "logo copy failed: %s", sourcePath) }
                .map { it.absolutePath }
                .getOrDefault("")
        }

    private suspend fun deleteLogoFileIfUnused(
        path: String,
        remainingSlots: List<ExportPresetSlot>,
    ) {
        if (path.isBlank()) return
        if (remainingSlots.any { it.watermarkConfig.logoPath == path }) return
        withContext(Dispatchers.IO) {
            runCatching { File(path).delete() }
                .onFailure { Timber.w(it, "logo delete failed: %s", path) }
        }
    }

    private companion object {
        const val WATERMARK_LOGO_DIR = "watermark_logo"
        const val DEFAULT_SLOT_NAME = "기본"
    }
}
