package com.crisiscleanup.sync.di

import com.crisiscleanup.core.common.Syncer
import com.crisiscleanup.core.data.util.SyncStatusMonitor
import com.crisiscleanup.sync.AppSyncer
import com.crisiscleanup.sync.status.WorkManagerSyncStatusMonitor
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface SyncModule {
    @Binds
    fun bindsSyncStatusMonitor(syncStatusMonitor: WorkManagerSyncStatusMonitor): SyncStatusMonitor

    @Singleton
    @Binds
    fun bindsSyncer(syncer: AppSyncer): Syncer
}
