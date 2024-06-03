plugins {
    alias(libs.plugins.nowinandroid.android.feature)
    alias(libs.plugins.nowinandroid.android.library.compose)
    alias(libs.plugins.nowinandroid.android.library.jacoco)
}

android {
    namespace = "com.crisiscleanup.feature.crisiscleanuplists"
}

dependencies {
    implementation(projects.core.data)
    implementation(projects.core.commonassets)
    implementation(projects.core.commoncase)

    implementation(libs.androidx.paging.compose)
    implementation(libs.androidx.paging.runtime.ktx)

    implementation(libs.kotlinx.datetime)
}