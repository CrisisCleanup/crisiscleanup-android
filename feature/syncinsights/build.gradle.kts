plugins {
    alias(libs.plugins.nowinandroid.android.feature)
    alias(libs.plugins.nowinandroid.android.library.compose)
    alias(libs.plugins.nowinandroid.android.library.jacoco)
}

android {
    namespace = "com.crisiscleanup.feature.syncinsights"
}

dependencies {
    implementation(projects.core.data)

    implementation(libs.kotlinx.datetime)
}