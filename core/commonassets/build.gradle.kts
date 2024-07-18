plugins {
    alias(libs.plugins.nowinandroid.android.library)
}

android {
    namespace = "com.crisiscleanup.core.commonassets"
}

dependencies {
    implementation(projects.core.model)
    implementation(projects.core.designsystem)
}