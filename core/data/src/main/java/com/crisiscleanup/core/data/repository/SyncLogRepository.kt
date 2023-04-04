package com.crisiscleanup.core.data.repository

import com.crisiscleanup.core.common.di.ApplicationScope
import com.crisiscleanup.core.common.network.CrisisCleanupDispatchers.IO
import com.crisiscleanup.core.common.network.Dispatcher
import com.crisiscleanup.core.common.sync.SyncLogger
import com.crisiscleanup.core.data.model.asEntity
import com.crisiscleanup.core.database.dao.SyncLogDao
import com.crisiscleanup.core.model.data.SyncLog
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import javax.inject.Inject

class SyncLogRepository @Inject constructor(
    private val syncLogDao: SyncLogDao,
    @ApplicationScope private val coroutineScope: CoroutineScope,
    @Dispatcher(IO) private val ioDispatcher: CoroutineDispatcher,
) : SyncLogger {
    private val logEntriesMutex = Mutex()
    private var logEntries = mutableListOf<SyncLog>()

    override fun log(message: String, type: String, details: String): SyncLogger {
        // TODO Enable logging only if dev mode/sync logging is enabled
        logEntries.add(
            SyncLog(
                Clock.System.now(),
                logType = type,
                message = message,
                details = details,
            )
        )
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
                syncLogDao.insertSyncLogs(entries.map(SyncLog::asEntity))
            }
        }
    }
}

@Module
@InstallIn(SingletonComponent::class)
interface SyncLogModule {
    @Binds
    fun bindsSyncLogRepository(repository: SyncLogRepository): SyncLogger
}