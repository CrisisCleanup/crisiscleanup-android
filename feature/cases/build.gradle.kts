plugins {
    alias(libs.plugins.nowinandroid.android.feature)
    alias(libs.plugins.nowinandroid.android.library.compose)
    alias(libs.plugins.nowinandroid.android.library.jacoco)
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin")
}

android {
    namespace = "com.crisiscleanup.feature.cases"
}

secrets {
    defaultPropertiesFileName = "secrets.defaults.properties"
}

dependencies {
    implementation(projects.core.commonassets)
    api(projects.core.commoncase)
    implementation(projects.core.mapmarker)
    implementation(projects.core.selectincident)

    implementation(libs.kotlinx.datetime)

    implementation(libs.google.maps.compose)
    implementation(libs.playservices.maps)
}