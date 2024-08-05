plugins {
    alias(libs.plugins.nowinandroid.android.library)
    alias(libs.plugins.nowinandroid.hilt)
}

android {
    namespace = "com.crisiscleanup.core.sync.test"
}

dependencies {
    api(projects.sync.work)
    implementation(projects.core.data)
    implementation(projects.core.testing)
    implementation(libs.hilt.android.testing)
}
