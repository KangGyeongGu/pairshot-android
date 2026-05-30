package com.pairshot.feature.album.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.pairshot.core.ui.component.PairShotTopMenu
import com.pairshot.core.ui.component.PairShotTopMenuDivider
import com.pairshot.core.ui.component.PairShotTopMenuItem
import com.pairshot.core.ui.component.PairShotTopMenuItemText
import com.pairshot.feature.album.R
import com.pairshot.core.ui.R as CoreR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumDetailTopBar(
    title: String,
    isSelectionMode: Boolean,
    selectedCount: Int,
    onNavigateBack: () -> Unit,
    onExitSelection: () -> Unit,
    onAddPairsClick: () -> Unit,
    onRenameClick: () -> Unit,
    onDeleteAlbumClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    CenterAlignedTopAppBar(
        modifier = modifier,
        title = {
            Text(
                text =
                if (isSelectionMode) {
                    pluralStringResource(
                        R.plurals.album_topbar_selection_count,
                        selectedCount,
                        selectedCount,
                    )
                } else {
                    title
                },
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        navigationIcon = {
            if (isSelectionMode) {
                IconButton(onClick = onExitSelection) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.album_desc_deselect),
                    )
                }
            } else {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(CoreR.string.common_desc_back),
                    )
                }
            }
        },
        actions = {
            if (!isSelectionMode) {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = stringResource(CoreR.string.common_desc_more),
                    )
                }
                PairShotTopMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                ) {
                    PairShotTopMenuItem(
                        text = {
                            PairShotTopMenuItemText(title = stringResource(R.string.album_button_add_pair))
                        },
                        onClick = {
                            menuExpanded = false
                            onAddPairsClick()
                        },
                    )
                    PairShotTopMenuDivider()
                    PairShotTopMenuItem(
                        text = {
                            PairShotTopMenuItemText(title = stringResource(R.string.album_menu_rename))
                        },
                        onClick = {
                            menuExpanded = false
                            onRenameClick()
                        },
                    )
                    PairShotTopMenuDivider()
                    PairShotTopMenuItem(
                        text = {
                            PairShotTopMenuItemText(
                                title = stringResource(R.string.album_menu_delete),
                                titleColor = MaterialTheme.colorScheme.error,
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onDeleteAlbumClick()
                        },
                    )
                }
            }
        },
        colors =
        TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onBackground,
            navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
            actionIconContentColor = MaterialTheme.colorScheme.onBackground,
        ),
    )
}
