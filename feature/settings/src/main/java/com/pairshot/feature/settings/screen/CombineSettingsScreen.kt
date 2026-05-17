package com.pairshot.feature.settings.screen

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pairshot.core.adsui.component.PairShotBannerAd
import com.pairshot.core.designsystem.PairShotCard
import com.pairshot.core.designsystem.PairShotScreen
import com.pairshot.core.designsystem.PairShotSpacing
import com.pairshot.core.designsystem.PairShotStroke
import com.pairshot.core.designsystem.PairShotTouchTarget
import com.pairshot.core.model.CombineConfig
import com.pairshot.core.model.CombineLayout
import com.pairshot.core.model.LabelAnchor
import com.pairshot.core.model.LabelPosition
import com.pairshot.core.model.LabelPositionMode
import com.pairshot.core.model.WatermarkConfig
import com.pairshot.core.rendering.CombinePreviewEntryPoint
import com.pairshot.core.rendering.CombinePreviewRenderer
import com.pairshot.core.ui.component.PairShotDialog
import com.pairshot.core.ui.component.SettingsCard
import com.pairshot.core.ui.component.SettingsDivider
import com.pairshot.core.ui.component.SettingsSectionLabel
import com.pairshot.core.ui.component.SettingsSliderItem
import com.pairshot.core.ui.component.SettingsSwitchItem
import com.pairshot.feature.settings.R
import com.pairshot.feature.settings.component.PositionPicker3x3Row
import com.pairshot.feature.settings.component.ProLockedSwitchItem
import dagger.hilt.android.EntryPointAccessors
import kotlin.math.roundToInt
import com.pairshot.core.ui.R as CoreR

