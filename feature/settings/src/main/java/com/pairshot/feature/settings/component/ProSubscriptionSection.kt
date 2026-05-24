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
import com.pairshot.core.billing.domain.SubscriptionStatus
import com.pairshot.core.designsystem.PairShotCard
import com.pairshot.core.designsystem.PairShotSpacing
import com.pairshot.core.domain.membership.Membership
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
    membership: Membership,
    subscriptionStatus: SubscriptionStatus,
    onLearnMore: () -> Unit,
    onViewSubscriptionOptions: () -> Unit,
    onManageSubscription: () -> Unit,
    onRestore: () -> Unit,
    onPromoCode: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        SettingsSectionLabel(label = stringResource(R.string.settings_section_pro_subscription))
        Spacer(modifier = Modifier.height(PairShotSpacing.sm))

        val membershipLabel =
            if (membership.isPro) {
                stringResource(R.string.settings_pro_membership_pro)
            } else {
                stringResource(R.string.settings_pro_membership_free)
            }
        val promoTrailing = promoTrailingText(membership)
        val hasActiveSubscription = subscriptionStatus is SubscriptionStatus.Active

        if (!membership.isPro) {
            FreeBlock(
                membershipLabel = membershipLabel,
                promoTrailing = promoTrailing,
                onLearnMore = onLearnMore,
                onPromoCode = onPromoCode,
            )
        } else {
            ProBlock(
                membershipLabel = membershipLabel,
                promoTrailing = promoTrailing,
                hasActiveSubscription = hasActiveSubscription,
                onViewSubscriptionOptions = onViewSubscriptionOptions,
                onManageSubscription = onManageSubscription,
                onRestore = onRestore,
                onPromoCode = onPromoCode,
            )
        }
        Spacer(modifier = Modifier.height(PairShotCard.innerPadding))
    }
}

@Composable
private fun promoTrailingText(membership: Membership): String? =
    when {
        membership.isPro && membership.proExpiresAtEpochMillis != null -> {
            stringResource(R.string.settings_pro_coupon_expires, formatDate(membership.proExpiresAtEpochMillis!!))
        }

        membership.isPro && membership.proExpiresAtEpochMillis == null -> {
            stringResource(R.string.settings_pro_coupon_unlimited)
        }

        !membership.isPro && membership.isAdFree && membership.adFreeExpiresAtEpochMillis != null -> {
            stringResource(R.string.settings_pro_coupon_expires, formatDate(membership.adFreeExpiresAtEpochMillis!!))
        }

        !membership.isPro && membership.isAdFree -> {
            stringResource(R.string.settings_pro_coupon_unlimited)
        }

        else -> {
            null
        }
    }

@Composable
private fun FreeBlock(
    membershipLabel: String,
    promoTrailing: String?,
    onLearnMore: () -> Unit,
    onPromoCode: () -> Unit,
) {
    SettingsCard {
        Column(modifier = Modifier.padding(PairShotCard.innerPadding)) {
            Text(
                text = stringResource(R.string.settings_pro_free_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(PairShotSpacing.md))
            ProValueRow(stringResource(R.string.settings_pro_value_unlimited))
            Spacer(modifier = Modifier.height(PairShotSpacing.sm))
            ProValueRow(stringResource(R.string.settings_pro_value_no_ads))
            Spacer(modifier = Modifier.height(PairShotSpacing.sm))
            ProValueRow(stringResource(R.string.settings_pro_value_pro_features))
            Spacer(modifier = Modifier.height(PairShotSpacing.lg))
            Button(
                onClick = onLearnMore,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(PairShotSpacing.md),
            ) {
                Text(text = stringResource(R.string.settings_pro_learn_more))
            }
        }
    }
    Spacer(modifier = Modifier.height(PairShotSpacing.sm))
    SettingsCard {
        MembershipItem(value = membershipLabel)
        SettingsDivider()
        SettingsItem(
            label = stringResource(R.string.settings_item_promo_code),
            trailing = promoTrailing,
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
            modifier = Modifier.padding(end = PairShotSpacing.md),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun ProBlock(
    membershipLabel: String,
    promoTrailing: String?,
    hasActiveSubscription: Boolean,
    onViewSubscriptionOptions: () -> Unit,
    onManageSubscription: () -> Unit,
    onRestore: () -> Unit,
    onPromoCode: () -> Unit,
) {
    SettingsCard {
        MembershipItem(value = membershipLabel)
        SettingsDivider()
        SettingsItem(
            label = stringResource(R.string.settings_pro_view_options),
            onClick = onViewSubscriptionOptions,
        )
        if (hasActiveSubscription) {
            SettingsDivider()
            SettingsItem(
                label = stringResource(R.string.settings_pro_manage_subscription),
                onClick = onManageSubscription,
            )
        }
        SettingsDivider()
        SettingsItem(
            label = stringResource(R.string.settings_pro_restore),
            onClick = onRestore,
        )
        SettingsDivider()
        SettingsItem(
            label = stringResource(R.string.settings_item_promo_code),
            trailing = promoTrailing,
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
