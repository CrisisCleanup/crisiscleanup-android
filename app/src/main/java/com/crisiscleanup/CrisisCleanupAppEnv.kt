package com.crisiscleanup

import com.crisiscleanup.core.common.AppEnv
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CrisisCleanupAppEnv @Inject constructor() : AppEnv {
    override val isDebuggable = !(BuildConfig.IS_RELEASE_BUILD || BuildConfig.IS_PROD_BUILD)
    override val isProduction = BuildConfig.IS_RELEASE_BUILD && BuildConfig.IS_PROD_BUILD
    override val isNotProduction = !isProduction

    override val isEarlybird = BuildConfig.IS_EARLYBIRD_BUILD

    override val apiEnvironment: String
        get() {
            val apiUrl = BuildConfig.API_BASE_URL
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
