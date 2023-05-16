plugins {
    id("nowinandroid.android.feature")
    id("nowinandroid.android.library.compose")
    id("nowinandroid.android.library.jacoco")
}

android {
    namespace = "com.crisiscleanup.feature.mediamanage"
}

dependencies {
    implementation(libs.coil.kt)
    implementation(libs.coil.kt.compose)
}