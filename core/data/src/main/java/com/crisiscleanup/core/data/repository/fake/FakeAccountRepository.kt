package com.crisiscleanup.core.data.repository.fake

import com.crisiscleanup.core.data.repository.AccountDataRepository
import com.crisiscleanup.core.model.data.AccountData
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

val emptyAccountData = AccountData(
    accessToken = "",
    tokenExpiry = Instant.fromEpochSeconds(0),
    displayName = "",
)

class FakeAccountRepository(
    private var accessToken: String = "access-token",
    private var email: String = "email@address.com",
    private var firstName: String = "First",
    private var lastName: String = "Last",
    private var expirySeconds: Long = 9999999999,
) : AccountDataRepository {
    private val _accountData =
        MutableSharedFlow<AccountData>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    private val current get() = _accountData.replayCache.firstOrNull() ?: emptyAccountData

    override val accountData: Flow<AccountData> = _accountData.filterNotNull()

    override val isAuthenticated: Flow<Boolean> = accountData.map {
        it.accessToken.isNotEmpty() && it.tokenExpiry > Clock.System.now()
    }

    override suspend fun clearAccount() = setAccount("", "", "", "", 0)

    override suspend fun setAccount(
        accessToken: String,
        email: String,
        firstName: String,
        lastName: String,
        expirySeconds: Long
    ) {
        this.accessToken = accessToken
        this.email = email
        this.firstName = firstName
        this.lastName = lastName
        this.expirySeconds = expirySeconds

        current.let {
            _accountData.tryEmit(
                it.copy(
                    accessToken = this.accessToken,
                    displayName = "${this.firstName} ${this.lastName}".trim(),
                    tokenExpiry = Instant.fromEpochSeconds(this.expirySeconds),
                )
            )
        }
    }
}