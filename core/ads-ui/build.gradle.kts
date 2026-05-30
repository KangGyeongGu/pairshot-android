plugins {
    id("pairshot.android.library")
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.pairshot.core.adsui"
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:ads"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:ui"))

    api(libs.play.services.ads)

    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)

    implementation(libs.activity.compose)
    implementation(libs.lifecycle.runtime.compose)

    implementation(libs.hilt.android)

    implementation(libs.timber)
}
