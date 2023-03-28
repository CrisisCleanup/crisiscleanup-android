package com.crisiscleanup.feature.caseeditor.model

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import com.crisiscleanup.core.model.data.Worksite
import com.crisiscleanup.core.network.model.DynamicValue

open class FormFieldsInputData(
    worksite: Worksite,
    groupNode: FormFieldNode,
    ignoreFieldKeys: Set<String> = emptySet(),
) : CaseDataWriter {
    private val worksiteIn = worksite.copy()

    private val worksiteFormData = worksite.formData ?: emptyMap()
    private val formFieldData = groupNode.children
        .filter { !ignoreFieldKeys.contains(it.fieldKey) }
        .map { node ->
            var dynamicValue = DynamicValue("")
            worksiteFormData[node.fieldKey]?.let {
                dynamicValue = DynamicValue(
                    it.valueString,
                    it.isBoolean,
                    it.valueBoolean,
                )
            }
            FieldDynamicValue(
                node.formField,
                node.options,
                node.children.map(FormFieldNode::fieldKey).toSet(),
                if (node.parentKey == groupNode.fieldKey) 0 else 1,
                dynamicValue,
            )
        }
    val mutableFormFieldData = formFieldData.map {
        mutableStateOf(it)
    }

    private val groupFields = formFieldData.filter { it.childrenCount > 0 }

    val groupExpandState = mutableStateMapOf<String, Boolean>()
        .also { map ->
            groupFields.filter { it.dynamicValue.isBooleanTrue }
                .forEach {
                    map[it.key] = true
                }
        }

    override fun updateCase() = updateCase(worksiteIn)

    private fun resetUnmodifiedGroups(fieldData: Map<String, DynamicValue>): Map<String, DynamicValue> {
        if (groupFields.isEmpty()) {
            return fieldData
        }

        val updatedFieldData = fieldData.toMutableMap()
        groupFields
            .filter { field ->
                val fieldValue = updatedFieldData[field.key]
                fieldValue?.isBoolean == true && !fieldValue.valueBoolean
            }
            .flatMap { it.childKeys }
            .forEach { childKey ->
                updatedFieldData[childKey]?.let { childValue ->
                    updatedFieldData[childKey] = childValue.copy(
                        valueString = "",
                        valueBoolean = false,
                    )
                }
            }
        return updatedFieldData
    }

    override fun updateCase(worksite: Worksite): Worksite? {
        var snapshotFieldData = mutableFormFieldData.associate {
            it.value.key to it.value.dynamicValue
        }

        // TODO Add test coverage
        snapshotFieldData = resetUnmodifiedGroups(snapshotFieldData)

        if (!worksite.seekChange(snapshotFieldData)) {
            return worksite
        }

        val formData = worksite.copyModifiedFormData(snapshotFieldData)
        return worksite.copy(
            formData = formData,
        )
    }
}
