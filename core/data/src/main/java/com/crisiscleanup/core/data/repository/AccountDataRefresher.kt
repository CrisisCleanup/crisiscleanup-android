package com.crisiscleanup.core.data.repository

import com.crisiscleanup.core.common.event.AccountEventBus
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.common.network.CrisisCleanupDispatchers
import com.crisiscleanup.core.common.network.Dispatcher
import com.crisiscleanup.core.datastore.AccountInfoDataSource
import com.crisiscleanup.core.network.CrisisCleanupNetworkDataSource
import com.crisiscleanup.core.network.model.profilePictureUrl
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

@Singleton
class AccountDataRefresher @Inject constructor(
    private val dataSource: AccountInfoDataSource,
    private val networkDataSource: CrisisCleanupNetworkDataSource,
    private val accountDataRepository: AccountDataRepository,
    private val organizationsRepository: OrganizationsRepository,
    private val accountEventBus: AccountEventBus,
    @Dispatcher(CrisisCleanupDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
    @Logger(CrisisCleanupLoggers.Auth) private val logger: AppLogger,
) {
    private var accountDataUpdateTime = Instant.fromEpochSeconds(0)

    private suspend fun refreshAccountData(
        syncTag: String,
        force: Boolean,
        cacheTimeSpan: Duration = 1.days,
    ) {
        if (dataSource.refreshToken.isBlank()) {
            return
        }
        if (!force && accountDataUpdateTime.plus(cacheTimeSpan) > Clock.System.now()) {
            return
        }

        logger.logCapture("Syncing $syncTag")
        try {
            val profile = networkDataSource.getProfileData()
            if (profile.organization.isActive == false) {
                accountEventBus.onAccountInactiveOrganization(dataSource.accountData.first().id)
            } else if (profile.hasAcceptedTerms != null) {
                dataSource.update(
                    profile.files?.profilePictureUrl,
                    profile.hasAcceptedTerms!!,
                    profile.approvedIncidents!!,
                    profile.activeRoles,
                )

                accountDataUpdateTime = Clock.System.now()
            }
        } catch (e: Exception) {
            logger.logException(e)
        }
    }

    suspend fun updateProfilePicture() {
        refreshAccountData("profile pic", false)
    }

    suspend fun updateMyOrganization(force: Boolean) = withContext(ioDispatcher) {
        val organizationId = accountDataRepository.accountData.first().org.id
        if (organizationId > 0) {
            organizationsRepository.syncOrganization(organizationId, force, true)
        }
    }

    suspend fun updateAcceptedTerms() {
        refreshAccountData("accept terms", true)
    }

    suspend fun updateApprovedIncidents(force: Boolean = false) {
        refreshAccountData("approved incidents", force)
    }
}
