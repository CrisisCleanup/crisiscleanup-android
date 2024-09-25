plugins {
    alias(libs.plugins.nowinandroid.android.library)
    alias(libs.plugins.nowinandroid.android.library.compose)
}

android {
    namespace = "com.crisiscleanup.core.commoncase"
}

dependencies {
    api(projects.core.common)
    implementation(projects.core.commonassets)
    implementation(projects.core.data)
    implementation(projects.core.designsystem)
    implementation(projects.core.mapmarker)
    implementation(projects.core.model)
    implementation(projects.core.ui)

    implementation(libs.androidx.constraintlayout)

    implementation(libs.google.maps.compose)
    implementation(libs.playservices.maps)

    implementation(libs.kotlinx.datetime)
}