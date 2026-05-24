plugins {
    id("pairshot.android.library")
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.pairshot.core.designsystem"

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":core:model"))

    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.tooling.preview)
}
