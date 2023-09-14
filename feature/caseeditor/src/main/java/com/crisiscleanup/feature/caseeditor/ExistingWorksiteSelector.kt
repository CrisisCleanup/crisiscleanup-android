package com.crisiscleanup.feature.caseeditor

import com.crisiscleanup.core.data.model.ExistingWorksiteIdentifier
import com.crisiscleanup.core.data.model.ExistingWorksiteIdentifierNone
import com.crisiscleanup.core.data.repository.IncidentsRepository
import com.crisiscleanup.core.data.repository.WorksitesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

class ExistingWorksiteSelector @Inject constructor(
    private val worksiteProvider: EditableWorksiteProvider,
    private val incidentsRepository: IncidentsRepository,
    private val worksitesRepository: WorksitesRepository,
) {
    val selected = MutableStateFlow(ExistingWorksiteIdentifierNone)

    suspend fun onNetworkWorksiteSelected(networkWorksiteId: Long) {
        val incidentId = worksiteProvider.editableWorksite.value.incidentId
        incidentsRepository.getIncident(incidentId, false)?.let {
            val worksiteId = worksitesRepository.getLocalId(networkWorksiteId)
            if (worksiteId > 0) {
                selected.value = ExistingWorksiteIdentifier(incidentId, worksiteId)
            }
        }
    }
}
