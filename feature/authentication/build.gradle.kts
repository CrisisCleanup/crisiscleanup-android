plugins {
    id("nowinandroid.android.feature")
    id("nowinandroid.android.library.compose")
    id("nowinandroid.android.library.jacoco")
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin")
}

android {
    buildFeatures {
        buildConfig = true
    }
    namespace = "com.crisiscleanup.feature.authentication"
}

secrets {
    defaultPropertiesFileName = "secrets.defaults.properties"
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:datastore"))
    implementation(project(":core:network"))

    // Depending modules/apps likely need to compare ktx Instants
    api(libs.kotlinx.datetime)

    implementation(libs.jwt.decode)
    implementation(libs.retrofit.core)

    testImplementation(libs.mockk.android)
}