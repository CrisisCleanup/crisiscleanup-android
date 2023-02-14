package com.crisiscleanup.core.network.model

import com.crisiscleanup.core.network.model.util.InstantSerializer
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// TODO Test
@Serializable
data class NetworkWorksitesCountResult(
    val errors: List<NetworkCrisisCleanupApiError>? = null,
    val count: Int? = null,
)

@Serializable
data class NetworkWorksitesFullResult(
    val errors: List<NetworkCrisisCleanupApiError>? = null,
    val count: Int? = null,
    val results: List<NetworkWorksiteFull>? = null,
)

// Start from worksites/api/WorksiteSerializer
@Serializable
data class NetworkWorksiteFull(
    val id: Long,
    val address: String,
    @SerialName("auto_contact_frequency_t")
    val autoContactFrequencyT: String, // "formOptions.often", "formOptions.not_often", "formOptions.never"
    // TODO Is this necessary to save when location is given?
    @SerialName("blurred_location")
    val blurredLocation: Location,
    @SerialName("case_number")
    val caseNumber: String,
    val city: String,
    val county: String,
    val email: String? = null,
    val events: List<NetworkEvent>,
    val favorite: Favorite?,
    val files: List<NetworkFile>,
    val flags: List<NetworkFlag>,
    // TODO How to deal w/ serialization
//    @SerialName("form_data")
//    val formData: List<FormData>,
    val incident: Long,
    @SerialName("key_work_type")
    val keyWorkType: WorkType?,
    val location: Location,
    val name: String,
    val notes: List<Note>,
    val phone1: String,
    val phone2: String?,
    @SerialName("pluscode")
    val plusCode: String? = null,
    @SerialName("postal_code")
    val postalCode: String,
    @SerialName("reported_by")
    val reportedBy: Long?,
    // TODO This has disappeared?
//    @SerialName("send_sms")
//    val sendSms: Boolean,
    // TODO This has disappeared?
//    @SerialName("skip_duplicate_check")
//    val skipDuplicateCheck: Boolean,
    val state: String,
    val svi: Float?,
    val time: List<Time>,
    @Serializable(InstantSerializer::class)
    @SerialName("updated_at")
    val updatedAt: Instant,
    @SerialName("what3words")
    val what3words: String? = null,
    @SerialName("work_types")
    val workTypes: List<WorkType>,
) {
    @Serializable
    data class Favorite(
        val id: Long,
        @SerialName("type_t")
        val typeT: String,
    )

    @Serializable
    data class FormData(
        @SerialName("field_key")
        val key: String?,
        @SerialName("field_value")
        val value: String,
    )

    @Serializable
    data class Location(
        val type: String,
        val coordinates: List<Double>,
    )

    @Serializable
    data class Note(
        val id: Long,
        @Serializable(InstantSerializer::class)
        @SerialName("created_at")
        val createdAt: Instant,
        @SerialName("is_survivor")
        val isSurvivor: Boolean,
        val note: String?,
    )

    @Serializable
    data class Time(
        val id: Long,
        @SerialName("created_by_name")
        val createdByName: String,
        @SerialName("created_by_org")
        val createdByOrg: Long,
        val seconds: Int,
        val volunteers: Int,
        val worksite: Int
    )

    @Serializable
    data class WorkType(
        val id: Long,
        @Serializable(InstantSerializer::class)
        @SerialName("created_at")
        val createdAt: Instant? = null,
        @SerialName("claimed_by")
        val orgClaim: Long? = null,
        @Serializable(InstantSerializer::class)
        @SerialName("next_recur_at")
        val nextRecurAt: Instant? = null,
        val phase: Int? = null,
        val recur: String? = null,
        val status: String,
        @SerialName("work_type")
        val workType: String,
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
        // TODO differs from [NetworkFlag]/full why?
        @SerialName("is_high_priority")
        val isHighPriority: Boolean?,
        @SerialName("reason_t")
        val reasonT: String?,
        @Serializable(InstantSerializer::class)
        @SerialName("flag_invalidated_at")
        val inValidatedAt: Instant?,
    )
}

@Serializable
data class NetworkWorksitesShortResult(
    val errors: List<NetworkCrisisCleanupApiError>? = null,
    val count: Int? = null,
    val results: List<NetworkWorksiteShort>? = null,
)

@Serializable
data class NetworkWorksiteShort(
    val id: Long,
    val address: String,
    @SerialName("case_number")
    val caseNumber: String,
    val city: String,
    val county: String,
    // TODO Differs from full
    @Serializable(InstantSerializer::class)
    @SerialName("created_at")
    val createdAt: Instant,
    // TODO Differs from full
    @SerialName("favorite_id")
    val favoriteId: Long? = null,
    val flags: List<NetworkWorksiteFull.FlagShort>,
    @SerialName("key_work_type")
    val keyWorkType: NetworkWorksiteFull.KeyWorkTypeShort?,
    val location: NetworkWorksiteFull.Location,
    val name: String,
    @SerialName("postal_code")
    val postalCode: String,
    val state: String,
    val svi: Float?,
    @Serializable(InstantSerializer::class)
    @SerialName("updated_at")
    val updatedAt: Instant,
    @SerialName("work_types")
    val workTypes: List<NetworkWorksiteFull.WorkTypeShort>,
)