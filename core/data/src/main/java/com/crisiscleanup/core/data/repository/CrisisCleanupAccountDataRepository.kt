package com.crisiscleanup.core.data.repository

import com.crisiscleanup.core.common.di.ApplicationScope
import com.crisiscleanup.core.common.event.AuthEventBus
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.common.network.CrisisCleanupDispatchers.IO
import com.crisiscleanup.core.common.network.Dispatcher
import com.crisiscleanup.core.datastore.AccountInfoDataSource
import com.crisiscleanup.core.model.data.AccountData
import com.crisiscleanup.core.model.data.OrgData
import com.crisiscleanup.core.network.CrisisCleanupAuthApi
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import org.jetbrains.annotations.VisibleForTesting
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@Singleton
class CrisisCleanupAccountDataRepository @Inject constructor(
    private val dataSource: AccountInfoDataSource,
    private val authApi: CrisisCleanupAuthApi,
    authEventBus: AuthEventBus,
    @Logger(CrisisCleanupLoggers.Auth) private val logger: AppLogger,
    @ApplicationScope private val externalScope: CoroutineScope,
    @Dispatcher(IO) private val ioDispatcher: CoroutineDispatcher,
) : AccountDataRepository {
    override val accountData: Flow<AccountData> = dataSource.accountData
        .shareIn(
            scope = externalScope,
            SharingStarted.WhileSubscribed(),
            replay = 1,
        )

    override val isAuthenticated: Flow<Boolean> = accountData.map { it.hasAuthenticated }

    override val refreshToken: String
        get() = dataSource.refreshToken

    override val accessToken: String
        get() = dataSource.accessToken

    @VisibleForTesting
    internal val observeJobs: List<Job>

    init {
        val logoutsJob = externalScope.launch(ioDispatcher) {
            authEventBus.logouts.collect { onLogout() }
        }
        observeJobs = listOf(logoutsJob)
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
        hasAcceptedTerms: Boolean,
    ) {
        dataSource.setAccount(
            refreshToken,
            accessToken,
            id,
            email,
            firstName,
            lastName,
            expirySeconds,
            profilePictureUri,
            org,
            hasAcceptedTerms,
        )
    }

    override suspend fun updateAccountTokens(
        refreshToken: String,
        accessToken: String,
        expirySeconds: Long,
    ) {
        val isClearing = refreshToken.isBlank()
        dataSource.updateAccountTokens(
            refreshToken,
            if (isClearing) "" else accessToken,
            if (isClearing) 0 else expirySeconds,
        )
    }

    override suspend fun updateAccountTokens() {
        try {
            val accountData = this.accountData.first()
            val now = Clock.System.now()
            if (accountData.areTokensValid &&
                accountData.tokenExpiry < now.plus(10.minutes)
            ) {
                authApi.refreshTokens(refreshToken)?.let { refreshResult ->
                    updateAccountTokens(
                        refreshResult.refreshToken,
                        refreshResult.accessToken,
                        now.plus(refreshResult.expiresIn.seconds).epochSeconds,
                    )
                    logger.logDebug("Refreshed soon/expiring account tokens")
                }
            }
        } catch (e: Exception) {
            logger.logException(e)
        }
    }

    override suspend fun clearAccountTokens() {
        updateAccountTokens("", "", 0)
    }

    private suspend fun onLogout() {
        dataSource.clearAccount()
    }

    // For dev
    fun expireAccessToken() {
        externalScope.launch {
            dataSource.ignoreNextAccountChange()
            dataSource.updateAccountTokens(refreshToken, "", 1)
        }
    }
}
