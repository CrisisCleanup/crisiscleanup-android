package com.crisiscleanup.core.network.model

import com.crisiscleanup.core.network.model.util.InstantSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class NetworkWorksitePush(
    val id: Long?,
    val address: String,
    @SerialName("auto_contact_frequency_t")
    val autoContactFrequencyT: String,
    @SerialName("case_number")
    val caseNumber: String?,
    val city: String,
    val county: String,
    val email: String,
    // val events: List<NetworkEvent>,
    val favorite: NetworkType?,
    // val files: List<NetworkFile>,
    // Flags are followup requests.
    // val flags
    @SerialName("form_data")
    val formData: List<KeyDynamicValuePair>,
    val incident: Long,
    @SerialName("key_work_type")
    val keyWorkType: NetworkWorkType?,
    val location: NetworkWorksiteFull.Location,
    val name: String,
    // Notes are followup requests.
    // val notes
    val phone1: String,
    @SerialName("phone1_notes")
    val phone1Notes: String?,
    val phone2: String,
    @SerialName("phone2_notes")
    val phone2Notes: String?,
    @SerialName("pluscode")
    val plusCode: String?,
    @SerialName("postal_code")
    val postalCode: String?,
    @SerialName("reported_by")
    val reportedBy: Long?,
    val state: String,
    val svi: Float?,
    // @SerialName("time")
    // val times: List<NetworkWorksiteFull.Time>,
    @Serializable(InstantSerializer::class)
    @SerialName("updated_at")
    val updatedAt: Instant,
    @SerialName("what3words")
    val what3words: String,
    @SerialName("work_types")
    val workTypes: List<NetworkWorkType>?,

    @SerialName("skip_duplicate_check")
    val skipDuplicateCheck: Boolean? = null,
    @SerialName("send_sms")
    val sendSms: Boolean? = null,
)
