package com.pairshot.feature.settings.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import com.pairshot.core.adsui.component.PairShotBannerAd
import com.pairshot.core.designsystem.PairShotCard
import com.pairshot.core.designsystem.PairShotScreen
import com.pairshot.core.designsystem.PairShotSpacing
import com.pairshot.core.model.CombineConfig
import com.pairshot.core.model.CombineLayout
import com.pairshot.core.model.LabelPlacement
import com.pairshot.core.model.LabelPosition
import com.pairshot.core.model.LabelPositionMode
import com.pairshot.core.model.WatermarkConfig
import com.pairshot.core.model.withLabelPlacement
import com.pairshot.core.ui.component.SettingsCard
import com.pairshot.core.ui.component.SettingsDivider
import com.pairshot.core.ui.component.SettingsSectionLabel
import com.pairshot.core.ui.component.SettingsSliderItem
import com.pairshot.core.ui.component.SettingsSwitchItem
import com.pairshot.core.ui.component.colorpicker.ColorPickerDialog
import com.pairshot.feature.settings.R
import com.pairshot.feature.settings.component.ColorItem
import com.pairshot.feature.settings.component.CombinePreviewSection
import com.pairshot.feature.settings.component.LabelBgColorPickerDialog
import com.pairshot.feature.settings.component.LabelTextItem
import com.pairshot.feature.settings.component.PositionPickerGridRow
import com.pairshot.feature.settings.component.ProLockedSwitchItem
import com.pairshot.feature.settings.component.SegmentedToggleRow
import com.pairshot.feature.settings.component.borderLabelAnchorOrder
import com.pairshot.feature.settings.component.labelAnchorOrder
import kotlinx.collections.immutable.toImmutableList
import kotlin.math.roundToInt
import com.pairshot.core.ui.R as CoreR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CombineSettingsScreen(
    combineConfig: CombineConfig,
    watermarkConfig: WatermarkConfig,
    isProSubscriber: Boolean,
    onCombineConfigChange: (CombineConfig) -> Unit,
    onNavigateBack: () -> Unit,
    onProLock: () -> Unit,
    modifier: Modifier = Modifier,
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
            title = { Text(stringResource(R.string.combine_dialog_color_title)) },
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
            title = { Text(stringResource(R.string.combine_dialog_color_title)) },
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
        modifier = modifier,
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
                        SegmentedToggleRow(
                            label = stringResource(R.string.combine_direction_label),
                            entries = CombineLayout.entries.toImmutableList(),
                            selected = combineConfig.layout,
                            onSelect = { layout ->
                                onCombineConfigChange(combineConfig.copy(layout = layout))
                            },
                            labelOf = { layout ->
                                when (layout) {
                                    CombineLayout.HORIZONTAL -> stringResource(R.string.combine_direction_horizontal)
                                    CombineLayout.VERTICAL -> stringResource(R.string.combine_direction_vertical)
                                }
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
                            enabled = !(combineConfig.labelEnabled && combineConfig.labelPlacement == LabelPlacement.INSIDE_BORDER),
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
                                onClick = onProLock,
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
                                SegmentedToggleRow(
                                    label = stringResource(R.string.combine_label_placement_label),
                                    entries = LabelPlacement.entries.toImmutableList(),
                                    selected = combineConfig.labelPlacement,
                                    onSelect = { placement ->
                                        onCombineConfigChange(combineConfig.withLabelPlacement(placement))
                                    },
                                    labelOf = { placement ->
                                        when (placement) {
                                            LabelPlacement.INSIDE_IMAGE -> stringResource(
                                                R.string.combine_label_placement_inside_image
                                            )
                                            LabelPlacement.INSIDE_BORDER -> stringResource(
                                                R.string.combine_label_placement_inside_border
                                            )
                                        }
                                    },
                                )
                                when (combineConfig.labelPlacement) {
                                    LabelPlacement.INSIDE_IMAGE -> {
                                        SettingsDivider()
                                        SegmentedToggleRow(
                                            label = stringResource(R.string.combine_label_mode_label),
                                            entries = LabelPositionMode.entries.toImmutableList(),
                                            selected = combineConfig.labelPositionMode,
                                            onSelect = { mode ->
                                                onCombineConfigChange(combineConfig.copy(labelPositionMode = mode))
                                            },
                                            labelOf = { mode ->
                                                when (mode) {
                                                    LabelPositionMode.FULL_WIDTH -> stringResource(
                                                        R.string.combine_label_mode_full_width
                                                    )
                                                    LabelPositionMode.FREE -> stringResource(
                                                        R.string.combine_label_mode_free
                                                    )
                                                }
                                            },
                                        )
                                        if (combineConfig.labelPositionMode == LabelPositionMode.FULL_WIDTH) {
                                            SettingsDivider()
                                            SegmentedToggleRow(
                                                label = stringResource(R.string.combine_label_position_label),
                                                entries = LabelPosition.entries.toImmutableList(),
                                                selected = combineConfig.labelPosition,
                                                onSelect = { position ->
                                                    onCombineConfigChange(combineConfig.copy(labelPosition = position))
                                                },
                                                labelOf = { position ->
                                                    when (position) {
                                                        LabelPosition.TOP -> stringResource(
                                                            R.string.combine_label_position_top
                                                        )
                                                        LabelPosition.BOTTOM -> stringResource(
                                                            R.string.combine_label_position_bottom
                                                        )
                                                    }
                                                },
                                            )
                                        } else {
                                            SettingsDivider()
                                            PositionPickerGridRow(
                                                label = stringResource(R.string.combine_item_position_before),
                                                positions = labelAnchorOrder,
                                                selectedPosition = combineConfig.beforeLabelAnchor,
                                                onPositionChange = { anchor ->
                                                    onCombineConfigChange(
                                                        combineConfig.copy(beforeLabelAnchor = anchor)
                                                    )
                                                },
                                            )
                                            SettingsDivider()
                                            PositionPickerGridRow(
                                                label = stringResource(R.string.combine_item_position_after),
                                                positions = labelAnchorOrder,
                                                selectedPosition = combineConfig.afterLabelAnchor,
                                                onPositionChange = { anchor ->
                                                    onCombineConfigChange(combineConfig.copy(afterLabelAnchor = anchor))
                                                },
                                            )
                                        }
                                    }

                                    LabelPlacement.INSIDE_BORDER -> {
                                        SettingsDivider()
                                        PositionPickerGridRow(
                                            label = stringResource(R.string.combine_item_position_before),
                                            positions = borderLabelAnchorOrder,
                                            selectedPosition = combineConfig.beforeLabelAnchor,
                                            onPositionChange = { anchor ->
                                                onCombineConfigChange(combineConfig.copy(beforeLabelAnchor = anchor))
                                            },
                                        )
                                        SettingsDivider()
                                        PositionPickerGridRow(
                                            label = stringResource(R.string.combine_item_position_after),
                                            positions = borderLabelAnchorOrder,
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
                }

                val labelBgSectionVisible = labelSectionsVisible && combineConfig.labelPlacement == LabelPlacement.INSIDE_IMAGE

                item(key = "label_label_bg") {
                    AnimatedVisibility(
                        visible = labelBgSectionVisible,
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
                        visible = labelBgSectionVisible,
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
                                                    onCombineConfigChange(
                                                        combineConfig.copy(labelBgCornerDp = v.toInt())
                                                    )
                                                },
                                                onLiveUpdate = { v ->
                                                    onCombineConfigChange(
                                                        combineConfig.copy(labelBgCornerDp = v.toInt())
                                                    )
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
