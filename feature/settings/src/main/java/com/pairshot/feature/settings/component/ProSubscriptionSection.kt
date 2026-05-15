package com.pairshot.feature.settings.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pairshot.core.billing.domain.SubscriptionStatus
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
    subscriptionStatus: SubscriptionStatus,
    onLearnMore: () -> Unit,
    onManageSubscription: () -> Unit,
    onRestore: () -> Unit,
    onPromoCode: () -> Unit,
) {
    SettingsSectionLabel(label = stringResource(R.string.settings_section_pro_subscription))
    Spacer(modifier = Modifier.height(PairShotSpacing.iconTextGap))

    when (entitlement.source) {
        EntitlementSource.NONE -> FreeBlock(onLearnMore = onLearnMore, onPromoCode = onPromoCode)
        EntitlementSource.SUBSCRIPTION ->
            SubscriptionBlock(
                expiryEpochMs = entitlement.expiresAtEpochMs ?: (subscriptionStatus as? SubscriptionStatus.Active)?.expiryEpochMs,
                onManageSubscription = onManageSubscription,
                onRestore = onRestore,
                onPromoCode = onPromoCode,
            )
        EntitlementSource.COUPON ->
            CouponBlock(
                expiryEpochMs = entitlement.expiresAtEpochMs,
                onRestore = onRestore,
                onPromoCode = onPromoCode,
            )
    }
    Spacer(modifier = Modifier.height(PairShotSpacing.cardPadding))
}

@Composable
private fun FreeBlock(
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
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.settings_pro_free_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(12.dp))
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
        SettingsItem(
            label = stringResource(R.string.settings_item_promo_code),
            onClick = onPromoCode,
        )
    }
}

@Composable
private fun SubscriptionBlock(
    expiryEpochMs: Long?,
    onManageSubscription: () -> Unit,
    onRestore: () -> Unit,
    onPromoCode: () -> Unit,
) {
    SettingsCard {
        SettingsItem(
            label = stringResource(R.string.settings_pro_manage_subscription),
            trailing = expiryEpochMs?.let { formatDate(it) },
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
            onClick = onPromoCode,
        )
    }
}

@Composable
private fun CouponBlock(
    expiryEpochMs: Long?,
    onRestore: () -> Unit,
    onPromoCode: () -> Unit,
) {
    SettingsCard {
        SettingsItem(
            label = stringResource(R.string.settings_pro_restore),
            onClick = onRestore,
        )
        SettingsDivider()
        SettingsItem(
            label = stringResource(R.string.settings_item_promo_code),
            trailing =
                if (expiryEpochMs != null) {
                    formatDate(expiryEpochMs)
                } else {
                    stringResource(R.string.settings_pro_coupon_unlimited)
                },
            onClick = onPromoCode,
        )
    }
}

private val DATE_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault())

private fun formatDate(epochMs: Long): String = DATE_FORMATTER.format(Instant.ofEpochMilli(epochMs))
