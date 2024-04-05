plugins {
    alias(libs.plugins.nowinandroid.jvm.library)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(libs.kotlinx.datetime)

    testImplementation(libs.junit4)
}