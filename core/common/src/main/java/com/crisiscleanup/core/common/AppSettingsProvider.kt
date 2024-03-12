package com.crisiscleanup.core.common

import javax.inject.Inject

interface AppSettingsProvider {
    val apiBaseUrl: String
    val baseUrl: String
    val mapsApiKey: String

    val termsOfServiceUrl: String
    val privacyPolicyUrl: String
    val gettingStartedVideoUrl: String

    val debugEmail: String
    val debugPassword: String
}

class SecretsAppSettingsProvider @Inject constructor() : AppSettingsProvider {
    override val apiBaseUrl: String
        get() = BuildConfig.API_BASE_URL
    override val baseUrl: String
        get() = BuildConfig.BASE_URL
    override val mapsApiKey: String
        get() = BuildConfig.MAPS_API_KEY

    override val termsOfServiceUrl = "$baseUrl/terms?view=plain"
    override val privacyPolicyUrl = "$baseUrl/privacy?view=plain"
    override val gettingStartedVideoUrl = BuildConfig.GETTING_STARTED_VIDEO_URL

    override val debugEmail: String
        get() = BuildConfig.DEBUG_EMAIL_ADDRESS
    override val debugPassword: String
        get() = BuildConfig.DEBUG_ACCOUNT_PASSWORD
}
