package com.pairshot.core.data.export

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShareImagePreparer
@Inject
constructor(
    @ApplicationContext private val context: Context,
) {
    fun prepareTempDir(name: String): File {
        val dir = File(context.cacheDir, name)
        dir.deleteRecursively()
        dir.mkdirs()
        return dir
    }

    fun prepareShareImageDir(): File {
        val dir = File(context.cacheDir, "share_images")
        dir.deleteRecursively()
        dir.mkdirs()
        return dir
    }

    fun getFileProviderUri(file: File): Uri {
        val authority = "${context.packageName}.fileprovider"
        return FileProvider.getUriForFile(context, authority, file)
    }

    fun copyFromContentUri(
        sourceUri: String,
        destFile: File,
    ) {
        val uri = Uri.parse(sourceUri)
        (
            context.contentResolver.openInputStream(uri)
                ?: throw IOException("cannot read source file: $sourceUri")
            ).use { input ->
            destFile.outputStream().use { output -> input.copyTo(output) }
        }
    }
}
