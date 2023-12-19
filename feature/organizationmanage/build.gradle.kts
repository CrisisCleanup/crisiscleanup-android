plugins {
    id("nowinandroid.android.feature")
    id("nowinandroid.android.library.compose")
    id("nowinandroid.android.library.jacoco")
}

android {
    namespace = "com.crisiscleanup.feature.organizationmanage"
}

dependencies {
    implementation(project(":core:data"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:domain"))
}