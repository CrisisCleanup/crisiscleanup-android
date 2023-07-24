package com.crisiscleanup.core.data.repository

import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.datastore.AccountInfoDataSource
import com.crisiscleanup.core.network.CrisisCleanupNetworkDataSource
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.days

@Singleton
class AccountDataRefresher @Inject constructor(
    private val dataSource: AccountInfoDataSource,
    private val networkDataSource: CrisisCleanupNetworkDataSource,
    @Logger(CrisisCleanupLoggers.Auth) private val logger: AppLogger,
) {
    private var profilePictureUpdateTime = Instant.fromEpochSeconds(0)

    suspend fun updateProfilePicture() {
        if (dataSource.refreshToken.isBlank() ||
            profilePictureUpdateTime.plus(1.days) > Clock.System.now()
        ) {
            return
        }

        try {
            networkDataSource.getProfilePic()?.let {
                dataSource.updateProfilePicture(it)
            }
        } catch (e: Exception) {
            logger.logException(e)
        }
    }
}