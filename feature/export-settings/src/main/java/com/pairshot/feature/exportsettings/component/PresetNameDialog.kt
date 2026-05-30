package com.pairshot.feature.exportsettings.component

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.pairshot.core.model.ExportPresetSlot
import com.pairshot.core.ui.component.PairShotDialog
import com.pairshot.feature.exportsettings.R
import com.pairshot.core.ui.R as CoreR

@Composable
fun PresetNameDialog(
    titleResId: Int,
    initialName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var input by remember { mutableStateOf(initialName) }
    PairShotDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = {
            Text(
                text = stringResource(titleResId),
                style = MaterialTheme.typography.titleMedium,
            )
        },
        text = {
            OutlinedTextField(
                value = input,
                onValueChange = { value ->
                    if (value.length <= ExportPresetSlot.MAX_NAME_LENGTH) {
                        input = value
                    }
                },
                singleLine = true,
                supportingText = {
                    Text(
                        text = "${input.length}/${ExportPresetSlot.MAX_NAME_LENGTH}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                },
                placeholder = {
                    Text(text = stringResource(R.string.export_preset_name_placeholder))
                },
            )
        },
        confirmButton = {
            Row {
                TextButton(
                    onClick = { onConfirm(input.trim()) },
                    enabled = input.trim().isNotEmpty(),
                ) {
                    Text(text = stringResource(CoreR.string.common_button_confirm))
                }
                TextButton(onClick = onDismiss) {
                    Text(text = stringResource(CoreR.string.common_button_cancel))
                }
            }
        },
    )
}
