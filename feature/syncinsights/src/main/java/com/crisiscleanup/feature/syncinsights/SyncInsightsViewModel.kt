package com.crisiscleanup.feature.syncinsights

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import com.crisiscleanup.core.common.di.ApplicationScope
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.common.network.CrisisCleanupDispatchers.IO
import com.crisiscleanup.core.common.network.Dispatcher
import com.crisiscleanup.core.common.sync.SyncPusher
import com.crisiscleanup.core.data.repository.SyncLogRepository
import com.crisiscleanup.core.data.repository.WorksiteChangeRepository
import com.crisiscleanup.core.data.repository.WorksitesRepository
import com.crisiscleanup.core.model.data.SyncLog
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SyncInsightsViewModel @Inject constructor(
    syncLogRepository: SyncLogRepository,
    private val worksitesRepository: WorksitesRepository,
    worksiteChangeRepository: WorksiteChangeRepository,
    private val syncPusher: SyncPusher,
    @ApplicationScope private val externalScope: CoroutineScope,
    @Dispatcher(IO) private val ioDispatcher: CoroutineDispatcher,
    @Logger(CrisisCleanupLoggers.Sync) private val logger: AppLogger,
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

    val openWorksiteId = mutableStateOf(Pair(0L, 0L))

    val syncLogs = syncLogRepository.pageLogs()
        .flowOn(ioDispatcher)
        .cachedIn(viewModelScope)

    fun syncPending() {
        if (worksitesPendingSync.value.isNotEmpty()) {
            externalScope.launch {
                syncPusher.syncPushWorksitesAsync().await()
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
