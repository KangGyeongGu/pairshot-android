package com.pairshot.feature.settings.dialog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.pairshot.core.designsystem.PairShotSpacing
import com.pairshot.core.model.AppTheme
import com.pairshot.core.ui.component.PairShotBottomSheet
import com.pairshot.feature.settings.R
import com.pairshot.core.ui.R as CoreR

@Composable
internal fun ThemeDialog(
    current: AppTheme,
    onSelect: (AppTheme) -> Unit,
    onDismiss: () -> Unit,
) {
    var selected by remember { mutableStateOf(current) }

    PairShotBottomSheet(onDismissRequest = onDismiss) {
        Text(
            text = stringResource(R.string.settings_dialog_theme_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(PairShotSpacing.md))

        AppTheme.entries.forEach { option ->
            val labelRes =
                when (option) {
                    AppTheme.SYSTEM -> R.string.settings_theme_system
                    AppTheme.LIGHT -> R.string.settings_theme_light
                    AppTheme.DARK -> R.string.settings_theme_dark
                }
            Row(
                modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable { selected = option }
                    .padding(vertical = PairShotSpacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(
                    selected = selected == option,
                    onClick = { selected = option },
                    colors =
                    RadioButtonDefaults.colors(
                        selectedColor = MaterialTheme.colorScheme.primary,
                    ),
                )
                Text(
                    text = stringResource(labelRes),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = PairShotSpacing.sm),
                )
            }
        }

        Spacer(modifier = Modifier.height(PairShotSpacing.sm))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(onClick = onDismiss) {
                Text(stringResource(CoreR.string.common_button_cancel))
            }
            TextButton(
                onClick = {
                    onSelect(selected)
                    onDismiss()
                },
            ) {
                Text(
                    stringResource(R.string.settings_dialog_apply),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
