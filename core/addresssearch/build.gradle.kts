plugins {
    id("nowinandroid.android.library")
    id("nowinandroid.android.library.jacoco")
    id("nowinandroid.android.hilt")
}

android {
    namespace = "com.crisiscleanup.core.addresssearch"
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:model"))

    implementation(libs.kotlinx.datetime)

    implementation(libs.android.maps.util)
    implementation(libs.google.places)
    implementation(libs.kotlinx.coroutines.playservices)
}
