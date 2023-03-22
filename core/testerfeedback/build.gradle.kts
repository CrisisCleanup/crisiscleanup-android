plugins {
    id("nowinandroid.android.library")
    id("nowinandroid.android.library.jacoco")
    id("nowinandroid.android.hilt")
}

android {
    namespace = "com.crisiscleanup.core.testerfeedback"
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:testerfeedbackapi"))
    api(libs.square.seismic)
}