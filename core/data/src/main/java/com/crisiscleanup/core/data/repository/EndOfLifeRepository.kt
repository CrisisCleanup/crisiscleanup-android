package com.crisiscleanup.core.data.repository

import com.crisiscleanup.core.common.AppEnv
import com.crisiscleanup.core.common.di.ApplicationScope
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.model.data.BuildEndOfLife
import com.crisiscleanup.core.network.endoflife.EndOfLifeClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

interface EndOfLifeRepository {
    fun saveEndOfLifeData()
}

class AppEndOfLifeRepository @Inject constructor(
    private val endOfLifeClient: EndOfLifeClient,
    private val appMetricsRepository: LocalAppMetricsRepository,
    private val appEnv: AppEnv,
    @ApplicationScope private val externalScope: CoroutineScope,
    @Logger(CrisisCleanupLoggers.App) private val logger: AppLogger,
) : EndOfLifeRepository {
    override fun saveEndOfLifeData() {
        if (appEnv.isEarlybird) {
            externalScope.launch {
                try {
                    endOfLifeClient.getEarlybirdEndOfLife()?.let {
                        appMetricsRepository.setEarlybirdEnd(
                            BuildEndOfLife(
                                it.expires,
                                it.title ?: "",
                                it.message,
                                it.link ?: "",
                            ),
                        )
                    }
                } catch (e: Exception) {
                    logger.logException(e)
                }
            }
        }
    }
}
