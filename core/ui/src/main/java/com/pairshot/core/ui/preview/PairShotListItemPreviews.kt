package com.pairshot.core.ui.preview

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import com.pairshot.core.designsystem.PairShotTheme
import com.pairshot.core.ui.component.PairShotListItem
import com.pairshot.core.ui.component.PairShotSwitchListItem
import com.pairshot.core.ui.component.SettingsCard
import com.pairshot.core.ui.component.SettingsDivider

@PreviewAccessibility
@Composable
@Suppress("UnusedPrivateMember")
private fun PairShotListItemAccessibilityPreview() {
    PairShotTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column {
                SettingsCard {
                    PairShotListItem(
                        headline = "이미지 화질",
                        trailing = "고화질",
                        onClick = {},
                    )
                    SettingsDivider()
                    PairShotListItem(
                        headline = "워터마크 사용자 설정 — 큰 글꼴에서 두 줄로 자연스럽게 wrap 되는지 확인",
                        trailing = "필수 항목 미입력",
                        trailingIsError = true,
                        onClick = {},
                    )
                    SettingsDivider()
                    PairShotSwitchListItem(
                        headline = "오버레이 표시",
                        checked = true,
                        onCheckedChange = {},
                    )
                    SettingsDivider()
                    PairShotSwitchListItem(
                        headline = "워터마크 사용 (한국어 긴 라벨로 wrap 여부 검증)",
                        checked = false,
                        onCheckedChange = {},
                    )
                }
            }
        }
    }
}
