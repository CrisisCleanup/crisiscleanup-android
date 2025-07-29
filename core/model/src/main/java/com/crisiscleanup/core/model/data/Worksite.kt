package com.crisiscleanup.core.model.data

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

enum class AutoContactFrequency(val literal: String) {
    None(""),
    Often("formOptions.often"),
    NotOften("formOptions.not_often"),
    Never("formOptions.never"),
}

private val autoContactFrequencyMap =
    AutoContactFrequency.entries.associateBy { it.literal }

data class Worksite(
    val id: Long,
    val address: String,
    val autoContactFrequencyT: String,
    val autoContactFrequency: AutoContactFrequency = autoContactFrequency(autoContactFrequencyT),
    val caseNumber: String,
    val city: String,
    val county: String,
    val createdAt: Instant?,
    val email: String? = null,
    val favoriteId: Long?,
    val files: List<NetworkImage> = emptyList(),
    val flags: List<WorksiteFlag>? = null,
    val formData: Map<String, WorksiteFormValue>? = null,
    val incidentId: Long,
    val keyWorkType: WorkType?,
    val latitude: Double,
    val longitude: Double,
    val name: String,
    val networkId: Long,
    val notes: List<WorksiteNote> = emptyList(),
    val phone1: String,
    val phone1Notes: String = "",
    val phone2: String,
    val phone2Notes: String = "",
    val plusCode: String? = null,
    val postalCode: String,
    val reportedBy: Long?,
    val state: String,
    val svi: Float?,
    val updatedAt: Instant?,
    val what3Words: String? = null,
    val workTypes: List<WorkType>,
    val workTypeRequests: List<WorkTypeRequest> = emptyList(),
    /**
     * Local state of favorite when editing a worksite
     *
     * Has precedent over [favoriteId]. If [favoriteId] is defined but this is false it means the favorite was untoggled (or member flag was unchecked).
     */
    val isAssignedToOrgMember: Boolean = false,
) {
    companion object {
        fun autoContactFrequency(literal: String) =
            autoContactFrequencyMap[literal] ?: AutoContactFrequency.None
    }

    val isNew = id <= 0

    val isLocalFavorite: Boolean
        get() = isAssignedToOrgMember

    val hasHighPriorityFlag: Boolean
        get() = flags?.any(WorksiteFlag::isHighPriorityFlag) ?: false
    val hasWrongLocationFlag: Boolean
        get() = flags?.any(WorksiteFlag::isWrongLocationFlag) ?: false
    val hasDuplicateFlag: Boolean
        get() = flags?.any(WorksiteFlag::isDuplicateFlag) ?: false

    val crossStreetNearbyLandmark: String
        get() = formData?.get(CROSS_STREET_FIELD_KEY)?.valueString ?: ""

    private fun toggleFlag(flag: WorksiteFlagType): Worksite {
        val flagReason = flag.literal
        val toggledFlags = if (flags?.any { it.reasonT == flagReason } == true) {
            flags.filterNot { it.reasonT == flagReason }
        } else {
            val addFlag = WorksiteFlag.flag(flagReason)
            flags?.toMutableList()?.apply { add(addFlag) } ?: listOf(addFlag)
        }
        return copy(flags = toggledFlags)
    }

    fun toggleHighPriorityFlag() = toggleFlag(WorksiteFlagType.HighPriority)

    val isReleaseEligible = createdAt?.let {
        Clock.System.now().minus(it) > WorkTypeReleaseDaysThreshold
    } ?: false
}

val EmptyWorksite = Worksite(
    id = -1L,
    address = "",
    autoContactFrequencyT = "",
    caseNumber = "",
    city = "",
    county = "",
    createdAt = Clock.System.now(),
    favoriteId = null,
    incidentId = EmptyIncident.id,
    keyWorkType = null,
    latitude = 0.0,
    longitude = 0.0,
    name = "",
    networkId = -1L,
    phone1 = "",
    phone2 = "",
    postalCode = "",
    reportedBy = null,
    state = "",
    svi = null,
    updatedAt = null,
    workTypes = emptyList(),
)

