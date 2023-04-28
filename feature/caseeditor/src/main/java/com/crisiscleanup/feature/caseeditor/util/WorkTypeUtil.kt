package com.crisiscleanup.feature.caseeditor.util

import com.crisiscleanup.core.model.data.WorkType
import com.crisiscleanup.core.model.data.WorkTypeStatus
import com.crisiscleanup.core.model.data.Worksite
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

fun resolveModifiedWorkTypes(
    workTypeLookup: Map<String, String>,
    initialWorksite: Worksite,
    modifiedWorksite: Worksite,
    createdAt: Instant = Clock.System.now(),
): Pair<List<WorkType>, WorkType?> {
    val worksiteWorkTypes = initialWorksite.workTypes.associateBy(WorkType::workTypeLiteral)
    val formWorkTypes = modifiedWorksite.formData!!
        .mapNotNull { workTypeLookup[it.key] }
        .toSet()
        .map {
            worksiteWorkTypes[it] ?: WorkType(
                id = 0,
                createdAt = createdAt,
                orgClaim = null,
                nextRecurAt = null,
                phase = null,
                recur = null,
                statusLiteral = WorkTypeStatus.OpenUnassigned.literal,
                workTypeLiteral = it,
            )
        }
    val initialWorkTypes = initialWorksite.workTypes.sortedBy(WorkType::workTypeLiteral)
    val modifiedWorkTypes = formWorkTypes.sortedBy(WorkType::workTypeLiteral)
    if (initialWorkTypes == modifiedWorkTypes) {
        return Pair(initialWorksite.workTypes, initialWorksite.keyWorkType)
    }

    val formWorkTypeTypes = formWorkTypes.map(WorkType::workType).toSet()
    var keyWorkType = initialWorksite.keyWorkType
    if (keyWorkType == null || !formWorkTypeTypes.contains(keyWorkType.workType)) {
        keyWorkType = formWorkTypes.toMutableList()
            .sortedBy(WorkType::workTypeLiteral)
            .firstOrNull()
    }

    return Pair(formWorkTypes, keyWorkType)
}
