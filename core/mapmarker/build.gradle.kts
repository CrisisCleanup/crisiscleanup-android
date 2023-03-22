plugins {
    id("nowinandroid.android.library")
    id("nowinandroid.android.library.compose")
    id("nowinandroid.android.library.jacoco")
    kotlin("kapt")
}

android {
    namespace = "com.crisiscleanup.core.mapmarker"
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:model"))
    implementation(project(":core:renderscript-toolkit"))

    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.runtime)

    implementation(libs.android.maps.util)
    implementation(libs.google.maps.compose)
    implementation(libs.playservices.maps)
    implementation(libs.android.material.material)

    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
}