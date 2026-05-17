package com.pairshot.feature.settings.screen

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.pairshot.core.adsui.component.PairShotBannerAd
import com.pairshot.core.designsystem.PairShotCard
import com.pairshot.core.designsystem.PairShotScreen
import com.pairshot.core.designsystem.PairShotSpacing
import com.pairshot.feature.settings.R
import com.pairshot.core.ui.R as CoreR

private val licenseItems =
    listOf(
        LicenseItem(
            name = "Jetpack Compose",
            author = "Google",
            license = "Apache License 2.0",
            url = "https://developer.android.com/jetpack/compose",
        ),
        LicenseItem(
            name = "CameraX",
            author = "Google",
            license = "Apache License 2.0",
            url = "https://developer.android.com/training/camerax",
        ),
        LicenseItem(
            name = "Hilt / Dagger",
            author = "Google",
            license = "Apache License 2.0",
            url = "https://dagger.dev/hilt/",
        ),
        LicenseItem(
            name = "Room",
            author = "Google",
            license = "Apache License 2.0",
            url = "https://developer.android.com/training/data-storage/room",
        ),
        LicenseItem(
            name = "Glide",
            author = "bumptech",
            license = "BSD, part MIT and Apache 2.0",
            url = "https://github.com/bumptech/glide",
        ),
        LicenseItem(
            name = "Navigation Compose",
            author = "Google",
            license = "Apache License 2.0",
            url = "https://developer.android.com/jetpack/compose/navigation",
        ),
        LicenseItem(
            name = "DataStore",
            author = "Google",
            license = "Apache License 2.0",
            url = "https://developer.android.com/topic/libraries/architecture/datastore",
        ),
        LicenseItem(
            name = "Material 3",
            author = "Google",
            license = "Apache License 2.0",
            url = "https://m3.material.io/",
        ),
        LicenseItem(
            name = "kotlinx.coroutines",
            author = "JetBrains",
            license = "Apache License 2.0",
            url = "https://github.com/Kotlin/kotlinx.coroutines",
        ),
        LicenseItem(
            name = "kotlinx.serialization",
            author = "JetBrains",
            license = "Apache License 2.0",
            url = "https://github.com/Kotlin/kotlinx.serialization",
        ),
        LicenseItem(
            name = "Google Play Services Location",
            author = "Google",
            license = "Android Software Development Kit License",
            url = "https://developers.google.com/android/guides/overview",
        ),
        LicenseItem(
            name = "AndroidX SplashScreen",
            author = "Google",
            license = "Apache License 2.0",
            url = "https://developer.android.com/develop/ui/views/launch/splash-screen",
        ),
        LicenseItem(
            name = "AndroidX ExifInterface",
            author = "Google",
            license = "Apache License 2.0",
            url = "https://developer.android.com/jetpack/androidx/releases/exifinterface",
        ),
        LicenseItem(
            name = "AndroidX Lifecycle",
            author = "Google",
            license = "Apache License 2.0",
            url = "https://developer.android.com/jetpack/androidx/releases/lifecycle",
        ),
        LicenseItem(
            name = "AndroidX Activity Compose",
            author = "Google",
            license = "Apache License 2.0",
            url = "https://developer.android.com/jetpack/androidx/releases/activity",
        ),
    )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicenseScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.license_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = stringResource(CoreR.string.common_desc_back),
                        )
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                    ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
        ) {
            PairShotBannerAd()
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
            ) {
                items(
                    items = licenseItems,
                    key = { it.name },
                ) { item ->
                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(item.url))
                                    context.startActivity(intent)
                                }.padding(
                                    horizontal = PairShotScreen.horizontalPadding,
                                    vertical = PairShotCard.innerPadding,
                                ),
                    ) {
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "${item.author} · ${item.license}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                }
            }
        }
    }
}
