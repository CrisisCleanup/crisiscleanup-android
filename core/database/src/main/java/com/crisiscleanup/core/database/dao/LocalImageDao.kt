package com.crisiscleanup.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.crisiscleanup.core.database.model.NetworkFileLocalImageEntity

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
}