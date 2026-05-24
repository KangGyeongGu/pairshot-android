import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.android.library")
            }

            extensions.configure<LibraryExtension> {
                compileSdk = 36
                defaultConfig {
                    minSdk = 26
                }
                compileOptions {
                    sourceCompatibility = org.gradle.api.JavaVersion.VERSION_17
                    targetCompatibility = org.gradle.api.JavaVersion.VERSION_17
                }
                lint {
                    warningsAsErrors = false
                    abortOnError = true
                    checkReleaseBuilds = true
                    error += listOf("HardcodedText", "MissingTranslation", "UnusedResources")
                    disable += listOf("GradleDependency", "OldTargetApi")
                }
            }

            extensions.configure<KotlinAndroidProjectExtension> {
                jvmToolchain {
                    languageVersion.set(JavaLanguageVersion.of(21))
                }
            }

            plugins.withId("org.jetbrains.kotlin.plugin.compose") {
                val composeMetricsDir =
                    layout.buildDirectory
                        .dir("compose_metrics")
                        .map { it.asFile.absolutePath }
                tasks.withType<KotlinCompilationTask<*>>().configureEach {
                    compilerOptions.freeCompilerArgs.addAll(
                        composeMetricsDir.map { dir ->
                            listOf(
                                "-P",
                                "plugin:androidx.compose.compiler.plugins.kotlin:reportsDestination=$dir",
                                "-P",
                                "plugin:androidx.compose.compiler.plugins.kotlin:metricsDestination=$dir",
                            )
                        },
                    )
                }
            }
        }
    }
}
