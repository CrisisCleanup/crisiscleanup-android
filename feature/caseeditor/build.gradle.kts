plugins {
    id("nowinandroid.android.feature")
    id("nowinandroid.android.library.compose")
    id("nowinandroid.android.library.jacoco")
}

android {
    namespace = "com.crisiscleanup.feature.caseeditor"
}

dependencies {
    implementation(project(":core:appheader"))
    implementation(project(":core:network"))

    implementation(libs.kotlinx.datetime)
}