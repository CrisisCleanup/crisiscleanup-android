package com.crisiscleanup.core.network.model

import com.crisiscleanup.core.network.model.util.InstantSerializer
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class NetworkWorksitesFullResult(
    val errors: List<NetworkCrisisCleanupApiError>? = null,
    val count: Int? = null,
    val results: List<NetworkWorksiteFull>? = null,
)

// Start from worksites/api/WorksiteSerializer
// Update [NetworkWorksiteCoreData] below with similar changes
@Serializable
data class NetworkWorksiteFull(
    val id: Long,
    val address: String,
    @SerialName("auto_contact_frequency_t")
    val autoContactFrequencyT: String,
    @SerialName("case_number")
    val caseNumber: String,
    val city: String,
    val county: String,
    val email: String? = null,
    val events: List<NetworkEvent>,
    val favorite: NetworkType?,
    val files: List<NetworkFile>,
    val flags: List<NetworkFlag>,
    @SerialName("form_data")
    val formData: List<KeyDynamicValuePair>,
    val incident: Long,
    @SerialName("key_work_type")
    private val keyWorkType: NetworkWorkType?,
    val location: Location,
    val name: String,
    val notes: List<NetworkNote>,
    val phone1: String,
    val phone2: String?,
    @SerialName("pluscode")
    val plusCode: String? = null,
    @SerialName("postal_code")
    val postalCode: String?,
    @SerialName("reported_by")
    val reportedBy: Long?,
    val state: String,
    val svi: Float?,
    @SerialName("time")
    val times: List<Time>,
    @Serializable(InstantSerializer::class)
    @SerialName("updated_at")
    val updatedAt: Instant,
    @SerialName("what3words")
    val what3words: String? = null,
    @SerialName("work_types")
    private val workTypes: List<NetworkWorkType>,
) {
    @Transient
    var newestWorkTypes: List<NetworkWorkType> = emptyList()
        private set

    @Transient
    var newestKeyWorkType: NetworkWorkType? = null
        private set

    init {
        val newMap = mutableMapOf<String, Pair<Int, NetworkWorkType>>()
        workTypes.forEachIndexed { index, workType ->
            val literal = workType.workType
            val similar = newMap[literal]
            if (similar == null || workType.id!! > similar.second.id!!) {
                newMap[literal] = Pair(index, workType)
            }
        }

        if (newMap.size == workTypes.size) {
            newestWorkTypes = workTypes
            newestKeyWorkType = keyWorkType
        } else {
            newestWorkTypes = newMap.values
                .sortedBy { it.first }
                .map { it.second }
            newestKeyWorkType = keyWorkType?.let {
                newMap[it.workType]?.second
            }
        }
    }

    @Serializable
    data class Location(
        val type: String,
        val coordinates: List<Double>,
    )

    @Serializable
    data class Time(
        val id: Long,
        @SerialName("created_by_name")
        val createdByName: String?,
        @SerialName("created_by_org")
        val createdByOrg: Long?,
        val seconds: Int,
        val volunteers: Int,
        val worksite: Int
    )

    @Serializable
    data class KeyWorkTypeShort(
        @SerialName("work_type")
        val workType: String,
        @SerialName("claimed_by")
        val orgClaim: Long?,
        val status: String,
    )

    @Serializable
    data class WorkTypeShort(
        val id: Long,
        @SerialName("work_type")
        val workType: String,
        @SerialName("claimed_by")
        val orgClaim: Long?,
        val status: String,
    )

    @Serializable
    data class FlagShort(
        @SerialName("is_high_priority")
        val isHighPriority: Boolean?,
        @SerialName("reason_t")
        val reasonT: String?,
        @Serializable(InstantSerializer::class)
        @SerialName("flag_invalidated_at")
        val invalidatedAt: Instant?,
    )
}

@Serializable
data class NetworkWorksitesShortResult(
    val errors: List<NetworkCrisisCleanupApiError>? = null,
    val count: Int? = null,
    val results: List<NetworkWorksiteShort>? = null,
)

@Serializable
data class NetworkWorksiteLocationSearchResult(
    val errors: List<NetworkCrisisCleanupApiError>? = null,
    val count: Int? = null,
    val results: List<NetworkWorksiteLocationSearch>? = null,
)

