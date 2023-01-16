package com.crisiscleanup.core.data.repository.fake

import com.crisiscleanup.core.data.repository.AccountDataRepository
import com.crisiscleanup.core.model.data.AccountData
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
class FakeAccountRepository(
    private var accessToken: String = "access-token",
    private var email: String = "email@address.com",
    private var firstName: String = "First",
    private var lastName: String = "Last",
    private var expirySeconds: Long = 9999999999,
    private var profilePictureUri: String = "",
) : AccountDataRepository {
    private val _accountData =
        MutableSharedFlow<AccountData>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    private val current get() = _accountData.replayCache.firstOrNull() ?: emptyAccountData

    override val accountData: Flow<AccountData> = _accountData.filterNotNull()

    override val accessTokenCached: String = accessToken

    override val isAuthenticated: Flow<Boolean> = accountData.map {
        it.accessToken.isNotEmpty()
    }

    override suspend fun clearAccount() = setAccount("", "", "", "", 0, "")

    override suspend fun setAccount(
        accessToken: String,
        email: String,
        firstName: String,
        lastName: String,
        expirySeconds: Long,
        profilePictureUri: String,
    ) {
        this.accessToken = accessToken
        this.email = email
        this.firstName = firstName
        this.lastName = lastName
        this.expirySeconds = expirySeconds
        this.profilePictureUri = profilePictureUri

        current.let {
            _accountData.tryEmit(
                it.copy(
                    accessToken = this.accessToken,
                    displayName = "${this.firstName} ${this.lastName}".trim(),
                    tokenExpiry = Instant.fromEpochSeconds(this.expirySeconds),
                    emailAddress = this.email,
                    profilePictureUri = this.profilePictureUri,
                )
            )
        }
    }
}

val demoAccountRepository = FakeAccountRepository(
    accessToken = "access-token",
    email = "demo@crisiscleanup.org",
    firstName = "Demo",
    lastName = "User",
    expirySeconds = 2673619756,
    profilePictureUri = "https://app.staging.crisiscleanup.io/img/ccu-logo-black-500w.5a7f5c6e.png",
)