package com.crisiscleanup.core.sync.test

import com.crisiscleanup.core.data.util.SyncStatusMonitor
import com.crisiscleanup.sync.di.SyncModule
import dagger.Binds
import dagger.Module
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [SyncModule::class],
)
interface TestSyncModule {
    @Binds
    fun bindsSyncStatusMonitor(syncStatusMonitor: NeverSyncingSyncStatusMonitor): SyncStatusMonitor
}
