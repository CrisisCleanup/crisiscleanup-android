package com.crisiscleanup.feature.caseeditor.model

import com.crisiscleanup.core.model.data.Worksite

class WorkInputData(
    worksite: Worksite,
    groupNode: FormFieldNode,
    ignoreFieldKeys: Set<String> = emptySet(),
) : FormFieldsInputData(worksite, groupNode, ignoreFieldKeys) {

}