package com.crisiscleanup.core.data.repository

import android.util.Log
import com.crisiscleanup.core.common.di.ApplicationScope
import com.crisiscleanup.core.common.network.CrisisCleanupDispatchers.IO
import com.crisiscleanup.core.common.network.Dispatcher
import com.crisiscleanup.core.common.sync.SyncLogger
import com.crisiscleanup.core.data.model.asEntity
import com.crisiscleanup.core.database.dao.SyncLogDao
import com.crisiscleanup.core.database.model.PopulatedSyncLog
import com.crisiscleanup.core.database.model.asExternalModel
import com.crisiscleanup.core.model.data.SyncLog
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import javax.inject.Inject

interface SyncLogRepository {
    fun streamLogCount(): Flow<Int>

    fun getLogs(limit: Int, offset: Int): List<SyncLog>

    fun trimOldLogs()
}

class PagingSyncLogRepository @Inject constructor(
    private val syncLogDao: SyncLogDao,
    @ApplicationScope private val coroutineScope: CoroutineScope,
    @Dispatcher(IO) private val ioDispatcher: CoroutineDispatcher,
) : SyncLogger, SyncLogRepository {
    private val logEntriesMutex = Mutex()
    private var logEntries = mutableListOf<SyncLog>()

    override var type = ""

    override fun log(message: String, details: String, type: String): SyncLogger {
        // TODO Enable logging only if dev mode/sync logging is enabled
        logEntries.add(
            SyncLog(
                0,
                Clock.System.now(),
                logType = type.ifEmpty { this.type },
                message = message,
                details = details,
            )
        )
        return this
    }

    override fun clear(): SyncLogger {
        logEntries = mutableListOf()
        return this
    }

    override fun flush() {
        coroutineScope.launch(ioDispatcher) {
            val entries: Collection<SyncLog>
            logEntriesMutex.withLock {
                entries = logEntries
                logEntries = mutableListOf()
            }
            if (entries.isNotEmpty()) {
                try {
                    syncLogDao.insertSyncLogs(entries.map(SyncLog::asEntity))
                } catch (e: Exception) {
                    Log.e("sync-log-exception", e.message, e)
                }
            }
        }
    }

    override fun streamLogCount() = syncLogDao.streamLogCount()

    override fun getLogs(limit: Int, offset: Int) =
        syncLogDao.getSyncLogs(limit, offset).map(PopulatedSyncLog::asExternalModel)

    override fun trimOldLogs() = syncLogDao.trimOldSyncLogs()
}

@Module
@InstallIn(SingletonComponent::class)
interface SyncLogModule {
    @Binds
    fun bindsSyncLogger(logger: PagingSyncLogRepository): SyncLogger

    @Binds
    fun bindsSyncLogRepository(repository: PagingSyncLogRepository): SyncLogRepository
}