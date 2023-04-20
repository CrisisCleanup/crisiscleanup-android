plugins {
    id("nowinandroid.android.library")
}

android {
    namespace = "com.crisiscleanup.core.commonassets"
}

dependencies {
    implementation(project(":core:model"))
}