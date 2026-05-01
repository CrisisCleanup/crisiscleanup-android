plugins {
    alias(libs.plugins.nowinandroid.android.feature.impl)
    alias(libs.plugins.nowinandroid.android.library.compose)
    alias(libs.plugins.nowinandroid.android.library.jacoco)
}

android {
    namespace = "com.crisiscleanup.feature.dashboard"
}

dependencies {
}