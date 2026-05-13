package com.pairshot.core.coupon.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.pairshot.core.coupon.BuildConfig
import com.pairshot.core.coupon.R
import com.pairshot.core.coupon.domain.ActivationResult
import com.pairshot.core.coupon.domain.CouponListItem
import com.pairshot.core.coupon.domain.CouponListItemStatus
import com.pairshot.core.ui.component.PairShotDialog
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private enum class RegisterMode { Input, Scanning }

private val ScannerSize = 260.dp
private val SpinnerSize = 20.dp
private val SpinnerStroke = 2.dp
private const val AUTO_DISMISS_DELAY_MS = 1500L
private const val COUPON_ID_FALLBACK_LENGTH = 8
private const val INACTIVE_ROW_ALPHA = 0.6f

@Composable
fun CouponRegisterDialog(
    activationState: CouponActivationUiState,
    myCoupons: List<CouponListItem>,
    myCouponsLoading: Boolean,
    initialCode: String = "",
    onActivate: (String) -> Unit,
    onLoadMyCoupons: () -> Unit,
    onDismiss: () -> Unit,
) {
    var code by remember(initialCode) { mutableStateOf(initialCode) }
    var mode by remember { mutableStateOf(RegisterMode.Input) }

    LaunchedEffect(initialCode) {
        if (initialCode.isNotBlank()) code = initialCode
    }

    LaunchedEffect(Unit) {
        onLoadMyCoupons()
    }

    LaunchedEffect(activationState) {
        if (activationState is CouponActivationUiState.Success) {
            delay(AUTO_DISMISS_DELAY_MS)
            onDismiss()
        }
    }

    val isLoading = activationState is CouponActivationUiState.Loading

    PairShotDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val titleRes =
                    when (mode) {
                        RegisterMode.Input -> R.string.coupon_dialog_title
                        RegisterMode.Scanning -> R.string.coupon_qr_scanner_title
                    }
                Text(text = stringResource(titleRes))
                if (mode == RegisterMode.Input) {
                    val uriHandler = LocalUriHandler.current
                    Text(
                        text = stringResource(R.string.coupon_dialog_need_coupon),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier =
                            Modifier.clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() },
                            ) { uriHandler.openUri(BuildConfig.COUPON_INFO_URL) },
                    )
                }
            }
        },
        text = {
            when (mode) {
                RegisterMode.Input -> {
                    CouponInputContent(
                        code = code,
                        onCodeChange = { code = it },
                        activationState = activationState,
                        isLoading = isLoading,
                        myCoupons = myCoupons,
                        myCouponsLoading = myCouponsLoading,
                        onScanClick = { mode = RegisterMode.Scanning },
                    )
                }

                RegisterMode.Scanning -> {
                    CouponScanContent(
                        onDetected = { scanned ->
                            code = scanned
                            mode = RegisterMode.Input
                        },
                    )
                }
            }
        },
        confirmButton = {
            when (mode) {
                RegisterMode.Input -> {
                    if (activationState !is CouponActivationUiState.Success) {
                        TextButton(
                            onClick = { onActivate(code) },
                            enabled = !isLoading && code.isNotBlank(),
                        ) {
                            Text(text = stringResource(R.string.coupon_dialog_register))
                        }
                    }
                }

                RegisterMode.Scanning -> {
                    TextButton(onClick = { mode = RegisterMode.Input }) {
                        Text(text = stringResource(R.string.coupon_dialog_cancel))
                    }
                }
            }
        },
        dismissButton =
            if (mode == RegisterMode.Input && activationState !is CouponActivationUiState.Success) {
                {
                    TextButton(onClick = onDismiss, enabled = !isLoading) {
                        Text(text = stringResource(R.string.coupon_dialog_cancel))
                    }
                }
            } else {
                null
            },
    )
}

