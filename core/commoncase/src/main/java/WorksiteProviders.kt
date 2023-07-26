package com.crisiscleanup.core.commoncase

import com.crisiscleanup.core.model.data.EmptyWorksite
import com.crisiscleanup.core.model.data.Worksite
import kotlinx.coroutines.flow.MutableStateFlow

interface WorksiteProvider {
    val editableWorksite: MutableStateFlow<Worksite>
}

fun WorksiteProvider.reset(incidentId: Long) = run {
    editableWorksite.value = EmptyWorksite.copy(
        incidentId = incidentId,
    )
}
