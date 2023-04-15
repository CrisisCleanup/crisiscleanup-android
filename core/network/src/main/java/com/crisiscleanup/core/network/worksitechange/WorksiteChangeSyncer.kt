package com.crisiscleanup.core.network.worksitechange

import com.crisiscleanup.core.common.AppEnv
import com.crisiscleanup.core.common.NetworkMonitor
import com.crisiscleanup.core.common.sync.SyncLogger
import com.crisiscleanup.core.model.data.AccountData
import com.crisiscleanup.core.model.data.SavedWorksiteChange
import com.crisiscleanup.core.model.data.WorksiteSyncResult
import com.crisiscleanup.core.network.CrisisCleanupNetworkDataSource
import com.crisiscleanup.core.network.CrisisCleanupWriteApi
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import javax.inject.Inject

interface WorksiteChangeSyncer {
    suspend fun sync(
        accountData: AccountData,
        startingReferenceChange: SavedWorksiteChange,
        sortedChanges: List<SavedWorksiteChange>,
        hasPriorUnsyncedChanges: Boolean = false,
        worksiteNetworkId: Long,
        flagIdLookup: Map<Long, Long>,
        noteIdLookup: Map<Long, Long>,
        workTypeIdLookup: Map<Long, Long>,
        syncLogger: SyncLogger,
    ): WorksiteSyncResult
}

class NetworkWorksiteChangeSyncer @Inject constructor(
    private val changeSetOperator: WorksiteChangeSetOperator,
    private val networkDataSource: CrisisCleanupNetworkDataSource,
    private val writeApiClient: CrisisCleanupWriteApi,
    private val networkMonitor: NetworkMonitor,
    private val appEnv: AppEnv,
) : WorksiteChangeSyncer {
    private fun deserializeChanges(savedChange: SavedWorksiteChange): SyncWorksiteChange {
        val worksiteChange: WorksiteChange = when (val version = savedChange.dataVersion) {
            1 -> Json.decodeFromString(savedChange.serializedData)
            else -> error("Worksite change version $version not implemented")
        }
        return SyncWorksiteChange(
            savedChange.id,
            savedChange.createdAt,
            savedChange.syncUuid,
            savedChange.isPartiallySynced,
            worksiteChange,
        )
    }

    override suspend fun sync(
        accountData: AccountData,
        startingReferenceChange: SavedWorksiteChange,
        sortedChanges: List<SavedWorksiteChange>,
        hasPriorUnsyncedChanges: Boolean,
        worksiteNetworkId: Long,
        flagIdLookup: Map<Long, Long>,
        noteIdLookup: Map<Long, Long>,
        workTypeIdLookup: Map<Long, Long>,
        syncLogger: SyncLogger,
    ): WorksiteSyncResult {
        val syncManager = WorksiteChangeProcessor(
            changeSetOperator,
            networkDataSource,
            writeApiClient,
            accountData,
            networkMonitor,
            appEnv,
            syncLogger,
            hasPriorUnsyncedChanges,
            worksiteNetworkId,
            flagIdLookup,
            noteIdLookup,
            workTypeIdLookup,
        )
        val changes = sortedChanges.map { deserializeChanges(it) }
        syncManager.process(
            deserializeChanges(startingReferenceChange),
            changes,
        )
        return syncManager.syncResult
    }
}

@Module
@InstallIn(SingletonComponent::class)
interface WorksiteChangeSyncerModule {
    @Binds
    fun bindsWorksiteChangeSyncer(syncer: NetworkWorksiteChangeSyncer): WorksiteChangeSyncer
}
