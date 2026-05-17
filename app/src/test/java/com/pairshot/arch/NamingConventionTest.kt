@file:Suppress("ktlint:standard:property-naming")

package com.pairshot.arch

import com.pairshot.arch.config.DoNotIncludeAndroidGenerated
import com.pairshot.arch.config.DoNotIncludeKotlinWhenMappings
import com.pairshot.arch.config.HasInvokeMethod
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.junit.AnalyzeClasses
import com.tngtech.archunit.junit.ArchTest
import com.tngtech.archunit.lang.ArchRule
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes

@AnalyzeClasses(
    packages = ["com.pairshot"],
    importOptions = [
        ImportOption.DoNotIncludeTests::class,
        DoNotIncludeAndroidGenerated::class,
        DoNotIncludeKotlinWhenMappings::class,
    ],
)
class NamingConventionTest {
    @ArchTest
    val `N-01 UseCase classes should end with UseCase`: ArchRule =
        classes()
            .that()
            .haveSimpleNameEndingWith("UseCase")
            .and()
            .resideInAPackage("..domain..")
            .and()
            .areTopLevelClasses()
            .should()
            .haveSimpleNameEndingWith("UseCase")
            .because("UseCase naming convention")

    @ArchTest
    val `N-02 ViewModel subclasses should end with ViewModel`: ArchRule =
        classes()
            .that()
            .areAssignableTo(androidx.lifecycle.ViewModel::class.java)
            .and()
            .areTopLevelClasses()
            .should()
            .haveSimpleNameEndingWith("ViewModel")
            .because("ViewModel naming convention")

    @ArchTest
    val `N-03 domain Repository interfaces should be interfaces`: ArchRule =
        classes()
            .that()
            .haveSimpleNameEndingWith("Repository")
            .and()
            .resideInAPackage("..domain..")
            .and()
            .areTopLevelClasses()
            .should()
            .beInterfaces()
            .because("Domain repository must be an interface — implementation belongs in data layer")

    @ArchTest
    val `N-04 RepositoryImpl should reside in data repository package`: ArchRule =
        classes()
            .that()
            .haveSimpleNameEndingWith("RepositoryImpl")
            .and()
            .areTopLevelClasses()
            .should()
            .resideInAPackage("..data.repository..")
            .because("Repository implementations belong in data.repository package")

    @ArchTest
    val `N-05 UseCase should have invoke method`: ArchRule =
        classes()
            .that()
            .haveSimpleNameEndingWith("UseCase")
            .and()
            .resideInAPackage("..domain..")
            .and()
            .areTopLevelClasses()
            .and()
            .haveSimpleNameNotEndingWith("_Factory")
            .should(HasInvokeMethod())
            .because("UseCase must have operator fun invoke()")
}
