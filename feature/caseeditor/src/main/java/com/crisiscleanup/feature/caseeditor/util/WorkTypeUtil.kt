package com.crisiscleanup.feature.caseeditor.util

import androidx.compose.runtime.MutableState
import com.crisiscleanup.core.model.data.WorkType
import com.crisiscleanup.core.model.data.Worksite
import com.crisiscleanup.feature.caseeditor.model.FieldDynamicValue
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

// TODO Additional test coverage
fun Worksite.updateWorkTypeStatuses(
    workTypeLookup: Map<String, String>,
    formFieldData: List<MutableState<FieldDynamicValue>>,
    createdAt: Instant = Clock.System.now(),
): Worksite {
    val workTypeFrequencyLookup = formFieldData
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

    val existingWorkTypeLookup = workTypes
        .sortedBy { it.id }
        .associateBy(WorkType::workTypeLiteral)

    val newWorkTypes = mutableListOf<WorkType>()
    val keepWorkTypes = mutableMapOf<String, WorkType>()
    formFieldData
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
                val existingWorkType = existingWorkTypeLookup[workTypeLiteral]
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
                        ),
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
    val initialOrder = workTypes.map(WorkType::workTypeLiteral)
    val copyWorkTypes = existingWorkTypeLookup.values
        .asSequence()
        .map {
            val index = initialOrder.indexOf(it.workTypeLiteral)
            Pair(index, it)
        }
        .sortedBy { it.first }
        .map { it.second }
        .mapNotNull { keepWorkTypes[it.workTypeLiteral] }
        .toMutableList()
    copyWorkTypes.addAll(newWorkTypes.sortedBy(WorkType::workTypeLiteral))

    return copy(workTypes = copyWorkTypes)
}

// TODO Test coverage
fun Worksite.updateKeyWorkType(reference: Worksite) = copy(
    keyWorkType = reference.keyWorkType?.workType?.let { matchWorkType ->
        workTypes.find { it.workType == matchWorkType }
    } ?: workTypes.firstOrNull(),
)
