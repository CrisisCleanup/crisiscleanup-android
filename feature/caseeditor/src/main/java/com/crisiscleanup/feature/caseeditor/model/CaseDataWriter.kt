package com.crisiscleanup.feature.caseeditor.model

import com.crisiscleanup.core.model.data.Worksite

interface CaseDataWriter {
    /**
     * @return Updated worksite or null if there is an error with internal data
     */
    fun updateCase(): Worksite?
    fun updateCase(worksite: Worksite): Worksite?

    fun copyCase(worksite: Worksite): Worksite
}
