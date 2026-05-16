package com.pairshot.feature.settings.dialog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.pairshot.core.designsystem.PairShotSpacing
import com.pairshot.core.designsystem.PairShotScreen
import com.pairshot.core.designsystem.PairShotIconSize
import com.pairshot.core.designsystem.PairShotStroke
import com.pairshot.core.ui.component.PairShotBottomSheet
import com.pairshot.core.ui.component.PairShotDialog
import com.pairshot.core.ui.component.SettingsSliderItem
import com.pairshot.feature.settings.R
import kotlin.math.roundToInt
import com.pairshot.core.ui.R as CoreR

private val InputFieldMinHeight = PairShotIconSize.xl
private val InputErrorHeight = PairShotScreen.horizontalPadding
private const val JPEG_QUALITY_LOW = 75
private const val JPEG_QUALITY_HIGH = 85
private const val JPEG_QUALITY_BEST = 95

private data class QualityOption(
    val label: String,
    val description: String,
    val value: Int,
)

@Composable
private fun qualityOptions(): List<QualityOption> =
    listOf(
        QualityOption(stringResource(R.string.settings_quality_low), stringResource(R.string.settings_quality_low_desc), JPEG_QUALITY_LOW),
        QualityOption(
            stringResource(R.string.settings_quality_high),
            stringResource(R.string.settings_quality_high_desc),
            JPEG_QUALITY_HIGH,
        ),
        QualityOption(
            stringResource(R.string.settings_quality_best),
            stringResource(R.string.settings_quality_best_desc),
            JPEG_QUALITY_BEST,
        ),
    )

private val fileNameSafePattern = Regex("[^a-zA-Z0-9가-힣ㄱ-ㅎㅏ-ㅣ_-]")

@Composable
internal fun OverlayAlphaDialog(
    currentAlpha: Float,
    onAlphaChange: (Float) -> Unit,
    onDismiss: () -> Unit,
) {
    PairShotBottomSheet(onDismissRequest = onDismiss) {
        Text(
            text = stringResource(R.string.settings_dialog_overlay_opacity_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(PairShotSpacing.lg))
        SettingsSliderItem(
            label = "",
            value = currentAlpha,
            valueRange = 0f..1.0f,
            steps = 9,
            valueLabel = { "${(it * 100).roundToInt()}%" },
            onValueChange = onAlphaChange,
        )
        Spacer(modifier = Modifier.height(PairShotSpacing.sm))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(onClick = onDismiss) {
                Text(stringResource(CoreR.string.common_button_confirm))
            }
        }
    }
}

@Composable
internal fun ClearCacheDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    PairShotDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_dialog_cache_clear_title)) },
        text = { Text(stringResource(R.string.settings_dialog_cache_clear_message)) },
        confirmButton = {
            TextButton(
                onClick = {
                    onDismiss()
                    onConfirm()
                },
            ) {
                Text(stringResource(R.string.settings_dialog_cache_clear_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(CoreR.string.common_button_cancel))
            }
        },
    )
}

@Composable
internal fun ImageQualityDialog(
    currentQuality: Int,
    onQualityChange: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedQuality by rememberSaveable { mutableIntStateOf(currentQuality) }

    PairShotBottomSheet(onDismissRequest = onDismiss) {
        Text(
            text = stringResource(R.string.settings_dialog_image_quality_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(PairShotSpacing.md))
        qualityOptions().forEach { option ->
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable { selectedQuality = option.value }
                        .padding(vertical = PairShotSpacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(
                    selected = selectedQuality == option.value,
                    onClick = { selectedQuality = option.value },
                    colors =
                        RadioButtonDefaults.colors(
                            selectedColor = MaterialTheme.colorScheme.primary,
                        ),
                )
                Column(modifier = Modifier.padding(start = PairShotSpacing.sm)) {
                    Text(
                        text = option.label,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = option.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(PairShotSpacing.sm))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onDismiss) { Text(stringResource(CoreR.string.common_button_cancel)) }
            Spacer(modifier = Modifier.width(PairShotSpacing.sm))
            TextButton(
                onClick = {
                    onQualityChange(selectedQuality)
                    onDismiss()
                },
            ) {
                Text(stringResource(R.string.settings_dialog_apply), color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
internal fun FileNamePrefixDialog(
    currentPrefix: String,
    onPrefixChange: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var prefixInput by rememberSaveable { mutableStateOf(currentPrefix) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    val isError = prefixInput.isBlank()
    val outlineColor = MaterialTheme.colorScheme.outline
    val errorColor = MaterialTheme.colorScheme.error
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface

    val underscoreSuffix =
        remember {
            VisualTransformation { original ->
                TransformedText(
                    text = AnnotatedString(original.text + "_"),
                    offsetMapping =
                        object : OffsetMapping {
                            override fun originalToTransformed(offset: Int) = offset

                            override fun transformedToOriginal(offset: Int) = offset.coerceAtMost(original.text.length)
                        },
                )
            }
        }

    PairShotBottomSheet(onDismissRequest = onDismiss) {
        Text(
            text = stringResource(R.string.settings_dialog_file_name_prefix_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(PairShotSpacing.lg))
        Box(modifier = Modifier.fillMaxWidth()) {
            BasicTextField(
                value = prefixInput,
                onValueChange = { raw ->
                    prefixInput = raw.replace(fileNameSafePattern, "")
                },
                singleLine = true,
                textStyle =
                    MaterialTheme.typography.bodyLarge.copy(
                        color = onSurfaceColor,
                    ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                visualTransformation = underscoreSuffix,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .padding(bottom = PairShotSpacing.xs)
                        .drawBehind {
                            val lineColor = if (isError) errorColor else outlineColor
                            drawLine(
                                color = lineColor,
                                start = Offset(0f, size.height),
                                end = Offset(size.width, size.height),
                                strokeWidth = PairShotStroke.hairline.toPx(),
                            )
                        },
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier.heightIn(min = InputFieldMinHeight),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        if (prefixInput.isEmpty()) {
                            Text(
                                text = stringResource(R.string.settings_dialog_prefix_hint),
                                style = MaterialTheme.typography.bodyLarge,
                                color = onSurfaceVariantColor,
                            )
                        }
                        innerTextField()
                    }
                },
            )
        }
        Box(modifier = Modifier.height(InputErrorHeight)) {
            if (isError) {
                Text(
                    text = stringResource(R.string.settings_dialog_prefix_required),
                    style = MaterialTheme.typography.bodySmall,
                    color = errorColor,
                )
            }
        }
        if (prefixInput != "PAIRSHOT") {
            TextButton(
                onClick = { prefixInput = "PAIRSHOT" },
                modifier = Modifier.align(Alignment.End),
            ) {
                Text(stringResource(R.string.settings_dialog_reset_default))
            }
        }
        Spacer(modifier = Modifier.height(PairShotSpacing.sm))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(onClick = onDismiss) { Text(stringResource(CoreR.string.common_button_cancel)) }
            Spacer(modifier = Modifier.width(PairShotSpacing.sm))
            TextButton(
                onClick = {
                    onPrefixChange(prefixInput)
                    onDismiss()
                },
                enabled = prefixInput.isNotBlank(),
            ) {
                Text(stringResource(CoreR.string.common_button_save), color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
