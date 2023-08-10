package com.crisiscleanup.core.database.model

import androidx.room.Embedded
import androidx.room.Relation
import com.crisiscleanup.core.model.data.CasesFilter
import com.crisiscleanup.core.model.data.OrganizationLocationAreaBounds

data class PopulatedFilterDataWorksite(
    @Embedded
    val base: PopulatedWorksite,

    @Relation(
        parentColumn = "id",
        entityColumn = "worksite_id",
    )
    val formData: List<WorksiteFormDataEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "worksite_id",
    )
    val flags: List<WorksiteFlagEntity>,
) {
    fun passesFilter(
        filters: CasesFilter,
        organizationAffiliates: Set<Long>,
        latRad: Double?,
        lngRad: Double?,
        locationAreaBounds: OrganizationLocationAreaBounds,
    ) = filters.passes(
        base.entity,
        flags,
        formData,
        base.workTypes,
        organizationAffiliates,
        latRad,
        lngRad,
        base.isFavorite,
        locationAreaBounds,
    )
}