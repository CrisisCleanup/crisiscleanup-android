buildscript {
    repositories {
        google()
        mavenCentral()
    }
    // For Firebase support
    dependencies {
        classpath("com.google.gms:google-services:4.3.14")
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.secrets) apply false
}