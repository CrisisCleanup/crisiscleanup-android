plugins {
    id("nowinandroid.android.library")
    id("nowinandroid.android.library.jacoco")
    id("nowinandroid.android.library.compose")
    id("nowinandroid.android.hilt")
    id("kotlinx-serialization")
}

android {
    namespace = "com.crisiscleanup.core.data"
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:model"))
    implementation(project(":core:database"))
    implementation(project(":core:datastore"))
    implementation(project(":core:network"))

    testImplementation(project(":core:testing"))
    testImplementation(project(":core:datastore-test"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.compose.runtime)

    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
}