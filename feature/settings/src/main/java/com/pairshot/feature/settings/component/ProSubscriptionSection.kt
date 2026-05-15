package com.pairshot.feature.settings.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pairshot.core.coupon.domain.CouponStatus
import com.pairshot.core.designsystem.PairShotSpacing
import com.pairshot.core.domain.entitlement.EntitlementSource
import com.pairshot.core.domain.entitlement.ProEntitlement
import com.pairshot.core.ui.component.SettingsCard
import com.pairshot.core.ui.component.SettingsDivider
import com.pairshot.core.ui.component.SettingsItem
import com.pairshot.core.ui.component.SettingsSectionLabel
import com.pairshot.feature.settings.R
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun ProSubscriptionSection(
    entitlement: ProEntitlement,
    couponStatus: CouponStatus,
    onLearnMore: () -> Unit,
    onManageSubscription: () -> Unit,
    onRestore: () -> Unit,
    onPromoCode: () -> Unit,
) {
    SettingsSectionLabel(label = stringResource(R.string.settings_section_pro_subscription))
    Spacer(modifier = Modifier.height(PairShotSpacing.iconTextGap))

    val isPro = entitlement.source == EntitlementSource.SUBSCRIPTION
    val membershipLabel =
        if (isPro) {
            stringResource(R.string.settings_pro_membership_pro)
        } else {
            stringResource(R.string.settings_pro_membership_free)
        }
    val couponTrailing = couponTrailingText(couponStatus)

    when (entitlement.source) {
        EntitlementSource.NONE ->
            FreeBlock(
                membershipLabel = membershipLabel,
                couponTrailing = couponTrailing,
                onLearnMore = onLearnMore,
                onPromoCode = onPromoCode,
            )
        EntitlementSource.SUBSCRIPTION ->
            SubscriptionBlock(
                membershipLabel = membershipLabel,
                couponTrailing = couponTrailing,
                onManageSubscription = onManageSubscription,
                onRestore = onRestore,
                onPromoCode = onPromoCode,
            )
        EntitlementSource.COUPON ->
            CouponBlock(
                membershipLabel = membershipLabel,
                couponTrailing = couponTrailing,
                onRestore = onRestore,
                onPromoCode = onPromoCode,
            )
    }
    Spacer(modifier = Modifier.height(PairShotSpacing.cardPadding))
}

@Composable
private fun couponTrailingText(status: CouponStatus): String? =
    when (status) {
        CouponStatus.None -> null
        is CouponStatus.Active -> {
            val expiry = status.expiresAtEpochMillis
            if (expiry != null) {
                stringResource(R.string.settings_pro_coupon_expires, formatDate(expiry))
            } else {
                stringResource(R.string.settings_pro_coupon_unlimited)
            }
        }
        is CouponStatus.Expired ->
            stringResource(R.string.settings_pro_coupon_expires, formatDate(status.expiresAtEpochMillis))
    }

@Composable
private fun FreeBlock(
    membershipLabel: String,
    couponTrailing: String?,
    onLearnMore: () -> Unit,
    onPromoCode: () -> Unit,
) {
    SettingsCard {
        Column(modifier = Modifier.padding(PairShotSpacing.cardPadding)) {
            Text(
                text = stringResource(R.string.settings_pro_free_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(12.dp))
            ProValueRow(stringResource(R.string.settings_pro_value_unlimited))
            Spacer(modifier = Modifier.height(8.dp))
            ProValueRow(stringResource(R.string.settings_pro_value_no_ads))
            Spacer(modifier = Modifier.height(8.dp))
            ProValueRow(stringResource(R.string.settings_pro_value_pro_features))
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onLearnMore,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(text = stringResource(R.string.settings_pro_learn_more))
            }
        }
    }
    Spacer(modifier = Modifier.height(PairShotSpacing.iconTextGap))
    SettingsCard {
        MembershipItem(value = membershipLabel)
        SettingsDivider()
        SettingsItem(
            label = stringResource(R.string.settings_item_promo_code),
            trailing = couponTrailing,
            onClick = onPromoCode,
        )
    }
}

@Composable
private fun ProValueRow(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Outlined.Check,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(end = 10.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun SubscriptionBlock(
    membershipLabel: String,
    couponTrailing: String?,
    onManageSubscription: () -> Unit,
    onRestore: () -> Unit,
    onPromoCode: () -> Unit,
) {
    SettingsCard {
        MembershipItem(value = membershipLabel)
        SettingsDivider()
        SettingsItem(
            label = stringResource(R.string.settings_pro_manage_subscription),
            onClick = onManageSubscription,
        )
        SettingsDivider()
        SettingsItem(
            label = stringResource(R.string.settings_pro_restore),
            onClick = onRestore,
        )
        SettingsDivider()
        SettingsItem(
            label = stringResource(R.string.settings_item_promo_code),
            trailing = couponTrailing,
            onClick = onPromoCode,
        )
    }
}

@Composable
private fun CouponBlock(
    membershipLabel: String,
    couponTrailing: String?,
    onRestore: () -> Unit,
    onPromoCode: () -> Unit,
) {
    SettingsCard {
        MembershipItem(value = membershipLabel)
        SettingsDivider()
        SettingsItem(
            label = stringResource(R.string.settings_pro_restore),
            onClick = onRestore,
        )
        SettingsDivider()
        SettingsItem(
            label = stringResource(R.string.settings_item_promo_code),
            trailing = couponTrailing,
            onClick = onPromoCode,
        )
    }
}

@Composable
private fun MembershipItem(value: String) {
    SettingsItem(
        label = stringResource(R.string.settings_pro_membership),
        trailing = value,
    )
}

private val DATE_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault())

private fun formatDate(epochMs: Long): String = DATE_FORMATTER.format(Instant.ofEpochMilli(epochMs))
