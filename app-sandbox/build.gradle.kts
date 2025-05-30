import com.google.samples.apps.nowinandroid.NiaBuildType

plugins {
    alias(libs.plugins.nowinandroid.android.application)
    alias(libs.plugins.nowinandroid.android.application.compose)
    alias(libs.plugins.nowinandroid.android.application.flavors)
    alias(libs.plugins.nowinandroid.hilt)
}

android {
    defaultConfig {
        applicationId = "com.crisiscleanup.sandbox"
        versionCode = 1
        versionName = "0.0.1"

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        val debug by getting {
            applicationIdSuffix = NiaBuildType.DEBUG.applicationIdSuffix
        }
        val release by getting {
            applicationIdSuffix = NiaBuildType.RELEASE.applicationIdSuffix
        }
    }

    packaging {
        resources {
            excludes.add("/META-INF/{AL2.0,LGPL2.1}")
        }
    }
    namespace = "com.crisiscleanup.sandbox"
}

dependencies {
    implementation(projects.feature.caseeditor)

    implementation(projects.core.appnav)
    implementation(projects.core.commoncase)
    implementation(projects.core.data)
    implementation(projects.core.designsystem)
    implementation(projects.core.mapmarker)
    implementation(projects.core.network)
    implementation(projects.core.ui)

    implementation(libs.kotlinx.serialization.json)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.lifecycle.runtimeCompose)
    implementation(libs.androidx.compose.runtime.tracing)
    implementation(libs.androidx.compose.material3.windowSizeClass)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.window.manager)
    implementation(libs.androidx.profileinstaller)

    implementation(libs.coil.kt)
    implementation(libs.coil.kt.compose)

    implementation(libs.kotlinx.coroutines.playservices)
    implementation(libs.playservices.maps)
}

dependencyGuard {
    configuration("prodReleaseRuntimeClasspath")
}
