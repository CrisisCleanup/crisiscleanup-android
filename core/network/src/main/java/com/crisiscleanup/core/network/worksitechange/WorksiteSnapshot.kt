package com.crisiscleanup.core.network.worksitechange

import com.crisiscleanup.core.network.model.*
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class WorksiteSnapshot(
    val core: CoreSnapshot,
    val flags: List<FlagSnapshot>,
    val notes: List<NoteSnapshot>,
    val workTypes: List<WorkTypeSnapshot>,
) {
    /**
     * @param noteIdLookup Local ID to network ID. Missing in map or non-positive network ID indicates not yet successfully synced to backend.
     */
    fun getNewNetworkNotes(noteIdLookup: Map<Long, Long>): List<Pair<Long, NetworkNote>> {
        return notes
            .filter { (noteIdLookup[it.localId] ?: -1) <= 0 }
            .filter { it.note.id <= 0 }
            .filter { it.note.note.isNotBlank() }
            .map {
                with(it.note) {
                    Pair(
                        it.localId,
                        NetworkNote(
                            id = null,
                            createdAt = createdAt,
                            isSurvivor = isSurvivor,
                            note = note,
                        ),
                    )
                }
            }
    }

    fun matchingWorkTypeOrNull(workTypeLocalId: Long) =
        workTypes.firstOrNull { it.localId == workTypeLocalId }
            ?.workType?.let { workType ->
                with(workType) {
                    if (id >= 0) {
                        NetworkWorkType(
                            id = id,
                            createdAt = createdAt,
                            orgClaim = orgClaim,
                            nextRecurAt = nextRecurAt,
                            phase = phase,
                            recur = recur,
                            status = status,
                            workType = workType.workType,
                        )
                    } else {
                        null
                    }
                }
            }
}

@Serializable
data class CoreSnapshot(
    val id: Long,
    val address: String,
    val autoContactFrequencyT: String,
    val caseNumber: String,
    val city: String,
    val county: String,
    val createdAt: Instant?,
    val email: String?,
    val favoriteId: Long?,
    // val flags: List<WorksiteFlag>,
    val formData: Map<String, DynamicValue>,
    val incidentId: Long,
    // Keys to a work type in workTypes
    val keyWorkTypeId: Long?,
    val latitude: Double,
    val longitude: Double,
    val name: String,
    val networkId: Long,
    // val notes: List<WorksiteNote> = emptyList(),
    val phone1: String,
    val phone2: String,
    val plusCode: String? = null,
    val postalCode: String,
    val reportedBy: Long?,
    val state: String,
    val svi: Float?,
    val updatedAt: Instant?,
    val what3Words: String? = null,
    // val workTypes: List<WorkType>,
    val isAssignedToOrgMember: Boolean,
) {
    val networkFormData: List<KeyDynamicValuePair>
        get() = formData.map {
            KeyDynamicValuePair(
                key = it.key,
                value = it.value,
            )
        }

    val pointLocation: NetworkWorksiteFull.Location
        get() = NetworkWorksiteFull.Location(
            type = "Point",
            coordinates = listOf(longitude, latitude)
        )
}

@Serializable
data class FlagSnapshot(
    val localId: Long,
    val flag: Flag,
) {
    @Serializable
    data class Flag(
        val id: Long,
        val action: String,
        val createdAt: Instant,
        val isHighPriority: Boolean,
        val notes: String,
        val reasonT: String,
        val reason: String,
        val requestedAction: String,
    )
}

fun FlagSnapshot.asNetworkFlag(): NetworkFlag {
    with(flag) {
        return NetworkFlag(
            id = if (id > 0) id else null,
            action = action.ifBlank { null },
            createdAt = createdAt,
            isHighPriority = isHighPriority,
            notes = notes.ifBlank { null },
            reasonT = reasonT,
            requestedAction = requestedAction.ifBlank { null },
        )
    }
}

@Serializable
data class NoteSnapshot(
    val localId: Long,
    val note: Note,
) {
    @Serializable
    data class Note(
        val id: Long,
        val createdAt: Instant,
        val isSurvivor: Boolean,
        val note: String,
    )
}

@Serializable
data class WorkTypeSnapshot(
    val localId: Long,
    val workType: WorkType
) {
    @Serializable
    data class WorkType(
        val id: Long,
        val createdAt: Instant? = null,
        val orgClaim: Long? = null,
        val nextRecurAt: Instant? = null,
        val phase: Int? = null,
        val recur: String? = null,
        val status: String,
        val workType: String,
    ) {
        fun changeFrom(reference: WorkType, localId: Long, changedAt: Instant): WorkTypeChange? {
            if (workType.trim() != reference.workType.trim()) {
                return null
            }

            return WorkTypeChange(
                localId,
                -1,
                this,
                changedAt,
                isClaimChange = orgClaim != reference.orgClaim,
                isStatusChange = status.trim() != reference.status.trim(),
            )
        }
    }
}

data class WorkTypeChange(
    val localId: Long,
    val networkId: Long,
    val workType: WorkTypeSnapshot.WorkType,
    val changedAt: Instant,
    val isClaimChange: Boolean,
    val isStatusChange: Boolean,
    val hasChange: Boolean = isClaimChange || isStatusChange
)
