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
    AutoContactFrequency.values().map { it.literal to it }.toMap()

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
    val phone2: String,
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

    val crossStreetNearbyLandmark: String
        get() = formData?.get(CROSS_STREET_FIELD_KEY)?.valueString ?: ""

    private fun toggleFlag(flagReason: String): Worksite {
        val toggledFlags = if (flags?.any { it.reasonT == flagReason } == true) {
            flags.filterNot(WorksiteFlag::isHighPriorityFlag)
        } else {
            val highPriorityFlag = WorksiteFlag.highPriority()
            flags?.toMutableList()?.apply { add(highPriorityFlag) } ?: listOf(highPriorityFlag)
        }
        return copy(flags = toggledFlags)
    }

    fun toggleHighPriorityFlag() = toggleFlag(HIGH_PRIORITY_FLAG)
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

const val HIGH_PRIORITY_FLAG = "flag.worksite_high_priority"
internal const val WRONG_LOCATION_FLAG = "flag.worksite_wrong_location"

data class WorksiteFlag(
    val id: Long,
    val action: String,
    val createdAt: Instant,
    val isHighPriority: Boolean,
    val notes: String,
    val reasonT: String,
    val reason: String,
    val requestedAction: String,
) {
    companion object {
        private fun worksiteFlag(
            reasonT: String,
            reason: String = "",
        ) = WorksiteFlag(
            id = 0,
            action = "",
            createdAt = Clock.System.now(),
            isHighPriority = reasonT == HIGH_PRIORITY_FLAG,
            notes = "",
            reasonT = reasonT,
            reason = reason,
            requestedAction = "",
        )

        fun highPriority() = worksiteFlag(HIGH_PRIORITY_FLAG)
        fun wrongLocation() = worksiteFlag(WRONG_LOCATION_FLAG)
    }

    val isHighPriorityFlag = isHighPriority && reasonT == HIGH_PRIORITY_FLAG
    val isWrongLocationFlag = reasonT == WRONG_LOCATION_FLAG
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

