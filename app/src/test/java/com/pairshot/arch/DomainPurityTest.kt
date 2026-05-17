@file:Suppress("ktlint:standard:property-naming")

package com.pairshot.arch

import com.pairshot.arch.config.DoNotIncludeAndroidGenerated
import com.pairshot.arch.config.DoNotIncludeKotlinWhenMappings
import com.tngtech.archunit.base.DescribedPredicate
import com.tngtech.archunit.core.domain.JavaClass
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.junit.AnalyzeClasses
import com.tngtech.archunit.junit.ArchTest
import com.tngtech.archunit.lang.ArchRule
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses

@AnalyzeClasses(
    packages = ["com.pairshot"],
    importOptions = [
        ImportOption.DoNotIncludeTests::class,
        DoNotIncludeAndroidGenerated::class,
        DoNotIncludeKotlinWhenMappings::class,
    ],
)
class DomainPurityTest {
    @ArchTest
    val `D-01 domain should not import android`: ArchRule =
        noClasses()
            .that()
            .resideInAPackage("..domain..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("android..")
            .because("Domain layer must be pure Kotlin — no Android framework imports")

    @ArchTest
    val `D-02 domain should not import androidx`: ArchRule =
        noClasses()
            .that()
            .resideInAPackage("..domain..")
            .should()
            .dependOnClassesThat(
                object : DescribedPredicate<JavaClass>("reside in 'androidx..' (excluding Compose compiler internals)") {
                    override fun test(clazz: JavaClass): Boolean =
                        clazz.packageName.startsWith("androidx.") &&
                            !clazz.fullName.startsWith("androidx.compose.runtime.internal.")
                },
            ).because("Domain layer must be pure Kotlin — no AndroidX imports")

    @ArchTest
    val `D-03 domain should not import data layer`: ArchRule =
        noClasses()
            .that()
            .resideInAPackage("..domain..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("com.pairshot.data..")
            .because("Domain must not depend on Data layer directly")

    @ArchTest
    val `D-04 domain should not use Uri`: ArchRule =
        noClasses()
            .that()
            .resideInAPackage("..domain..")
            .should()
            .dependOnClassesThat()
            .haveFullyQualifiedName("android.net.Uri")
            .because("Domain must use String instead of Uri")

    @ArchTest
    val `D-05 domain should not use Context`: ArchRule =
        noClasses()
            .that()
            .resideInAPackage("..domain..")
            .should()
            .dependOnClassesThat()
            .haveFullyQualifiedName("android.content.Context")
            .because("Domain must not depend on Android Context")

    @ArchTest
    val `D-06 domain should not use Bitmap`: ArchRule =
        noClasses()
            .that()
            .resideInAPackage("..domain..")
            .should()
            .dependOnClassesThat()
            .haveFullyQualifiedName("android.graphics.Bitmap")
            .because("Domain must be pure Kotlin — Bitmap belongs in Data/Core layer")

    @ArchTest
    val `D-07 domain should not use File`: ArchRule =
        noClasses()
            .that()
            .resideInAPackage("..domain..")
            .should()
            .dependOnClassesThat()
            .haveFullyQualifiedName("java.io.File")
            .because("Domain must be pure Kotlin — file operations belong in Data layer")
}
