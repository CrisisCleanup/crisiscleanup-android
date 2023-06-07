package com.crisiscleanup.feature.caseeditor.util

import com.crisiscleanup.core.model.data.WorkType
import com.crisiscleanup.core.model.data.Worksite
import com.crisiscleanup.feature.caseeditor.model.FormFieldsInputData
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

// TODO Test coverage
fun updateWorkTypeStatuses(
    workTypeLookup: Map<String, String>,
    worksite: Worksite,
    inputData: FormFieldsInputData,
    createdAt: Instant = Clock.System.now(),
): Worksite {
    val worksiteWorkTypes = worksite.workTypes.associateBy(WorkType::workTypeLiteral)
    val workTypeFrequencyLookup = inputData.mutableFormFieldData
        .filter {
            with(it.value) {
                field.isFrequency && dynamicValue.valueString.isNotBlank()
            }
        }
        .associate {
            with(it.value) {
                field.parentKey to dynamicValue.valueString
            }
        }

    val newWorkTypes = mutableListOf<WorkType>()
    val keepWorkTypes = mutableMapOf<String, WorkType>()
    inputData.mutableFormFieldData
        .filter {
            with(it.value) {
                dynamicValue.isBooleanTrue &&
                        isWorkTypeGroup &&
                        workTypeLookup[key] != null
            }
        }
        .forEach {
            with(it.value) {
                val workTypeLiteral = workTypeLookup[key]!!
                val existingWorkType = worksiteWorkTypes[workTypeLiteral]
                val recur = workTypeFrequencyLookup[field.fieldKey]?.ifBlank { null }
                if (existingWorkType == null) {
                    newWorkTypes.add(
                        WorkType(
                            id = 0,
                            createdAt = createdAt,
                            orgClaim = null,
                            nextRecurAt = null,
                            phase = null,
                            recur = recur,
                            statusLiteral = workTypeStatus.literal,
                            workTypeLiteral = workTypeLiteral,
                        )
                    )
                } else {
                    val isRecurChanged = recur != existingWorkType.recur
                    keepWorkTypes[workTypeLiteral] = existingWorkType.copy(
                        statusLiteral = workTypeStatus.literal,
                        nextRecurAt = if (isRecurChanged) null else existingWorkType.nextRecurAt,
                        recur = recur,
                    )
                }
            }
        }

    // Some work types may appear multiple times (with different IDs)...
    val distinctWorkTypes = worksite.workTypes.map(WorkType::workType).toSet()
    var workTypes = if (distinctWorkTypes.size < worksite.workTypes.size) {
        worksite.workTypes.sortedBy(WorkType::id)
            .associateBy(WorkType::workType)
            .values
            .sortedBy { workType -> worksite.workTypes.indexOf(workType) }
    } else worksite.workTypes

    workTypes = workTypes
        .mapNotNull { keepWorkTypes[it.workTypeLiteral] }
        .toMutableList().apply { addAll(newWorkTypes) }

    return worksite.copy(workTypes = workTypes)
}

// TODO Test coverage
fun Worksite.updateKeyWorkType(reference: Worksite) = copy(
    keyWorkType = reference.keyWorkType?.workType?.let { matchWorkType ->
        workTypes.find { it.workType == matchWorkType }
    } ?: workTypes.firstOrNull()
)