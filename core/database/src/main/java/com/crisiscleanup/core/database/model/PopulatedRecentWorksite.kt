package com.crisiscleanup.core.database.model

import androidx.room.Embedded
import androidx.room.Relation
import com.crisiscleanup.core.model.data.WorkType
import com.crisiscleanup.core.model.data.WorksiteSummary

data class PopulatedRecentWorksite(
    @Embedded
    val entity: RecentWorksiteEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
    )
    val worksite: WorksiteEntity
)

fun PopulatedRecentWorksite.asSummary(): WorksiteSummary {
    with(worksite) {
        return WorksiteSummary(
            id,
            networkId,
            name,
            address,
            city,
            state,
            postalCode,
            county,
            caseNumber,
            WorkType(
                0,
                statusLiteral = keyWorkTypeStatus,
                workTypeLiteral = keyWorkTypeType,
            ),
        )
    }
}
