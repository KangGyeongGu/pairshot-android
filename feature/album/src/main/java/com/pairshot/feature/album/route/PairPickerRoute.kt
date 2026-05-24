package com.pairshot.feature.album.route

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pairshot.feature.album.screen.PairPickerScreen
import com.pairshot.feature.album.viewmodel.PairPickerEvent
import com.pairshot.feature.album.viewmodel.PairPickerUiState
import com.pairshot.feature.album.viewmodel.PairPickerViewModel

@Composable
fun PairPickerRoute(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PairPickerViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnNavigateBack by rememberUpdatedState(onNavigateBack)

    LaunchedEffect(viewModel.events) {
        viewModel.events.collect { event ->
            when (event) {
                is PairPickerEvent.NavigateBack -> currentOnNavigateBack()
            }
        }
    }

    when (val state = uiState) {
        PairPickerUiState.Loading -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }

        is PairPickerUiState.Ready -> {
            PairPickerScreen(
                state = state,
                onToggle = viewModel::toggleSelection,
                onConfirm = viewModel::confirmSelection,
                onClose = onNavigateBack,
                modifier = modifier,
            )
        }
    }
}
