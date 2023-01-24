plugins {
    id("nowinandroid.android.library")
    id("nowinandroid.android.hilt")
}

android {
    namespace = "com.crisiscleanup.core.sync.test"
}

dependencies {
    api(project(":sync:work"))
    implementation(project(":core:data"))
    implementation(project(":core:testing"))
}
