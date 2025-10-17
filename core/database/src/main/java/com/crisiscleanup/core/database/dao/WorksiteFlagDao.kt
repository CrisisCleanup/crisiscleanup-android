package com.crisiscleanup.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.crisiscleanup.core.database.model.PopulatedIdNetworkId
import com.crisiscleanup.core.database.model.WorksiteFlagEntity
import kotlin.time.Instant

@Dao
interface WorksiteFlagDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertIgnoreFlag(flag: WorksiteFlagEntity): Long

    @Transaction
    @Query(
        """
        UPDATE worksite_flags SET
        `action`        =:action,
        created_at      =:createdAt,
        is_high_priority=:isHighPriority,
        network_id      =:networkId,
        notes           =:notes,
        requested_action=:requestedAction
        WHERE worksite_id=:worksiteId AND reason_t=:reasonT
        """,
    )
    fun syncUpdateFlag(
        worksiteId: Long,
        networkId: Long,
        action: String?,
        createdAt: Instant,
        isHighPriority: Boolean?,
        notes: String?,
        reasonT: String,
        requestedAction: String?,
    )

    @Transaction
    @Query(
        """
        DELETE FROM worksite_flags
        WHERE worksite_id=:worksiteId AND reason_t NOT IN(:reasons)
        """,
    )
    fun syncDeleteUnspecified(worksiteId: Long, reasons: Collection<String>)

    @Transaction
    @Query(
        """
        DELETE FROM worksite_flags
        WHERE worksite_id=:worksiteId AND id NOT IN(:ids)
        """,
    )
    fun deleteUnspecified(worksiteId: Long, ids: Collection<Long>)

    @Transaction
    @Query(
        """
        SELECT id, network_id
        FROM worksite_flags
        WHERE worksite_id=:worksiteId AND network_id>-1
        """,
    )
    fun getNetworkedIdMap(worksiteId: Long): List<PopulatedIdNetworkId>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertIgnore(flags: Collection<WorksiteFlagEntity>): List<Long>

    @Update
    fun update(flags: Collection<WorksiteFlagEntity>)

    @Transaction
    @Query(
        """
        UPDATE OR IGNORE worksite_flags
        SET network_id=:networkId
        WHERE id=:id
        """,
    )
    fun updateNetworkId(id: Long, networkId: Long)

    @Transaction
    @Query("SELECT COUNT(id) FROM worksite_flags WHERE worksite_id=:worksiteId AND network_id<=0")
    fun getUnsyncedCount(worksiteId: Long): Int

    @Transaction
    @Query("SELECT DISTINCT reason_t FROM worksite_flags WHERE worksite_id=:worksiteId")
    fun getReasons(worksiteId: Long): List<String>
}
