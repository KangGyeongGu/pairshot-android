package com.pairshot.core.storage

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

data class ZipImageEntry(
    val uri: Uri,
    val entryPath: String,
)

@Singleton
class ZipManager
@Inject
constructor(
    @ApplicationContext private val context: Context,
) {
    suspend fun createZip(
        entries: List<ZipImageEntry>,
        outputUri: Uri,
        onProgress: (current: Int, total: Int) -> Unit,
    ) = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        resolver.openOutputStream(outputUri)?.use { outputStream ->
            ZipOutputStream(outputStream.buffered()).use { zip ->
                entries.forEachIndexed { index, entry ->
                    resolver.openInputStream(entry.uri)?.use { input ->
                        zip.putNextEntry(ZipEntry(entry.entryPath))
                        input.copyTo(zip)
                        zip.closeEntry()
                    }
                    onProgress(index + 1, entries.size)
                }
            }
        } ?: error("cannot open SAF output stream: $outputUri")
    }

    suspend fun createZipToFile(
        entries: List<ZipImageEntry>,
        outputFile: java.io.File,
        onProgress: (current: Int, total: Int) -> Unit,
    ) = withContext(Dispatchers.IO) {
        outputFile.parentFile?.mkdirs()
        val resolver = context.contentResolver
        outputFile.outputStream().buffered().use { outputStream ->
            ZipOutputStream(outputStream).use { zip ->
                entries.forEachIndexed { index, entry ->
                    resolver.openInputStream(entry.uri)?.use { input ->
                        zip.putNextEntry(ZipEntry(entry.entryPath))
                        input.copyTo(zip)
                        zip.closeEntry()
                    }
                    onProgress(index + 1, entries.size)
                }
            }
        }
    }
}
