package com.crisiscleanup.core.data.repository

import com.crisiscleanup.core.common.epochZero
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
import com.crisiscleanup.core.network.model.NetworkUserEquipment
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

interface EquipmentRepository {
    val streamEquipmentLookup: Flow<Map<Int, EquipmentData>>

    suspend fun saveEquipment(force: Boolean = false)

    suspend fun saveUserEquipment(force: Boolean = false)
}

@Singleton
class CrisisCleanupEquipmentRepository @Inject constructor(
    private val networkDataSource: CrisisCleanupNetworkDataSource,
    private val equipmentDao: EquipmentDao,
    @Logger(CrisisCleanupLoggers.App) private val logger: AppLogger,
) : EquipmentRepository {
    private var equipmentQueryTimestamp = Instant.epochZero

    private val isUpdatingUserEquipment = MutableStateFlow(false)
    private var userEquipmentQueryTimestamp = Instant.epochZero

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
                offset += equipments.size
                if (offset >= totalCount.coerceAtMost(queryLimit)) {
                    break
                }
            }

            equipmentQueryTimestamp = syncTimestamp
        } catch (e: Exception) {
            logger.logException(e)
        }
    }

    override suspend fun saveUserEquipment(force: Boolean) {
        if (!isUpdatingUserEquipment.compareAndSet(expect = false, update = true)) {
            return
        }

        try {
            if (!force &&
                Clock.System.now().minus(userEquipmentQueryTimestamp) < 2.hours
            ) {
                return
            }

            val syncTimestamp = Clock.System.now()
            val userEquipmentLookup = mutableMapOf<Long, MutableSet<Int>>()
            try {
                // TODO Query equipment only belonging to an organization and affiliates
                val queryCount = 1000
                val queryLimit = 10000
                var offset = 0
                while (offset < queryLimit) {
                    val equipmentList = networkDataSource.getUserEquipment(queryCount, offset)

                    val equipmentCount = equipmentList.results?.size ?: 0
                    if (equipmentCount == 0) {
                        break
                    }

                    val equipments = equipmentList.results!!.map(NetworkUserEquipment::asEntity)
                    equipments.forEach { equipment ->
                        val userId = equipment.userId
                        if (!userEquipmentLookup.contains(userId)) {
                            userEquipmentLookup[userId] = mutableSetOf()
                        }
                        userEquipmentLookup[userId]!!.add(equipment.equipmentId)
                    }
                    equipmentDao.upsertUserEquipment(equipments)

                    val totalCount = equipmentList.count ?: 0
                    offset += equipments.size
                    if (offset >= totalCount.coerceAtMost(queryLimit)) {
                        break
                    }
                }

                userEquipmentQueryTimestamp = syncTimestamp
            } catch (e: Exception) {
                logger.logException(e)
            }

            for ((userId, equipmentIds) in userEquipmentLookup) {
                equipmentDao.deleteUnspecifiedUserEquipment(userId, equipmentIds)
            }
        } finally {
            isUpdatingUserEquipment.value = false
        }
    }
}
