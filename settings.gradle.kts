pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // For RRule library (and possibly others)
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "crisiscleanup"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include(":app")
include(":app-sandbox")
include(":core:addresssearch")
include(":core:appnav")
include(":core:app-component")
include(":core:common")
include(":core:commonassets")
include(":core:commoncase")
include(":core:data")
include(":core:data-test")
include(":core:database")
include(":core:datastore")
include(":core:datastore-proto")
include(":core:datastore-test")
include(":core:designsystem")
include(":core:mapmarker")
include(":core:model")
include(":core:network")
include(":core:renderscript-toolkit")
include(":core:selectincident")
include(":core:ui")
include(":core:testing")
include(":feature:authentication")
include(":feature:caseeditor")
include(":feature:cases")
include(":feature:dashboard")
include(":feature:incidentcache")
include(":feature:lists")
include(":feature:mediamanage")
include(":feature:menu")
include(":feature:organizationmanage")
include(":feature:syncinsights")
include(":feature:team")
include(":feature:userfeedback")
include(":lint")
include(":sync:work")
include(":sync:sync-test")
include(":ui-test-hilt-manifest")
