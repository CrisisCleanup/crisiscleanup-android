package com.crisiscleanup.core.data

import com.crisiscleanup.core.database.dao.WorksiteChangeDao
import com.crisiscleanup.core.model.data.closedWorkTypeStatuses
import com.crisiscleanup.core.model.data.statusFromLiteral
import com.crisiscleanup.core.network.worksitechange.WorkTypeSnapshot
import com.crisiscleanup.core.network.worksitechange.WorksiteChange
import kotlinx.serialization.json.Json
import javax.inject.Inject

interface WorkTypeAnalyzer {
    fun countUnsyncedClaimCloseWork(
        orgId: Long,
        incidentId: Long,
        ignoreWorksiteIds: Set<Long>,
    ): ClaimCloseCounts
}

private val WorkTypeSnapshot.WorkType.isClosed: Boolean
    get() {
        val workTypeStatus = statusFromLiteral(status)
        return closedWorkTypeStatuses.contains(workTypeStatus)
    }

class WorksiteChangeWorkTypeAnalyzer @Inject constructor(
    private val worksiteChangeDao: WorksiteChangeDao,
) : WorkTypeAnalyzer {
    override fun countUnsyncedClaimCloseWork(
        orgId: Long,
        incidentId: Long,
        ignoreWorksiteIds: Set<Long>,
    ): ClaimCloseCounts {
        val worksiteChangesLookup = mutableMapOf<Long, Pair<String, String?>>()
        worksiteChangeDao.getOrgChanges(orgId)
            .filter {
                !ignoreWorksiteIds.contains(it.entity.worksiteId)
            }
            .onEach {
                with(it.entity) {
                    val entry = worksiteChangesLookup[worksiteId]
                    worksiteChangesLookup[worksiteId] = if (entry == null) {
                        Pair(changeData, null)
                    } else {
                        Pair(entry.first, changeData)
                    }
                }
            }

        val workTypeChanges =
            mutableListOf<Pair<WorkTypeSnapshot.WorkType, WorkTypeSnapshot.WorkType?>>()

        val json = Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }
        worksiteChangesLookup.forEach { entry ->
            val (firstSerializedChange, lastSerializedChange) = entry.value
            val firstChange = json.decodeFromString<WorksiteChange>(firstSerializedChange)
            firstChange.start?.let { firstSnapshot ->
                if (firstSnapshot.core.networkId > 0) {
                    val lastSnapshot = lastSerializedChange?.let {
                        json.decodeFromString<WorksiteChange>(lastSerializedChange).change
                    } ?: firstChange.change
                    if (lastSnapshot.core.incidentId == incidentId) {
                        val startWorkLookup = firstSnapshot.workTypes.associateBy { it.localId }
                        val lastWorkLookup = lastSnapshot.workTypes.associateBy { it.localId }
                        for ((id, startWorkType) in startWorkLookup) {
                            // TODO Test coverage on last work type is null
                            val lastWorkType = lastWorkLookup[id]?.workType
                            val change = Pair(startWorkType.workType, lastWorkType)
                            workTypeChanges.add(change)
                        }
                    }
                }
            }
        }

        var claimCount = 0
        var closeCount = 0
        for ((startWorkType, lastWorkType) in workTypeChanges) {
            val wasClaimed = startWorkType.orgClaim == orgId
            val isClaimed = lastWorkType?.orgClaim == orgId
            if (wasClaimed != isClaimed) {
                claimCount += if (isClaimed) 1 else -1

                if (isClaimed && lastWorkType.isClosed) {
                    closeCount++
                }
            } else if (isClaimed) {
                val wasClosed = startWorkType.isClosed
                val isClosed = lastWorkType.isClosed
                if (wasClosed != isClosed) {
                    closeCount += if (isClosed) 1 else -1
                }
            }
        }

        return ClaimCloseCounts(claimCount = claimCount, closeCount = closeCount)
    }
}

data class ClaimCloseCounts(
    val claimCount: Int,
    val closeCount: Int,
)
