package com.crisiscleanup.core.data.repository

import com.crisiscleanup.core.model.data.AccountData
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant

interface AccountDataRepository {
    /**
     * Stream of [AccountData]
     */
    val accountData: Flow<AccountData>

    /**
     * Stream of authenticated account status
     *
     * See if the authenticated account is expired by querying [accountExpiration].
     */
    val isAuthenticated: Flow<Boolean>

    /**
     * Instant the account (token) expires
     */
    val accountExpiration: Flow<Instant>

    /**
     * Clear (authenticated) account info
     */
    suspend fun clearAccount()

    /**
     * Set authenticated account info
     */
    suspend fun setAccount(
        accessToken: String,
        email: String,
        firstName: String,
        lastName: String,
        expirySeconds: Long,
        profilePictureUri: String,
    )
}