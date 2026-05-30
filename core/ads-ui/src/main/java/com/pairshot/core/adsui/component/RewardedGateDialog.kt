package com.pairshot.core.adsui.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.pairshot.core.adsui.R
import com.pairshot.core.domain.premium.PremiumFeature
import com.pairshot.core.ui.component.PairShotDialog
import com.pairshot.core.ui.R as CoreUiR

@Composable
fun RewardedGateDialog(
    feature: PremiumFeature,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bodyResId =
        when (feature) {
            PremiumFeature.WATERMARK_DETAIL -> R.string.rewarded_gate_body_watermark_detail
            PremiumFeature.COMBINE_DETAIL -> R.string.rewarded_gate_body_combine_detail
            PremiumFeature.EXPORT_PRESET -> R.string.rewarded_gate_body_export_preset
        }
    PairShotDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.rewarded_gate_title),
                style = MaterialTheme.typography.titleMedium,
            )
        },
        text = {
            Text(
                text = stringResource(bodyResId),
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = stringResource(R.string.rewarded_gate_confirm),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(CoreUiR.string.common_button_cancel))
            }
        },
    )
}
