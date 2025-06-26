plugins {
    alias(libs.plugins.nowinandroid.android.library)
    alias(libs.plugins.nowinandroid.android.library.compose)
    alias(libs.plugins.nowinandroid.android.library.jacoco)
}

android {
    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    namespace = "com.crisiscleanup.core.appcomponent"
}

dependencies {
    lintPublish(projects.lint)

    implementation(projects.core.common)
    implementation(projects.core.commonassets)
    implementation(projects.core.commoncase)
    implementation(projects.core.data)
    implementation(projects.core.designsystem)
    implementation(projects.core.selectincident)

    implementation(libs.androidx.core.ktx)
    api(libs.androidx.compose.foundation)
    api(libs.androidx.compose.foundation.layout)
    api(libs.androidx.compose.material3)
    api(libs.androidx.compose.runtime)
    api(libs.androidx.compose.ui.util)
    implementation(libs.androidx.lifecycle.runtimeCompose)

    implementation(libs.coil.kt.compose)

    androidTestImplementation(projects.core.testing)
}