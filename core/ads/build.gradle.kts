import java.util.Properties

plugins {
    id("pairshot.android.library")
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.compose)
}

val localProperties =
    Properties().apply {
        val file = rootProject.file("local.properties")
        if (file.exists()) load(file.inputStream())
    }

android {
    namespace = "com.pairshot.core.ads"
    buildFeatures {
        compose = true
        buildConfig = true
    }

    defaultConfig {
        buildConfigField(
            "String",
            "ADMOB_BANNER_AD_UNIT_ID",
            "\"${localProperties.getProperty("ADMOB_BANNER_DEFAULT") ?: ""}\"",
        )
        buildConfigField(
            "String",
            "ADMOB_INTERSTITIAL_EXPORT_COMPLETE_AD_UNIT_ID",
            "\"${localProperties.getProperty("ADMOB_INTERSTITIAL_EXPORT_COMPLETE") ?: ""}\"",
        )
        buildConfigField(
            "String",
            "ADMOB_APP_OPEN_AD_UNIT_ID",
            "\"${localProperties.getProperty("ADMOB_APP_OPEN") ?: ""}\"",
        )
        buildConfigField(
            "String",
            "ADMOB_NATIVE_AD_UNIT_ID",
            "\"${localProperties.getProperty("ADMOB_NATIVE") ?: ""}\"",
        )
        buildConfigField(
            "String",
            "ADMOB_REWARDED_AD_UNIT_ID",
            "\"${localProperties.getProperty("ADMOB_REWARDED") ?: ""}\"",
        )
    }
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:domain"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:ui"))

    api(libs.play.services.ads)

    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)

    implementation(libs.activity.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.process)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.timber)
}
