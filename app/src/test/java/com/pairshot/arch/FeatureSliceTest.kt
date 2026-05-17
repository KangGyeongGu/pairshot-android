@file:Suppress("ktlint:standard:property-naming")

package com.pairshot.arch

import com.pairshot.arch.config.DoNotIncludeAndroidGenerated
import com.pairshot.arch.config.DoNotIncludeKotlinWhenMappings
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.junit.AnalyzeClasses
import com.tngtech.archunit.junit.ArchTest
import com.tngtech.archunit.lang.ArchRule
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices

@AnalyzeClasses(
    packages = ["com.pairshot"],
    importOptions = [
        ImportOption.DoNotIncludeTests::class,
        DoNotIncludeAndroidGenerated::class,
        DoNotIncludeKotlinWhenMappings::class,
    ],
)
class FeatureSliceTest {
    @ArchTest
    val `F-01 no cyclic dependencies between features`: ArchRule =
        slices()
            .matching("com.pairshot.feature.(*)..")
            .should()
            .beFreeOfCycles()
            .because("Feature modules must not have cyclic dependencies")

    @ArchTest
    val `F-02 features should not depend on each other`: ArchRule =
        slices()
            .matching("com.pairshot.feature.(*)..")
            .should()
            .notDependOnEachOther()
            .ignoreDependency(
                com.tngtech.archunit.core.domain.JavaClass.Predicates
                    .resideOutsideOfPackage("com.pairshot.feature.tutorial.."),
                com.tngtech.archunit.core.domain.JavaClass.Predicates
                    .resideInAPackage("com.pairshot.feature.tutorial.."),
            ).because(
                "Feature modules should be independent — shared code goes to core/. " +
                    "Tutorial is a cross-cutting overlay that other features may import.",
            )
}
