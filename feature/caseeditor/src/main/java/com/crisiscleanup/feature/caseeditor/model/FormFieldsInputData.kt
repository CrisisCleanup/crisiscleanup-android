package com.crisiscleanup.feature.caseeditor.model

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import com.crisiscleanup.core.commoncase.model.FormFieldNode
import com.crisiscleanup.core.model.data.WorkType
import com.crisiscleanup.core.model.data.WorkTypeStatus
import com.crisiscleanup.core.model.data.Worksite
import com.crisiscleanup.core.network.model.DynamicValue

open class FormFieldsInputData(
    worksite: Worksite,
    groupNode: FormFieldNode,
    ignoreFieldKeys: Set<String> = emptySet(),
    private val autoManageGroups: Boolean = false,
    val helpText: String = groupNode.formField.help,
    private val isWorkInputData: Boolean = false,
) : CaseDataWriter {
    private val worksiteIn = worksite.copy()

    private val managedGroups = mutableSetOf<String>()

    private val workTypeMap = worksite.workTypes.associateBy(WorkType::workTypeLiteral)

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
                managedGroups.add(node.fieldKey)
                dynamicValue = DynamicValue("", isBoolean = true, true)
            }

            var fieldData = FieldDynamicValue(
                node.formField,
                node.options,
                node.children.map(FormFieldNode::fieldKey).toSet(),
                if (node.parentKey == groupNode.fieldKey) 0 else 1,
                dynamicValue,
            )

            // TODO Add test coverage
            if (isWorkInputData && fieldData.isWorkTypeGroup) {
                val fieldWorkType = node.formField.selectToggleWorkType
                val status = workTypeMap[fieldWorkType]?.status
                fieldData = fieldData.copy(workTypeStatus = status ?: WorkTypeStatus.OpenUnassigned)
            }

            fieldData
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

    protected open fun onPreCommitFieldData(data: Map<String, DynamicValue>) =
        if (!autoManageGroups || managedGroups.isEmpty()) {
            data
        } else {
            data.filter { !managedGroups.contains(it.key) }
        }

    override fun updateCase() = updateCase(worksiteIn)

    private fun updateCase(worksite: Worksite, validate: Boolean): Worksite? {
        var snapshotFieldData = mutableFormFieldData.associate {
            it.value.key to it.value.dynamicValue
        }

        if (validate) {
            // TODO Test coverage
            requiredFormFields.forEach {
                if (it.htmlType != "checkbox" &&
                    snapshotFieldData[it.fieldKey]?.valueString?.isNotBlank() != true
                ) {
                    return null
                }
            }
        }

        // TODO Test coverage
        snapshotFieldData = resetUnmodifiedGroups(snapshotFieldData)

        // TODO Test coverage of default autoManageGroups behavior does not cause a change in worksite form data
        val committingFieldData = onPreCommitFieldData(snapshotFieldData)

        if (!worksite.seekChange(committingFieldData)) {
            return worksite
        }

        val formData = worksite.copyModifiedFormData(committingFieldData)
        return worksite.copy(
            formData = formData,
        )
    }

    override fun updateCase(worksite: Worksite) = updateCase(worksite, true)

    override fun copyCase(worksite: Worksite) = updateCase(worksite, false)!!
}
