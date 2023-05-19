package com.crisiscleanup.core.network.worksitechange

import com.crisiscleanup.core.network.CrisisCleanupWriteApi
import javax.inject.Inject

class WorksitePhotoChangeSyncer @Inject constructor(
    private val writeApiClient: CrisisCleanupWriteApi,
) {
    suspend fun deletePhotoFiles(
        networkWorksiteId: Long,
        deleteFileIds: List<Long>,
    ) {
        deleteFileIds.filter { it > 0 }
            .forEach { writeApiClient.deleteFile(networkWorksiteId, it) }
    }
}