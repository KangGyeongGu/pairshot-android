package com.pairshot.feature.settings.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pairshot.core.designsystem.PairShotProBadge
import com.pairshot.core.designsystem.PairShotSpacing
import com.pairshot.core.model.WatermarkConfig
import com.pairshot.core.model.WatermarkType
import com.pairshot.core.ui.component.SettingsCard
import com.pairshot.core.ui.component.SettingsDivider
import com.pairshot.core.ui.component.SettingsSliderItem
import com.pairshot.feature.settings.R
import kotlin.math.roundToInt

private const val LABEL_WEIGHT = 0.35f
private const val INPUT_WEIGHT = 0.65f

@Composable
internal fun WatermarkTextSection(
    watermarkConfig: WatermarkConfig,
    onWatermarkConfigChange: (WatermarkConfig) -> Unit,
) {
    SettingsCard {
        WatermarkTextItem(
            text = watermarkConfig.text,
            onTextChange = { text ->
                onWatermarkConfigChange(watermarkConfig.copy(text = text))
            },
        )
        SettingsDivider()
        SettingsSliderItem(
            label = stringResource(R.string.watermark_field_opacity),
            value = watermarkConfig.alpha,
            valueRange = 0f..1f,
            valueLabel = { "${(it * 100).roundToInt()}%" },
            onValueChange = { v -> onWatermarkConfigChange(watermarkConfig.copy(alpha = v)) },
            onLiveUpdate = { v -> onWatermarkConfigChange(watermarkConfig.copy(alpha = v)) },
        )
        SettingsDivider()
        SettingsSliderItem(
            label = stringResource(R.string.watermark_field_lines),
            value = watermarkConfig.diagonalCount.toFloat(),
            valueRange = 0f..20f,
            steps = 19,
            valueLabel = { it.toInt().toString() },
            onValueChange = { v -> onWatermarkConfigChange(watermarkConfig.copy(diagonalCount = v.toInt())) },
            onLiveUpdate = { v -> onWatermarkConfigChange(watermarkConfig.copy(diagonalCount = v.toInt())) },
        )
        SettingsDivider()
        SettingsSliderItem(
            label = stringResource(R.string.watermark_field_repeat),
            value = watermarkConfig.repeatDensity,
            valueRange = 0f..3.0f,
            valueLabel = { "%.1f".format(it) },
            onValueChange = { v -> onWatermarkConfigChange(watermarkConfig.copy(repeatDensity = v)) },
            onLiveUpdate = { v -> onWatermarkConfigChange(watermarkConfig.copy(repeatDensity = v)) },
        )
    }
}

@Composable
internal fun WatermarkTypeItem(
    selectedType: WatermarkType,
    isProSubscriber: Boolean,
    onTypeChange: (WatermarkType) -> Unit,
    onProLocked: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = PairShotSpacing.cardPadding,
                    vertical = PairShotSpacing.cardPadding,
                ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.watermark_type_label),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(PairShotSpacing.iconTextGap)) {
            WatermarkType.entries.forEach { type ->
                val isSelected = type == selectedType
                val isLocked = type == WatermarkType.LOGO && !isProSubscriber
                Box(
                    modifier =
                        Modifier
                            .clip(MaterialTheme.shapes.small)
                            .background(
                                if (isSelected) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                } else {
                                    Color.Transparent
                                },
                            ).clickable {
                                if (isLocked) onProLocked() else onTypeChange(type)
                            }.padding(
                                horizontal = PairShotSpacing.itemGap,
                                vertical = PairShotSpacing.iconTextGap,
                            ),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text =
                                when (type) {
                                    WatermarkType.TEXT -> stringResource(R.string.watermark_type_text)
                                    WatermarkType.LOGO -> stringResource(R.string.watermark_type_logo)
                                },
                            style =
                                MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                ),
                            color =
                                if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                        )
                        if (isLocked) {
                            Spacer(modifier = Modifier.width(PairShotSpacing.iconTextGap))
                            PairShotProBadge()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WatermarkTextItem(
    text: String,
    onTextChange: (String) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    var textFieldValue by remember {
        mutableStateOf(TextFieldValue(text = text, selection = TextRange(text.length)))
    }
    LaunchedEffect(text) {
        if (textFieldValue.text != text) {
            textFieldValue = TextFieldValue(text = text, selection = TextRange(text.length))
        }
    }

    val dividerColor = MaterialTheme.colorScheme.outline
    val textColor = MaterialTheme.colorScheme.onSurface
    val hintColor = MaterialTheme.colorScheme.onSurfaceVariant
    val textStyle =
        MaterialTheme.typography.bodyMedium.copy(
            color = textColor,
            textAlign = TextAlign.End,
        )
    val cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)

    val focusRequester = remember { FocusRequester() }
    var isFocused by remember { mutableStateOf(false) }

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = PairShotSpacing.cardPadding,
                    vertical = PairShotSpacing.cardPadding,
                ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.watermark_text_field),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(LABEL_WEIGHT),
        )
        BasicTextField(
            value = textFieldValue,
            onValueChange = { newValue ->
                textFieldValue = newValue
                onTextChange(newValue.text)
            },
            singleLine = true,
            textStyle = textStyle,
            cursorBrush = cursorBrush,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            modifier =
                Modifier
                    .weight(INPUT_WEIGHT)
                    .focusRequester(focusRequester)
                    .onFocusChanged { isFocused = it.isFocused },
            decorationBox = { innerTextField ->
                Column {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.CenterEnd,
                    ) {
                        if (textFieldValue.text.isEmpty()) {
                            Text(
                                text = stringResource(R.string.watermark_text_input_hint),
                                style = MaterialTheme.typography.bodyMedium,
                                color = hintColor,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.End,
                            )
                        }
                        innerTextField()
                    }
                    if (isFocused) {
                        Spacer(modifier = Modifier.height(4.dp))
                        HorizontalDivider(color = dividerColor, thickness = 1.dp)
                    }
                }
            },
        )
    }
}
