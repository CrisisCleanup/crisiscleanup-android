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

    @Upsert
    fun upsert(files: NetworkFileEntity)

    @Transaction
    @Query(
        """
        DELETE FROM network_files WHERE id IN(
            SELECT id FROM network_file_local_images i
            INNER JOIN worksite_to_network_file w ON w.network_file_id=i.id
            WHERE worksite_id=:worksiteId AND is_deleted<>0 AND i.id NOT IN(:ids)
        )
        """
    )
    fun deleteDeleted(worksiteId: Long, ids: Collection<Long>)

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

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertIgnoreCrossReference(crossReferences: WorksiteNetworkFileCrossRef)

    @Transaction
    @Query("SELECT full_url FROM network_files WHERE id=:id")
    fun streamNetworkImageUrl(id: Long): Flow<String>

    @Transaction
    @Query("SELECT * FROM worksite_to_network_file WHERE network_file_id=:fileId")
    fun getWorksiteFromFile(fileId: Long): WorksiteNetworkFileCrossRef?

    @Transaction
    @Query(
        """
            SELECT f.file_id
            FROM network_files f
            LEFT JOIN network_file_local_images fi ON f.id=fi.id
            INNER JOIN worksite_to_network_file wf ON f.id=wf.network_file_id
            WHERE worksite_id=:worksiteId AND fi.is_deleted<>0
        """
    )
    fun getDeletedPhotoNetworkFileIds(worksiteId: Long): List<Long>
}