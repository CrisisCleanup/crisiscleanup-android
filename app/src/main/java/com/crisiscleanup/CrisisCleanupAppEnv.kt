package com.crisiscleanup

import com.crisiscleanup.core.common.AppEnv
import com.crisiscleanup.core.common.AppSettingsProvider
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CrisisCleanupAppEnv @Inject constructor(
    private val settingsProvider: AppSettingsProvider,
) : AppEnv {
    override val isDebuggable = !(BuildConfig.IS_RELEASE_BUILD || BuildConfig.IS_PROD_BUILD)
    override val isProduction = BuildConfig.IS_RELEASE_BUILD && BuildConfig.IS_PROD_BUILD
    override val isNotProduction = !isProduction

    override val isEarlybird = BuildConfig.IS_EARLYBIRD_BUILD

    override val apiEnvironment: String
        get() {
            val apiUrl = settingsProvider.apiBaseUrl
            return when {
                apiUrl.startsWith("https://api.dev.crisiscleanup.io") -> "Dev"
                apiUrl.startsWith("https://api.staging.crisiscleanup.io") -> "Staging"
                apiUrl.startsWith("https://api.crisiscleanup.org") -> "Production"
                else -> "Local?"
            }
        }

    override fun runInNonProd(block: () -> Unit) {
        if (isNotProduction) {
            block()
        }
    }
}
