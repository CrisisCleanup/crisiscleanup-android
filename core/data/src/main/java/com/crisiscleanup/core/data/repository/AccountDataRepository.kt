package com.crisiscleanup.core.data.repository

import com.crisiscleanup.core.model.data.AccountData
import com.crisiscleanup.core.model.data.OrgData
import kotlinx.coroutines.flow.Flow

interface AccountDataRepository {
    /**
     * Stream of [AccountData]
     */
    val accountData: Flow<AccountData>

    /**
     * Authenticated meaning the user has authenticated successfully at least once
     */
    val isAuthenticated: Flow<Boolean>

    val refreshToken: String
    val accessToken: String

    /**
     * Set authenticated account info
     */
    suspend fun setAccount(
        refreshToken: String,
        accessToken: String,
        id: Long,
        email: String,
        phone: String,
        firstName: String,
        lastName: String,
        expirySeconds: Long,
        profilePictureUri: String,
        org: OrgData,
        hasAcceptedTerms: Boolean,
        approvedIncidentIds: Set<Long>,
        activeRoles: Set<Int>,
    )

    suspend fun updateAccountTokens(
        refreshToken: String,
        accessToken: String,
        expirySeconds: Long,
    )

    suspend fun updateAccountTokens()

    suspend fun clearAccountTokens()
}
