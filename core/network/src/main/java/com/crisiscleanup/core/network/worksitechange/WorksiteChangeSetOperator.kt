package com.crisiscleanup.core.network.worksitechange

import com.crisiscleanup.core.network.model.DynamicValue
import com.crisiscleanup.core.network.model.KeyDynamicValuePair
import com.crisiscleanup.core.network.model.NetworkFlag
import com.crisiscleanup.core.network.model.NetworkNote
import com.crisiscleanup.core.network.model.NetworkWorkType
import com.crisiscleanup.core.network.model.NetworkWorksiteFull
import com.crisiscleanup.core.network.model.NetworkWorksitePush
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

// Updates to below (and related) must pass regression tests.
// Think through any and all data changes carefully and completely in terms of
// - Local propagation
// - Global propagation
// - Local consistency
// - Global consistency
// - Fully applied deltas
// - Partially applied deltas

class WorksiteChangeSetOperator @Inject constructor() {
    // TODO Write tests
    fun getNewSet(snapshot: WorksiteSnapshot): WorksiteChangeSet {
        val coreB = snapshot.core

        val worksitePush = NetworkWorksitePush(
            id = null,
            address = coreB.address,
            autoContactFrequencyT = coreB.autoContactFrequencyT,
            caseNumber = null,
            city = coreB.city,
            county = coreB.county,
            email = coreB.email ?: "",
            // New does not have favorite. Member of my org makes a followup request.
            favorite = null,
            formData = coreB.networkFormData,
            incident = coreB.incidentId,
            keyWorkType = null,
            location = coreB.pointLocation,
            name = coreB.name,
            // Notes are followup requests.
            phone1 = coreB.phone1,
            phone2 = coreB.phone2,
            plusCode = coreB.plusCode,
            postalCode = coreB.postalCode,
            reportedBy = coreB.reportedBy,
            state = coreB.state,
            svi = null,
            updatedAt = coreB.updatedAt ?: Clock.System.now(),
            what3words = "",
            workTypes = null,

            skipDuplicateCheck = true,
            sendSms = true,
        )

        val workTypeChanges = snapshot.workTypes
            .filter { it.workType.orgClaim != null }
            .map {
                it.workType.claimNew(
                    it.localId,
                    it.workType.createdAt ?: worksitePush.updatedAt,
                )
            }

        return WorksiteChangeSet(
            worksitePush.updatedAt,
            worksitePush,
            if (coreB.isAssignedToOrgMember) true else null,
            snapshot.getNewNetworkNotes(emptyMap()),
            Pair(snapshot.flags.map { Pair(it.localId, it.asNetworkFlag()) }, emptyList()),
            workTypeChanges = workTypeChanges,
        )
    }

    // TODO Write tests
    fun getChangeSet(
        base: NetworkWorksiteFull,
        start: WorksiteSnapshot,
        change: WorksiteSnapshot,
        flagIdLookup: Map<Long, Long>,
        noteIdLookup: Map<Long, Long>,
        workTypeIdLookup: Map<Long, Long>,
    ): WorksiteChangeSet {
        val coreA = start.core
        val coreB = change.core

        val updatedAt = coreB.updatedAt ?: Clock.System.now()

        val (newWorkTypes, workTypeChanges, _) = base.getWorkTypeChanges(
            start.workTypes,
            change.workTypes,
            updatedAt,
            workTypeIdLookup,
        )

        // TODO Is this correct? And complete? Select one by default if no match?
        val keyWorkType =
            coreB.keyWorkTypeId?.let { localId -> change.matchingWorkTypeOrNull(localId) }

        val formDataPush = base.getFormDataChanges(coreA.formData, coreB.formData)
        val worksitePush = base.getCoreChange(coreA, coreB, formDataPush, keyWorkType, updatedAt)

        val isAssignedToOrgMember = base.getFavoriteChange(coreA, coreB)

        val addNotes = change.getNewNetworkNotes(noteIdLookup)
        val newNotes = base.filterDuplicateNotes(addNotes)

        val flagChanges = base.getFlagChanges(start.flags, change.flags, flagIdLookup)

        // TODO Review data consistency rules and guarantee correctness.
        //      This applies to both form data and work types.
        //      - At least one work type must be specified.
        //        This should not be an issue if local guarantees each snapshot has at least one work type.
        //      - Fallback to keeping at least one of the existing work types

        return WorksiteChangeSet(
            updatedAt,
            worksitePush,
            isAssignedToOrgMember,
            newNotes,
            flagChanges,
            newWorkTypes,
            workTypeChanges,
        )
    }
}

