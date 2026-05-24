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
class ViewModelBoundaryTest {
    @ArchTest
    val `V-01 ViewModel should not use Context`: ArchRule =
        noClasses()
            .that()
            .areAssignableTo(androidx.lifecycle.ViewModel::class.java)
            .should()
            .dependOnClassesThat()
            .haveFullyQualifiedName("android.content.Context")
            .because("ViewModel must not own Context — delegate to UseCase or Data layer")

    @ArchTest
    val `V-02 ViewModel should not own concrete CameraX types`: ArchRule =
        noClasses()
            .that()
            .areAssignableTo(androidx.lifecycle.ViewModel::class.java)
            .should()
            .dependOnClassesThat()
            .haveFullyQualifiedName("androidx.camera.core.ImageCapture")
            .orShould()
            .dependOnClassesThat()
            .haveFullyQualifiedName("androidx.camera.core.Preview")
            .orShould()
            .dependOnClassesThat()
            .haveFullyQualifiedName("androidx.camera.core.CameraInfo")
            .orShould()
            .dependOnClassesThat()
            .haveFullyQualifiedName("androidx.camera.core.CameraControl")
            .orShould()
            .dependOnClassesThat()
            .haveFullyQualifiedName("androidx.camera.lifecycle.ProcessCameraProvider")
            .orShould()
            .dependOnClassesThat()
            .haveFullyQualifiedName("androidx.camera.extensions.ExtensionsManager")
            .because(
                "ViewModel must only hold CameraSession interface (Pragmatic B-4). " +
                    "SurfaceRequest via StateFlow is allowed. Concrete CameraX types are forbidden.",
            )

    @ArchTest
    val `V-03 ViewModel should not use hardware sensors`: ArchRule =
        noClasses()
            .that()
            .areAssignableTo(androidx.lifecycle.ViewModel::class.java)
            .should()
            .dependOnClassesThat()
            .resideInAPackage("android.hardware..")
            .because("ViewModel must not own sensor managers — delegate to UI Coordinator")

    @ArchTest
    val `V-04 ViewModel should not use ExifInterface`: ArchRule =
        noClasses()
            .that()
            .areAssignableTo(androidx.lifecycle.ViewModel::class.java)
            .should()
            .dependOnClassesThat()
            .resideInAPackage("androidx.exifinterface..")
            .because("ViewModel must not perform EXIF correction — delegate to Data/Core layer")

    @ArchTest
    val `V-05 ViewModel should not use MediaStore`: ArchRule =
        noClasses()
            .that()
            .areAssignableTo(androidx.lifecycle.ViewModel::class.java)
            .should()
            .dependOnClassesThat()
            .haveFullyQualifiedName("android.provider.MediaStore")
            .because("ViewModel must not access MediaStore directly — delegate to Data layer")

    @ArchTest
    val `V-06 ViewModel should not use ContentResolver`: ArchRule =
        noClasses()
            .that()
            .areAssignableTo(androidx.lifecycle.ViewModel::class.java)
            .should()
            .dependOnClassesThat()
            .haveFullyQualifiedName("android.content.ContentResolver")
            .because("ViewModel must not use ContentResolver — delegate to Data layer")

    @ArchTest
    val `V-07 ViewModel should not use java io File`: ArchRule =
        noClasses()
            .that()
            .areAssignableTo(androidx.lifecycle.ViewModel::class.java)
            .should()
            .dependOnClassesThat()
            .haveFullyQualifiedName("java.io.File")
            .because("ViewModel must not manipulate files — delegate to Data layer")

    @ArchTest
    val `V-08 ViewModel should not use ZipOutputStream`: ArchRule =
        noClasses()
            .that()
            .areAssignableTo(androidx.lifecycle.ViewModel::class.java)
            .should()
            .dependOnClassesThat()
            .resideInAPackage("java.util.zip..")
            .because("ViewModel must not create ZIP — delegate to Data layer")

    @ArchTest
    val `V-09 ViewModel should not use ActivityResultLauncher`: ArchRule =
        noClasses()
            .that()
            .areAssignableTo(androidx.lifecycle.ViewModel::class.java)
            .should()
            .dependOnClassesThat()
            .haveFullyQualifiedName("androidx.activity.result.ActivityResultLauncher")
            .because("ViewModel must not own ActivityResultLauncher — belongs to UI layer")

    @ArchTest
    val `V-10 ViewModel should not use android util Range`: ArchRule =
        noClasses()
            .that()
            .areAssignableTo(androidx.lifecycle.ViewModel::class.java)
            .should()
            .dependOnClassesThat()
            .haveFullyQualifiedName("android.util.Range")
            .because("ViewModel must use pure Kotlin types (IntRange, Int pairs) — not Android platform Range")

    @ArchTest
    val `V-11 ViewModel should not use android util Rational`: ArchRule =
        noClasses()
            .that()
            .areAssignableTo(androidx.lifecycle.ViewModel::class.java)
            .should()
            .dependOnClassesThat()
            .haveFullyQualifiedName("android.util.Rational")
            .because("ViewModel must use pure Int numerator/denominator — not Android platform Rational")

    @ArchTest
    val `V-12 ViewModel should not use android graphics Bitmap directly`: ArchRule =
        noClasses()
            .that()
            .areAssignableTo(androidx.lifecycle.ViewModel::class.java)
            .and()
            .haveSimpleNameNotContaining("AfterCameraViewModel")
            .and()
            .haveSimpleNameNotContaining("PairPreviewViewModel")
            .should()
            .dependOnClassesThat()
            .haveFullyQualifiedName("android.graphics.Bitmap")
            .because(
                "ViewModel should not hold Bitmap directly. " +
                    "AfterCameraViewModel is permitted for overlay bitmap state management (rotation preprocessing). " +
                    "PairPreviewViewModel is permitted for live composite preview rendering.",
            )

    @ArchTest
    val `V-14 ViewModel should not use android graphics Matrix`: ArchRule =
        noClasses()
            .that()
            .areAssignableTo(androidx.lifecycle.ViewModel::class.java)
            .should()
            .dependOnClassesThat()
            .haveFullyQualifiedName("android.graphics.Matrix")
            .because(
                "ViewModel must not perform bitmap transform — delegate to infra layer (CameraSession.prepareOverlay)"
            )

    @ArchTest
    val `V-13 ViewModel should not depend on infra impl classes`: ArchRule =
        noClasses()
            .that()
            .areAssignableTo(androidx.lifecycle.ViewModel::class.java)
            .should()
            .dependOnClassesThat()
            .haveFullyQualifiedName("com.pairshot.core.infra.camera.CameraSessionImpl")
            .orShould()
            .dependOnClassesThat()
            .haveFullyQualifiedName("com.pairshot.core.infra.sensor.SensorSessionImpl")
            .because(
                "ViewModel must only hold infra interfaces (CameraSession/SensorSession). " +
                    "Concrete *Impl types are forbidden (Pragmatic B-4).",
            )
}
