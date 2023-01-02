plugins {
    id("nowinandroid.android.library")
    id("nowinandroid.android.hilt")
}

android {
    namespace = "com.crisiscleanup.core.datastore.test"
}

dependencies {
    api(project(":core:datastore"))
    implementation(project(":core:testing"))

    api(libs.androidx.dataStore.core)
}
