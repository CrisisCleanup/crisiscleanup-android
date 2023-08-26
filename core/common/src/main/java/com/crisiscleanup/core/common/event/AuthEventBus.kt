package com.crisiscleanup.core.common.event

import com.crisiscleanup.core.common.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

interface AuthEventBus {
    val logouts: Flow<Boolean>
    val refreshedTokens: Flow<Boolean>

    val showResetPassword: Flow<Boolean>
    val resetPasswords: StateFlow<String>
    val emailLoginLinks: Flow<String>

    fun onLogout()

    fun onTokensRefreshed()

    fun onResetPassword(code: String)

    fun onEmailLoginLink(code: String)
}

@Singleton
class CrisisCleanupAuthEventBus @Inject constructor(
    @ApplicationScope private val externalScope: CoroutineScope,
) : AuthEventBus {
    override val logouts = MutableSharedFlow<Boolean>(0)
    override val refreshedTokens = MutableSharedFlow<Boolean>(0)

    override val resetPasswords = MutableStateFlow("")
    override val showResetPassword = resetPasswords.map { it.isNotBlank() }
    override val emailLoginLinks = MutableSharedFlow<String>(1)

    override fun onLogout() {
        externalScope.launch {
            logouts.emit(true)
        }
    }

    override fun onTokensRefreshed() {
        externalScope.launch {
            refreshedTokens.emit(true)
        }
    }

    override fun onResetPassword(code: String) {
        externalScope.launch {
            resetPasswords.value = code
        }
    }

    override fun onEmailLoginLink(code: String) {
        externalScope.launch {
            emailLoginLinks.emit(code)
        }
    }
}
