package com.pairshot.core.ui.component

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.pairshot.core.designsystem.ModalShape
import com.pairshot.core.designsystem.PairShotDialogTokens
import com.pairshot.core.designsystem.PairShotSpacing

private val DialogHorizontalMargin = PairShotSpacing.xxl
private val DialogConfirmMaxWidth = PairShotDialogTokens.confirmMaxWidth
private val DialogOptionMaxWidth = PairShotDialogTokens.optionMaxWidth

@Composable
fun confirmDialogWidth(): Dp {
    val windowInfo = LocalWindowInfo.current
    val density = LocalDensity.current
    val sw = with(density) { windowInfo.containerSize.width.toDp() }
    return (sw - DialogHorizontalMargin).coerceAtMost(DialogConfirmMaxWidth)
}

@Composable
fun optionDialogWidth(): Dp {
    val windowInfo = LocalWindowInfo.current
    val density = LocalDensity.current
    val sw = with(density) { windowInfo.containerSize.width.toDp() }
    return (sw - DialogHorizontalMargin).coerceAtMost(DialogOptionMaxWidth)
}

@Composable
fun inputDialogWidth(): Dp {
    val windowInfo = LocalWindowInfo.current
    val density = LocalDensity.current
    val sw = with(density) { windowInfo.containerSize.width.toDp() }
    return (sw - DialogHorizontalMargin).coerceAtMost(DialogOptionMaxWidth)
}

@Composable
fun PairShotDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: @Composable (() -> Unit)? = null,
    icon: @Composable (() -> Unit)? = null,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    properties: DialogProperties = DialogProperties(),
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = confirmButton,
        dismissButton = dismissButton,
        icon = icon,
        title = title,
        text = text,
        modifier = modifier,
        shape = ModalShape,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = if (isSystemInDarkTheme()) PairShotSpacing.lg else PairShotSpacing.sm,
        properties = properties,
    )
}
