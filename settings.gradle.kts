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
    }
}
rootProject.name = "crisiscleanup"
include(":app")
include(":core:addresssearch")
include(":core:appheader")
include(":core:appnav")
include(":core:common")
include(":core:data")
include(":core:data-test")
include(":core:database")
include(":core:datastore")
include(":core:datastore-test")
include(":core:designsystem")
include(":core:domain")
include(":core:mapmarker")
include(":core:model")
include(":core:network")
include(":core:renderscript-toolkit")
include(":core:testerfeedbackapi")
include(":core:testerfeedback")
include(":core:ui")
include(":core:testing")
include(":feature:authentication")
include(":feature:caseeditor")
include(":feature:cases")
include(":feature:dashboard")
include(":feature:menu")
include(":feature:settings")
include(":feature:team")
include(":lint")
include(":sync:work")
include(":sync:sync-test")
include(":ui-test-hilt-manifest")
