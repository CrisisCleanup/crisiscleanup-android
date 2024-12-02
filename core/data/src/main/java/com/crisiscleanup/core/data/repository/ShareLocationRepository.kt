package com.crisiscleanup.core.data.repository

import com.crisiscleanup.core.common.LocationProvider
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.network.CrisisCleanupWriteApi
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.minutes

interface ShareLocationRepository {
    suspend fun shareLocation()
}

@Singleton
class CrisisCleanupShareLocationRepository @Inject
constructor(
    private val accountDataRepository: AccountDataRepository,
    private val appPreferencesRepository: LocalAppPreferencesRepository,
    private val locationProvider: LocationProvider,
    private val writeApiClient: CrisisCleanupWriteApi,
    @Logger(CrisisCleanupLoggers.App) private val logger: AppLogger,
) : ShareLocationRepository {

    private var shareTimestamp = AtomicReference(Instant.fromEpochSeconds(0))
    private val shareInterval = 1.minutes

    override suspend fun shareLocation() {
        val shareLocationWithOrg =
            appPreferencesRepository.userPreferences.first().shareLocationWithOrg
        val areTokensValid = accountDataRepository.accountData.first().areTokensValid
        if (shareLocationWithOrg &&
            areTokensValid
        ) {
            locationProvider.getLocation()?.let { location ->
                synchronized(shareTimestamp) {
                    val now = Clock.System.now()
                    if (shareTimestamp.get() + shareInterval > now) {
                        return
                    }
                    shareTimestamp.set(now)
                }

                try {
                    writeApiClient.shareLocation(location.first, location.second)
                } catch (e: Exception) {
                    logger.logException(e)
                }
            }
        }
    }
}
