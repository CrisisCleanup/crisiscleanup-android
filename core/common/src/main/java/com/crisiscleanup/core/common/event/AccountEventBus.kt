package com.crisiscleanup.core.common.event

import com.crisiscleanup.core.common.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

interface AccountEventBus {
    val logouts: Flow<Boolean>
    val refreshedTokens: Flow<Boolean>
    val inactiveOrganizations: Flow<Long>

    fun onLogout()

    fun onTokensRefreshed()

    fun onAccountInactiveOrganization(accountId: Long)
    fun clearAccountInactiveOrganization()
}

@Singleton
class CrisisCleanupAccountEventBus @Inject constructor(
    @ApplicationScope private val externalScope: CoroutineScope,
) : AccountEventBus {
    override val logouts = MutableSharedFlow<Boolean>(0)
    override val refreshedTokens = MutableSharedFlow<Boolean>(0)

    override val inactiveOrganizations = MutableSharedFlow<Long>(0)

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

    override fun onAccountInactiveOrganization(accountId: Long) {
        externalScope.launch {
            inactiveOrganizations.emit(accountId)
        }
    }

    override fun clearAccountInactiveOrganization() {
        externalScope.launch {
            inactiveOrganizations.emit(0)
        }
    }
}