const val CROSS_STREET_FIELD_KEY = "cross_street"

data class WorksiteFormValue(
    val isBoolean: Boolean = false,
    val valueString: String,
    val valueBoolean: Boolean = false,
) {
    companion object {
        val trueValue = WorksiteFormValue(true, "", true)
    }

    val isBooleanTrue = isBoolean && valueBoolean
    val hasValue = isBooleanTrue || (!isBoolean && valueString.isNotBlank())
}

enum class WorksiteFlagType(val literal: String) {
    HighPriority("flag.worksite_high_priority"),
    UpsetClient("flag.worksite_upset_client"),
    MarkForDeletion("flag.worksite_mark_for_deletion"),
    ReportAbuse("flag.worksite_abuse"),
    Duplicate("flag.duplicate"),
    WrongLocation("flag.worksite_wrong_location"),
    WrongIncident("flag.worksite_wrong_incident"),
}

private val flagLiteralLookup = WorksiteFlagType.entries.associateBy(WorksiteFlagType::literal)

fun flagFromLiteral(literal: String) = flagLiteralLookup[literal]

data class WorksiteFlag(
    val id: Long,
    val action: String,
    val createdAt: Instant,
    val isHighPriority: Boolean,
    val notes: String,
    val reasonT: String,
    val reason: String,
    val requestedAction: String,
    val attr: FlagAttributes?,
) {
    companion object {
        internal fun flag(
            reasonT: String,
            reason: String = "",
            notes: String = "",
            requestedAction: String = "",
            isHighPriorityBool: Boolean = false,
        ) = WorksiteFlag(
            id = 0,
            action = "",
            createdAt = Clock.System.now(),
            isHighPriority = isHighPriorityBool,
            notes = notes,
            reasonT = reasonT,
            reason = reason,
            requestedAction = requestedAction,
            attr = null,
        )

        fun flag(
            flag: WorksiteFlagType,
            notes: String = "",
            requestedAction: String = "",
            isHighPriorityBool: Boolean = false,
        ) = flag(
            flag.literal,
            notes = notes,
            requestedAction = requestedAction,
            isHighPriorityBool = isHighPriorityBool,
        )

        fun highPriority() = flag(WorksiteFlagType.HighPriority)
        fun wrongLocation() = flag(WorksiteFlagType.WrongLocation)
    }

    val isHighPriorityFlag = reasonT == WorksiteFlagType.HighPriority.literal
    val isWrongLocationFlag = reasonT == WorksiteFlagType.WrongLocation.literal
    val isDuplicateFlag = reasonT == WorksiteFlagType.Duplicate.literal

    val flagType: WorksiteFlagType?
        get() = flagLiteralLookup[reasonT]

    data class FlagAttributes(
        // Upset client
        val involvesMyOrg: Boolean?,
        // Report abuse
        val haveContactedOtherOrg: Boolean?,
        // Upset client or report abuse
        val organizations: List<Long>,
    )
}

data class WorksiteNote(
    val id: Long,
    val createdAt: Instant,
    val isSurvivor: Boolean,
    val note: String,
) {
    companion object {
        fun create(isSurvivor: Boolean = false) = WorksiteNote(
            id = 0,
            createdAt = Clock.System.now(),
            isSurvivor = isSurvivor,
            note = "",
        )
    }
}

val Collection<WorksiteNote>.hasSurvivorNote: Boolean
    get() = any { it.isSurvivor }

enum class WorksiteSortBy(val literal: String, val translateKey: String) {
    None("", "worksiteFilters.sort_by"),
    CaseNumber("case-number", "worksiteFilters.sort_by_case_number"),
    Nearest("nearest", "worksiteFilters.sort_by_nearest"),
    Name("name", "worksiteFilters.sort_by_name"),
    City("city", "worksiteFilters.sort_by_city"),
    CountyParish("county-parish", "worksiteFilters.sort_by_county"),
}

private val sortByLookup = WorksiteSortBy.entries.associateBy(WorksiteSortBy::literal)

fun worksiteSortByFromLiteral(literal: String) = sortByLookup[literal] ?: WorksiteSortBy.None
