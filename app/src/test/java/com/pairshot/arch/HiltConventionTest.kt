@file:Suppress("ktlint:standard:property-naming")

package com.pairshot.arch

import com.pairshot.arch.config.DoNotIncludeAndroidGenerated
import com.pairshot.arch.config.DoNotIncludeKotlinWhenMappings
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.junit.AnalyzeClasses
import com.tngtech.archunit.junit.ArchTest
import com.tngtech.archunit.lang.ArchRule
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses

@AnalyzeClasses(
    packages = ["com.pairshot"],
    importOptions = [
        ImportOption.DoNotIncludeTests::class,
        DoNotIncludeAndroidGenerated::class,
        DoNotIncludeKotlinWhenMappings::class,
    ],
)
class HiltConventionTest {
    @ArchTest
    val `H-01 ViewModel should be annotated with HiltViewModel`: ArchRule =
        classes()
            .that()
            .areAssignableTo(androidx.lifecycle.ViewModel::class.java)
            .and()
            .areTopLevelClasses()
            .should()
            .beAnnotatedWith(dagger.hilt.android.lifecycle.HiltViewModel::class.java)
            .because("All ViewModels must use @HiltViewModel for Hilt injection")

    @ArchTest
    val `H-02 Modules should reside in di package`: ArchRule =
        classes()
            .that()
            .areAnnotatedWith(dagger.Module::class.java)
            .and()
            .areTopLevelClasses()
            .should()
            .resideInAnyPackage("com.pairshot.di..", "..core..di..", "..feature..di..")
            .because("All Hilt @Module classes must be in di/ package (app or module-scoped)")

    @ArchTest
    val `H-03 ViewModel should not depend on core data layer`: ArchRule =
        noClasses()
            .that()
            .areAssignableTo(androidx.lifecycle.ViewModel::class.java)
            .should()
            .dependOnClassesThat()
            .resideInAPackage("com.pairshot.core.data..")
            .because("ViewModel must access data through Domain layer only")

    @ArchTest
    val `H-04 Feature should not access Room directly`: ArchRule =
        noClasses()
            .that()
            .resideInAPackage("com.pairshot.feature..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("androidx.room..")
            .because("Feature must not access Room database directly")
}