internal fun NetworkWorksiteFull.getCoreChange(
    coreA: CoreSnapshot,
    coreB: CoreSnapshot,
    formDataPush: List<KeyDynamicValuePair>,
    keyWorkTypePush: NetworkWorkType?,
    updatedAtPush: Instant,
): NetworkWorksitePush? {
    if (coreA.copy(
            updatedAt = coreB.updatedAt,
            isAssignedToOrgMember = coreB.isAssignedToOrgMember,
        ) == coreB
    ) {
        return null
    }

    val isLocationChange = coreA.latitude != coreB.latitude ||
        coreA.longitude != coreB.longitude
    val locationPush = if (isLocationChange) coreB.pointLocation else location

    // Pass explicit "" to clear a value on the backend
    return NetworkWorksitePush(
        id = id,
        address = address.change(coreA.address, coreB.address),
        autoContactFrequencyT = autoContactFrequencyT.change(
            coreA.autoContactFrequencyT,
            coreB.autoContactFrequencyT,
        ),
        caseNumber = caseNumber,
        city = city.change(coreA.city, coreB.city),
        county = baseChange(county, coreA.county, coreB.county) ?: "",
        email = baseChange(email, coreA.email, coreB.email) ?: "",
        // Member of my org/favorite change is performed in a followup call
        favorite = favorite,
        formData = formDataPush,
        incident = if (coreB.incidentId == coreA.incidentId) incident else coreB.incidentId,
        keyWorkType = keyWorkTypePush,
        location = locationPush,
        name = name.change(coreA.name, coreB.name),
        phone1 = phone1.change(coreA.phone1, coreB.phone1),
        phone2 = baseChange(phone2, coreA.phone2, coreB.phone2) ?: "",
        plusCode = baseChange(plusCode, coreA.plusCode, coreB.plusCode)?.ifBlank { null },
        postalCode = baseChange(postalCode, coreA.postalCode, coreB.postalCode),
        reportedBy = reportedBy,
        state = state.change(coreA.state, coreB.state),
        svi = svi,
        updatedAt = updatedAtPush,
        what3words = baseChange(what3words, coreA.what3Words, coreB.what3Words) ?: "",
        // TODO Review if this works
        workTypes = emptyList(),

        skipDuplicateCheck = true,
    )
}

internal fun NetworkWorksiteFull.getFavoriteChange(
    coreA: CoreSnapshot,
    coreB: CoreSnapshot,
): Boolean? {
    val isFavoriteA = coreA.isAssignedToOrgMember
    val isFavoriteB = coreB.isAssignedToOrgMember
    return if (isFavoriteA == isFavoriteB || isFavoriteB == (favorite != null)) {
        null
    } else {
        isFavoriteB
    }
}

/**
 * Determines changes in flags between snapshots relative to [NetworkWorksiteFull.flags]
 *
 * New flags are ignored if existing networked flags have matching [NetworkFlag.reasonT].
 * Local flags should assume networked flags where reason matches.
 *
 * Flags are marked for deletion only where existing networked flags have matching [NetworkFlag.reasonT].
 *
 * @param flagIdLookup Local ID to network ID. Missing in map or non-positive network ID indicates not yet successfully synced to backend.
 * @return New flags and existing flag IDs to delete.
 */
