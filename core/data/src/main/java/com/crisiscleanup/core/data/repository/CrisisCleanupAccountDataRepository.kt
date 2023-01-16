package com.crisiscleanup.core.data.repository

import com.crisiscleanup.core.datastore.AccountInfoDataSource
import com.crisiscleanup.core.model.data.AccountData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CrisisCleanupAccountDataRepository @Inject constructor(
    private val dataSource: AccountInfoDataSource
) : AccountDataRepository {
    /* UPDATE [CrisisCleanupAccountDataRepositoryTest] when changing below */

    override var accessTokenCached: String = ""
        private set

    override val accountData: Flow<AccountData> = dataSource.accountData.map {
        accessTokenCached = it.accessToken
        it
    }

    // TODO Test coverage including at feature/app level
    override val isAuthenticated: Flow<Boolean> = accountData.map {
        it.accessToken.isNotEmpty()
    }

    override suspend fun clearAccount() = dataSource.clearAccount()

    override suspend fun setAccount(
        accessToken: String,
        email: String,
        firstName: String,
        lastName: String,
        expirySeconds: Long,
        profilePictureUri: String,
    ) {
        // TODO Update tests for set and clear
        accessTokenCached = accessToken
        dataSource.setAccount(
            accessToken,
            email,
            firstName,
            lastName,
            expirySeconds,
            profilePictureUri,
        )
    }
}