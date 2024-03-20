plugins {
    id("nowinandroid.android.feature")
    id("nowinandroid.android.library.compose")
    id("nowinandroid.android.library.jacoco")
}

android {
    namespace = "com.crisiscleanup.feature.menu"
}

dependencies {
    implementation(projects.core.commonassets)
    implementation(projects.core.commoncase)
    implementation(projects.core.selectincident)
}