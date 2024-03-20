plugins {
    alias(libs.plugins.nowinandroid.android.library)
    alias(libs.plugins.nowinandroid.android.library.compose)
    alias(libs.plugins.nowinandroid.android.library.jacoco)
}

android {
    namespace = "com.crisiscleanup.core.mapmarker"
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.core.data)
    implementation(projects.core.designsystem)
    implementation(projects.core.model)
    implementation(projects.core.renderscriptToolkit)

    implementation(libs.kotlinx.datetime)

    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.runtime)

    implementation(libs.android.maps.util)
    implementation(libs.google.maps.compose)
    implementation(libs.playservices.maps)
    implementation(libs.android.material.material)

    implementation(libs.hilt.android)
}