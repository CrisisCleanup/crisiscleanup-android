package com.crisiscleanup.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.crisiscleanup.core.database.model.PopulatedSyncLog
import com.crisiscleanup.core.database.model.SyncLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncLogDao {
    // TODO Page results
    @Transaction
    @Query("SELECT * FROM sync_logs ORDER BY log_time DESC LIMIT 200")
    fun streamSyncLogs(): Flow<List<PopulatedSyncLog>>

    @Insert
    fun insertSyncLogs(logs: Collection<SyncLogEntity>)
}