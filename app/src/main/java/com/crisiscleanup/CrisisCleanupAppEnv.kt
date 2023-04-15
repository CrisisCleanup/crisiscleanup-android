package com.crisiscleanup

import com.crisiscleanup.core.common.AppEnv
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CrisisCleanupAppEnv @Inject constructor() : AppEnv {
    override val isDebuggable = !(BuildConfig.IS_RELEASE_BUILD || BuildConfig.IS_PROD_BUILD)
    override val isProduction = BuildConfig.IS_RELEASE_BUILD && BuildConfig.IS_PROD_BUILD
    override fun runInNonProd(block: () -> Unit) {
        if (!isProduction) {
            block()
        }
    }
}