plugins {
    alias(libs.plugins.nowinandroid.android.feature)
    alias(libs.plugins.nowinandroid.android.library.compose)
    alias(libs.plugins.nowinandroid.android.library.jacoco)
}

android {
    namespace = "com.crisiscleanup.feature.incidentcache"
}

dependencies {
    implementation(projects.core.selectincident)

    implementation(libs.kotlinx.datetime)

    implementation(libs.google.maps.compose)
    implementation(libs.playservices.maps)
}