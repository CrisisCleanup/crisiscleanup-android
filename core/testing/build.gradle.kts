plugins {
    alias(libs.plugins.nowinandroid.android.library)
    alias(libs.plugins.nowinandroid.hilt)
}

android {
    namespace = "com.crisiscleanup.core.testing"
}

dependencies {
    api(libs.kotlinx.coroutines.test)
    api(projects.core.common)
    api(projects.core.data)
    api(projects.core.model)

    implementation(libs.androidx.test.rules)
    implementation(libs.hilt.android.testing)
    implementation(libs.kotlinx.datetime)
}
