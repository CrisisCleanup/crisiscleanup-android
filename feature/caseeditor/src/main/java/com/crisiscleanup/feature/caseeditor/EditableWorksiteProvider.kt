package com.crisiscleanup.feature.caseeditor

import com.crisiscleanup.core.mapmarker.model.IncidentBounds
import com.crisiscleanup.core.mapmarker.model.MapViewCameraBoundsDefault
import com.crisiscleanup.core.model.data.EmptyIncident
import com.crisiscleanup.core.model.data.EmptyWorksite
import com.crisiscleanup.core.model.data.Incident
import com.crisiscleanup.core.model.data.Worksite
import com.crisiscleanup.feature.caseeditor.model.FormFieldNode
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Singleton

interface EditableWorksiteProvider {
    var incident: Incident
    var incidentBounds: IncidentBounds
    val editableWorksite: MutableStateFlow<Worksite>
    var formFields: List<FormFieldNode>
}

fun EditableWorksiteProvider.reset(incidentId: Long = EmptyIncident.id) = run {
    incident = EmptyIncident
    incidentBounds = IncidentBounds(emptyList(), MapViewCameraBoundsDefault.bounds)
    editableWorksite.value = EmptyWorksite.copy(
        incidentId = incidentId,
    )
    formFields = emptyList()
}

@Singleton
class SingleEditableWorksiteProvider @Inject constructor() : EditableWorksiteProvider {
    override var incident = EmptyIncident
    override var incidentBounds = IncidentBounds(emptyList(), MapViewCameraBoundsDefault.bounds)
    override val editableWorksite = MutableStateFlow(EmptyWorksite)
    override var formFields = emptyList<FormFieldNode>()
}

@Module
@InstallIn(SingletonComponent::class)
interface EditableWorksiteModule {
    @Binds
    @Singleton
    fun bindsEditableWorksiteProvider(
        provider: SingleEditableWorksiteProvider
    ): EditableWorksiteProvider
}