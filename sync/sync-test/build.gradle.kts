plugins {
    id("nowinandroid.android.library")
    id("nowinandroid.android.hilt")
}

android {
    namespace = "com.crisiscleanup.core.sync.test"
}

dependencies {
    api(project(":sync:work"))
    implementation(projects.core.data)
    implementation(projects.core.testing)
}
