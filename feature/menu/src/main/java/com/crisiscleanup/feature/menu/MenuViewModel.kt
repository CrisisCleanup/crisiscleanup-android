package com.crisiscleanup.feature.menu

import androidx.lifecycle.ViewModel
import com.crisiscleanup.core.common.AppEnv
import com.crisiscleanup.core.common.AppVersionProvider
import com.crisiscleanup.core.common.DatabaseVersionProvider
import com.crisiscleanup.core.common.event.AuthEventManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MenuViewModel @Inject constructor(
    private val appVersionProvider: AppVersionProvider,
    private val authEventManager: AuthEventManager,
    appEnv: AppEnv,
    private val databaseVersionProvider: DatabaseVersionProvider,
) : ViewModel() {
    val isDebuggable = appEnv.isDebuggable
    private val isNotProduction = !appEnv.isProduction

    val versionText: String
        get() {
            val version = appVersionProvider.version
            return "${version.second} (${version.first})"
        }

    val databaseVersionText: String
        get() = if (isNotProduction) "DB ${databaseVersionProvider.databaseVersion}" else ""

    fun simulateTokenExpired() {
        if (isDebuggable) {
            authEventManager.onExpiredToken()
        }
    }
}