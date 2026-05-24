package com.pairshot.feature.home.dialog

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.pairshot.core.designsystem.PairShotSpacing
import com.pairshot.core.infra.location.LocationResult
import com.pairshot.core.ui.component.PairShotDialog
import com.pairshot.feature.home.R
import com.pairshot.core.ui.R as CoreR

@Composable
fun CreateAlbumDialog(
    currentLocation: LocationResult?,
    onFetchLocation: () -> Unit,
    onConfirm: (name: String, address: String?, latitude: Double?, longitude: Double?) -> Unit,
    onDismiss: () -> Unit,
) {
    var albumName by remember { mutableStateOf(currentLocation?.shortAddress ?: "") }

    val locationPermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions(),
        ) { grants ->
            val granted =
                grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                    grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            if (granted) onFetchLocation()
        }

    LaunchedEffect(Unit) {
        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            ),
        )
    }

    LaunchedEffect(currentLocation) {
        if (currentLocation != null && albumName.isBlank()) {
            albumName = currentLocation.shortAddress ?: ""
        }
    }

    PairShotDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.home_dialog_album_create_title),
                style = MaterialTheme.typography.titleMedium,
            )
        },
        text = {
            val locationAddress = currentLocation?.address
            Column(modifier = Modifier.imePadding()) {
                Text(
                    text = stringResource(R.string.home_dialog_album_create_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(PairShotSpacing.md))
                OutlinedTextField(
                    value = albumName,
                    onValueChange = { albumName = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            text = stringResource(R.string.home_dialog_album_create_placeholder),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    supportingText =
                    if (locationAddress != null) {
                        {
                            Text(
                                text = locationAddress,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        null
                    },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        albumName.trim(),
                        currentLocation?.address,
                        currentLocation?.latitude,
                        currentLocation?.longitude,
                    )
                },
                enabled = albumName.isNotBlank(),
            ) {
                Text(
                    text = stringResource(CoreR.string.common_button_confirm),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(CoreR.string.common_button_cancel),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        },
    )
}
