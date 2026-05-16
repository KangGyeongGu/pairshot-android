pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "PairShot"

include(":app")

include(":core:model")
include(":core:domain")
include(":core:navigation")
include(":core:ui")
include(":core:designsystem")
include(":core:rendering")
include(":core:infra")
include(":core:database")
include(":core:storage")
include(":core:datastore")
include(":core:data")
include(":core:ads")
include(":core:ads-ui")
include(":core:promotion")
include(":core:billing")
include(":core:entitlement")

include(":feature:camera")
include(":feature:settings")
include(":feature:home")
include(":feature:album")
include(":feature:pair-preview")
include(":feature:export-settings")
include(":feature:paywall")

include(":microbenchmark:rendering")
