package com.crisiscleanup.feature.caseeditor.model

import androidx.compose.runtime.snapshots.SnapshotStateMap
import com.crisiscleanup.core.model.data.IncidentFormField
import com.crisiscleanup.core.model.data.Worksite
import com.crisiscleanup.core.network.model.DynamicValue

class DetailsInputData(
    worksite: Worksite,
    groupNode: FormFieldNode,
    private val ignoreFieldKeys: Set<String> = emptySet(),
) : CaseDataWriter {
    private val worksiteIn = worksite.copy()

    val formKeys: List<String>
    val fieldMap: Map<String, FieldState>
    val snapshotMap = SnapshotStateMap<String, FieldDynamicValue>()

    init {
        val visibleFields = groupNode.children
            .filter { !ignoreFieldKeys.contains(it.fieldKey) }
        formKeys = visibleFields.map(FormFieldNode::fieldKey)
        fieldMap = visibleFields.associate {
            it.fieldKey to FieldState(it)
        }
        visibleFields.onEach {
            val value = FieldDynamicValue(
                isGlass = it.formField.isReadOnlyBreakGlass,
            )
            snapshotMap[it.fieldKey] = value
        }
    }

    override fun updateCase() = updateCase(worksiteIn)

    override fun updateCase(worksite: Worksite): Worksite? {
        // TODO
        return null
    }
}

class FieldState(
    val node: FormFieldNode,
    val field: IncidentFormField = node.formField,
) {
    val listItemContentType = "item-${field.htmlType}"
}

data class FieldDynamicValue(
    val dynamicValue: DynamicValue = DynamicValue(""),
    val isGlass: Boolean = false,
    val isGlassBroken: Boolean = false,
)