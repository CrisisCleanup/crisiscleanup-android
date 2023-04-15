plugins {
    id("nowinandroid.android.library")
    id("nowinandroid.android.hilt")
}

android {
    namespace = "com.crisiscleanup.core.data.test"
}

dependencies {
    implementation(project(":core:common"))
    api(project(":core:data"))
    implementation(project(":core:testing"))
}
