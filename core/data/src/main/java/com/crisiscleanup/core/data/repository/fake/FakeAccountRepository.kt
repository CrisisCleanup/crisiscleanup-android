package com.crisiscleanup.core.data.repository.fake

import com.crisiscleanup.core.data.repository.AccountDataRepository
import com.crisiscleanup.core.model.data.AccountData
import com.crisiscleanup.core.model.data.OrgData
import com.crisiscleanup.core.model.data.emptyAccountData
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Instant

/**
 * For specifying account information (without needing internet)
 */
class FakeAccountRepository : AccountDataRepository {
    private val _accountData =
        MutableSharedFlow<AccountData>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    private val current get() = _accountData.replayCache.firstOrNull() ?: emptyAccountData

    override val accountData: Flow<AccountData> = _accountData.filterNotNull()


    override val isAuthenticated: Flow<Boolean> = accountData.map { it.hasAuthenticated }

    override var refreshToken: String = ""
        private set
    override var accessToken: String = ""
        private set

    private fun setAccountTokens(refreshToken: String, accessToken: String) {
        this.refreshToken = refreshToken
        this.accessToken = accessToken
    }

    override suspend fun setAccount(
        refreshToken: String,
        accessToken: String,
        id: Long,
        email: String,
        firstName: String,
        lastName: String,
        expirySeconds: Long,
        profilePictureUri: String,
        org: OrgData,
    ) {
        setAccountTokens(refreshToken, accessToken)
        _accountData.tryEmit(
            this.current.copy(
                id = id,
                fullName = "$firstName $lastName".trim(),
                tokenExpiry = Instant.fromEpochSeconds(expirySeconds),
                emailAddress = email,
                profilePictureUri = profilePictureUri,
                org = org,
            )
        )
    }

    override suspend fun updateAccountTokens(
        refreshToken: String,
        accessToken: String,
        expirySeconds: Long
    ) {
        setAccountTokens(refreshToken, accessToken)
        current.copy(tokenExpiry = Instant.fromEpochSeconds(expirySeconds))
        )
    }

    override suspend fun updateAccountTokens() {
    }

    override suspend fun clearAccountTokens() {
        _accountData.tryEmit(
            current.copy(tokenExpiry = Instant.fromEpochSeconds(expirySeconds))
        )
    }

    override suspend fun updateAccountTokens() {
    }

    override suspend fun clearAccountTokens() {
        updateAccountTokens("", "", 0)
    }
}
