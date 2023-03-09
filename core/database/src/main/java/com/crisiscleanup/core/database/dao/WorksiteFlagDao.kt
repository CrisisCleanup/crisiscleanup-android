package com.crisiscleanup.core.database.dao

import androidx.room.*
import com.crisiscleanup.core.database.model.WorksiteFlagEntity
import kotlinx.datetime.Instant

@Dao
interface WorksiteFlagDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertIgnoreFlag(flag: WorksiteFlagEntity): Long

    @Transaction
    @Query(
        """
        UPDATE worksite_flags SET
        created_at      =:createdAt,
        action          =:action,
        is_high_priority=:isHighPriority,
        notes           =:notes,
        reason_t        =:reasonT,
        requested_action=:requestedAction
        WHERE worksite_id=:worksiteId AND network_id=:networkId AND local_global_uuid=''
        """
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
        WHERE worksite_id=:worksiteId AND network_id NOT IN(:networkIds)
        """
    )
    fun syncDeleteUnspecified(worksiteId: Long, networkIds: Collection<Long>)
}
