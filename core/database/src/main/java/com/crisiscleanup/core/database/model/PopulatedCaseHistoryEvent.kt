package com.crisiscleanup.core.database.model

import androidx.room.Embedded
import androidx.room.Relation
import com.crisiscleanup.core.common.KeyTranslator
import com.crisiscleanup.core.model.data.CaseHistoryEvent

data class PopulatedCaseHistoryEvent(
    @Embedded
    val entity: CaseHistoryEventEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
    )
    val attr: CaseHistoryEventAttrEntity,
)

fun PopulatedCaseHistoryEvent.asExternalModel(translator: KeyTranslator): CaseHistoryEvent {
    val translate = { key: String? -> key?.let { translator.translate(it) } ?: "" }
    with(entity) {
        return CaseHistoryEvent(
            id = id,
            worksiteId = worksiteId,
            createdAt = createdAt,
            createdBy = createdBy,
            eventKey = eventKey,
            pastTenseDescription = translate(pastTenseT)
                .replace("{incident_name}", attr.incidentName)
                .replace("{patient_case_number}", attr.patientCaseNumber ?: "?")
                .replace("{patient_label_t}", translate(attr.patientLabelT))
                .replace("{patient_location_name}", attr.patientLocationName ?: "")
                .replace("{patient_name_t}", translate(attr.patientNameT))
                .replace("{patient_reason_t}", translate(attr.patientReasonT))
                .replace("{patient_status_name_t}", translate(attr.patientStatusNameT))
                .replace("{recipient_case_number}", attr.recipientCaseNumber ?: "")
                .replace("{recipient_name}", attr.recipientName ?: "?")
                .replace("{recipient_name_t}", translate(attr.recipientNameT)),
            actorLocationName = actorLocationName,
            recipientLocationName = recipientLocationName,
        )
    }
}