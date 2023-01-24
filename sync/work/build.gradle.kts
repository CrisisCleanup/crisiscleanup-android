plugins {
    id("nowinandroid.android.library")
    id("nowinandroid.android.library.jacoco")
    id("nowinandroid.android.hilt")
}

android {
    defaultConfig {
        testInstrumentationRunner = "com.crisiscleanup.core.testing.CrisisCleanupTestRunner"
    }
    namespace = "com.crisiscleanup.sync"
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:model"))
    implementation(project(":core:data"))
    implementation(project(":core:datastore"))

    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.tracing.ktx)
    implementation(libs.androidx.startup)
    implementation(libs.androidx.work.ktx)
    implementation(libs.hilt.ext.work)

    testImplementation(project(":core:testing"))
    androidTestImplementation(project(":core:testing"))

    kapt(libs.hilt.ext.compiler)

    androidTestImplementation(libs.androidx.work.testing)
}