@Serializable
data class NetworkWorksiteShort(
    val id: Long,
    val address: String,
    @SerialName("case_number")
    val caseNumber: String,
    val city: String,
    val county: String,
    // Full does not have this field. Updates should not overwrite
    @Serializable(InstantSerializer::class)
    @SerialName("created_at")
    val createdAt: Instant,
    // Differs from full
    @SerialName("favorite_id")
    val favoriteId: Long? = null,
    val flags: List<NetworkWorksiteFull.FlagShort>,
    val incident: Long,
    @SerialName("key_work_type")
    private val keyWorkType: NetworkWorksiteFull.KeyWorkTypeShort?,
    val location: NetworkWorksiteFull.Location,
    val name: String,
    @SerialName("postal_code")
    val postalCode: String?,
    val state: String,
    val svi: Float?,
    @Serializable(InstantSerializer::class)
    @SerialName("updated_at")
    val updatedAt: Instant,
    @SerialName("work_types")
    private val workTypes: List<NetworkWorksiteFull.WorkTypeShort>,
) {
    @Transient
    var newestWorkTypes: List<NetworkWorksiteFull.WorkTypeShort> = emptyList()
        private set

    @Transient
    var newestKeyWorkType: NetworkWorksiteFull.KeyWorkTypeShort? = null
        private set

    init {
        val newMap = mutableMapOf<String, Pair<Int, NetworkWorksiteFull.WorkTypeShort>>()
        workTypes.forEachIndexed { index, workType ->
            val literal = workType.workType
            val similar = newMap[literal]
            if (similar == null || workType.id > similar.second.id) {
                newMap[literal] = Pair(index, workType)
            }
        }

        if (newMap.size == workTypes.size) {
            newestWorkTypes = workTypes
            newestKeyWorkType = keyWorkType
        } else {
            newestWorkTypes = newMap.values
                .sortedBy { it.first }
                .map { it.second }
            newestKeyWorkType = keyWorkType?.let {
                newMap[it.workType]?.second?.let { kwt ->
                    NetworkWorksiteFull.KeyWorkTypeShort(
                        kwt.workType,
                        kwt.orgClaim,
                        kwt.status,
                    )
                }
            }
        }
    }
}

@Serializable
data class NetworkWorksitesCoreDataResult(
    val errors: List<NetworkCrisisCleanupApiError>? = null,
    val count: Int? = null,
    val results: List<NetworkWorksiteCoreData>? = null,
)

// Copy similar changes from [NetworkWorksiteFull] above
@Serializable
data class NetworkWorksiteCoreData(
    val id: Long,
    val address: String,
    @SerialName("auto_contact_frequency_t")
    val autoContactFrequencyT: String,
    @SerialName("case_number")
    val caseNumber: String,
    val city: String,
    val county: String,
    val email: String? = null,
    val favorite: NetworkType?,
    val flags: List<NetworkFlag>,
    @SerialName("form_data")
    val formData: List<KeyDynamicValuePair>,
    val incident: Long,
    val location: NetworkWorksiteFull.Location,
    val name: String,
    val notes: List<NetworkNote>,
    val phone1: String,
    val phone2: String?,
    @SerialName("pluscode")
    val plusCode: String? = null,
    @SerialName("postal_code")
    val postalCode: String?,
    @SerialName("reported_by")
    val reportedBy: Long?,
    val state: String,
    val svi: Float?,
    @Serializable(InstantSerializer::class)
    @SerialName("updated_at")
    val updatedAt: Instant,
    @SerialName("what3words")
    val what3words: String? = null,
    @SerialName("work_types")
    private val workTypes: List<NetworkWorkType>,
) {
    @Transient
    var newestWorkTypes: List<NetworkWorkType> = emptyList()
        private set

    init {
        val newMap = mutableMapOf<String, Pair<Int, NetworkWorkType>>()
        workTypes.forEachIndexed { index, workType ->
            val literal = workType.workType
            val similar = newMap[literal]
            if (similar == null || workType.id!! > similar.second.id!!) {
                newMap[literal] = Pair(index, workType)
            }
        }

        newestWorkTypes = if (newMap.size == workTypes.size) {
            workTypes
        } else {
            newMap.values
                .sortedBy { it.first }
                .map { it.second }
        }
    }
}