plugins {
    id("nowinandroid.android.library")
    id("nowinandroid.android.library.jacoco")
    id("nowinandroid.android.hilt")
    id("nowinandroid.android.room")
}

android {
    defaultConfig {
        testInstrumentationRunner =
            "com.crisiscleanup.core.testing.CrisisCleanupTestRunner"
    }
    namespace = "com.crisiscleanup.core.database"

    sourceSets {
        // For migration test to be able to find schemas
        getByName("androidTest").assets.srcDirs(files("$projectDir/schemas")) // Room
    }

    // Due to test errors "files found with path 'META-INF/LICENSE.md'" and related
    packaging {
        resources {
            merges.add("META-INF/{LICENSE.md,LICENSE-notice.md}")
        }
    }
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:common"))

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.datetime)

    androidTestImplementation(project(":core:testing"))
    androidTestImplementation(libs.room.testing)
    androidTestImplementation(libs.mockk.android)
}