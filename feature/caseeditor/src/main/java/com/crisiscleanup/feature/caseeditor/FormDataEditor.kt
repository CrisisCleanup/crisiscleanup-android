package com.crisiscleanup.feature.caseeditor

import com.crisiscleanup.feature.caseeditor.model.FormFieldsInputData

interface FormDataEditor {
    val inputData: FormFieldsInputData

    fun validateSaveWorksite(): Boolean
}

open class EditableFormDataEditor(
    formGroupKey: String,
    private val worksiteProvider: EditableWorksiteProvider,
    ignoreFieldKeys: Set<String> = emptySet(),
) : FormDataEditor {
    final override val inputData: FormFieldsInputData

    init {
        val groupNode = worksiteProvider.getGroupNode(formGroupKey)
        val worksite = worksiteProvider.editableWorksite.value
        inputData = FormFieldsInputData(worksite, groupNode, ignoreFieldKeys)
    }

    override fun validateSaveWorksite(): Boolean {
        val updatedWorksite = inputData.updateCase()
        if (updatedWorksite != null) {
            worksiteProvider.editableWorksite.value = updatedWorksite
            return true
        }
        return false
    }
}
