package com.crisiscleanup.core.database.dao

import androidx.room.*
import com.crisiscleanup.core.database.model.PopulatedIdNetworkId
import com.crisiscleanup.core.database.model.WorksiteNoteEntity
import kotlinx.datetime.Instant

@Dao
interface WorksiteNoteDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertIgnoreNote(note: WorksiteNoteEntity): Long

    @Transaction
    @Query(
        """
        UPDATE worksite_notes SET
        created_at  =:createdAt,
        is_survivor =:isSurvivor,
        note        =:note
        WHERE worksite_id=:worksiteId AND network_id=:networkId AND local_global_uuid=''
        """
    )
    fun syncUpdateNote(
        worksiteId: Long,
        networkId: Long,
        createdAt: Instant,
        isSurvivor: Boolean,
        note: String,
    )

    @Transaction
    @Query(
        """
        DELETE FROM worksite_notes
        WHERE worksite_id=:worksiteId AND network_id NOT IN(:networkIds)
        """
    )
    fun syncDeleteUnspecified(worksiteId: Long, networkIds: Collection<Long>)

    @Transaction
    @Query(
        """
        SELECT id, network_id
        FROM worksite_notes
        WHERE worksite_id=:worksiteId AND network_id>-1 AND id IN(:ids)
        """
    )
    fun getNetworkedIdMap(
        worksiteId: Long,
        ids: Collection<Long>,
    ): List<PopulatedIdNetworkId>

    @Insert
    fun insert(notes: Collection<WorksiteNoteEntity>)
}
