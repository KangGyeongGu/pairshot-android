package com.pairshot.core.ui.preview

import androidx.compose.ui.tooling.preview.Preview

@Preview(name = "textScale Small (0.9)", group = "a11y", fontScale = 0.9f, showBackground = true)
@Preview(name = "textScale Normal (1.0)", group = "a11y", fontScale = 1.0f, showBackground = true)
@Preview(name = "textScale Large (1.15)", group = "a11y", fontScale = 1.15f, showBackground = true)
@Preview(name = "textScale Extra Large (1.3)", group = "a11y", fontScale = 1.3f, showBackground = true)
@Preview(
    name = "Small phone · textScale Extra Large",
    group = "a11y",
    fontScale = 1.3f,
    device = "spec:width=320dp,height=640dp,dpi=320",
    showBackground = true,
)
annotation class PreviewAccessibility
