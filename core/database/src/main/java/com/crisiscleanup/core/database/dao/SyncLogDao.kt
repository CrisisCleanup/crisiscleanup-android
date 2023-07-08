package com.crisiscleanup.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.crisiscleanup.core.database.model.PopulatedSyncLog
import com.crisiscleanup.core.database.model.SyncLogEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.days

@Dao
interface SyncLogDao {
    @Transaction
    @Query("SELECT COUNT(id) FROM sync_logs")
    fun streamLogCount(): Flow<Int>

    @Transaction
    @Query("SELECT * FROM sync_logs ORDER BY log_time DESC LIMIT :limit OFFSET :offset")
    fun getSyncLogs(limit: Int = 20, offset: Int = 0): List<PopulatedSyncLog>

    @Insert
    fun insertSyncLogs(logs: Collection<SyncLogEntity>)

    @Transaction
    @Query("DELETE FROM sync_logs WHERE log_time<:minLogTime")
    fun trimOldSyncLogs(minLogTime: Instant = Clock.System.now().minus(14.days))
}