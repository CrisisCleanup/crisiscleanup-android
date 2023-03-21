plugins {
    id("nowinandroid.android.library")
    id("nowinandroid.android.library.jacoco")
    id("nowinandroid.android.hilt")
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin")
}

android {
    buildFeatures {
        buildConfig = true
    }
    namespace = "com.crisiscleanup.core.addresssearch"
}

secrets {
    defaultPropertiesFileName = "secrets.defaults.properties"
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:model"))

    implementation(libs.kotlinx.datetime)

    implementation(libs.android.maps.util)
    implementation(libs.google.places)
    implementation(libs.kotlinx.coroutines.playservices)
}
