plugins {
    alias(libs.plugins.nowinandroid.android.library)
    alias(libs.plugins.nowinandroid.android.library.jacoco)
    alias(libs.plugins.nowinandroid.hilt)
}

android {
    namespace = "com.crisiscleanup.core.addresssearch"
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.core.model)

    implementation(libs.kotlinx.datetime)

    implementation(libs.android.maps.util)
    implementation(libs.google.places)
    implementation(libs.kotlinx.coroutines.playservices)
}