internal fun NetworkWorksiteFull.getFlagChanges(
    start: List<FlagSnapshot>,
    change: List<FlagSnapshot>,
    flagIdLookup: Map<Long, Long>,
): Pair<List<Pair<Long, NetworkFlag>>, Collection<Long>> {
    fun updateNetworkIds(snapshots: List<FlagSnapshot>) = snapshots.map {
        var snapshot = it
        if (it.flag.id <= 0) {
            flagIdLookup[it.localId]?.let { networkId ->
                snapshot = it.copy(
                    flag = it.flag.copy(
                        id = networkId,
                    ),
                )
            }
        }
        snapshot
    }

    val startUpdated = updateNetworkIds(start)
    val changeUpdated = updateNetworkIds(change)

    val startReasons = startUpdated.map { it.flag.reasonT }.toSet()
    val existingReasons = flags.map(NetworkFlag::reasonT).toSet()

    val newFlags = changeUpdated
        .filter { it.flag.id <= 0 }
        .filter { !existingReasons.contains(it.flag.reasonT) }
        .map { Pair(it.localId, it.asNetworkFlag()) }

    val keepReasons = changeUpdated.map { it.flag.reasonT }.toSet()
    val deleteReasons = startReasons
        .filter { !keepReasons.contains(it) }
        .toSet()
    val deleteFlagIds = flags
        .filter { deleteReasons.contains(it.reasonT) }
        // Incoming network ID is always defined
        .map { it.id!! }

    return Pair(newFlags, deleteFlagIds)
}

internal fun NetworkWorksiteFull.getFormDataChanges(
    start: Map<String, DynamicValue>,
    change: Map<String, DynamicValue>,
): List<KeyDynamicValuePair> {
    if (start == change) {
        return formData
    }

    val newFormData = change.mapNotNull {
        if (start.contains(it.key)) {
            null
        } else {
            KeyDynamicValuePair(it.key, it.value)
        }
    }
    val deletedFormData = start.mapNotNull {
        if (change.contains(it.key)) {
            null
        } else {
            it.key
        }
    }
    val crossChangeFormData = change.mapNotNull {
        if (start.contains(it.key)) {
            it
        } else {
            null
        }
    }
    val unchangedFormData = crossChangeFormData
        .mapNotNull { changeData ->
            val crossStartValue = start[changeData.key]!!
            val changeValue = changeData.value
            if (changeValue.isBooleanEqual(crossStartValue) ||
                changeValue.isStringEqual(crossStartValue)
            ) {
                changeData
            } else {
                null
            }
        }
        .associate { it.key to it.value }
    val changedFormData = crossChangeFormData
        .mapNotNull {
            if (unchangedFormData.contains(it.key)) {
                null
            } else {
                it
            }
        }
        .associate { it.key to it.value }

    if (deletedFormData.isEmpty() && newFormData.isEmpty() && changedFormData.isEmpty()) {
        return formData
    }

    val mutableFormData = formData
        .associate { it.key to it.value }
        .toMutableMap()
    deletedFormData.forEach { mutableFormData.remove(it) }
    newFormData.forEach { mutableFormData[it.key] = it.value }
    changedFormData.forEach { mutableFormData[it.key] = it.value }
    unchangedFormData.forEach {
        // No change between snapshots implies existing values are fine (even if non-existent).
        // If previous snapshots were applied successfully these cases shouldn't exist.
        // Ignore edge cases when this exists as trying to determine intention is highly improbable.
    }

    return mutableFormData.map {
        KeyDynamicValuePair(it.key, it.value)
    }
}

internal fun NetworkWorksiteFull.filterDuplicateNotes(
    addNotes: List<Pair<Long, NetworkNote>>,
    matchDuration: Duration = 12.hours,
): List<Pair<Long, NetworkNote>> {
    val existingNotes = notes.filter { it.note?.isNotEmpty() == true }
        .associateBy { it.note!!.trim().lowercase() }
    return addNotes.filter { (_, addNote) ->
        existingNotes[addNote.note?.trim()?.lowercase()]?.let { matchingNote ->
            val timeSpan = addNote.createdAt - matchingNote.createdAt
            if (timeSpan.absoluteValue < matchDuration) {
                return@filter false
            }
        }
        true
    }
}

