package com.crisiscleanup.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface WorksiteSyncStatDao {
    @Transaction
    @Query("SELECT COUNT(*) FROM worksite_sync_stats")
    @Deprecated("Use updated stats table(s)")
    fun getWorksiteSyncStatCount(): Long
}
