package com.crisiscleanup.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
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

    @Transaction
    @Query("SELECT * FROM user_equipments WHERE is_local_modified<>0 AND network_id>0")
    fun getLocallyModifiedEquipment(): List<UserEquipmentEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertIgnoreUserEquipment(entity: UserEquipmentEntity): Long

    @Transaction
    @Query(
        """
        UPDATE user_equipments
        SET user_id         =:userId,
            equipment_id    =:equipmentId,
            quantity        =:quantity
        WHERE network_id=:networkId AND local_global_uuid=''
        """,
    )
    fun syncUpdateEquipment(
        networkId: Long,
        userId: Long,
        equipmentId: Int,
        quantity: Int,
    )

    @Transaction
    @Query(
        """
        DELETE FROM user_equipments
        WHERE user_id=:userId AND equipment_id NOT IN(:equipmentIds) AND is_local_modified=0
        """,
    )
    fun deleteUnspecifiedUserEquipment(userId: Long, equipmentIds: Collection<Int>)
}
