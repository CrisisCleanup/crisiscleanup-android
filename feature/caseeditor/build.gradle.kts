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
    implementation(project(":core:addresssearch"))
    implementation(project(":core:commoncase"))
    implementation(project(":core:mapmarker"))
    implementation(project(":core:network"))

    implementation(libs.kotlinx.datetime)

    implementation(libs.androidx.constraintlayout)
    implementation(libs.google.maps.compose)
    implementation(libs.playservices.maps)
    implementation(libs.apache.commons.text)

    testImplementation(project(":core:testing"))
    testImplementation(libs.mockk.android)
}