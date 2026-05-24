import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.extensions.DetektExtension

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.detekt)
    alias(libs.plugins.bcv)
    alias(libs.plugins.kover)
    alias(libs.plugins.owasp.dependencycheck)
    alias(libs.plugins.gradle.doctor)
}

doctor {
    javaHome {
        ensureJavaHomeMatches.set(false)
        ensureJavaHomeIsSet.set(false)
    }
    warnWhenNotUsingParallelGC.set(false)
    disallowMultipleDaemons.set(false)
}

dependencyCheck {
    formats = listOf("HTML", "JSON")
    failBuildOnCVSS = 7f
    suppressionFile = "config/owasp-suppressions.xml"
    analyzers.apply {
        ossIndexEnabled = false
    }
}

dependencies {
    rootProject.subprojects.forEach { sub ->
        if (sub.name != "convention") kover(sub)
    }
}

apiValidation {
    ignoredProjects.addAll(
        rootProject.subprojects
            .map { it.name }
            .filterNot { it == "domain" || it == "model" },
    )
}

subprojects {
    if (name != "convention") {
        apply(plugin = "org.jetbrains.kotlinx.kover")
        apply(plugin = "io.gitlab.arturbosch.detekt")

        extensions.configure<DetektExtension> {
            toolVersion = "1.23.7"
            source.setFrom(
                "src/main/java",
                "src/main/kotlin",
                "src/test/java",
                "src/test/kotlin",
            )
            config.setFrom(rootProject.files("detekt.yml"))
            buildUponDefaultConfig = true
            autoCorrect = true
            parallel = true
        }

        dependencies {
            add("detektPlugins", "io.gitlab.arturbosch.detekt:detekt-formatting:1.23.7")
            add("detektPlugins", "io.nlopez.compose.rules:detekt:0.4.22")
        }

        tasks.withType<Detekt>().configureEach {
            jvmTarget = "17"
            autoCorrect = true
            reports {
                html.required.set(true)
                xml.required.set(false)
                txt.required.set(false)
                md.required.set(false)
            }
        }
    }
}
