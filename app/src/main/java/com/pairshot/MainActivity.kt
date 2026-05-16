package com.pairshot

import android.os.Bundle
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.metrics.performance.JankStats
import androidx.metrics.performance.PerformanceMetricsState
import com.pairshot.app.navigation.PairShotNavHost
import com.pairshot.app.navigation.SelectionActionViewModel
import com.pairshot.app.navigation.SelectionMessage
import com.pairshot.app.navigation.StartupDecisionViewModel
import com.pairshot.app.navigation.effect.ExportShareEffect
import com.pairshot.app.navigation.effect.SaveZipToDocumentEffect
import com.pairshot.core.ads.di.AdsEntryPoint
import com.pairshot.core.ads.initializer.AdsInitializer
import com.pairshot.core.designsystem.PairShotSnackbarTokens
import com.pairshot.core.designsystem.PairShotSpacing
import com.pairshot.core.designsystem.PairShotTheme
import com.pairshot.core.ui.component.PairShotSnackbarController
import com.pairshot.core.ui.component.PairShotSnackbarHost
import com.pairshot.core.ui.component.SnackbarEvent
import com.pairshot.core.ui.component.SnackbarVariant
import com.pairshot.core.ui.component.TopProgressPill
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.EntryPointAccessors
import timber.log.Timber
import javax.inject.Inject

private const val SNACKBAR_OFFSET_WHEN_PROGRESS_DP = 80

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject
    lateinit var adsInitializer: AdsInitializer

    private lateinit var jankStats: JankStats
    private var onStartupReady: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        var startupReady = false
        splash.setKeepOnScreenCondition { !startupReady }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        adsInitializer.initialize(this)
        this.onStartupReady = { startupReady = true }

        val metricsStateHolder = PerformanceMetricsState.getHolderForHierarchy(window.decorView)
        jankStats =
            JankStats.createAndTrack(window) { frameData ->
                if (frameData.isJank && BuildConfig.DEBUG) {
                    Timber.tag("JankStats").w(frameData.toString())
                }
            }
        metricsStateHolder.state?.putState("screen", "Camera")

        setContent {
            PairShotTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    AppRootContent(
                        onRouteChanged = { route ->
                            metricsStateHolder.state?.putState("screen", route)
                        },
                        onStartupReady = { onStartupReady?.invoke() },
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        jankStats.isTrackingEnabled = true
    }

    override fun onPause() {
        super.onPause()
        jankStats.isTrackingEnabled = false
    }
}

@Composable
private fun AppRootContent(
    onRouteChanged: (String) -> Unit,
    onStartupReady: () -> Unit,
) {
    val selectionVm: SelectionActionViewModel = hiltViewModel()
    val startupVm: StartupDecisionViewModel = hiltViewModel()
    val initialRoute by startupVm.initialRoute.collectAsStateWithLifecycle()
    val progress by selectionVm.progress.collectAsStateWithLifecycle()
    val snackbarController = remember { PairShotSnackbarController() }

    LaunchedEffect(initialRoute) {
        if (initialRoute != null) onStartupReady()
    }
    val resolvedRoute = initialRoute ?: return

    val context = LocalContext.current
    val activity = LocalActivity.current
    val interstitialAdController =
        remember(context) {
            EntryPointAccessors
                .fromApplication(
                    context.applicationContext,
                    AdsEntryPoint::class.java,
                ).interstitialAdController()
        }

    LaunchedEffect(Unit) {
        selectionVm.messages.collect { msg ->
            snackbarController.show(msg.toSnackbarEvent())
        }
    }

    val saveSelectedToDevice =
        remember(interstitialAdController, activity, selectionVm) {
            { ids: Set<Long> ->
                val act = activity
                if (act == null) {
                    selectionVm.saveSelectionToDevice(ids)
                } else {
                    interstitialAdController.showIfAvailable(act) {
                        selectionVm.saveSelectionToDevice(ids)
                    }
                }
            }
        }

    ExportShareEffect(actions = selectionVm.exportAction)

    SaveZipToDocumentEffect(
        requests = selectionVm.saveDocumentRequests,
        onResult = selectionVm::onSaveDocumentResult,
    )

    Box(modifier = Modifier.fillMaxSize()) {
        PairShotNavHost(
            onDestinationChanged = onRouteChanged,
            onShareSelected = selectionVm::shareSelection,
            onSaveSelectedToDevice = saveSelectedToDevice,
            startDestination = resolvedRoute,
        )

        progress?.let { p ->
            TopProgressPill(
                label =
                    pluralStringResource(
                        R.plurals.progress_label_with_count,
                        p.total,
                        p.label.asString(),
                        p.total,
                    ),
                progress = if (p.total > 0) p.current.toFloat() / p.total else 0f,
                progressText = "${p.current}/${p.total}",
                modifier =
                    Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(top = PairShotSnackbarTokens.topOffset),
            )
        }

        PairShotSnackbarHost(
            controller = snackbarController,
            modifier =
                Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(
                        top =
                            if (progress != null) {
                                SNACKBAR_OFFSET_WHEN_PROGRESS_DP.dp
                            } else {
                                PairShotSnackbarTokens.topOffset
                            },
                    ),
        )
    }
}

private fun SelectionMessage.toSnackbarEvent(): SnackbarEvent {
    val variant =
        when (this) {
            is SelectionMessage.Success -> SnackbarVariant.SUCCESS
            is SelectionMessage.Warning -> SnackbarVariant.WARNING
            is SelectionMessage.Error -> SnackbarVariant.ERROR
        }
    return SnackbarEvent(message = text, variant = variant)
}
