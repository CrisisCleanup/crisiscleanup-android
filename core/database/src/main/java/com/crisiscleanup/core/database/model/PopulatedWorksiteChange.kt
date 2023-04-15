package com.crisiscleanup.core.database.model

import androidx.room.Embedded
import com.crisiscleanup.core.model.data.SavedWorksiteChange

data class PopulatedWorksiteChange(
    @Embedded
    val entity: WorksiteChangeEntity,
)

fun PopulatedWorksiteChange.asExternalModel(maxSyncLimit: Int = 3) = SavedWorksiteChange(
    id = entity.id,
    syncUuid = entity.syncUuid,
    createdAt = entity.createdAt,
    organizationId = entity.organizationId,
    worksiteId = entity.worksiteId,
    dataVersion = entity.changeModelVersion,
    serializedData = entity.changeData,
    saveAttempt = entity.saveAttempt,
    archiveActionLiteral = entity.archiveAction,
    stopSyncing = entity.saveAttempt >= maxSyncLimit,
)
