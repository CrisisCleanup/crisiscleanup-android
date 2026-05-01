plugins {
    alias(libs.plugins.nowinandroid.android.feature.impl)
    alias(libs.plugins.nowinandroid.android.library.compose)
    alias(libs.plugins.nowinandroid.android.library.jacoco)
    alias(libs.plugins.secrets)
}

android {
    namespace = "com.crisiscleanup.feature.incidentcache"
}

secrets {
    defaultPropertiesFileName = "secrets.defaults.properties"
}

dependencies {
    implementation(projects.core.mapmarker)
    implementation(projects.core.selectincident)

    implementation(libs.kotlinx.datetime)

    implementation(libs.google.maps.compose)
    implementation(libs.playservices.maps)
}