private const val LABEL_WEIGHT = 0.45f
private const val INPUT_WEIGHT = 0.55f
private const val RGB_CHANNEL_MASK = 0xFFFFFF
private const val HUE_DEGREES_MAX = 360f
private const val HSV_COMPONENT_COUNT = 3
private const val HSV_GRAYSCALE_THRESHOLD = 0.15f
private const val HSV_DARK_BRIGHTNESS_START = 0.15f
private const val HSV_SWATCH_BRIGHTNESS = 0.9f
private const val HSV_FULL_VALUE = 1f
private const val BRIGHTNESS_DARK_DEFAULT = 0.05f
private const val HUE_PRESET_RED = 0f
private const val HUE_PRESET_ORANGE = 30f
private const val HUE_PRESET_YELLOW = 60f
private const val HUE_PRESET_GREEN = 120f
private const val HUE_PRESET_CYAN = 210f
private const val HUE_PRESET_BLUE = 240f
private const val HUE_PRESET_PURPLE = 270f
private const val ASPECT_RATIO_HORIZONTAL = 2f
private const val ASPECT_RATIO_VERTICAL = 0.5f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CombineSettingsScreen(
    combineConfig: CombineConfig,
    watermarkConfig: WatermarkConfig,
    isProSubscriber: Boolean,
    onCombineConfigChange: (CombineConfig) -> Unit,
    onNavigateBack: () -> Unit,
    onProLocked: () -> Unit,
) {
    var borderColorPickerVisible by remember { mutableStateOf(false) }
    var labelTextColorPickerVisible by remember { mutableStateOf(false) }
    var labelBgColorPickerVisible by remember { mutableStateOf(false) }

    if (borderColorPickerVisible) {
        ColorPickerDialog(
            initialColor = combineConfig.borderColorArgb,
            onDismiss = { borderColorPickerVisible = false },
            onConfirm = { color ->
                onCombineConfigChange(combineConfig.copy(borderColorArgb = color))
                borderColorPickerVisible = false
            },
        )
    }

    if (labelTextColorPickerVisible) {
        ColorPickerDialog(
            initialColor = combineConfig.labelTextColorArgb,
            onDismiss = { labelTextColorPickerVisible = false },
            onConfirm = { color ->
                onCombineConfigChange(combineConfig.copy(labelTextColorArgb = color))
                labelTextColorPickerVisible = false
            },
        )
    }

    if (labelBgColorPickerVisible) {
        LabelBgColorPickerDialog(
            initialColor = combineConfig.labelBgColorArgb,
            borderColorArgb = combineConfig.borderColorArgb,
            borderEnabled = combineConfig.borderEnabled,
            initialMatchesBorder = combineConfig.labelBgMatchesBorder,
            onDismiss = { labelBgColorPickerVisible = false },
            onConfirm = { color, matchesBorder ->
                onCombineConfigChange(
                    combineConfig.copy(
                        labelBgColorArgb = color,
                        labelBgMatchesBorder = matchesBorder,
                    ),
                )
                labelBgColorPickerVisible = false
            },
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.combine_settings_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = stringResource(CoreR.string.common_desc_back),
                        )
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                    ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
        ) {
            PairShotBannerAd()
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding =
                    PaddingValues(
                        horizontal = PairShotScreen.horizontalPadding,
                        vertical = PairShotCard.innerPadding,
                    ),
            ) {
                item(key = "label_layout") {
                    SettingsSectionLabel(label = stringResource(R.string.combine_section_alignment))
                    Spacer(modifier = Modifier.height(PairShotSpacing.sm))
                }

                item(key = "card_layout") {
                    SettingsCard {
                        CombineLayoutItem(
                            selectedLayout = combineConfig.layout,
                            onLayoutChange = { layout ->
                                onCombineConfigChange(combineConfig.copy(layout = layout))
                            },
                        )
                    }
                }

                item(key = "gap_1") {
                    Spacer(modifier = Modifier.height(PairShotSpacing.xxl))
                }

                item(key = "label_border") {
                    SettingsSectionLabel(label = stringResource(R.string.combine_section_border))
                    Spacer(modifier = Modifier.height(PairShotSpacing.sm))
                }

                item(key = "card_border") {
                    SettingsCard {
                        SettingsSwitchItem(
                            label = stringResource(R.string.combine_item_border_use),
                            checked = combineConfig.borderEnabled,
                            onCheckedChange = { checked ->
                                onCombineConfigChange(combineConfig.copy(borderEnabled = checked))
                            },
                        )
                        AnimatedVisibility(
                            visible = combineConfig.borderEnabled,
                            enter = expandVertically(),
                            exit = shrinkVertically(),
                        ) {
                            Column {
                                SettingsDivider()
                                SettingsSliderItem(
                                    label = stringResource(R.string.combine_item_border_thickness),
                                    value = combineConfig.borderThicknessDp.toFloat(),
                                    valueRange = 0f..32f,
                                    steps = 31,
                                    valueLabel = { "${it.toInt()}dp" },
                                    onValueChange = { v ->
                                        onCombineConfigChange(combineConfig.copy(borderThicknessDp = v.toInt()))
                                    },
                                    onLiveUpdate = { v ->
                                        onCombineConfigChange(combineConfig.copy(borderThicknessDp = v.toInt()))
                                    },
                                )
                                SettingsDivider()
                                ColorItem(
                                    label = stringResource(R.string.combine_item_color),
                                    colorArgb = combineConfig.borderColorArgb,
                                    onClick = { borderColorPickerVisible = true },
                                )
                            }
                        }
                    }
                }

                item(key = "gap_2") {
                    Spacer(modifier = Modifier.height(PairShotSpacing.xxl))
                }

                item(key = "label_label") {
                    SettingsSectionLabel(label = stringResource(R.string.combine_section_label))
                    Spacer(modifier = Modifier.height(PairShotSpacing.sm))
                }

                val labelSectionsVisible = combineConfig.labelEnabled && isProSubscriber

                item(key = "card_label_text") {
                    SettingsCard {
                        if (isProSubscriber) {
                            SettingsSwitchItem(
                                label = stringResource(R.string.combine_item_label_use),
                                checked = combineConfig.labelEnabled,
                                onCheckedChange = { checked ->
                                    onCombineConfigChange(combineConfig.copy(labelEnabled = checked))
                                },
                            )
                        } else {
                            ProLockedSwitchItem(
                                label = stringResource(R.string.combine_item_label_use),
                                onClick = onProLocked,
                            )
                        }
                        AnimatedVisibility(
                            visible = labelSectionsVisible,
                            enter = expandVertically(),
                            exit = shrinkVertically(),
                        ) {
                            Column {
                                SettingsDivider()
                                LabelTextItem(
                                    labelName = stringResource(R.string.combine_item_label_before),
                                    text = combineConfig.beforeLabel,
                                    onTextChange = { text ->
                                        onCombineConfigChange(combineConfig.copy(beforeLabel = text))
                                    },
                                )
                                SettingsDivider()
                                LabelTextItem(
                                    labelName = stringResource(R.string.combine_item_label_after),
                                    text = combineConfig.afterLabel,
                                    onTextChange = { text ->
                                        onCombineConfigChange(combineConfig.copy(afterLabel = text))
                                    },
                                )
                                SettingsDivider()
                                SettingsSliderItem(
                                    label = stringResource(R.string.combine_item_text_size),
                                    value = combineConfig.labelSizeRatio,
                                    valueRange = 0f..0.10f,
                                    steps = 9,
                                    valueLabel = { "${(it * 100).roundToInt()}%" },
                                    onValueChange = { v ->
                                        onCombineConfigChange(combineConfig.copy(labelSizeRatio = v))
                                    },
                                    onLiveUpdate = { v ->
                                        onCombineConfigChange(combineConfig.copy(labelSizeRatio = v))
                                    },
                                )
                                SettingsDivider()
                                ColorItem(
                                    label = stringResource(R.string.combine_item_text_color),
                                    colorArgb = combineConfig.labelTextColorArgb,
                                    onClick = { labelTextColorPickerVisible = true },
                                )
                            }
                        }
                    }
                }

                item(key = "label_label_mode") {
                    AnimatedVisibility(
                        visible = labelSectionsVisible,
                        enter = expandVertically(),
                        exit = shrinkVertically(),
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(PairShotSpacing.xxl))
                            SettingsSectionLabel(label = stringResource(R.string.combine_section_label_mode))
                            Spacer(modifier = Modifier.height(PairShotSpacing.sm))
                        }
                    }
                }

                item(key = "card_label_position") {
                    AnimatedVisibility(
                        visible = labelSectionsVisible,
                        enter = expandVertically(),
                        exit = shrinkVertically(),
                    ) {
                        Column {
                            SettingsCard {
                                LabelPositionModeItem(
                                    selectedMode = combineConfig.labelPositionMode,
                                    onModeChange = { mode ->
                                        onCombineConfigChange(combineConfig.copy(labelPositionMode = mode))
                                    },
                                )
                                if (combineConfig.labelPositionMode == LabelPositionMode.FULL_WIDTH) {
                                    SettingsDivider()
                                    LabelPositionItem(
                                        selectedPosition = combineConfig.labelPosition,
                                        onPositionChange = { position ->
                                            onCombineConfigChange(combineConfig.copy(labelPosition = position))
                                        },
                                    )
                                } else {
                                    SettingsDivider()
                                    PositionPicker3x3Row(
                                        label = stringResource(R.string.combine_item_position_before),
                                        positions = labelAnchorOrder,
                                        selectedPosition = combineConfig.beforeLabelAnchor,
                                        onPositionChange = { anchor ->
                                            onCombineConfigChange(combineConfig.copy(beforeLabelAnchor = anchor))
                                        },
                                    )
                                    SettingsDivider()
                                    PositionPicker3x3Row(
                                        label = stringResource(R.string.combine_item_position_after),
                                        positions = labelAnchorOrder,
                                        selectedPosition = combineConfig.afterLabelAnchor,
                                        onPositionChange = { anchor ->
                                            onCombineConfigChange(combineConfig.copy(afterLabelAnchor = anchor))
                                        },
                                    )
                                }
                            }
                        }
                    }
                }

                item(key = "label_label_bg") {
                    AnimatedVisibility(
                        visible = labelSectionsVisible,
                        enter = expandVertically(),
                        exit = shrinkVertically(),
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(PairShotSpacing.xxl))
                            SettingsSectionLabel(label = stringResource(R.string.combine_section_label_background))
                            Spacer(modifier = Modifier.height(PairShotSpacing.sm))
                        }
                    }
                }

                item(key = "card_label_bg") {
                    AnimatedVisibility(
                        visible = labelSectionsVisible,
                        enter = expandVertically(),
                        exit = shrinkVertically(),
                    ) {
                        Column {
                            SettingsCard {
                                SettingsSwitchItem(
                                    label = stringResource(R.string.combine_item_background_use),
                                    checked = combineConfig.labelBgEnabled,
                                    onCheckedChange = { checked ->
                                        onCombineConfigChange(combineConfig.copy(labelBgEnabled = checked))
                                    },
                                )
                                AnimatedVisibility(
                                    visible = combineConfig.labelBgEnabled,
                                    enter = expandVertically(),
                                    exit = shrinkVertically(),
                                ) {
                                    Column {
                                        SettingsDivider()
                                        ColorItem(
                                            label = stringResource(R.string.combine_item_color),
                                            colorArgb = combineConfig.labelBgColorArgb,
                                            onClick = { labelBgColorPickerVisible = true },
                                        )
                                        SettingsDivider()
                                        SettingsSliderItem(
                                            label = stringResource(R.string.combine_item_opacity),
                                            value = combineConfig.labelBgAlpha,
                                            valueRange = 0f..1f,
                                            valueLabel = { "${(it * 100).roundToInt()}%" },
                                            onValueChange = { v ->
                                                onCombineConfigChange(combineConfig.copy(labelBgAlpha = v))
                                            },
                                            onLiveUpdate = { v ->
                                                onCombineConfigChange(combineConfig.copy(labelBgAlpha = v))
                                            },
                                        )
                                        if (combineConfig.labelPositionMode == LabelPositionMode.FREE) {
                                            SettingsDivider()
                                            SettingsSliderItem(
                                                label = stringResource(R.string.combine_item_curvature),
                                                value = combineConfig.labelBgCornerDp.toFloat(),
                                                valueRange = 0f..50f,
                                                steps = 49,
                                                valueLabel = { "${it.toInt()}dp" },
                                                onValueChange = { v ->
                                                    onCombineConfigChange(combineConfig.copy(labelBgCornerDp = v.toInt()))
                                                },
                                                onLiveUpdate = { v ->
                                                    onCombineConfigChange(combineConfig.copy(labelBgCornerDp = v.toInt()))
                                                },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item(key = "gap_3") {
                    Spacer(modifier = Modifier.height(PairShotSpacing.xxl))
                }

                item(key = "label_preview") {
                    SettingsSectionLabel(label = stringResource(R.string.combine_section_preview))
                    Spacer(modifier = Modifier.height(PairShotSpacing.sm))
                }

                item(key = "combine_preview") {
                    CombinePreviewSection(
                        config = combineConfig,
                        watermarkConfig = watermarkConfig,
                    )
                    Spacer(modifier = Modifier.height(PairShotSpacing.xxl))
                }
            }
        }
    }
}

@Composable
private fun CombineLayoutItem(
    selectedLayout: CombineLayout,
    onLayoutChange: (CombineLayout) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = PairShotCard.innerPadding,
                    vertical = PairShotCard.innerPadding,
                ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.combine_direction_label),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(PairShotSpacing.sm)) {
            CombineLayout.entries.forEach { layout ->
                val isSelected = layout == selectedLayout
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
                            ).clickable { onLayoutChange(layout) }
                            .padding(
                                horizontal = PairShotSpacing.md,
                                vertical = PairShotSpacing.sm,
                            ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text =
                            when (layout) {
                                CombineLayout.HORIZONTAL -> stringResource(R.string.combine_direction_horizontal)
                                CombineLayout.VERTICAL -> stringResource(R.string.combine_direction_vertical)
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
                }
            }
        }
    }
}

@Composable
private fun LabelPositionItem(
    selectedPosition: LabelPosition,
    onPositionChange: (LabelPosition) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = PairShotCard.innerPadding,
                    vertical = PairShotCard.innerPadding,
                ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.combine_label_position_label),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(PairShotSpacing.sm)) {
            LabelPosition.entries.forEach { position ->
                val isSelected = position == selectedPosition
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
                            ).clickable { onPositionChange(position) }
                            .padding(
                                horizontal = PairShotSpacing.md,
                                vertical = PairShotSpacing.sm,
                            ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text =
                            when (position) {
                                LabelPosition.TOP -> stringResource(R.string.combine_label_position_top)
                                LabelPosition.BOTTOM -> stringResource(R.string.combine_label_position_bottom)
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
                }
            }
        }
    }
}

