package com.crisiscleanup.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.crisiscleanup.core.database.model.NetworkFileLocalImageEntity
import com.crisiscleanup.core.database.model.PopulatedLocalImageDescription
import com.crisiscleanup.core.database.model.PopulatedWorksiteImageCount
import com.crisiscleanup.core.database.model.WorksiteLocalImageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LocalImageDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertIgnore(localImage: NetworkFileLocalImageEntity)

    @Transaction
    @Query("SELECT * FROM network_file_local_images WHERE id=:id")
    fun getNetworkFileLocalImage(id: Long): NetworkFileLocalImageEntity?

    @Transaction
    @Query("UPDATE network_file_local_images SET is_deleted=1 WHERE id=:id")
    fun markNetworkImageForDelete(id: Long)

    @Transaction
    @Query("UPDATE network_file_local_images SET rotate_degrees=:rotationDegrees WHERE id=:id")
    fun updateNetworkImageRotation(id: Long, rotationDegrees: Int)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertIgnore(image: WorksiteLocalImageEntity): Long

    @Transaction
    @Query("SELECT uri FROM worksite_local_images WHERE id=:id")
    fun streamLocalImageUri(id: Long): Flow<String?>

    @Transaction
    @Query("SELECT * FROM worksite_local_images WHERE id=:id")
    fun getLocalImage(id: Long): WorksiteLocalImageEntity?

    @Transaction
    @Query(
        """
        UPDATE worksite_local_images
        SET tag=:tag
        WHERE worksite_id=:worksiteId AND local_document_id=:documentId
        """,
    )
    fun update(
        worksiteId: Long,
        documentId: String,
        tag: String,
    )

    @Transaction
    @Query("UPDATE OR IGNORE worksite_local_images SET rotate_degrees=:rotationDegrees WHERE id=:id")
    fun updateLocalImageRotation(id: Long, rotationDegrees: Int)

    @Transaction
    @Query("DELETE FROM worksite_local_images WHERE id=:id")
    fun deleteLocalImage(id: Long)

    @Transaction
    @Query(
        """
        SELECT id, uri, tag
        FROM worksite_local_images
        WHERE worksite_id=:worksiteId
        ORDER BY id ASC
        """,
    )
    fun getWorksiteLocalImages(worksiteId: Long): List<PopulatedLocalImageDescription>

    @Transaction
    @Query(
        """
        SELECT worksite_id, count
        FROM (
            SELECT DISTINCT worksite_id, COUNT(id) AS count, MIN(id) AS min_id
            FROM worksite_local_images
            GROUP BY worksite_id
            ORDER BY min_id ASC
        )
        """,
    )
    fun getUploadImageWorksiteIds(): List<PopulatedWorksiteImageCount>

    @Transaction
    @Query(
        """
        SELECT id
        FROM worksite_local_images
        ORDER BY id DESC
        LIMIT 1
        """,
    )
    fun getNewestLocalImageId(): Long?
}
