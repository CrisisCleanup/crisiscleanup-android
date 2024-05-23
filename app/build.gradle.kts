import com.google.samples.apps.nowinandroid.NiaBuildType

plugins {
    id("nowinandroid.android.application")
    id("nowinandroid.android.application.compose")
    id("nowinandroid.android.application.flavors")
    id("nowinandroid.android.application.jacoco")
    id("nowinandroid.android.hilt")
    id("jacoco")
    id("nowinandroid.android.application.firebase")
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin")
}

android {
    defaultConfig {
        val buildVersion = 202
        applicationId = "com.crisiscleanup"
        versionCode = buildVersion
        versionName = "0.9.${buildVersion - 168}"

        // Custom test runner to set up Hilt dependency graph
        testInstrumentationRunner = "com.crisiscleanup.core.testing.CrisisCleanupTestRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        val debug by getting {
            applicationIdSuffix = NiaBuildType.DEBUG.applicationIdSuffix

            buildConfigField("Boolean", "IS_RELEASE_BUILD", "false")
        }
        val release by getting {
            isMinifyEnabled = true
            applicationIdSuffix = NiaBuildType.RELEASE.applicationIdSuffix
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
                "proguard-playservices.pro",
                "proguard-crashlytics.pro",
            )

            buildConfigField("Boolean", "IS_RELEASE_BUILD", "true")

            // TODO Review proper syntax
            ndk {
                debugSymbolLevel = "SYMBOL_TABLE"
            }

            // To publish on the Play store a private signing key is required, but to allow anyone
            // who clones the code to sign and run the release variant, use the debug signing key.
            // Uncomment to install locally. Change to build for Play store.
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    productFlavors {
        val demo by getting {
            buildConfigField("Boolean", "IS_PROD_BUILD", "false")
            buildConfigField("Boolean", "IS_EARLYBIRD_BUILD", "false")
        }

        val prod by getting {
            buildConfigField("Boolean", "IS_PROD_BUILD", "true")
            buildConfigField("Boolean", "IS_EARLYBIRD_BUILD", "false")
        }

        val earlybird by getting {
            buildConfigField("Boolean", "IS_PROD_BUILD", "true")
            buildConfigField("Boolean", "IS_EARLYBIRD_BUILD", "true")
        }

        val aussie by getting {
            buildConfigField("Boolean", "IS_PROD_BUILD", "true")
            buildConfigField("Boolean", "IS_EARLYBIRD_BUILD", "false")
        }
    }

    packaging {
        resources {
            excludes.add("/META-INF/{AL2.0,LGPL2.1}")
        }
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
    namespace = "com.crisiscleanup"
}

secrets {
    defaultPropertiesFileName = "secrets.defaults.properties"
}

androidComponents {
    beforeVariants { variantBuilder ->
        // Unnecessary variants
        if (variantBuilder.name == "prodDebug" ||
            variantBuilder.name == "earlybirdDebug" ||
            variantBuilder.name == "aussieDebug"
        ) {
            variantBuilder.enable = false
        }
    }
}

dependencies {
    implementation(projects.feature.authentication)
    implementation(projects.feature.caseeditor)
    implementation(projects.feature.cases)
    implementation(projects.feature.dashboard)
    implementation(projects.feature.menu)
    implementation(projects.feature.mediamanage)
    implementation(projects.feature.organizationmanage)
    implementation(projects.feature.syncinsights)
    implementation(projects.feature.team)
    implementation(projects.feature.userfeedback)

    implementation(projects.core.appnav)
    implementation(projects.core.common)
    implementation(projects.core.data)
    implementation(projects.core.designsystem)
    implementation(projects.core.model)
    implementation(projects.core.network)
    implementation(projects.core.ui)

    implementation(projects.sync.work)

    androidTestImplementation(projects.core.testing)
    androidTestImplementation(projects.core.dataTest)
    androidTestImplementation(projects.core.datastoreTest)
    androidTestImplementation(projects.core.network)
    androidTestImplementation(libs.androidx.navigation.testing)
    androidTestImplementation(libs.accompanist.testharness)
    androidTestImplementation(kotlin("test"))
    debugImplementation(libs.androidx.compose.ui.testManifest)
    debugImplementation(projects.uiTestHiltManifest)

    implementation(libs.kotlinx.serialization.json)

    implementation(libs.accompanist.systemuicontroller)
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
    implementation(libs.playservices.auth)
    implementation(libs.playservices.auth.phone)
    implementation(libs.playservices.location)
    implementation(libs.zxing)

    implementation(libs.coil.kt)

    // For Firebase support
    implementation(platform(libs.firebase.bom))

    implementation(libs.kotlinx.coroutines.playservices)
    implementation(libs.playservices.maps)
}

dependencyGuard {
    configuration("prodReleaseRuntimeClasspath")
}
