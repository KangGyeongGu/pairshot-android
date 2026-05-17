plugins {
    id("pairshot.android.feature")
}

android {
    namespace = "com.pairshot.feature.camera"
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:domain"))
    implementation(project(":core:navigation"))
    implementation(project(":core:ui"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:rendering"))
    implementation(project(":core:infra"))
    implementation(project(":core:ads"))
    implementation(project(":core:ads-ui"))
    implementation(project(":feature:tutorial"))

    implementation(libs.camerax.compose)
    implementation(libs.camerax.view)
}
