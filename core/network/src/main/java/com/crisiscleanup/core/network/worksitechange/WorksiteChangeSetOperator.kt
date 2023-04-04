package com.crisiscleanup.core.network.worksitechange

import com.crisiscleanup.core.network.model.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import javax.inject.Inject

// Updates to below (and related) must pass regression tests.
// Think through any and all data changes carefully and completely in terms of
// - Local propagation
// - Global propagation
// - Local consistency
// - Global consistency
// - Fully applied deltas
// - Partially applied deltas

class WorksiteChangeSetOperator @Inject constructor() {
    fun getNewSet(snapshot: WorksiteSnapshot): WorksiteChangeSet {
        val coreB = snapshot.core

        val worksitePush = NetworkWorksitePush(
            id = null,
            address = coreB.address,
            autoContactFrequencyT = coreB.autoContactFrequencyT,
            caseNumber = null,
            city = coreB.city,
            county = coreB.county,
            email = coreB.email,
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
            what3words = null,
            workTypes = null,

            skipDuplicateCheck = true,
            sendSms = true,
        )

        return WorksiteChangeSet(
            worksitePush.updatedAt,
            worksitePush,
            if (coreB.isAssignedToOrgMember) true else null,
            snapshot.getNewNetworkNotes(emptyMap()),
            Pair(snapshot.flags.map(FlagSnapshot::asNetworkFlag), emptyList()),
        )
    }

    fun getChangeSet(
        base: NetworkWorksiteFull,
        start: WorksiteSnapshot,
        change: WorksiteSnapshot,
        flagIdLookup: Map<Long, Long>,
        noteIdLookup: Map<Long, Long>,
    ): WorksiteChangeSet {
        val coreA = start.core
        val coreB = change.core

        val updatedAt = coreB.updatedAt ?: Clock.System.now()

        // TODO Is this correct? And complete? Select one by default if no match?
        val keyWorkType =
            coreB.keyWorkTypeId?.let { localId -> change.matchingWorkTypeOrNull(localId) }

        val formDataPush = base.getFormDataChanges(coreA.formData, coreB.formData)
        val worksitePush = base.getCoreChange(coreA, coreB, formDataPush, keyWorkType, updatedAt)

        val isAssignedToOrgMember = base.getFavoriteChange(coreA, coreB)

        val addNotes = change.getNewNetworkNotes(noteIdLookup)

        val flagChanges = base.getFlagChanges(start.flags, change.flags, flagIdLookup)

        val workTypeChanges = base.getWorkTypeChanges(start.workTypes, change.workTypes, updatedAt)

        return WorksiteChangeSet(
            updatedAt,
            worksitePush,
            isAssignedToOrgMember,
            addNotes,
            flagChanges,
            workTypeChanges,
        )
    }
}

internal fun NetworkWorksiteFull.getCoreChange(
    coreA: CoreSnapshot,
    coreB: CoreSnapshot,
    formDataPush: List<KeyDynamicValuePair>,
    keyWorkTypePush: NetworkWorksiteFull.WorkType?,
    updatedAtPush: Instant,
): NetworkWorksitePush? {
    if (coreA.copy(updatedAt = coreB.updatedAt) == coreB) {
        return null
    }

    val isLocationChange = coreA.latitude != coreB.latitude ||
            coreA.longitude != coreB.longitude
    val locationPush = if (isLocationChange) coreB.pointLocation else location

    return NetworkWorksitePush(
        id = id,
        address = address.change(coreA.address, coreB.address),
        autoContactFrequencyT = autoContactFrequencyT.change(
            coreA.autoContactFrequencyT,
            coreB.autoContactFrequencyT,
        ),
        caseNumber = caseNumber,
        city = city.change(coreA.city, coreB.city),
        county = county.change(coreA.county, coreB.county),
        email = baseChange(email, coreA.email, coreB.email)?.ifEmpty { null },
        // Member of my org/favorite change is performed in a followup call
        favorite = favorite,
        formData = formDataPush,
        incident = incident,
        keyWorkType = keyWorkTypePush,
        location = locationPush,
        name = name.change(coreA.name, coreB.name),
        phone1 = phone1.change(coreA.phone1, coreB.phone1),
        phone2 = baseChange(phone2, coreA.phone2, coreB.phone2)?.ifEmpty { null },
        plusCode = baseChange(plusCode, coreA.plusCode, coreB.plusCode)?.ifEmpty { null },
        postalCode = baseChange(postalCode, coreA.postalCode, coreB.postalCode),
        reportedBy = reportedBy,
        state = state.change(coreA.state, coreB.state),
        svi = svi,
        updatedAt = updatedAtPush,
        what3words = baseChange(what3words, coreA.what3Words, coreB.what3Words)?.ifEmpty { null },
        // TODO Review if this works
        workTypes = emptyList(),

        skipDuplicateCheck = true,
    )
}

