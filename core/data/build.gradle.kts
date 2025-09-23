plugins {
    alias(libs.plugins.nowinandroid.android.library)
    alias(libs.plugins.nowinandroid.android.library.compose)
    alias(libs.plugins.nowinandroid.android.library.jacoco)
    alias(libs.plugins.nowinandroid.hilt)
    id("kotlinx-serialization")
}

android {
    namespace = "com.crisiscleanup.core.data"
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }
}

dependencies {
    implementation(projects.core.common)
    api(projects.core.model)
    implementation(projects.core.database)
    implementation(projects.core.datastore)
    implementation(projects.core.network)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.paging.common)

    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(projects.core.testing)
    testImplementation(projects.core.datastoreTest)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
}