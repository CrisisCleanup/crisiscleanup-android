package com.crisiscleanup.feature.caseeditor

import com.crisiscleanup.core.commoncase.model.DetailsFormGroupKey
import com.crisiscleanup.core.commoncase.model.HazardsFormGroupKey
import com.crisiscleanup.core.commoncase.model.VolunteerReportFormGroupKey
import com.crisiscleanup.core.commoncase.model.WorkFormGroupKey
import com.crisiscleanup.core.model.data.Worksite
import com.crisiscleanup.feature.caseeditor.model.FormFieldsInputData
import com.crisiscleanup.feature.caseeditor.util.updateWorkTypeStatuses
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

interface FormDataEditor {
    val inputData: FormFieldsInputData

    fun validateSaveWorksite(): Boolean
}

open class EditableFormDataEditor(
    formGroupKey: String,
    private val worksiteProvider: EditableWorksiteProvider,
    ignoreFieldKeys: Set<String> = emptySet(),
    autoManageGroups: Boolean = false,
    isWorkInputData: Boolean = false,
) : FormDataEditor {
    final override val inputData: FormFieldsInputData

    init {
        val groupNode = worksiteProvider.getGroupNode(formGroupKey)
        val worksite = worksiteProvider.editableWorksite.value
        inputData = FormFieldsInputData(
            worksite,
            groupNode,
            ignoreFieldKeys,
            autoManageGroups = autoManageGroups,
            isWorkInputData = isWorkInputData,
        )
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

internal val excludeDetailsFormFields = setOf("cross_street", "email")

internal class EditableDetailsDataEditor(
    worksiteProvider: EditableWorksiteProvider,
) : EditableFormDataEditor(
    DetailsFormGroupKey,
    worksiteProvider,
    excludeDetailsFormFields,
    autoManageGroups = true,
)

internal class EditableWorkDataEditor(
    worksiteProvider: EditableWorksiteProvider,
) : EditableFormDataEditor(WorkFormGroupKey, worksiteProvider, isWorkInputData = true) {
    fun transferWorkTypes(
        workTypeLookup: Map<String, String>,
        worksite: Worksite,
        createdAt: Instant = Clock.System.now(),
    ) = updateWorkTypeStatuses(workTypeLookup, worksite, inputData, createdAt)
}

internal class EditableHazardsDataEditor(
    worksiteProvider: EditableWorksiteProvider,
) : EditableFormDataEditor(HazardsFormGroupKey, worksiteProvider)

internal class EditableVolunteerReportDataEditor(
    worksiteProvider: EditableWorksiteProvider,
) : EditableFormDataEditor(VolunteerReportFormGroupKey, worksiteProvider)
