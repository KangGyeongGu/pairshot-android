package com.pairshot.core.ui.component.colorpicker

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.pairshot.core.ui.R
import com.pairshot.core.ui.component.PairShotDialog

@Composable
fun ColorPickerDialog(
    initialColor: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state = rememberColorPickerState(initialColor)
    PairShotDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = title,
        text = { ColorPickerContent(state = state) },
        confirmButton = {
            TextButton(onClick = { onConfirm(state.currentArgb()) }) {
                Text(stringResource(R.string.common_button_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_button_cancel))
            }
        },
    )
}
