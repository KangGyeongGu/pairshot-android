package com.pairshot.core.storage

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.io.FileNotFoundException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaSourceVerifier
@Inject
constructor(
    @ApplicationContext private val context: Context,
) {
    fun exists(uri: Uri): Boolean =
        try {
            context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { true } ?: false
        } catch (e: FileNotFoundException) {
            Timber.d(e, "media source missing: %s", uri)
            false
        } catch (e: SecurityException) {
            Timber.w(e, "media source permission denied: %s", uri)
            false
        } catch (e: IllegalArgumentException) {
            Timber.w(e, "media source uri invalid: %s", uri)
            false
        }

    fun exists(uriString: String?): Boolean {
        if (uriString.isNullOrBlank()) return false
        return exists(Uri.parse(uriString))
    }
}
