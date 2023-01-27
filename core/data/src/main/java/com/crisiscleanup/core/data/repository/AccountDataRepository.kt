package com.crisiscleanup.core.data.repository

import com.crisiscleanup.core.model.data.AccountData
import kotlinx.coroutines.flow.Flow

interface AccountDataRepository {
    /**
     * Stream of [AccountData]
     */
    val accountData: Flow<AccountData>

    /**
     * Cached value of access token
     *
     * The token may be expired (if defined). Access [accountData] for latest values.
     */
    val accessTokenCached: String

    /**
     * Stream of authenticated account status
     *
     * See if the authenticated account is expired in [accountData].
     */
    val isAuthenticated: Flow<Boolean>

    /**
     * Clear (authenticated) account info
     */
    suspend fun clearAccount()

    /**
     * Set authenticated account info
     */
    suspend fun setAccount(
        id: Long,
        accessToken: String,
        email: String,
        firstName: String,
        lastName: String,
        expirySeconds: Long,
        profilePictureUri: String,
    )
}