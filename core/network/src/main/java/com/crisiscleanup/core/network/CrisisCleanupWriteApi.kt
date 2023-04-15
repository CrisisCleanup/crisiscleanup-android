package com.crisiscleanup.core.network

import com.crisiscleanup.core.network.model.*
import kotlinx.datetime.Instant

interface CrisisCleanupWriteApi {
    suspend fun saveWorksite(
        modifiedAt: Instant,
        syncUuid: String,
        worksite: NetworkWorksitePush,
    ): NetworkWorksiteFull

    suspend fun favoriteWorksite(worksiteId: Long): NetworkType
    suspend fun unfavoriteWorksite(worksiteId: Long, favoriteId: Long)
    suspend fun addFlag(worksiteId: Long, flag: NetworkFlag): NetworkFlag
    suspend fun deleteFlag(worksiteId: Long, flagId: Long)
    suspend fun addNote(worksiteId: Long, note: String): NetworkNote
    suspend fun updateWorkTypeStatus(workTypeId: Long, status: String): NetworkWorkType
    suspend fun claimWorkTypes(worksiteId: Long, workTypes: Collection<String>)
    suspend fun unclaimWorkTypes(worksiteId: Long, workTypes: Collection<String>)
}
