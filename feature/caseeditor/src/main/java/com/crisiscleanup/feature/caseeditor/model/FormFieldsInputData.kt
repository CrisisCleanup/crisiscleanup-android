package com.crisiscleanup.feature.caseeditor.model

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
                node.children.size,
                if (node.parentKey == groupNode.fieldKey) 1 else 0,
                dynamicValue,
            )
        }
    val mutableFormFieldData = formFieldData.map {
        mutableStateOf(it)
    }

    override fun updateCase() = updateCase(worksiteIn)

    override fun updateCase(worksite: Worksite): Worksite? {
        val snapshotValues =
            mutableFormFieldData.associate { it.value.key to it.value.dynamicValue }

        if (!worksite.seekChange(snapshotValues)) {
            return worksite
        }

        val formData = worksite.copyModifiedFormData(snapshotValues)
        return worksite.copy(
            formData = formData,
        )
    }
}
