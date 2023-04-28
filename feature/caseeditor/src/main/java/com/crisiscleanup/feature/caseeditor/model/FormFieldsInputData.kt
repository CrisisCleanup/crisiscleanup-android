package com.crisiscleanup.feature.caseeditor.model

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import com.crisiscleanup.core.model.data.Worksite
import com.crisiscleanup.core.network.model.DynamicValue

open class FormFieldsInputData(
    worksite: Worksite,
    groupNode: FormFieldNode,
    ignoreFieldKeys: Set<String> = emptySet(),
    private val autoManageGroups: Boolean = false,
    val helpText: String = groupNode.formField.help,
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

            // TODO Add test coverage
            val isActiveGroup = autoManageGroups &&
                    node.children.isNotEmpty() &&
                    node.children.any { child ->
                        val childFormValue = worksiteFormData[child.fieldKey]
                        childFormValue?.hasValue == true
                    }
            if (isActiveGroup && !dynamicValue.isBooleanTrue) {
                dynamicValue = DynamicValue("", isBoolean = true, true)
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

    private val requiredFormFields = groupNode.children
        .filter { it.formField.isRequired }
        .map { it.formField }

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

    private fun removeGroupFields(data: Map<String, DynamicValue>): Map<String, DynamicValue> {
        val groups = formFieldData
            .filter { it.childrenCount > 0 }
            .map { it.key }
            .toSet()
        if (groups.isNotEmpty()) {
            return data.filter { !groups.contains(it.key) }
        }

        return data
    }

    protected open fun onPreCommitFieldData(data: Map<String, DynamicValue>) =
        if (autoManageGroups) removeGroupFields(data) else data

    override fun updateCase() = updateCase(worksiteIn)

    override fun updateCase(worksite: Worksite): Worksite? {
        var snapshotFieldData = mutableFormFieldData.associate {
            it.value.key to it.value.dynamicValue
        }

        // TODO Test coverage
        requiredFormFields.forEach {
            if (it.htmlType != "checkbox" &&
                snapshotFieldData[it.fieldKey]?.valueString?.isNotBlank() != true
            ) {
                return null
            }
        }

        // TODO Test coverage
        snapshotFieldData = resetUnmodifiedGroups(snapshotFieldData)

        // TODO Test coverage of default removeGroupFields and autoManageGroups behavior does not cause a change in worksite form data
        val committingFieldData = onPreCommitFieldData(snapshotFieldData)

        if (!worksite.seekChange(committingFieldData)) {
            return worksite
        }

        val formData = worksite.copyModifiedFormData(committingFieldData)
        return worksite.copy(
            formData = formData,
        )
    }
}
