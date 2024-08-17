package com.crisiscleanup.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.crisiscleanup.core.database.model.EquipmentEntity
import com.crisiscleanup.core.database.model.PopulatedEquipment
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
}
