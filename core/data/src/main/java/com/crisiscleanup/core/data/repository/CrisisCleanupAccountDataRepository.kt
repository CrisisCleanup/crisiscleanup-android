package com.crisiscleanup.core.data.repository

import com.crisiscleanup.core.common.di.ApplicationScope
import com.crisiscleanup.core.common.event.AuthEventManager
import com.crisiscleanup.core.common.event.ExpiredTokenListener
import com.crisiscleanup.core.common.event.LogoutListener
import com.crisiscleanup.core.common.network.CrisisCleanupDispatchers.IO
import com.crisiscleanup.core.common.network.Dispatcher
import com.crisiscleanup.core.datastore.AccountInfoDataSource
import com.crisiscleanup.core.model.data.AccountData
import com.crisiscleanup.core.model.data.OrgData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CrisisCleanupAccountDataRepository @Inject constructor(
    private val dataSource: AccountInfoDataSource,
    authEventManager: AuthEventManager,
    @ApplicationScope private val externalScope: CoroutineScope,
    @Dispatcher(IO) private val ioDispatcher: CoroutineDispatcher,
) : AccountDataRepository, LogoutListener, ExpiredTokenListener {
    /* UPDATE [CrisisCleanupAccountDataRepositoryTest] when changing below */

    override var accessTokenCached: String = ""
        private set

    override val accountData: Flow<AccountData> = dataSource.accountData.map {
        accessTokenCached = it.accessToken
        it
    }

    override val isAuthenticated: Flow<Boolean> = accountData.map {
        it.accessToken.isNotEmpty()
    }

    init {
        authEventManager.addLogoutListener(this)
        authEventManager.addExpiredTokenListener(this)
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

    // LogoutListener

    override suspend fun onLogout() = clearAccount()

    // ExpiredTokenListener

    override fun onExpiredToken() {
        accessTokenCached = ""
        externalScope.launch(ioDispatcher) {
            dataSource.expireToken()
        }
    }
}