plugins {
    alias(libs.plugins.nowinandroid.android.feature)
    alias(libs.plugins.nowinandroid.android.library.compose)
    alias(libs.plugins.nowinandroid.android.library.jacoco)
}

android {
    namespace = "com.crisiscleanup.feature.authentication"
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.core.datastore)
    implementation(projects.core.designsystem)
    implementation(projects.core.network)

    testImplementation(projects.core.testing)

    // Depending modules/apps likely need to compare ktx Instants
    api(libs.kotlinx.datetime)

    implementation(libs.jwt.decode)
    implementation(libs.retrofit.core)
    implementation(libs.androidx.camera)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.mlkit.barcodescanning)

    testImplementation(libs.mockk.android)
}