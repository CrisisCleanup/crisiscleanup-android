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
    val refreshedTokens: Flow<Boolean>

    fun onLogout()

    fun onTokensRefreshed()
}

@Singleton
class CrisisCleanupAuthEventBus @Inject constructor(
    @ApplicationScope private val externalScope: CoroutineScope,
) : AuthEventBus {
    override val logouts = MutableSharedFlow<Boolean>(0)
    override val refreshedTokens = MutableSharedFlow<Boolean>(0)

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
}