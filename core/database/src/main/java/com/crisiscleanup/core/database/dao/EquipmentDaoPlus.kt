package com.crisiscleanup.core.database.dao

import androidx.room.withTransaction
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.common.sync.SyncLogger
import com.crisiscleanup.core.database.CrisisCleanupDatabase
import com.crisiscleanup.core.database.model.UserEquipmentEntity
import javax.inject.Inject

class EquipmentDaoPlus @Inject constructor(
    internal val db: CrisisCleanupDatabase,
    private val syncLogger: SyncLogger,
    @Logger(CrisisCleanupLoggers.Equipment) private val appLogger: AppLogger,
) {
    private fun getLocalModifiedEquipments() =
        db.equipmentDao().getLocallyModifiedEquipment().associateBy { it.networkId }

    suspend fun syncEquipment(networkEquipments: List<UserEquipmentEntity>) = db.withTransaction {
        val modifiedEquipments = getLocalModifiedEquipments()
        val unmodifiedEquipments =
            networkEquipments.filter { !modifiedEquipments.contains(it.networkId) }

        // TODO Write tests
        val equipmentDao = db.equipmentDao()
        for (equipment in unmodifiedEquipments) {
            val id = equipmentDao.insertIgnoreUserEquipment(equipment)
            if (id < 0) {
                with(equipment) {
                    equipmentDao.syncUpdateEquipment(
                        networkId = networkId,
                        userId = userId,
                        equipmentId = equipmentId,
                        quantity = quantity,
                    )
                }
            }
        }
    }
}
