plugins {
    alias(libs.plugins.nowinandroid.android.library)
    alias(libs.plugins.nowinandroid.android.library.jacoco)
    alias(libs.plugins.nowinandroid.android.hilt)
}

android {
    defaultConfig {
        testInstrumentationRunner = "com.crisiscleanup.core.testing.CrisisCleanupTestRunner"
    }
    namespace = "com.crisiscleanup.sync"
}

dependencies {
    ksp(libs.hilt.ext.compiler)

    implementation(projects.core.common)
    implementation(projects.core.model)
    implementation(projects.core.data)
    implementation(projects.core.datastore)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.datetime)

    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.tracing.ktx)
    implementation(libs.androidx.startup)
    implementation(libs.androidx.work.ktx)
    implementation(libs.hilt.ext.work)
    implementation(libs.hilt.android.testing)

    testImplementation(projects.core.testing)
    androidTestImplementation(projects.core.testing)

    androidTestImplementation(libs.androidx.work.testing)
    androidTestImplementation(libs.hilt.android.testing)

}
