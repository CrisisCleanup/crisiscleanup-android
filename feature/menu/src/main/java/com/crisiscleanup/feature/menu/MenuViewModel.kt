package com.crisiscleanup.feature.menu

import androidx.lifecycle.ViewModel
import com.crisiscleanup.core.common.AppEnv
import com.crisiscleanup.core.common.AppVersionProvider
import com.crisiscleanup.core.common.event.AuthEventManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MenuViewModel @Inject constructor(
    private val appVersionProvider: AppVersionProvider,
    private val authEventManager: AuthEventManager,
    private val appEnv: AppEnv,
) : ViewModel() {
    val isDebug = appEnv.isDebuggable

    val versionText: String
        get() {
            val version = appVersionProvider.version
            return "${version.second} (${version.first})"
        }

    fun simulateTokenExpired() {
        if (appEnv.isDebuggable) {
            authEventManager.onExpiredToken()
        }
    }
}