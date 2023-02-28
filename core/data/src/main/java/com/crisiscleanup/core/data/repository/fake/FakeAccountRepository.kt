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

    override val accessTokenCached: String = ""

    override val isAuthenticated: Flow<Boolean> = accountData.map {
        it.accessToken.isNotEmpty()
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
        current.let {
            _accountData.tryEmit(
                it.copy(
                    id = id,
                    accessToken = accessToken,
                    fullName = "$firstName $lastName".trim(),
                    tokenExpiry = Instant.fromEpochSeconds(expirySeconds),
                    emailAddress = email,
                    profilePictureUri = profilePictureUri,
                    org = org,
                )
            )
        }
    }
}
