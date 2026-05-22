plugins {
    id("pairshot.android.library")
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.pairshot.core.rendering"
}

dependencies {
    implementation(project(":core:model"))
    implementation(libs.exifinterface)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation(libs.timber)

    testImplementation(libs.junit)
}
