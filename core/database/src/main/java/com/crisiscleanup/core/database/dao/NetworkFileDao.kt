package com.crisiscleanup.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.crisiscleanup.core.database.model.NetworkFileEntity
import com.crisiscleanup.core.database.model.WorksiteNetworkFileCrossRef
import kotlinx.coroutines.flow.Flow

@Dao
interface NetworkFileDao {
    @Upsert
    fun upsert(files: List<NetworkFileEntity>)

    @Transaction
    @Query(
        """
        DELETE FROM worksite_to_network_file
        WHERE worksite_id=:worksiteId AND network_file_id NOT IN(:networkFileIds)
        """
    )
    fun deleteUnspecifiedCrossReferences(worksiteId: Long, networkFileIds: Collection<Long>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertIgnoreCrossReferences(crossReferences: Collection<WorksiteNetworkFileCrossRef>)

    @Transaction
    @Query("SELECT full_url FROM network_files WHERE id=:id")
    fun streamNetworkImageUrl(id: Long): Flow<String>
}