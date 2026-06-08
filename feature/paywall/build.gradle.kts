import java.util.Properties

plugins {
    id("pairshot.android.feature")
}

val localProperties =
    Properties().apply {
        val file = rootProject.file("local.properties")
        if (file.exists()) load(file.inputStream())
    }

android {
    namespace = "com.pairshot.feature.paywall"

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        buildConfigField(
            "String",
            "TERMS_URL",
            "\"${localProperties.getProperty("TERMS_URL") ?: ""}\"",
        )
        buildConfigField(
            "String",
            "PRIVACY_POLICY_URL",
            "\"${localProperties.getProperty("PRIVACY_POLICY_URL") ?: ""}\"",
        )
    }
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:domain"))
    implementation(project(":core:navigation"))
    implementation(project(":core:ui"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:billing"))
    implementation(project(":core:ads"))
}
