plugins {
    id("nowinandroid.android.library")
    id("nowinandroid.android.library.compose")
}

android {
    namespace = "com.crisiscleanup.core.commonassets"
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:designsystem"))
}