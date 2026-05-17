@file:Suppress("ktlint:standard:property-naming")

package com.pairshot.arch

import com.pairshot.arch.config.DoNotIncludeAndroidGenerated
import com.pairshot.arch.config.DoNotIncludeKotlinWhenMappings
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
class UiBoundaryTest {
    @ArchTest
    val `U-01 Feature should not use ExifInterface`: ArchRule =
        noClasses()
            .that()
            .resideInAPackage("com.pairshot.feature..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("androidx.exifinterface..")
            .because("Feature must not parse EXIF directly — use :core:rendering ExifBitmapLoader")

    @ArchTest
    val `U-02 Feature should not use BitmapFactory`: ArchRule =
        noClasses()
            .that()
            .resideInAPackage("com.pairshot.feature..")
            .should()
            .dependOnClassesThat()
            .haveFullyQualifiedName("android.graphics.BitmapFactory")
            .because("Feature must not decode bitmaps directly — use :core:rendering loader")

    @ArchTest
    val `U-03 Feature should not use MediaStore`: ArchRule =
        noClasses()
            .that()
            .resideInAPackage("com.pairshot.feature..")
            .should()
            .dependOnClassesThat()
            .haveFullyQualifiedName("android.provider.MediaStore")
            .because("Feature must not access MediaStore — delegate to Data layer")

    @ArchTest
    val `U-04 Feature should not use ZipOutputStream`: ArchRule =
        noClasses()
            .that()
            .resideInAPackage("com.pairshot.feature..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("java.util.zip..")
            .because("Feature must not create ZIP — delegate to Data layer")

    @ArchTest
    val `U-05 Feature should not use ContentResolver`: ArchRule =
        noClasses()
            .that()
            .resideInAPackage("com.pairshot.feature..")
            .should()
            .dependOnClassesThat()
            .haveFullyQualifiedName("android.content.ContentResolver")
            .because("Feature must not use ContentResolver — delegate to Data layer")

    @ArchTest
    val `U-06 Feature should not use android graphics Canvas`: ArchRule =
        noClasses()
            .that()
            .resideInAPackage("com.pairshot.feature..")
            .should()
            .dependOnClassesThat()
            .haveFullyQualifiedName("android.graphics.Canvas")
            .because("Feature must not render via Canvas — delegate to :core:rendering")
}
