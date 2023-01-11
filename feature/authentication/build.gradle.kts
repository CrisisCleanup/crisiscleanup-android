plugins {
    id("nowinandroid.android.feature")
    id("nowinandroid.android.library.compose")
    id("nowinandroid.android.library.jacoco")
}

android {
    namespace = "com.crisiscleanup.feature.authentication"
}

dependencies {
    implementation(project(":core:datastore"))

    implementation(libs.kotlinx.datetime)

    implementation(libs.jwt.decode)
}