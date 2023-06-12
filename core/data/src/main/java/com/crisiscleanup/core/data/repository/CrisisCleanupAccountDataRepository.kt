package com.crisiscleanup.core.data.repository

import com.crisiscleanup.core.common.di.ApplicationScope
import com.crisiscleanup.core.common.event.AuthEventBus
import com.crisiscleanup.core.common.network.CrisisCleanupDispatchers.IO
import com.crisiscleanup.core.common.network.Dispatcher
import com.crisiscleanup.core.datastore.AccountInfoDataSource
import com.crisiscleanup.core.model.data.AccountData
import com.crisiscleanup.core.model.data.OrgData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import org.jetbrains.annotations.VisibleForTesting
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CrisisCleanupAccountDataRepository @Inject constructor(
    private val dataSource: AccountInfoDataSource,
    authEventBus: AuthEventBus,
    @ApplicationScope private val externalScope: CoroutineScope,
    @Dispatcher(IO) private val ioDispatcher: CoroutineDispatcher,
) : AccountDataRepository {
    /* UPDATE [CrisisCleanupAccountDataRepositoryTest] when changing below */

    override var accessTokenCached: String = ""
        private set

    override val accountData: Flow<AccountData> = dataSource.accountData
        .map {
            accessTokenCached = it.accessToken
            it
        }
        .shareIn(
            scope = externalScope,
            SharingStarted.WhileSubscribed(),
            replay = 1,
        )

    override val isAuthenticated: Flow<Boolean> = accountData.map {
        it.accessToken.isNotEmpty()
    }

    @VisibleForTesting
    internal val observeJobs: List<Job>

    init {
        val logoutsJob = externalScope.launch(ioDispatcher) {
            authEventBus.logouts.collect { onLogout() }
        }
        val expiredTokensJob = externalScope.launch(ioDispatcher) {
            authEventBus.expiredTokens.collect { onExpiredToken() }
        }
        observeJobs = listOf(logoutsJob, expiredTokensJob)
    }

    override suspend fun setAccount(
        id: Long,
        accessToken: String,
        email: String,
        firstName: String,
        lastName: String,
        expirySeconds: Long,
        profilePictureUri: String,
        org: OrgData,
    ) {
        accessTokenCached = accessToken
        dataSource.setAccount(
            id,
            accessToken,
            email,
            firstName,
            lastName,
            expirySeconds,
            profilePictureUri,
            org,
        )
    }

    private suspend fun clearAccount() {
        accessTokenCached = ""
        dataSource.clearAccount()
    }

    private suspend fun onLogout() {
        clearAccount()
    }

    private suspend fun onExpiredToken() {
        accessTokenCached = ""
        dataSource.expireToken()
    }
}