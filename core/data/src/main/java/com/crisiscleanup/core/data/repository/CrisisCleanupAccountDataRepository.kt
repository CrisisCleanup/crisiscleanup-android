package com.crisiscleanup.core.data.repository

import com.crisiscleanup.core.common.event.AuthEventManager
import com.crisiscleanup.core.common.event.LogoutListener
import com.crisiscleanup.core.datastore.AccountInfoDataSource
import com.crisiscleanup.core.model.data.AccountData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CrisisCleanupAccountDataRepository @Inject constructor(
    private val dataSource: AccountInfoDataSource,
    authEventManager: AuthEventManager,
) : AccountDataRepository, LogoutListener {
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
    }

    override suspend fun setAccount(
        id: Long,
        accessToken: String,
        email: String,
        firstName: String,
        lastName: String,
        expirySeconds: Long,
        profilePictureUri: String,
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
        )
    }

    private suspend fun clearAccount() {
        accessTokenCached = ""
        dataSource.clearAccount()
    }

    // LogoutListener
    override suspend fun onLogout() = clearAccount()
}