internal fun NetworkWorksiteFull.getFavoriteChange(
    coreA: CoreSnapshot,
    coreB: CoreSnapshot,
): Boolean? {
    val isFavoriteA = coreA.isAssignedToOrgMember || coreA.favoriteId != null
    val isFavoriteB = coreB.isAssignedToOrgMember || coreB.favoriteId != null
    return if (isFavoriteA == isFavoriteB || isFavoriteB == (favorite != null)) null
    else isFavoriteB
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
): Pair<List<NetworkFlag>, Collection<Long>> {
    fun updateLocalFlags(snapshots: List<FlagSnapshot>) = snapshots.map {
        var snapshot = it
        if (it.flag.id <= 0) {
            flagIdLookup[it.localId]?.let { networkId ->
                snapshot = it.copy(
                    flag = it.flag.copy(
                        id = networkId,
                    )
                )
            }
        }
        snapshot
    }

    val startUpdated = updateLocalFlags(start)
    val changeUpdated = updateLocalFlags(change)

    val existingFlagReasonIdMap = flags.associate {
        it.reasonT to it.id
    }

    val newFlags = changeUpdated
        .filter { it.flag.id <= 0 }
        .filter { !existingFlagReasonIdMap.contains(it.flag.reasonT) }
        .map(FlagSnapshot::asNetworkFlag)

    val keepFlagReasons = changeUpdated
        .map { it.flag.reasonT }
        .toSet()
    val deleteFlagReasons = startUpdated
        .map { it.flag.reasonT }
        .filter { !keepFlagReasons.contains(it) }
        .toSet()
    val deleteFlagIds = flags
        .filter { deleteFlagReasons.contains(it.reasonT) }
        .mapNotNull { it.id }

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
        if (start.contains(it.key)) null
        else KeyDynamicValuePair(it.key, it.value)
    }
    val deletedFormData = start.mapNotNull {
        if (change.contains(it.key)) null
        else it.key
    }
    val crossChangeFormData = change.mapNotNull {
        if (start.contains(it.key)) it
        else null
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

    // TODO Data consistency rules
    //      - At least one work type must be specified.
    //        This should not be an issue if local guarantees each snapshot has at least one work type.
    //      - Any other?

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

internal fun NetworkWorksiteFull.getWorkTypeChanges(
    start: List<WorkTypeSnapshot>,
    change: List<WorkTypeSnapshot>,
    changedAt: Instant,
): Triple<List<WorkTypeSnapshot.WorkType>, List<WorkTypeChange>, Collection<Long>> {
    if (start == change) {
        return Triple(emptyList(), emptyList(), emptyList())
    }

    val existingWorkTypes = workTypes.associate {
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

    val startMap = start.associateBy { it.workType.workType }
    val changeMap = change.associateBy { it.workType.workType }

    val newWorkTypes = changeMap
        .mapNotNull {
            if (startMap.contains(it.key)) null
            else it
        }
        .map {
            WorkTypeChange(
                -1,
                it.value.workType,
                changedAt,
                isClaimChange = true,
                isStatusChange = true,
            )
        }
    val deletedWorkTypes = startMap
        .mapNotNull {
            if (changeMap.contains(it.key)) null
            else it
        }
        .mapNotNull { existingWorkTypes[it.key]?.id }
    val changedWorkTypes = changeMap
        .mapNotNull {
            if (startMap.contains(it.key)) it
            else null
        }
        .mapNotNull {
            val crossStartValue = startMap[it.key]!!
            it.value.workType.changeFrom(crossStartValue.workType, changedAt)
        }
        .filter(WorkTypeChange::hasChange)

    if (newWorkTypes.isEmpty() && deletedWorkTypes.isEmpty() && changedWorkTypes.isEmpty()) {
        return Triple(emptyList(), emptyList(), emptyList())
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
                it.value.workType.changeFrom(existingWorkType, changedAt)?.let { changeTo ->
                    val networkId = existingWorkType.id
                    changeTo.copy(
                        networkId = networkId,
                        workType = changeTo.workType.copy(
                            id = networkId,
                            createdAt = existingWorkType.createdAt,
                            nextRecurAt = existingWorkType.nextRecurAt,
                            phase = existingWorkType.phase,
                            recur = existingWorkType.recur,
                        )
                    )
                }
            }
        }
        .filter(WorkTypeChange::hasChange)
    val create = modified.filter { it.networkId <= 0 }.map { it.workType }
    val changing = modified.filter { it.networkId > 0 }

    return Triple(create, changing, deletedWorkTypes)
}