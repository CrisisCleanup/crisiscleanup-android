package com.crisiscleanup

import com.crisiscleanup.core.common.AppEnv
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CrisisCleanupAppEnv @Inject constructor() : AppEnv {
    override val isDebuggable: Boolean = CrisisCleanupApplication.isDebuggable
}