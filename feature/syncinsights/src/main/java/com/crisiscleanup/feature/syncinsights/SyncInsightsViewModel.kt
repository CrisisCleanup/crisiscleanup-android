package com.crisiscleanup.feature.syncinsights

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crisiscleanup.core.common.di.ApplicationScope
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.common.network.CrisisCleanupDispatchers.IO
import com.crisiscleanup.core.common.network.Dispatcher
import com.crisiscleanup.core.common.relativeTime
import com.crisiscleanup.core.common.sync.SyncPusher
import com.crisiscleanup.core.data.repository.SyncLogRepository
import com.crisiscleanup.core.data.repository.WorksiteChangeRepository
import com.crisiscleanup.core.data.repository.WorksitesRepository
import com.crisiscleanup.core.model.data.SyncLog
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SyncInsightsViewModel @Inject constructor(
    private val syncLogRepository: SyncLogRepository,
    private val worksitesRepository: WorksitesRepository,
    worksiteChangeRepository: WorksiteChangeRepository,
    private val syncPusher: SyncPusher,
    @ApplicationScope private val externalScope: CoroutineScope,
    @Dispatcher(IO) private val ioDispatcher: CoroutineDispatcher,
    @Logger(CrisisCleanupLoggers.App) private val logger: AppLogger,
) : ViewModel() {
    val worksitesPendingSync = worksiteChangeRepository.streamWorksitesPendingSync.mapLatest {
        it.map { worksite -> "(${worksite.incidentId}, ${worksite.id}) ${worksite.caseNumber}" }
    }
        .stateIn(
            scope = viewModelScope,
            initialValue = emptyList(),
            started = SharingStarted.WhileSubscribed(),
        )

    val isSyncing = worksiteChangeRepository.syncingWorksiteIds.mapLatest {
        it.isNotEmpty()
    }

    private val logSliceCount = 80
    private val logSliceCountHalf = (logSliceCount * 0.5f).toInt()
    val listBlockSize = 20

    private val queryLogState = MutableStateFlow(Pair(0, 0))

    val openWorksiteId = mutableStateOf(Pair(0L, 0L))

    val syncLogs = queryLogState
        .mapLatest { (startIndex, totalCount) ->
            val logs = syncLogRepository.getLogs(
                logSliceCount,
                (startIndex - logSliceCountHalf).coerceAtLeast(0),
            )

            val logItems = logs.mapIndexed { index, log ->
                val isContinuingLogType = index > 0 && logs[index - 1].logType == log.logType
                SyncLogItem(log, isContinuingLogType, log.logTime.relativeTime)
            }
            LogListQueryState(startIndex, totalCount, logItems)
        }
        .flowOn(ioDispatcher)
        .stateIn(
            scope = viewModelScope,
            initialValue = LogListQueryState(0, 0, emptyList()),
            started = SharingStarted.WhileSubscribed(),
        )

    init {
        syncLogRepository.streamLogCount()
            .onEach { totalCount ->
                queryLogState.value = Pair(syncLogs.value.startIndex, totalCount)
            }
            .flowOn(ioDispatcher)
            .launchIn(viewModelScope)
    }

    fun syncPending() {
        if (worksitesPendingSync.value.isNotEmpty()) {
            externalScope.launch {
                syncPusher.syncPushWorksitesAsync().await()
            }
        }
    }

    fun onListBlockPosition(blockPosition: Int) {
        with(syncLogs.value) {
            var position = blockPosition * listBlockSize
            position = (position - worksitesPendingSync.value.size - 2).coerceAtLeast(0)
            if (isBoundaryPosition(position)) {
                queryLogState.value = Pair(position, count)
            }
        }
    }

    private val worksiteLogIdCapture = Regex("worksite-(\\w+-)?(\\d+)-")

    fun onExpandLog(log: SyncLog) {
        worksiteLogIdCapture.find(log.logType)?.let { match ->
            viewModelScope.launch(ioDispatcher) {
                try {
                    if (match.groups.size > 1) {
                        match.groups[2]?.let { matchGroup ->
                            val worksiteId = matchGroup.value.toLong()
                            val localWorksite =
                                worksitesRepository.streamLocalWorksite(worksiteId).first()
                            val unsyncedCounts = worksitesRepository.getUnsyncedCounts(worksiteId)
                            logger.logDebug("Worksite $localWorksite")
                            logger.logDebug("Unsynced $unsyncedCounts")
                            openWorksiteId.value =
                                Pair(localWorksite!!.worksite.incidentId, worksiteId)
                        }
                    }
                } catch (e: Exception) {
                    logger.logException(e)
                }
            }
        }
    }
}

data class LogListQueryState(
    val startIndex: Int,
    val count: Int,
    private val data: List<SyncLogItem>,
    private val endIndex: Int = startIndex + data.size,
) {
    private val boundaryPositions = Pair(
        (startIndex + data.size * 0.3f).toInt(),
        (startIndex + data.size * 0.7f).toInt(),
    )

    private val hasInnerBoundary = boundaryPositions.first < boundaryPositions.second

    fun isBoundaryPosition(position: Int) = hasInnerBoundary &&
        (position < boundaryPositions.first || position > boundaryPositions.second)

    fun getLog(index: Int) = if (index in startIndex until endIndex) {
        data[index - startIndex]
    } else {
        null
    }
}

data class SyncLogItem(
    val syncLog: SyncLog,
    val isContinuingLogType: Boolean,
    val relativeTime: String,
)