@Composable
private fun LabelPositionModeItem(
    selectedMode: LabelPositionMode,
    onModeChange: (LabelPositionMode) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = PairShotCard.innerPadding,
                    vertical = PairShotCard.innerPadding,
                ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.combine_label_mode_label),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(PairShotSpacing.sm)) {
            LabelPositionMode.entries.forEach { mode ->
                val isSelected = mode == selectedMode
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
                            ).clickable { onModeChange(mode) }
                            .padding(
                                horizontal = PairShotSpacing.md,
                                vertical = PairShotSpacing.sm,
                            ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text =
                            when (mode) {
                                LabelPositionMode.FULL_WIDTH -> stringResource(R.string.combine_label_mode_full_width)
                                LabelPositionMode.FREE -> stringResource(R.string.combine_label_mode_free)
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
                }
            }
        }
    }
}

private val labelAnchorOrder =
    listOf(
        LabelAnchor.TOP_LEFT,
        LabelAnchor.TOP_CENTER,
        LabelAnchor.TOP_RIGHT,
        LabelAnchor.MIDDLE_LEFT,
        LabelAnchor.MIDDLE_CENTER,
        LabelAnchor.MIDDLE_RIGHT,
        LabelAnchor.BOTTOM_LEFT,
        LabelAnchor.BOTTOM_CENTER,
        LabelAnchor.BOTTOM_RIGHT,
    )

