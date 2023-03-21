package com.crisiscleanup.feature.caseeditor

import com.crisiscleanup.core.mapmarker.model.IncidentBounds
import com.crisiscleanup.core.mapmarker.model.MapViewCameraBoundsDefault
import com.crisiscleanup.core.model.data.EmptyWorksite
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
    val editableWorksite: MutableStateFlow<Worksite>
    var formFields: List<FormFieldNode>
    var incidentBounds: IncidentBounds
}

@Singleton
class SingleEditableWorksiteProvider @Inject constructor() : EditableWorksiteProvider {
    override val editableWorksite = MutableStateFlow(EmptyWorksite)
    override var formFields: List<FormFieldNode> = emptyList()
    override var incidentBounds: IncidentBounds =
        IncidentBounds(emptyList(), MapViewCameraBoundsDefault.bounds)
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