package com.crisiscleanup.core.common.event

import com.crisiscleanup.core.common.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

interface AuthEventBus {
    val logouts: Flow<Boolean>
    val expiredTokens: Flow<Boolean>
    val credentialRequests: Flow<Boolean>
    val saveCredentialRequests: Flow<Pair<String, String>>
    val passwordCredentialResults: Flow<PasswordCredentials>

    fun onLogout()
    fun onExpiredToken()
    fun onPasswordRequest()
    fun onSaveCredentials(emailAddress: String, password: String)
    fun onPasswordCredentialsResult(credentials: PasswordCredentials)
}

data class PasswordCredentials(
    val emailAddress: String,
    val password: String,
    val resultCode: PasswordRequestCode,
)

enum class PasswordRequestCode {
    Success,
    Fail,
    PasswordNotFound,
    PasswordAccessDenied,
}

@Singleton
class CrisisCleanupAuthEventBus @Inject constructor(
    @ApplicationScope private val externalScope: CoroutineScope,
) : AuthEventBus {
    override val logouts = MutableSharedFlow<Boolean>(0)
    override val expiredTokens = MutableSharedFlow<Boolean>(0)
    override val credentialRequests = MutableSharedFlow<Boolean>(0)
    override val saveCredentialRequests = MutableSharedFlow<Pair<String, String>>(0)
    override val passwordCredentialResults = MutableSharedFlow<PasswordCredentials>(0)

    override fun onLogout() {
        externalScope.launch {
            logouts.emit(true)
        }
    }

    override fun onExpiredToken() {
        externalScope.launch {
            expiredTokens.emit(true)
        }
    }

    override fun onPasswordRequest() {
        externalScope.launch {
            credentialRequests.emit(true)
        }
    }

    override fun onSaveCredentials(emailAddress: String, password: String) {
        externalScope.launch {
            saveCredentialRequests.emit(Pair(emailAddress, password))
        }
    }

    override fun onPasswordCredentialsResult(credentials: PasswordCredentials) {
        externalScope.launch {
            passwordCredentialResults.emit(credentials)
        }
    }
}