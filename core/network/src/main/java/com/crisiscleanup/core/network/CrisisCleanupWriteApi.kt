package com.crisiscleanup.core.network

import com.crisiscleanup.core.network.model.NetworkFlag
import com.crisiscleanup.core.network.model.NetworkNote
import com.crisiscleanup.core.network.model.NetworkType
import com.crisiscleanup.core.network.model.NetworkWorkType
import com.crisiscleanup.core.network.model.NetworkWorksiteFull
import com.crisiscleanup.core.network.model.NetworkWorksitePush
import kotlinx.datetime.Instant

interface CrisisCleanupWriteApi {
    suspend fun saveWorksite(
        modifiedAt: Instant,
        syncUuid: String,
        worksite: NetworkWorksitePush,
    ): NetworkWorksiteFull

    suspend fun favoriteWorksite(createdAt: Instant, worksiteId: Long): NetworkType
    suspend fun unfavoriteWorksite(createdAt: Instant, worksiteId: Long, favoriteId: Long)
    suspend fun addFlag(createdAt: Instant, worksiteId: Long, flag: NetworkFlag): NetworkFlag
    suspend fun deleteFlag(createdAt: Instant, worksiteId: Long, flagId: Long)
    suspend fun addNote(worksiteId: Long, note: String): NetworkNote
    suspend fun updateWorkTypeStatus(
        createdAt: Instant,
        workTypeId: Long,
        status: String,
    ): NetworkWorkType

    suspend fun claimWorkTypes(createdAt: Instant, worksiteId: Long, workTypes: Collection<String>)
    suspend fun unclaimWorkTypes(
        createdAt: Instant,
        worksiteId: Long,
        workTypes: Collection<String>,
    )

    suspend fun requestWorkTypes(worksiteId: Long, workTypes: List<String>, reason: String)
    suspend fun releaseWorkTypes(worksiteId: Long, workTypes: List<String>, reason: String)
}
