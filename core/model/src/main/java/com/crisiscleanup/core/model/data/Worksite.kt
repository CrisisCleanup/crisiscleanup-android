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
    val notes: List<WorksiteNote>? = null,
    val phone1: String,
    val phone2: String,
    val plusCode: String? = null,
    val postalCode: String,
    val reportedBy: Long?,
    val state: String,
    val svi: Float?,
    val updatedAt: Instant?,
    val what3words: String? = null,
    val workTypes: List<WorkType>,
) {
    companion object {
        fun autoContactFrequency(literal: String) =
            autoContactFrequencyMap[literal] ?: AutoContactFrequency.None
    }

    val hasHighPriorityFlag = flags?.any(WorksiteFlag::isHighPriorityFlag)
    val hasWrongLocationFlag = flags?.any(WorksiteFlag::isWrongLocationFlag)
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

data class WorksiteFormValue(
    val isBoolean: Boolean = false,
    val valueString: String,
    val valueBoolean: Boolean = false,
)

internal const val HIGH_PRIORITY_FLAG = "flag.worksite_high_priority"
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
    val isHighPriorityFlag = isHighPriority && reasonT == HIGH_PRIORITY_FLAG
    val isWrongLocationFlag = reasonT == WRONG_LOCATION_FLAG
}

data class WorksiteNote(
    val id: Long,
    val createdAt: Instant,
    val isSurvivor: Boolean,
    val note: String,
)
