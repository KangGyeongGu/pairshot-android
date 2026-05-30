package com.pairshot.app.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.pairshot.core.designsystem.PairShotMotionTokens
import com.pairshot.core.navigation.AfterCamera
import com.pairshot.core.navigation.AlbumDetail
import com.pairshot.core.navigation.Camera
import com.pairshot.core.navigation.CombineSettings
import com.pairshot.core.navigation.ExportSettings
import com.pairshot.core.navigation.Home
import com.pairshot.core.navigation.License
import com.pairshot.core.navigation.PairPicker
import com.pairshot.core.navigation.PairPreview
import com.pairshot.core.navigation.Paywall
import com.pairshot.core.navigation.Settings
import com.pairshot.core.navigation.SettingsHighlight
import com.pairshot.core.navigation.WatermarkSettings
import com.pairshot.feature.album.route.AlbumDetailRoute
import com.pairshot.feature.album.route.PairPickerRoute
import com.pairshot.feature.camera.route.AfterCameraRoute
import com.pairshot.feature.camera.route.CameraRoute
import com.pairshot.feature.exportsettings.route.ExportSettingsRoute
import com.pairshot.feature.home.route.HomeRoute
import com.pairshot.feature.pairpreview.route.PairPreviewRoute
import com.pairshot.feature.paywall.PaywallRoute
import com.pairshot.feature.settings.route.CombineSettingsRoute
import com.pairshot.feature.settings.route.SettingsRoute
import com.pairshot.feature.settings.route.WatermarkSettingsRoute
import com.pairshot.feature.settings.screen.LicenseScreen

