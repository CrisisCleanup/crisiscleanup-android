package com.crisiscleanup.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.crisiscleanup.core.database.model.EquipmentEntity
import com.crisiscleanup.core.database.model.PopulatedEquipment
import com.crisiscleanup.core.database.model.UserEquipmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EquipmentDao {
    @Transaction
    @Query(
        """
        SELECT id, list_order, is_common, selected_count, name_t
        FROM cleanup_equipment
        ORDER BY name_t
        """,
    )
    fun streamEquipment(): Flow<List<PopulatedEquipment>>

    @Upsert
    fun upsertEquipment(equipments: List<EquipmentEntity>)

    @Upsert
    fun upsertUserEquipment(equipments: List<UserEquipmentEntity>)

    @Transaction
    @Query(
        """
        DELETE FROM user_equipment
        WHERE user_id=:userId AND equipment_id NOT IN(:equipmentIds)
        """,
    )
    fun deleteUnspecifiedUserEquipment(userId: Long, equipmentIds: Collection<Int>)
}
