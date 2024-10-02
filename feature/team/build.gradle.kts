plugins {
    alias(libs.plugins.nowinandroid.android.feature)
    alias(libs.plugins.nowinandroid.android.library.compose)
    alias(libs.plugins.nowinandroid.android.library.jacoco)
}

android {
    namespace = "com.crisiscleanup.feature.team"
}

dependencies {
    implementation(projects.core.appComponent)
    implementation(projects.core.common)
    implementation(projects.core.commonassets)
    implementation(projects.core.commoncase)
    implementation(projects.core.mapmarker)
    implementation(projects.core.selectincident)

    implementation(libs.coil.kt.svg)
    implementation(libs.google.maps.compose)
    implementation(libs.kotlinx.datetime)
    implementation(libs.playservices.maps)
}