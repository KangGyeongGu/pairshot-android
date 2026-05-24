package com.pairshot.core.data.repository

import android.content.Context
import android.net.Uri
import com.pairshot.core.datastore.WatermarkPreferences
import com.pairshot.core.domain.settings.WatermarkRepository
import com.pairshot.core.model.WatermarkConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

class WatermarkRepositoryImpl
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val watermarkPreferences: WatermarkPreferences,
) : WatermarkRepository {
    override val watermarkConfigFlow: Flow<WatermarkConfig> =
        watermarkPreferences.watermarkConfigFlow

    override suspend fun saveConfig(config: WatermarkConfig) {
        watermarkPreferences.saveConfig(config)
    }

    override suspend fun getConfig(): WatermarkConfig = watermarkPreferences.watermarkConfigFlow.first()

    override suspend fun saveLogoFile(sourceUri: String): String =
        withContext(Dispatchers.IO) {
            val logoDir = File(context.filesDir, "watermark_logo")
            if (!logoDir.exists()) {
                logoDir.mkdirs()
            }
            val destFile = File(logoDir, "logo_${System.currentTimeMillis()}.png")
            val uri = Uri.parse(sourceUri)
            val inputStream =
                context.contentResolver.openInputStream(uri)
                    ?: error("Cannot open input stream for logo: $sourceUri")
            inputStream.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            destFile.absolutePath
        }

    override suspend fun pruneOldLogoFiles(keepPath: String) =
        withContext(Dispatchers.IO) {
            val logoDir = File(context.filesDir, "watermark_logo")
            if (!logoDir.exists()) return@withContext
            logoDir.listFiles()?.forEach { existing ->
                if (existing.absolutePath != keepPath) existing.delete()
            }
        }

    override suspend fun removeLogoFile() =
        withContext(Dispatchers.IO) {
            val logoDir = File(context.filesDir, "watermark_logo")
            if (logoDir.exists()) {
                logoDir.listFiles()?.forEach { it.delete() }
            }
        }
}
