package com.crisiscleanup.core.database.model

import androidx.room.Embedded
import androidx.room.Relation
import com.crisiscleanup.core.model.data.CaseHistoryEvent
import com.crisiscleanup.core.model.data.CaseHistoryEventAttr

data class PopulatedCaseHistoryEvent(
    @Embedded
    val entity: CaseHistoryEventEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
    )
    val attr: CaseHistoryEventAttrEntity,
)

fun PopulatedCaseHistoryEvent.asExternalModel(): CaseHistoryEvent {
    val eventAttr = with(attr) {
        CaseHistoryEventAttr(
            incidentName = incidentName,
            patientCaseNumber = patientCaseNumber,
            patientId = patientId,
            patientLabelT = patientLabelT,
            patientLocationName = patientLocationName,
            patientNameT = patientNameT,
            patientStatusNameT = patientStatusNameT,
            recipientCaseNumber = recipientCaseNumber,
            recipientId = recipientId,
            recipientName = recipientName,
        )
    }
    with(entity) {
        return CaseHistoryEvent(
            id = id,
            worksiteId = worksiteId,
            createdAt = createdAt,
            createdBy = createdBy,
            eventKey = eventKey,
            pastTenseT = pastTenseT,
            actorLocationName = actorLocationName,
            recipientLocationName = recipientLocationName,
            attr = eventAttr,
        )
    }
}