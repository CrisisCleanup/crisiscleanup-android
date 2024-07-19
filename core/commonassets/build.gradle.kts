plugins {
    alias(libs.plugins.nowinandroid.android.library)
    alias(libs.plugins.nowinandroid.android.library.compose)
}

android {
    namespace = "com.crisiscleanup.core.commonassets"
}

dependencies {
    implementation(projects.core.model)
    implementation(projects.core.designsystem)
}