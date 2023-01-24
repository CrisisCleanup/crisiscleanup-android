package com.crisiscleanup.core.testing.util

import com.crisiscleanup.core.data.util.SyncStatusMonitor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class TestSyncStatusMonitor : SyncStatusMonitor {

    private val syncStatusFlow = MutableStateFlow(false)

    override val isSyncing: Flow<Boolean> = syncStatusFlow

    /**
     * A test-only API to set the sync status from tests.
     */
    fun setSyncing(isSyncing: Boolean) {
        syncStatusFlow.value = isSyncing
    }
}
