package com.crisiscleanup.core.data.repository

import com.crisiscleanup.core.model.data.AccountData
import kotlinx.coroutines.flow.Flow

interface AccountDataRepository {
    /**
     * Stream of [AccountData]
     */
    val accountData: Flow<AccountData>

    /**
     * Stream of authenticated account status
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
        accessToken: String,
        email: String,
        firstName: String,
        lastName: String,
        expirySeconds: Long,
    )
}