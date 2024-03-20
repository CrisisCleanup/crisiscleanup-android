plugins {
    id("nowinandroid.android.feature")
    id("nowinandroid.android.library.compose")
    id("nowinandroid.android.library.jacoco")
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin")
}

android {
    defaultConfig {
        testInstrumentationRunner = "com.crisiscleanup.core.testing.CrisisCleanupTestRunner"
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }

    namespace = "com.crisiscleanup.feature.caseeditor"
}

secrets {
    defaultPropertiesFileName = "secrets.defaults.properties"
}

dependencies {
    implementation(projects.core.addresssearch)
    implementation(projects.core.commonassets)
    implementation(projects.core.commoncase)
    implementation(projects.core.designsystem)
    implementation(projects.core.mapmarker)
    implementation(projects.core.network)

    implementation(libs.apache.commons.text)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.coil.kt)
    implementation(libs.coil.kt.compose)
    implementation(libs.google.maps.compose)
    implementation(libs.kotlinx.datetime)
    implementation(libs.philjay.rrule)
    implementation(libs.playservices.maps)

    androidTestImplementation(kotlin("test"))

    testImplementation(projects.core.testing)
    testImplementation(libs.mockk.android)
}