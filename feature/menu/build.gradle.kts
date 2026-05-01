plugins {
    alias(libs.plugins.nowinandroid.android.feature.impl)
    alias(libs.plugins.nowinandroid.android.library.compose)
    alias(libs.plugins.nowinandroid.android.library.jacoco)
}

android {
    namespace = "com.crisiscleanup.feature.menu"
}

dependencies {
    implementation(projects.core.appComponent)
    implementation(projects.core.selectincident)
    implementation(projects.sync.work)

    implementation(libs.accompanist.permissions)
}