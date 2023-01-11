package com.crisiscleanup.core.data.repository

import com.crisiscleanup.core.datastore.AccountInfoDataSource
import com.crisiscleanup.core.model.data.AccountData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import javax.inject.Inject

class CrisisCleanupAccountDataRepository @Inject constructor(
    private val dataSource: AccountInfoDataSource
) : AccountDataRepository {
    /* UPDATE [CrisisCleanupAccountDataRepositoryTest] when changing below */

    override val accountData: Flow<AccountData> = dataSource.accountData

    override val isAuthenticated: Flow<Boolean> = accountData.map {
        it.accessToken.isNotEmpty() && it.tokenExpiry > Clock.System.now()
    }

    override suspend fun clearAccount() = dataSource.clearAccount()

    override suspend fun setAccount(
        accessToken: String,
        email: String,
        firstName: String,
        lastName: String,
        expirySeconds: Long
    ) {
        dataSource.setAccount(accessToken, email, firstName, lastName, expirySeconds)
    }
}