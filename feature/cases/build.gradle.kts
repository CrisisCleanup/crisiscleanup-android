plugins {
    alias(libs.plugins.nowinandroid.android.feature.impl)
    alias(libs.plugins.nowinandroid.android.library.compose)
    alias(libs.plugins.nowinandroid.android.library.jacoco)
    alias(libs.plugins.secrets)
}

android {
    namespace = "com.crisiscleanup.feature.cases"
}

secrets {
    defaultPropertiesFileName = "secrets.defaults.properties"
}

dependencies {
    implementation(projects.core.commonassets)
    implementation(projects.core.commoncase)
    implementation(projects.core.mapmarker)
    implementation(projects.core.selectincident)

    implementation(libs.kotlinx.datetime)

    implementation(libs.androidx.constraintlayout)
    implementation(libs.google.maps.compose)
    implementation(libs.playservices.maps)
}