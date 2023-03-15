plugins {
    id("nowinandroid.android.feature")
    id("nowinandroid.android.library.compose")
    id("nowinandroid.android.library.jacoco")
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin")
}

android {
    buildFeatures {
        buildConfig = true
    }
    namespace = "com.crisiscleanup.feature.caseeditor"
}

secrets {
    defaultPropertiesFileName = "secrets.defaults.properties"
}

dependencies {
    implementation(project(":core:network"))
    implementation(project(":core:mapmarker"))

    implementation(libs.kotlinx.datetime)

    implementation(libs.androidx.constraintlayout)
    implementation(libs.google.maps.compose)
    implementation(libs.playservices.maps)
}