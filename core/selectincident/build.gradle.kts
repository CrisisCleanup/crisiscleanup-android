plugins {
    id("nowinandroid.android.library")
    id("nowinandroid.android.library.compose")
    id("nowinandroid.android.library.jacoco")
    kotlin("kapt")
}

android {
    namespace = "com.crisiscleanup.core.selectincident"
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:domain"))
    implementation(project(":core:model"))

    api(libs.androidx.compose.foundation)
    api(libs.androidx.compose.foundation.layout)
    api(libs.androidx.compose.material3)
    debugApi(libs.androidx.compose.ui.tooling)
    api(libs.androidx.compose.ui.tooling.preview)
    api(libs.androidx.compose.ui.util)
    api(libs.androidx.compose.runtime)
}