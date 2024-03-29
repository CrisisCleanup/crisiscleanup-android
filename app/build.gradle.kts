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
        val buildVersion = 192
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
    implementation(project(":feature:authentication"))
    implementation(project(":feature:caseeditor"))
    implementation(project(":feature:cases"))
    implementation(project(":feature:dashboard"))
    implementation(project(":feature:menu"))
    implementation(project(":feature:mediamanage"))
    implementation(project(":feature:organizationmanage"))
    implementation(project(":feature:syncinsights"))
    implementation(project(":feature:team"))
    implementation(project(":feature:userfeedback"))

    implementation(project(":core:appnav"))
    implementation(project(":core:common"))
    implementation(project(":core:data"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:model"))
    implementation(project(":core:network"))
    implementation(project(":core:ui"))

    implementation(project(":sync:work"))

    androidTestImplementation(project(":core:testing"))
    androidTestImplementation(project(":core:datastore-test"))
    androidTestImplementation(project(":core:data-test"))
    androidTestImplementation(project(":core:network"))
    androidTestImplementation(libs.androidx.navigation.testing)
    androidTestImplementation(libs.accompanist.testharness)
    androidTestImplementation(kotlin("test"))
    debugImplementation(libs.androidx.compose.ui.testManifest)
    debugImplementation(project(":ui-test-hilt-manifest"))

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
    implementation(libs.playservices.location)
    implementation(libs.zxing)

    implementation(libs.coil.kt)

    // For Firebase support
    implementation(platform(libs.firebase.bom))

    implementation(libs.kotlinx.coroutines.playservices)
    implementation(libs.playservices.maps)
}

// androidx.test is forcing JUnit, 4.12. This forces it to use 4.13
configurations.configureEach {
    resolutionStrategy {
        force(libs.junit4)
        // Temporary workaround for https://issuetracker.google.com/174733673
        force("org.objenesis:objenesis:2.6")
    }
}
