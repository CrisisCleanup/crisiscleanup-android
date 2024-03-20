plugins {
    alias(libs.plugins.nowinandroid.android.library)
    alias(libs.plugins.nowinandroid.android.hilt)
}

android {
    namespace = "com.crisiscleanup.core.data.test"
}

dependencies {
    implementation(projects.core.common)
    api(projects.core.data)
    implementation(projects.core.testing)
    implementation(libs.hilt.android.testing)
}
