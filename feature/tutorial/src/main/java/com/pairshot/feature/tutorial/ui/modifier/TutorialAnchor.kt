package com.pairshot.feature.tutorial.ui.modifier

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import com.pairshot.core.domain.tutorial.AnchorBounds
import com.pairshot.core.domain.tutorial.AnchorKey
import com.pairshot.core.domain.tutorial.TutorialAnchorReporter
import dagger.hilt.android.EntryPointAccessors

@Composable
fun Modifier.tutorialAnchor(key: AnchorKey): Modifier {
    val reporter = rememberTutorialAnchorReporter()
    DisposableEffect(key) {
        onDispose { reporter.report(key, null) }
    }
    return this then
        Modifier.onGloballyPositioned { coords ->
            val rect = coords.boundsInWindow()
            reporter.report(
                key,
                AnchorBounds(
                    left = rect.left,
                    top = rect.top,
                    width = rect.width,
                    height = rect.height,
                ),
            )
        }
}

@Composable
private fun rememberTutorialAnchorReporter(): TutorialAnchorReporter {
    val context = LocalContext.current
    return remember(context) {
        EntryPointAccessors
            .fromApplication(context.applicationContext, TutorialAnchorEntryPoint::class.java)
            .tutorialAnchorReporter()
    }
}
