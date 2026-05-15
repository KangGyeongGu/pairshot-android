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
    namespace = "com.pairshot.feature.settings"

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
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
    implementation(project(":core:rendering"))
    implementation(project(":core:ads"))
    implementation(project(":core:ads-ui"))
    implementation(project(":core:coupon"))
    implementation(project(":core:billing"))

    implementation(libs.appcompat)
}
