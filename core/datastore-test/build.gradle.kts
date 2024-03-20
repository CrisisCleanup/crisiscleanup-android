plugins {
    alias(libs.plugins.nowinandroid.android.library)
    alias(libs.plugins.nowinandroid.android.hilt)
}

android {
    namespace = "com.crisiscleanup.core.datastore.test"
}

dependencies {
    api(projects.core.datastore)
    implementation(projects.core.testing)

    api(libs.androidx.dataStore.core)
}
