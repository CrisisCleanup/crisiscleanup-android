plugins {
    id("nowinandroid.android.library")
    id("nowinandroid.android.library.jacoco")
}

android {
    namespace = "com.crisiscleanup.core.appnav"
}

dependencies {
    implementation(libs.androidx.lifecycle.viewModelCompose)
    implementation(libs.androidx.navigation.compose)
}