internal fun NetworkWorksiteFull.getWorkTypeChanges(
    start: List<WorkTypeSnapshot>,
    change: List<WorkTypeSnapshot>,
    changedAt: Instant,
    workTypeIdLookup: Map<Long, Long> = emptyMap(),
): Triple<Map<String, WorkTypeChange>, List<WorkTypeChange>, Collection<Long>> {
    val existingWorkTypes = newestWorkTypes.associate {
        with(it) {
            val workTypeCopy = WorkTypeSnapshot.WorkType(
                // Incoming network ID is always defined
                id = id!!,
                createdAt = createdAt,
                orgClaim = orgClaim,
                nextRecurAt = nextRecurAt,
                phase = phase,
                recur = recur,
                status = status,
                workType = workType,
            )
            it.workType to workTypeCopy
        }
    }

    // TODO Add test where coverage is lacking. Start,change (not) in lookup,existing.
    fun updateNetworkIds(snapshots: List<WorkTypeSnapshot>) = snapshots.map {
        var snapshot = it
        var changeNetworkId = it.workType.id
        if (changeNetworkId <= 0) {
            workTypeIdLookup[it.localId]?.let { networkId ->
                changeNetworkId = networkId
            }
        }
        if (changeNetworkId <= 0) {
            existingWorkTypes[it.workType.workType]?.let { existingWorkType ->
                changeNetworkId = existingWorkType.id
            }
        }
        if (it.workType.id != changeNetworkId) {
            snapshot = it.copy(
                workType = it.workType.copy(
                    id = changeNetworkId,
                ),
            )
        }
        snapshot
    }

    val startMap = updateNetworkIds(start).associateBy { it.workType.workType }
    val changeMap = updateNetworkIds(change).associateBy { it.workType.workType }

    val newWorkTypes = changeMap
        .filter { it.value.workType.id <= 0 }
        .map {
            WorkTypeChange(
                it.value.localId,
                -1,
                it.value.workType,
                changedAt,
                isClaimChange = true,
                isStatusChange = true,
            )
        }

    val deletedWorkTypes = startMap
        .filter { !changeMap.contains(it.key) }
        .mapNotNull { existingWorkTypes[it.key]?.id }

    val changedWorkTypes = changeMap
        .mapNotNull {
            val localId = it.value.localId
            startMap[it.key]?.let { crossStartSnapshot ->
                it.value.workType.changeFrom(crossStartSnapshot.workType, localId, changedAt)
            } ?: existingWorkTypes[it.key]?.let { crossExisting ->
                it.value.workType.changeFrom(crossExisting, localId, changedAt)
            }
        }
        .filter(WorkTypeChange::hasChange)

    if (newWorkTypes.isEmpty() && deletedWorkTypes.isEmpty() && changedWorkTypes.isEmpty()) {
        return Triple(emptyMap(), emptyList(), emptyList())
    }

    val modified = newWorkTypes.associateBy { it.workType.workType }
        .toMutableMap()
        .apply {
            changedWorkTypes.forEach {
                this[it.workType.workType] = it
            }
        }
        .mapNotNull {
            val existingWorkType = existingWorkTypes[it.key]
            if (existingWorkType == null) {
                it.value
            } else {
                val localId = it.value.localId
                it.value.workType.changeFrom(existingWorkType, localId, changedAt)
                    ?.let { changeTo ->
                        val networkId = existingWorkType.id
                        changeTo.copy(
                            networkId = networkId,
                            workType = changeTo.workType.copy(
                                id = networkId,
                                createdAt = existingWorkType.createdAt,
                                nextRecurAt = existingWorkType.nextRecurAt,
                                phase = existingWorkType.phase,
                                recur = existingWorkType.recur,
                            ),
                        )
                    }
            }
        }
        .filter(WorkTypeChange::hasChange)
    val create = modified.filter { it.networkId <= 0 }
        .associateBy { it.workType.workType }
    val changing = modified.filter { it.networkId > 0 }

    return Triple(create, changing, deletedWorkTypes)
}
