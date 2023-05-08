plugins {
    id("nowinandroid.android.library")
    id("nowinandroid.android.library.jacoco")
    id("nowinandroid.android.hilt")
}

android {
    namespace = "com.crisiscleanup.core.common"
}

dependencies {
    implementation(libs.kotlinx.datetime)
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
    testImplementation(project(":core:testing"))
}