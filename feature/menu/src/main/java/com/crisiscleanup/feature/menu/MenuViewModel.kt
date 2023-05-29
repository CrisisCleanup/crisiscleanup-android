package com.crisiscleanup.feature.menu

import androidx.lifecycle.ViewModel
import com.crisiscleanup.core.common.AppEnv
import com.crisiscleanup.core.common.AppVersionProvider
import com.crisiscleanup.core.common.DatabaseVersionProvider
import com.crisiscleanup.core.common.di.ApplicationScope
import com.crisiscleanup.core.common.event.AuthEventBus
import com.crisiscleanup.core.common.network.CrisisCleanupDispatchers.IO
import com.crisiscleanup.core.common.network.Dispatcher
import com.crisiscleanup.core.common.sync.SyncPuller
import com.crisiscleanup.core.data.repository.SyncLogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MenuViewModel @Inject constructor(
    syncLogRepository: SyncLogRepository,
    private val appVersionProvider: AppVersionProvider,
    private val authEventBus: AuthEventBus,
    appEnv: AppEnv,
    private val syncPuller: SyncPuller,
    private val databaseVersionProvider: DatabaseVersionProvider,
    @ApplicationScope externalScope: CoroutineScope,
    @Dispatcher(IO) ioDispatcher: CoroutineDispatcher,
) : ViewModel() {
    val isDebuggable = appEnv.isDebuggable
    val isNotProduction = appEnv.isNotProduction

    val versionText: String
        get() {
            val version = appVersionProvider.version
            return "${version.second} (${version.first})"
        }

    val databaseVersionText: String
        get() = if (isNotProduction) "DB ${databaseVersionProvider.databaseVersion}" else ""

    init {
        externalScope.launch(ioDispatcher) {
            syncLogRepository.trimOldLogs()
        }
    }

    fun simulateTokenExpired() {
        if (isDebuggable) {
            authEventBus.onExpiredToken()
        }
    }

    fun syncWorksitesFull() {
        if (isDebuggable) {
            syncPuller.scheduleSyncWorksitesFull()
        }
    }
}