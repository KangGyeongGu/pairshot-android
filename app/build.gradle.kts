import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

val localProperties =
    Properties().apply {
        val file = rootProject.file("local.properties")
        if (file.exists()) load(file.inputStream())
    }

val appVersionTriple = resolveAppVersionFromGitTag(rootProject.projectDir)
val appVersionName = "${appVersionTriple.first}.${appVersionTriple.second}.${appVersionTriple.third}"
val appVersionCode =
    appVersionTriple.first * 10_000 + appVersionTriple.second * 100 + appVersionTriple.third

android {
    namespace = "com.pairshot"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.pairshot"
        minSdk = 26
        targetSdk = 35
        versionCode = appVersionCode
        versionName = appVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {

            val keystorePath =
                System.getenv("KEYSTORE_PATH")
                    ?: localProperties.getProperty("KEYSTORE_PATH")
            if (keystorePath != null) {
                storeFile = file(keystorePath)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                    ?: localProperties.getProperty("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                    ?: localProperties.getProperty("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
                    ?: localProperties.getProperty("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            manifestPlaceholders["admobAppId"] =
                localProperties.getProperty("ADMOB_TEST_APP_ID") ?: ""
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            manifestPlaceholders["admobAppId"] =
                localProperties.getProperty("ADMOB_APP_ID") ?: ""
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        jvmToolchain(21)
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

fun resolveAppVersionFromGitTag(projectRoot: java.io.File): Triple<Int, Int, Int> {
    val fallback = Triple(0, 0, 0)
    val tag =
        runCatching {
            val process =
                ProcessBuilder(
                    "git",
                    "describe",
                    "--tags",
                    "--abbrev=0",
                    "--match",
                    "v[0-9]*.[0-9]*.[0-9]*",
                ).directory(projectRoot)
                    .redirectErrorStream(false)
                    .start()
            val text =
                process.inputStream
                    .bufferedReader()
                    .readText()
                    .trim()
            process.waitFor()
            if (process.exitValue() == 0) text else null
        }.getOrNull() ?: return fallback

    val match = Regex("^v(\\d+)\\.(\\d+)\\.(\\d+)$").matchEntire(tag) ?: return fallback
    val (major, minor, patch) = match.destructured
    return Triple(major.toInt(), minor.toInt(), patch.toInt())
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:domain"))
    implementation(project(":core:navigation"))
    implementation(project(":core:rendering"))
    implementation(project(":core:infra"))
    implementation(project(":core:database"))
    implementation(project(":core:datastore"))
    implementation(project(":core:storage"))
    implementation(project(":core:data"))
    implementation(project(":core:ui"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:ads"))
    implementation(project(":core:promotion"))
    implementation(project(":core:billing"))
    implementation(project(":core:entitlement"))

    implementation(project(":feature:camera"))
    implementation(project(":feature:settings"))
    implementation(project(":feature:home"))
    implementation(project(":feature:album"))
    implementation(project(":feature:pair-preview"))
    implementation(project(":feature:export-settings"))
    implementation(project(":feature:paywall"))
    implementation(project(":feature:tutorial"))

    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.compose.tooling.preview)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.camerax.core)
    implementation(libs.camerax.camera2)
    implementation(libs.camerax.lifecycle)
    implementation(libs.camerax.view)
    implementation(libs.camerax.compose)
    implementation(libs.camerax.extensions)

    implementation(libs.navigation.compose)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.datastore.preferences)

    implementation(libs.activity.compose)
    implementation(libs.core.splashscreen)

    implementation(libs.appcompat)

    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.process)

    implementation(libs.concurrent.futures.ktx)

    implementation(libs.profileinstaller)

    implementation(libs.colorpicker.compose)

    implementation(libs.timber)

    implementation(libs.jankstats)

    debugImplementation(libs.leakcanary.android)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.archunit.junit5)
    testRuntimeOnly(libs.junit.platform.launcher)
    androidTestImplementation(composeBom)
    androidTestImplementation(libs.compose.ui.test)
}
