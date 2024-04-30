plugins {
    alias(libs.plugins.nowinandroid.android.feature)
    alias(libs.plugins.nowinandroid.android.library.compose)
    alias(libs.plugins.nowinandroid.android.library.jacoco)
}

android {
    namespace = "com.crisiscleanup.feature.mediamanage"
}

dependencies {
    implementation(projects.core.data)
    implementation(projects.core.designsystem)

    implementation(libs.coil.kt)
    implementation(libs.coil.kt.compose)
}