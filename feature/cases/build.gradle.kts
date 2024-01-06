plugins {
    id("nowinandroid.android.feature")
    id("nowinandroid.android.library.compose")
    id("nowinandroid.android.library.jacoco")
}

android {
    namespace = "com.crisiscleanup.feature.cases"
}

dependencies {
    implementation(project(":core:commonassets"))
    implementation(project(":core:commoncase"))
    implementation(project(":core:mapmarker"))
    implementation(project(":core:selectincident"))

    implementation(libs.kotlinx.datetime)

    implementation(libs.androidx.constraintlayout)
    implementation(libs.google.maps.compose)
    implementation(libs.playservices.maps)
}