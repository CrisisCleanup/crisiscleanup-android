plugins {
    id("nowinandroid.android.library")
    id("nowinandroid.android.library.jacoco")
}

android {
    namespace = "com.crisiscleanup.core.appnav"
}

dependencies {
    implementation(project(":core:common"))
    implementation(libs.androidx.lifecycle.viewModelCompose)
    implementation(libs.androidx.navigation.compose)
}