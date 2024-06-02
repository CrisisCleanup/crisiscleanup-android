plugins {
    alias(libs.plugins.nowinandroid.android.library)
    alias(libs.plugins.nowinandroid.android.library.jacoco)
    alias(libs.plugins.nowinandroid.android.hilt)
    alias(libs.plugins.nowinandroid.android.room)
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
    implementation(projects.core.model)
    implementation(projects.core.common)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.datetime)
    implementation(libs.androidx.paging.common)
    implementation(libs.room.paging)

    androidTestImplementation(projects.core.testing)
    androidTestImplementation(libs.room.testing)
    androidTestImplementation(libs.mockk.android)
}