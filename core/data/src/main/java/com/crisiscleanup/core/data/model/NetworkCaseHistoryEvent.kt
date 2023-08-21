package com.crisiscleanup.core.data.model

import com.crisiscleanup.core.database.model.CaseHistoryEventAttrEntity
import com.crisiscleanup.core.database.model.CaseHistoryEventEntity
import com.crisiscleanup.core.network.model.NetworkCaseHistoryEvent

fun NetworkCaseHistoryEvent.asEntities(worksiteId: Long): Pair<CaseHistoryEventEntity, CaseHistoryEventAttrEntity> =
    Pair(
        CaseHistoryEventEntity(
            id = id,
            worksiteId = worksiteId,
            createdAt = createdAt,
            createdBy = createdBy,
            eventKey = eventKey,
            pastTenseT = pastTenseT,
            actorLocationName = actorLocationName ?: "",
            recipientLocationName = recipientLocationName,
        ),
        with(attr) {
            CaseHistoryEventAttrEntity(
                id = id,
                incidentName = incidentName,
                patientCaseNumber = patientCaseNumber,
                patientId = patientId ?: 0,
                patientLabelT = patientLabelT,
                patientLocationName = patientLocationName,
                patientNameT = patientNameT,
                patientReasonT = patientReasonT,
                patientStatusNameT = patientStatusNameT,
                recipientCaseNumber = recipientCaseNumber,
                recipientId = recipientId,
                recipientName = recipientName,
                recipientNameT = recipientNameT,
            )
        },
    )
