plugins {
    alias(libs.plugins.nowinandroid.android.feature)
    alias(libs.plugins.nowinandroid.android.library.compose)
    alias(libs.plugins.nowinandroid.android.library.jacoco)
}

android {
    namespace = "com.crisiscleanup.feature.menu"
}

dependencies {
    implementation(projects.core.commonassets)
    implementation(projects.core.commoncase)
    implementation(projects.core.selectincident)
}