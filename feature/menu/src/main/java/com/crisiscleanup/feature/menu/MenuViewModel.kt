package com.crisiscleanup.feature.menu

import androidx.lifecycle.ViewModel
import com.crisiscleanup.core.common.AppEnv
import com.crisiscleanup.core.common.AppVersionProvider
import com.crisiscleanup.core.common.DatabaseVersionProvider
import com.crisiscleanup.core.common.di.ApplicationScope
import com.crisiscleanup.core.common.network.CrisisCleanupDispatchers.IO
import com.crisiscleanup.core.common.network.Dispatcher
import com.crisiscleanup.core.common.sync.SyncPuller
import com.crisiscleanup.core.data.repository.AccountDataRefresher
import com.crisiscleanup.core.data.repository.AccountDataRepository
import com.crisiscleanup.core.data.repository.CrisisCleanupAccountDataRepository
import com.crisiscleanup.core.data.repository.SyncLogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MenuViewModel @Inject constructor(
    syncLogRepository: SyncLogRepository,
    private val accountDataRepository: AccountDataRepository,
    private val accountDataRefresher: AccountDataRefresher,
    private val appVersionProvider: AppVersionProvider,
    appEnv: AppEnv,
    private val syncPuller: SyncPuller,
    private val databaseVersionProvider: DatabaseVersionProvider,
    @ApplicationScope private val externalScope: CoroutineScope,
    @Dispatcher(IO) ioDispatcher: CoroutineDispatcher,
) : ViewModel() {
    val isDebuggable = appEnv.isDebuggable
    val isNotProduction = appEnv.isNotProduction

    val versionText: String
        get() {
            val version = appVersionProvider.version
            return if (isNotProduction) "${version.second} (${version.first})"
            else version.second
        }

    val databaseVersionText: String
        get() = if (isNotProduction) "DB ${databaseVersionProvider.databaseVersion}" else ""

    init {
        externalScope.launch(ioDispatcher) {
            syncLogRepository.trimOldLogs()

            accountDataRefresher.updateProfilePicture()
        }
    }

    fun simulateTokenExpired() {
        if (isDebuggable) {
            (accountDataRepository as CrisisCleanupAccountDataRepository).expireAccessToken()
        }
    }

    fun clearRefreshToken() {
        if (isDebuggable) {
            externalScope.launch {
                accountDataRepository.clearAccountTokens()
            }
        }
    }

    fun syncWorksitesFull() {
        if (isDebuggable) {
            syncPuller.scheduleSyncWorksitesFull()
        }
    }
}