@Composable
fun PairShotNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    onDestinationChange: (String) -> Unit = {},
    onShareSelection: (Set<Long>) -> Unit = {},
    onSaveSelectionToDevice: (Set<Long>) -> Unit = {},
    startDestination: Any = Camera(),
) {
    val currentOnDestinationChange by rememberUpdatedState(onDestinationChange)
    DisposableEffect(navController) {
        val listener =
            NavController.OnDestinationChangedListener { _, destination, _ ->
                val route =
                    destination.route?.substringAfterLast(".")?.substringBefore("?")
                        ?: "Unknown"
                currentOnDestinationChange(route)
            }
        navController.addOnDestinationChangedListener(listener)
        onDispose { navController.removeOnDestinationChangedListener(listener) }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier =
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        enterTransition = {
            fadeIn(animationSpec = PairShotMotionTokens.enterTween())
        },
        exitTransition = {
            fadeOut(animationSpec = PairShotMotionTokens.exitTween())
        },
        popEnterTransition = {
            fadeIn(animationSpec = PairShotMotionTokens.popEnterTween())
        },
        popExitTransition = {
            fadeOut(animationSpec = PairShotMotionTokens.popExitTween())
        },
        sizeTransform = { null },
    ) {
        composable<Home> {
            HomeRoute(
                onNavigateToPairPreview = { pairId -> navController.navigate(PairPreview(pairId)) },
                onNavigateToAfterCamera = { pairId ->
                    navController.navigate(AfterCamera(initialPairId = pairId))
                },
                onNavigateToBeforeRetake = { pairId ->
                    navController.navigate(Camera(replaceBeforeForPairId = pairId))
                },
                onNavigateToAlbumDetail = { albumId -> navController.navigate(AlbumDetail(albumId)) },
                onNavigateToCamera = { navController.navigate(Camera()) },
                onNavigateToPaywall = { trigger ->
                    navController.navigate(Paywall(dismissible = true, trigger = trigger))
                },
                onNavigateToSettings = { navController.navigate(Settings()) },
                onNavigateToExportSettings = { ids ->
                    navController.navigate(ExportSettings(ids.joinToString(",")))
                },
                onShareSelection = onShareSelection,
                onSaveToDevice = onSaveSelectionToDevice,
            )
        }
        composable<AlbumDetail> {
            AlbumDetailRoute(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPairPreview = { pairId -> navController.navigate(PairPreview(pairId)) },
                onNavigateToAfterCamera = { pairId, albumId ->
                    navController.navigate(AfterCamera(initialPairId = pairId, albumId = albumId))
                },
                onNavigateToBeforeRetake = { pairId ->
                    navController.navigate(Camera(replaceBeforeForPairId = pairId))
                },
                onNavigateToCamera = { albumId ->
                    navController.navigate(Camera(albumId = albumId))
                },
                onNavigateToPairPicker = { albumId -> navController.navigate(PairPicker(albumId)) },
                onNavigateToExportSettings = { ids ->
                    navController.navigate(ExportSettings(ids.joinToString(",")))
                },
                onShareSelection = onShareSelection,
                onSaveSelectionToDevice = onSaveSelectionToDevice,
            )
        }
        composable<PairPicker> {
            PairPickerRoute(
                onNavigateBack = { navController.popBackStack() },
            )
        }
        composable<Camera> {
            CameraRoute(
                onNavigateBack = {
                    if (!navController.popBackStack()) {
                        navController.navigate(Home) {
                            popUpTo<Camera> { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                },
                onNavigateToPaywall = { trigger ->
                    navController.navigate(Paywall(dismissible = true, trigger = trigger))
                },
            )
        }
        composable<Paywall> { entry ->
            val paywall: Paywall = entry.toRoute()
            PaywallRoute(
                dismissible = paywall.dismissible,
                trigger = paywall.trigger,
                onDismiss = {
                    if (!navController.popBackStack()) {
                        navController.navigate(Camera()) {
                            popUpTo<Paywall> { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                },
                onEntitlement = {
                    if (paywall.dismissible) {
                        navController.popBackStack()
                    } else {
                        navController.navigate(Camera()) {
                            popUpTo<Paywall> { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                },
            )
        }
        composable<AfterCamera> {
            AfterCameraRoute(
                onNavigateBack = { navController.popBackStack() },
            )
        }
        dialog<PairPreview>(
            dialogProperties =
            DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnClickOutside = true,
            ),
        ) {
            PairPreviewRoute(
                onDismiss = { navController.popBackStack() },
                onShareSelection = { pairId -> onShareSelection(setOf(pairId)) },
                onSaveSelectionToDevice = { pairId -> onSaveSelectionToDevice(setOf(pairId)) },
                onNavigateToAfterCamera = { pairId ->
                    navController.navigate(AfterCamera(initialPairId = pairId))
                },
                onNavigateToBeforeRetake = { pairId ->
                    navController.navigate(Camera(replaceBeforeForPairId = pairId))
                },
            )
        }
        composable<ExportSettings> {
            ExportSettingsRoute(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToWatermarkSettings = {
                    navController.navigate(Settings(highlight = SettingsHighlight.WATERMARK))
                },
                onNavigateToCombineSettings = {
                    navController.navigate(Settings(highlight = SettingsHighlight.COMBINE))
                },
                onNavigateToPaywall = { trigger ->
                    navController.navigate(Paywall(dismissible = true, trigger = trigger))
                },
                onShare = onShareSelection,
                onSaveToDevice = onSaveSelectionToDevice,
            )
        }
        composable<Settings> {
            val settings: Settings = it.toRoute()
            SettingsRoute(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToLicense = { navController.navigate(License) },
                onNavigateToWatermarkSettings = { navController.navigate(WatermarkSettings) },
                onNavigateToCombineSettings = { navController.navigate(CombineSettings) },
                onNavigateToPaywall = { navController.navigate(Paywall(dismissible = true)) },
                highlight = settings.highlight,
            )
        }
        composable<License> {
            LicenseScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }
        composable<WatermarkSettings> {
            WatermarkSettingsRoute(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPaywall = { trigger ->
                    navController.navigate(Paywall(dismissible = true, trigger = trigger))
                },
            )
        }
        composable<CombineSettings> {
            CombineSettingsRoute(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPaywall = { trigger ->
                    navController.navigate(Paywall(dismissible = true, trigger = trigger))
                },
            )
        }
    }
}
