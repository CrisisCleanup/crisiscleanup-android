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
    private val appEnv: AppEnv,
    private val databaseVersionProvider: DatabaseVersionProvider,
) : ViewModel() {
    val isDebug = appEnv.isDebuggable

    val versionText: String
        get() {
            val version = appVersionProvider.version
            return "${version.second} (${version.first})"
        }

    val databaseVersionText: String
        get() = if (appEnv.isDebuggable) "DB ${databaseVersionProvider.databaseVersion}" else ""

    fun simulateTokenExpired() {
        if (appEnv.isDebuggable) {
            authEventManager.onExpiredToken()
        }
    }
}