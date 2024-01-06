plugins {
    id("nowinandroid.android.feature")
    id("nowinandroid.android.library.compose")
    id("nowinandroid.android.library.jacoco")
}

android {
    namespace = "com.crisiscleanup.feature.menu"
}

dependencies {
    implementation(project(":core:commonassets"))
    implementation(project(":core:commoncase"))
    implementation(project(":core:selectincident"))
}