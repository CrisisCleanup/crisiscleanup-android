package com.crisiscleanup.feature.caseeditor

import com.crisiscleanup.core.data.repository.IncidentsRepository
import com.crisiscleanup.core.data.repository.WorksitesRepository
import com.crisiscleanup.core.model.data.EmptyIncident
import com.crisiscleanup.core.model.data.EmptyWorksite
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

data class ExistingWorksiteIdentifier(
    val incidentId: Long,
    // This is the local (database) ID not network ID
    val worksiteId: Long,
) {
    val isDefined = incidentId != EmptyIncident.id &&
            worksiteId != EmptyWorksite.id
}

val existingWorksiteIdentifierNone = ExistingWorksiteIdentifier(
    EmptyIncident.id,
    EmptyWorksite.id,
)

class ExistingWorksiteSelector @Inject constructor(
    private val worksiteProvider: EditableWorksiteProvider,
    private val incidentsRepository: IncidentsRepository,
    private val worksitesRepository: WorksitesRepository,
) {
    val selected = MutableStateFlow(existingWorksiteIdentifierNone)

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