@Composable
private fun CouponInputContent(
    code: String,
    onCodeChange: (String) -> Unit,
    activationState: CouponActivationUiState,
    isLoading: Boolean,
    myCoupons: List<CouponListItem>,
    myCouponsLoading: Boolean,
    onScanClick: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        if (activationState is CouponActivationUiState.Success) {
            CouponSuccessFeedback(durationDays = activationState.durationDays)
        } else {
            OutlinedButton(
                onClick = onScanClick,
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = Icons.Outlined.QrCodeScanner,
                    contentDescription = null,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(R.string.coupon_dialog_scan_qr))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f))
                Text(
                    text = stringResource(R.string.coupon_dialog_or_separator),
                    modifier = Modifier.padding(horizontal = 8.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                HorizontalDivider(modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = code,
                onValueChange = onCodeChange,
                placeholder = { Text(text = stringResource(R.string.coupon_dialog_input_hint)) },
                enabled = !isLoading,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            val errorResId = activationState.errorMessageResId()
            if (errorResId != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(errorResId),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            if (isLoading) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(SpinnerSize),
                        strokeWidth = SpinnerStroke,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.coupon_dialog_registering),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        CouponListSection(coupons = myCoupons, isLoading = myCouponsLoading)
    }
}

@Composable
private fun CouponSuccessFeedback(durationDays: Long?) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        Icon(
            imageVector = Icons.Outlined.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(40.dp),
        )
        Spacer(modifier = Modifier.height(12.dp))
        val message =
            if (durationDays != null) {
                stringResource(R.string.coupon_success_registered_days, durationDays)
            } else {
                stringResource(R.string.coupon_success_registered_unlimited)
            }
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun CouponListSection(
    coupons: List<CouponListItem>,
    isLoading: Boolean,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.coupon_list_section_title),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(8.dp))
        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(SpinnerSize), strokeWidth = SpinnerStroke)
                }
            }

            coupons.isEmpty() -> {
                Text(
                    text = stringResource(R.string.coupon_list_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            else -> {
                LazyColumn(modifier = Modifier.heightIn(max = 220.dp)) {
                    items(coupons, key = { it.couponId }) { item ->
                        CouponListItemRow(item = item)
                    }
                }
            }
        }
    }
}

@Composable
private fun CouponListItemRow(item: CouponListItem) {
    val dateFormatter = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault()) }
    val activatedDate =
        remember(item.activatedAtEpochMillis) {
            dateFormatter.format(Instant.ofEpochMilli(item.activatedAtEpochMillis))
        }
    val displayCode = item.shortCode ?: item.couponId.take(COUPON_ID_FALLBACK_LENGTH)

    val rowAlpha =
        when (item.status) {
            CouponListItemStatus.ACTIVATED -> 1f
            CouponListItemStatus.EXPIRED, CouponListItemStatus.REVOKED -> INACTIVE_ROW_ALPHA
        }
    val codeColor =
        when (item.status) {
            CouponListItemStatus.ACTIVATED -> MaterialTheme.colorScheme.onSurface
            CouponListItemStatus.EXPIRED, CouponListItemStatus.REVOKED -> MaterialTheme.colorScheme.onSurfaceVariant
        }
    val codeDecoration =
        when (item.status) {
            CouponListItemStatus.REVOKED -> TextDecoration.LineThrough
            CouponListItemStatus.ACTIVATED, CouponListItemStatus.EXPIRED -> TextDecoration.None
        }
    val badgeColor =
        when (item.status) {
            CouponListItemStatus.ACTIVATED -> MaterialTheme.colorScheme.primary
            CouponListItemStatus.EXPIRED -> MaterialTheme.colorScheme.onSurfaceVariant
            CouponListItemStatus.REVOKED -> MaterialTheme.colorScheme.error
        }
    val badgeText =
        when (item.status) {
            CouponListItemStatus.ACTIVATED -> stringResource(R.string.coupon_list_status_activated)
            CouponListItemStatus.EXPIRED -> stringResource(R.string.coupon_list_status_expired)
            CouponListItemStatus.REVOKED -> stringResource(R.string.coupon_list_status_revoked)
        }

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .alpha(rowAlpha)
                .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = displayCode,
                    style = MaterialTheme.typography.bodyMedium,
                    color = codeColor,
                    textDecoration = codeDecoration,
                )
                item.batchLabel?.let { label ->
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier =
                            Modifier
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(4.dp),
                                ).padding(horizontal = 4.dp, vertical = 2.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            val durationText =
                if (item.durationDays != null) {
                    pluralStringResource(R.plurals.coupon_list_days_format, item.durationDays.toInt(), item.durationDays)
                } else {
                    stringResource(R.string.coupon_list_unlimited)
                }
            Text(
                text = "$durationText · $activatedDate",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = badgeText,
            style = MaterialTheme.typography.labelSmall,
            color = badgeColor,
        )
    }
}

@Composable
private fun CouponScanContent(onDetected: (String) -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        CouponQrScannerPane(
            onResult = onDetected,
            modifier =
                Modifier
                    .size(ScannerSize)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.scrim),
        )
    }
}

private fun CouponActivationUiState.errorMessageResId(): Int? =
    when (this) {
        is CouponActivationUiState.Failure -> {
            when (failure) {
                ActivationResult.Failure.InvalidFormat -> R.string.coupon_error_invalid_format
                ActivationResult.Failure.InvalidSignature -> R.string.coupon_error_invalid_signature
                ActivationResult.Failure.NotFound -> R.string.coupon_error_not_found
                ActivationResult.Failure.AlreadyUsedOnAnotherDevice -> R.string.coupon_error_already_used
                ActivationResult.Failure.Revoked -> R.string.coupon_error_revoked
                ActivationResult.Failure.NetworkError -> R.string.coupon_error_network
                ActivationResult.Failure.UnknownError -> R.string.coupon_error_unknown
            }
        }

        else -> {
            null
        }
    }
