plugins {
    id("nowinandroid.android.feature")
    id("nowinandroid.android.library.compose")
    id("nowinandroid.android.library.jacoco")
}

android {
    namespace = "com.crisiscleanup.feature.authentication"
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:datastore"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:network"))

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