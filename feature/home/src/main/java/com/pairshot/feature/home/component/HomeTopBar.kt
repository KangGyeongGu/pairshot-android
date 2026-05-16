package com.pairshot.feature.home.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pairshot.core.designsystem.PairShotProBadge
import com.pairshot.core.designsystem.PairShotRadius
import com.pairshot.feature.home.R
import com.pairshot.core.ui.R as CoreR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTopBar(
    selectionMode: Boolean,
    selectedCount: Int,
    allSelected: Boolean,
    isProSubscriber: Boolean,
    onExitSelectionMode: () -> Unit,
    onToggleSelectAll: () -> Unit,
    onEnterSelectionMode: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TopAppBar(
        title = {
            if (selectionMode) {
                Text(
                    text =
                        pluralStringResource(
                            R.plurals.home_topbar_selection_count,
                            selectedCount,
                            selectedCount,
                        ),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            } else {
                BrandTitle(isProSubscriber = isProSubscriber)
            }
        },
        navigationIcon = {
            if (selectionMode) {
                IconButton(onClick = onExitSelectionMode) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.home_desc_deselect),
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        },
        actions = {
            if (selectionMode) {
                TextButton(onClick = onToggleSelectAll) {
                    Text(
                        text =
                            stringResource(
                                if (allSelected) R.string.home_button_deselect_all else R.string.home_button_select_all,
                            ),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            } else {
                IconButton(onClick = onEnterSelectionMode) {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = stringResource(R.string.home_desc_selection_mode),
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
                IconButton(onClick = onNavigateToSettings) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = stringResource(CoreR.string.common_desc_settings),
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        },
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
            ),
        modifier = modifier,
    )
}

@Composable
private fun BrandTitle(isProSubscriber: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = stringResource(R.string.home_topbar_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (isProSubscriber) {
            Spacer(modifier = Modifier.width(PairShotRadius.sm))
            PairShotProBadge()
        }
    }
}
