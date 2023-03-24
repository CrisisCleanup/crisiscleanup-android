package com.crisiscleanup.feature.caseeditor.model

import com.crisiscleanup.core.common.AndroidResourceProvider
import com.crisiscleanup.core.model.data.Worksite

class DetailsInputData(
    worksite: Worksite,
    private val groupNode: FormFieldNode,
    private val resourceProvider: AndroidResourceProvider,
    private val ignoreFieldKeys: Set<String> = emptySet(),
) : CaseDataWriter {
    private val worksiteIn = worksite.copy()

    override fun updateCase() = updateCase(worksiteIn)

    override fun updateCase(worksite: Worksite): Worksite? {
        // TODO
        return null
    }
}
