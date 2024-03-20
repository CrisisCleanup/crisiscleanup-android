plugins {
    id("nowinandroid.android.feature")
    id("nowinandroid.android.library.compose")
    id("nowinandroid.android.library.jacoco")
}

android {
    namespace = "com.crisiscleanup.feature.syncinsights"
}

dependencies {
    implementation(projects.core.data)

    implementation(libs.kotlinx.datetime)
}