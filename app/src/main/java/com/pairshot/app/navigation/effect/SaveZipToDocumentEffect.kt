package com.pairshot.app.navigation.effect

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.pairshot.app.navigation.SaveDocumentRequest
import com.pairshot.app.navigation.SaveDocumentResult
import com.pairshot.core.ads.di.AdsEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.IOException

private const val ZIP_MIME_TYPE = "application/zip"

@Composable
fun SaveZipToDocumentEffect(
    requests: Flow<SaveDocumentRequest>,
    onResult: (SaveDocumentResult) -> Unit,
) {
    val context = LocalContext.current
    var pending by remember { mutableStateOf<SaveDocumentRequest?>(null) }
    var launcherResult by remember { mutableStateOf<Pair<SaveDocumentRequest, Uri?>?>(null) }

    val interstitialAdController =
        remember(context) {
            EntryPointAccessors
                .fromApplication(
                    context.applicationContext,
                    AdsEntryPoint::class.java,
                ).interstitialAdController()
        }

    val launcher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.CreateDocument(ZIP_MIME_TYPE),
        ) { destUri ->
            val request = pending ?: return@rememberLauncherForActivityResult
            pending = null
            launcherResult = request to destUri
        }

    LaunchedEffect(Unit) {
        requests.collect { request ->
            pending = request
            val activity = context.findActivity()
            val launchSaf: () -> Unit = { launcher.launch(request.suggestedName) }
            if (activity != null) {
                interstitialAdController.showIfAvailable(activity, launchSaf)
            } else {
                launchSaf()
            }
        }
    }

    LaunchedEffect(launcherResult) {
        val snapshot = launcherResult ?: return@LaunchedEffect
        launcherResult = null
        val (request, destUri) = snapshot
        if (destUri == null) {
            onResult(SaveDocumentResult.Cancelled(request.sourceFilePath))
            return@LaunchedEffect
        }
        val copyResult =
            withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openOutputStream(destUri)?.use { output ->
                        File(request.sourceFilePath).inputStream().use { input ->
                            input.copyTo(output)
                        }
                    } ?: error("Cannot open output stream for $destUri")
                }
            }
        copyResult
            .onSuccess {
                onResult(
                    SaveDocumentResult.Saved(
                        sourceFilePath = request.sourceFilePath,
                        displayName = request.suggestedName,
                    ),
                )
            }.onFailure { error ->
                Timber.e(error, "zip save to SAF uri failed")
                val wrapped = if (error is IOException || error is SecurityException) {
                    error
                } else {
                    IOException(
                        "save failed",
                        error
                    )
                }
                onResult(
                    SaveDocumentResult.Failed(
                        sourceFilePath = request.sourceFilePath,
                        error = wrapped,
                    ),
                )
            }
    }
}

private tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
