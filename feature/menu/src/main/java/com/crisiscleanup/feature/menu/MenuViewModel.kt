package com.crisiscleanup.feature.menu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crisiscleanup.core.common.AppEnv
import com.crisiscleanup.core.common.AppVersionProvider
import com.crisiscleanup.core.common.DatabaseVersionProvider
import com.crisiscleanup.core.common.event.AuthEventManager
import com.crisiscleanup.core.common.network.CrisisCleanupDispatchers.IO
import com.crisiscleanup.core.common.network.Dispatcher
import com.crisiscleanup.core.data.repository.SyncLogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MenuViewModel @Inject constructor(
    syncLogRepository: SyncLogRepository,
    private val appVersionProvider: AppVersionProvider,
    private val authEventManager: AuthEventManager,
    appEnv: AppEnv,
    private val databaseVersionProvider: DatabaseVersionProvider,
    @Dispatcher(IO) ioDispatcher: CoroutineDispatcher,
) : ViewModel() {
    val isDebuggable = appEnv.isDebuggable
    val isNotProduction = !appEnv.isProduction

    val versionText: String
        get() {
            val version = appVersionProvider.version
            return "${version.second} (${version.first})"
        }

    val databaseVersionText: String
        get() = if (isNotProduction) "DB ${databaseVersionProvider.databaseVersion}" else ""

    init {
        viewModelScope.launch(ioDispatcher) {
            syncLogRepository.trimOldLogs()
        }
    }

    fun simulateTokenExpired() {
        if (isDebuggable) {
            authEventManager.onExpiredToken()
        }
    }
}