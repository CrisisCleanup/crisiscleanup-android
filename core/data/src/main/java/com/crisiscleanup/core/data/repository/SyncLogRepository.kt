package com.crisiscleanup.core.data.repository

import android.util.Log
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.crisiscleanup.core.common.AppEnv
import com.crisiscleanup.core.common.di.ApplicationScope
import com.crisiscleanup.core.common.network.CrisisCleanupDispatchers.IO
import com.crisiscleanup.core.common.network.Dispatcher
import com.crisiscleanup.core.common.sync.SyncLogger
import com.crisiscleanup.core.data.IncidentSelector
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import kotlin.time.Clock

interface SyncLogRepository {
    fun streamLogCount(): Flow<Int>

    fun pageLogs(): Flow<PagingData<SyncLog>>

    fun trimOldLogs()
}

class PagingSyncLogRepository @Inject constructor(
    private val syncLogDao: SyncLogDao,
    private val appEnv: AppEnv,
    private val incidentSelector: IncidentSelector,
    @ApplicationScope private val coroutineScope: CoroutineScope,
    @Dispatcher(IO) private val ioDispatcher: CoroutineDispatcher,
) : SyncLogger, SyncLogRepository {
    private val logEntriesMutex = Mutex()
    private var logEntries = mutableListOf<SyncLog>()

    private val syncLogPager = Pager(
        config = PagingConfig(pageSize = 30),
        pagingSourceFactory = {
            syncLogDao.pageSyncLogs()
        },
    )

    private val isLogging: Boolean
        get() = appEnv.isNotProduction || incidentSelector.incidentId.value == 171L

    override var type = ""

    override fun log(message: String, details: String, type: String): SyncLogger {
        // TODO Log if sync logging is enabled
        if (isLogging) {
            logEntries.add(
                SyncLog(
                    0,
                    Clock.System.now(),
                    logType = type.ifEmpty { this.type },
                    message = message,
                    details = details,
                ),
            )
        }
        return this
    }

    override fun clear(): SyncLogger {
        coroutineScope.launch(ioDispatcher) {
            logEntriesMutex.withLock {
                logEntries = mutableListOf()
            }
        }
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

    override fun pageLogs() = syncLogPager.flow.map {
        it.map(PopulatedSyncLog::asExternalModel)
    }

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
