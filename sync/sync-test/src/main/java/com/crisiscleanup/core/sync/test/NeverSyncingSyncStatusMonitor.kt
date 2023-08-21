package com.crisiscleanup.core.sync.test

import com.crisiscleanup.core.data.util.SyncStatusMonitor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

class NeverSyncingSyncStatusMonitor @Inject constructor() : SyncStatusMonitor {
    override val isSyncing: Flow<Boolean> = flowOf(false)
}
