plugins {
    id("nowinandroid.android.library")
    id("nowinandroid.android.library.compose")
}

android {
    namespace = "com.crisiscleanup.core.commoncase"
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:commonassets"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:model"))
    implementation(project(":core:ui"))

    implementation(libs.kotlinx.datetime)
}