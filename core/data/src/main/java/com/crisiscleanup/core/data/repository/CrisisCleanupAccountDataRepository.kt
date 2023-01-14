package com.crisiscleanup.core.data.repository

import com.crisiscleanup.core.datastore.AccountInfoDataSource
import com.crisiscleanup.core.model.data.AccountData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Instant
import javax.inject.Inject

class CrisisCleanupAccountDataRepository @Inject constructor(
    private val dataSource: AccountInfoDataSource
) : AccountDataRepository {
    /* UPDATE [CrisisCleanupAccountDataRepositoryTest] when changing below */

    override val accountData: Flow<AccountData> = dataSource.accountData

    // TODO Test coverage including at feature/app level
    override val isAuthenticated: Flow<Boolean> = accountData.map {
        it.accessToken.isNotEmpty()
    }

    // TODO Test coverage including at feature/app level
    override val accountExpiration: Flow<Instant> = accountData.map { it.tokenExpiry }

    override suspend fun clearAccount() = dataSource.clearAccount()

    override suspend fun setAccount(
        accessToken: String,
        email: String,
        firstName: String,
        lastName: String,
        expirySeconds: Long,
        profilePictureUri: String,
    ) {
        dataSource.setAccount(
            accessToken,
            email,
            firstName,
            lastName,
            expirySeconds,
            profilePictureUri)
    }
}