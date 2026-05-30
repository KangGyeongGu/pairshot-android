package com.pairshot.app.navigation.effect

import android.app.Activity
import android.content.pm.ApplicationInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import com.google.android.play.core.ktx.launchReview
import com.google.android.play.core.ktx.requestReview
import com.google.android.play.core.review.ReviewManagerFactory
import com.google.android.play.core.review.testing.FakeReviewManager
import kotlinx.coroutines.flow.Flow
import timber.log.Timber

/**
 * Listens for first-successful-save events and launches the Google Play in-app
 * review flow on the host activity. The Play API decides whether the modal is
 * actually shown (quota / recently shown / store availability); we only
 * trigger once per install — the calling ViewModel guards the flag.
 *
 * Debug builds use [FakeReviewManager] which always succeeds without showing
 * the real modal, so the flag still flips during QA.
 */
@Composable
fun InAppReviewEffect(
    requests: Flow<Unit>,
    activity: Activity?,
) {
    val currentActivity by rememberUpdatedState(activity)
    LaunchedEffect(requests) {
        requests.collect {
            val host = currentActivity ?: return@collect
            runCatching {
                val isDebuggable =
                    (host.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
                val manager =
                    if (isDebuggable) FakeReviewManager(host) else ReviewManagerFactory.create(host)
                val info = manager.requestReview()
                manager.launchReview(host, info)
            }.onFailure { error ->
                Timber.w(error, "in-app review flow failed")
            }
        }
    }
}
