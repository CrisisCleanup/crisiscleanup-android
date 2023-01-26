// Decouples top app bar state from individual modules allowing app to implement behavior

plugins {
    id("nowinandroid.android.library")
    id("nowinandroid.android.library.compose")
    id("nowinandroid.android.library.jacoco")
}

android {
    namespace = "com.crisiscleanup.core.appheader"
}

dependencies {
    implementation(libs.androidx.compose.runtime)
}