@Composable
private fun LabelTextItem(
    labelName: String,
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

    val textColor = MaterialTheme.colorScheme.onSurface
    val hintColor = MaterialTheme.colorScheme.onSurfaceVariant
    val dividerColor = MaterialTheme.colorScheme.outline
    val textStyle =
        MaterialTheme.typography.bodyMedium.copy(
            color = textColor,
            textAlign = TextAlign.End,
        )
    val cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
    var isFocused by remember { mutableStateOf(false) }

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = PairShotCard.innerPadding,
                    vertical = PairShotCard.innerPadding,
                ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = labelName,
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
                    .onFocusChanged { isFocused = it.isFocused },
            decorationBox = { innerTextField ->
                Column {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.CenterEnd,
                    ) {
                        if (textFieldValue.text.isEmpty()) {
                            Text(
                                text = stringResource(R.string.combine_text_input_hint),
                                style = MaterialTheme.typography.bodyMedium,
                                color = hintColor,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.End,
                            )
                        }
                        innerTextField()
                    }
                    if (isFocused) {
                        Spacer(modifier = Modifier.height(PairShotSpacing.xs))
                        HorizontalDivider(color = dividerColor, thickness = PairShotStroke.hairline)
                    }
                }
            },
        )
    }
}

@Composable
private fun ColorItem(
    label: String,
    colorArgb: Int,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .height(PairShotTouchTarget.large)
                .padding(horizontal = PairShotCard.innerPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Box(
            modifier =
                Modifier
                    .size(PairShotSpacing.xl)
                    .clip(MaterialTheme.shapes.small)
                    .background(Color(colorArgb)),
        )
        Spacer(modifier = Modifier.width(PairShotSpacing.sm))
        Text(
            text = "#%06X".format(colorArgb and RGB_CHANNEL_MASK),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private val HUE_PRESETS: List<Float?> =
    listOf(
        null,
        -HSV_FULL_VALUE,
        HUE_PRESET_RED,
        HUE_PRESET_ORANGE,
        HUE_PRESET_YELLOW,
        HUE_PRESET_GREEN,
        HUE_PRESET_CYAN,
        HUE_PRESET_BLUE,
        HUE_PRESET_PURPLE,
    )

private fun nearestPresetIdx(hsv: FloatArray): Int {
    val isBlack = hsv[2] < HSV_GRAYSCALE_THRESHOLD && hsv[1] < HSV_GRAYSCALE_THRESHOLD
    val isWhiteish = !isBlack && hsv[1] < HSV_GRAYSCALE_THRESHOLD
    return when {
        isBlack -> {
            1
        }

        isWhiteish -> {
            0
        }

        else -> {
            HUE_PRESETS.indices.drop(2).minByOrNull { idx ->
                val hue = HUE_PRESETS[idx] ?: 0f
                val diff = kotlin.math.abs(hue - hsv[0])
                minOf(diff, HUE_DEGREES_MAX - diff)
            } ?: 2
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ColorPickerContent(
    selectedIdx: Int,
    onSelectedIdxChange: (Int) -> Unit,
    brightness: Float,
    onBrightnessChange: (Float) -> Unit,
    currentColor: Color,
) {
    val selectedHue = HUE_PRESETS[selectedIdx]
    val isGrayscale = selectedHue == null || selectedHue == -HSV_FULL_VALUE
    val gradientStart = if (isGrayscale) Color.Black else Color.hsv(selectedHue!!, HSV_FULL_VALUE, HSV_DARK_BRIGHTNESS_START)
    val gradientEnd = if (isGrayscale) Color.White else Color.hsv(selectedHue!!, HSV_FULL_VALUE, HSV_FULL_VALUE)
    val sliderRange = if (isGrayscale) 0f..HSV_FULL_VALUE else HSV_DARK_BRIGHTNESS_START..HSV_FULL_VALUE

    Column(verticalArrangement = Arrangement.spacedBy(PairShotSpacing.lg)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(PairShotSpacing.xs),
        ) {
            HUE_PRESETS.forEachIndexed { idx, hue ->
                val swatchColor =
                    when {
                        hue == null -> Color.White
                        hue == -1f -> Color.Black
                        else -> Color.hsv(hue, HSV_FULL_VALUE, HSV_SWATCH_BRIGHTNESS)
                    }
                val isSelected = idx == selectedIdx
                val needsOutline = hue == null || hue == -HSV_FULL_VALUE
                Box(
                    modifier =
                        Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(MaterialTheme.shapes.small)
                            .background(swatchColor)
                            .then(
                                when {
                                    isSelected -> {
                                        Modifier.border(
                                            PairShotStroke.thin,
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.shapes.small,
                                        )
                                    }

                                    needsOutline -> {
                                        Modifier.border(
                                            PairShotStroke.hairline,
                                            MaterialTheme.colorScheme.outlineVariant,
                                            MaterialTheme.shapes.small,
                                        )
                                    }

                                    else -> {
                                        Modifier
                                    }
                                },
                            ).clickable { onSelectedIdxChange(idx) },
                )
            }
        }

        androidx.compose.material3.Slider(
            value = brightness,
            onValueChange = onBrightnessChange,
            valueRange = sliderRange,
            track = { _ ->
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(PairShotSpacing.md)
                            .clip(CircleShape)
                            .background(Brush.horizontalGradient(listOf(gradientStart, gradientEnd))),
                )
            },
            thumb = { _ ->
                Box(
                    modifier =
                        Modifier
                            .size(PairShotSpacing.lg)
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(PairShotStroke.thin, MaterialTheme.colorScheme.outline, CircleShape),
                )
            },
            modifier = Modifier.fillMaxWidth(),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PairShotSpacing.sm),
        ) {
            Box(
                modifier =
                    Modifier
                        .size(PairShotSpacing.xl)
                        .clip(MaterialTheme.shapes.small)
                        .background(currentColor)
                        .border(PairShotStroke.hairline, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.small),
            )
            Text(
                text = "#%06X".format(currentColor.toArgb() and RGB_CHANNEL_MASK),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ColorPickerDialog(
    initialColor: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    val initialHsv = remember { FloatArray(HSV_COMPONENT_COUNT).also { android.graphics.Color.colorToHSV(initialColor, it) } }
    var selectedIdx by remember { mutableIntStateOf(nearestPresetIdx(initialHsv)) }
    var brightness by remember { mutableFloatStateOf(initialHsv[2].coerceIn(0f, 1f)) }

    val selectedHue = HUE_PRESETS[selectedIdx]
    val isGrayscale = selectedHue == null || selectedHue == -HSV_FULL_VALUE
    val currentColor =
        if (isGrayscale) Color.hsv(0f, 0f, brightness) else Color.hsv(selectedHue!!, 1f, brightness)

    PairShotDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.combine_dialog_color_title)) },
        text = {
            ColorPickerContent(
                selectedIdx = selectedIdx,
                onSelectedIdxChange = { idx ->
                    when (HUE_PRESETS[idx]) {
                        null -> brightness = 1.0f
                        -HSV_FULL_VALUE -> brightness = BRIGHTNESS_DARK_DEFAULT
                        else -> Unit
                    }
                    selectedIdx = idx
                },
                brightness = brightness,
                onBrightnessChange = { brightness = it },
                currentColor = currentColor,
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(currentColor.toArgb()) }) {
                Text(stringResource(CoreR.string.common_button_confirm))
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
private fun LabelBgColorPickerDialog(
    initialColor: Int,
    borderColorArgb: Int,
    borderEnabled: Boolean,
    initialMatchesBorder: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (color: Int, matchesBorder: Boolean) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    var matchesBorder by remember { mutableStateOf(initialMatchesBorder) }
    val initialHsv = remember { FloatArray(HSV_COMPONENT_COUNT).also { android.graphics.Color.colorToHSV(initialColor, it) } }
    var selectedIdx by remember { mutableIntStateOf(nearestPresetIdx(initialHsv)) }
    var brightness by remember { mutableFloatStateOf(initialHsv[2].coerceIn(0f, 1f)) }

    val selectedHue = HUE_PRESETS[selectedIdx]
    val isGrayscale = selectedHue == null || selectedHue == -HSV_FULL_VALUE
    val currentPickedColor =
        if (isGrayscale) Color.hsv(0f, 0f, brightness) else Color.hsv(selectedHue!!, 1f, brightness)

    PairShotDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.combine_dialog_bg_color_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(PairShotSpacing.lg)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.combine_dialog_match_border_color),
                        style = MaterialTheme.typography.bodyMedium,
                        color =
                            if (borderEnabled) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            },
                        modifier = Modifier.weight(1f),
                    )
                    androidx.compose.material3.Switch(
                        checked = matchesBorder,
                        onCheckedChange = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            matchesBorder = it
                        },
                        enabled = borderEnabled,
                    )
                }

                if (matchesBorder) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(PairShotSpacing.sm),
                    ) {
                        Box(
                            modifier =
                                Modifier
                                    .size(PairShotSpacing.xl)
                                    .clip(MaterialTheme.shapes.small)
                                    .background(Color(borderColorArgb))
                                    .border(PairShotStroke.hairline, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.small),
                        )
                        Text(
                            text = "#%06X".format(borderColorArgb and RGB_CHANNEL_MASK),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    ColorPickerContent(
                        selectedIdx = selectedIdx,
                        onSelectedIdxChange = { idx ->
                            when (HUE_PRESETS[idx]) {
                                null -> brightness = 1.0f
                                -HSV_FULL_VALUE -> brightness = BRIGHTNESS_DARK_DEFAULT
                                else -> Unit
                            }
                            selectedIdx = idx
                        },
                        brightness = brightness,
                        onBrightnessChange = { brightness = it },
                        currentColor = currentPickedColor,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(currentPickedColor.toArgb(), matchesBorder) }) {
                Text(stringResource(CoreR.string.common_button_confirm))
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
internal fun CombinePreviewSection(
    config: CombineConfig,
    watermarkConfig: WatermarkConfig,
    modifier: Modifier = Modifier,
) {
    val renderer = rememberCombinePreviewRenderer()
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(config, watermarkConfig) {
        val result = renderer.render(config, watermarkConfig)
        previewBitmap?.let { old ->
            if (old !== result && !old.isRecycled) old.recycle()
        }
        previewBitmap = result
    }

    DisposableEffect(Unit) {
        onDispose {
            previewBitmap?.takeIf { !it.isRecycled }?.recycle()
        }
    }

    val aspectRatio =
        when (config.layout) {
            CombineLayout.HORIZONTAL -> ASPECT_RATIO_HORIZONTAL
            CombineLayout.VERTICAL -> ASPECT_RATIO_VERTICAL
        }

    val bmp = previewBitmap
    if (bmp != null) {
        Image(
            bitmap = bmp.asImageBitmap(),
            contentDescription = stringResource(R.string.combine_preview_desc),
            contentScale = ContentScale.Fit,
            filterQuality = FilterQuality.High,
            modifier =
                modifier
                    .fillMaxWidth()
                    .aspectRatio(
                        bmp.width.toFloat().coerceAtLeast(1f) /
                            bmp.height.toFloat().coerceAtLeast(1f),
                    ),
        )
    } else {
        Box(
            modifier =
                modifier
                    .fillMaxWidth()
                    .aspectRatio(aspectRatio),
        )
    }
}

@Composable
private fun rememberCombinePreviewRenderer(): CombinePreviewRenderer {
    val context = LocalContext.current
    return remember(context) {
        EntryPointAccessors
            .fromApplication(
                context.applicationContext,
                CombinePreviewEntryPoint::class.java,
            ).combinePreviewRenderer()
    }
}
