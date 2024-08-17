package com.crisiscleanup.core.data.repository

import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.data.model.asEntity
import com.crisiscleanup.core.database.dao.EquipmentDao
import com.crisiscleanup.core.database.model.PopulatedEquipment
import com.crisiscleanup.core.database.model.asExternalModel
import com.crisiscleanup.core.model.data.EquipmentData
import com.crisiscleanup.core.network.CrisisCleanupNetworkDataSource
import com.crisiscleanup.core.network.model.NetworkEquipment
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.days

interface EquipmentRepository {
    val streamEquipmentLookup: Flow<Map<Long, EquipmentData>>

    suspend fun saveEquipment(force: Boolean = false)
}

@Singleton
class CrisisCleanupEquipmentRepository @Inject constructor(
    private val networkDataSource: CrisisCleanupNetworkDataSource,
    private val equipmentDao: EquipmentDao,
    @Logger(CrisisCleanupLoggers.App) private val logger: AppLogger,
) : EquipmentRepository {
    private var equipmentQueryTimestamp = Instant.fromEpochSeconds(0)

    override val streamEquipmentLookup = equipmentDao.streamEquipment()
        .mapLatest {
            it.map(PopulatedEquipment::asExternalModel)
                .associateBy(EquipmentData::id)
        }

    override suspend fun saveEquipment(force: Boolean) {
        if (!force &&
            Clock.System.now().minus(equipmentQueryTimestamp) < 1.days
        ) {
            return
        }

        val syncTimestamp = Clock.System.now()
        try {
            val queryCount = 60
            val queryLimit = 1000
            var offset = 0
            while (offset < queryLimit) {
                val equipmentList = networkDataSource.getEquipmentList(queryCount, offset)

                val equipmentCount = equipmentList.results?.size ?: 0
                if (equipmentCount == 0) {
                    break
                }

                val equipments = equipmentList.results!!.map(NetworkEquipment::asEntity)
                equipmentDao.upsertEquipment(equipments)

                val totalCount = equipmentList.count ?: 0
                offset += totalCount
                if (offset > totalCount.coerceAtMost(queryLimit)) {
                    break
                }
            }

            equipmentQueryTimestamp = syncTimestamp
        } catch (e: Exception) {
            logger.logException(e)
        }
    }
}
