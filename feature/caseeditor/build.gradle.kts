plugins {
    id("nowinandroid.android.feature")
    id("nowinandroid.android.library.compose")
    id("nowinandroid.android.library.jacoco")
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

dependencies {
    implementation(project(":core:addresssearch"))
    implementation(project(":core:commonassets"))
    implementation(project(":core:commoncase"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:mapmarker"))
    implementation(project(":core:network"))

    implementation(libs.apache.commons.text)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.coil.kt)
    implementation(libs.coil.kt.compose)
    implementation(libs.google.maps.compose)
    implementation(libs.kotlinx.datetime)
    implementation(libs.philjay.rrule)
    implementation(libs.playservices.maps)

    androidTestImplementation(kotlin("test"))

    testImplementation(project(":core:testing"))
    testImplementation(libs.mockk.android)
}