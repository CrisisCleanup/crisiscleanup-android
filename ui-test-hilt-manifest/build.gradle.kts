plugins {
    id("nowinandroid.android.library")
    kotlin("kapt")
    id("dagger.hilt.android.plugin")
}

dependencies {
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
}
android {
    namespace = "com.crisiscleanup.uitesthiltmanifest"
}
