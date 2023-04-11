package com.crisiscleanup.feature.caseeditor

import com.crisiscleanup.core.common.WorksiteLocationEditor
import com.crisiscleanup.core.mapmarker.model.IncidentBounds
import com.crisiscleanup.core.mapmarker.model.MapViewCameraBoundsDefault
import com.crisiscleanup.core.model.data.EmptyIncident
import com.crisiscleanup.core.model.data.EmptyWorksite
import com.crisiscleanup.core.model.data.Incident
import com.crisiscleanup.core.model.data.Worksite
import com.crisiscleanup.feature.caseeditor.model.EmptyFormFieldNode
import com.crisiscleanup.feature.caseeditor.model.FormFieldNode
import com.google.android.gms.maps.model.LatLng
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

interface EditableWorksiteProvider {
    var incident: Incident
    var incidentBounds: IncidentBounds
    val editableWorksite: MutableStateFlow<Worksite>
    var formFields: List<FormFieldNode>
    var formFieldTranslationLookup: Map<String, String>

    val isStale: Boolean
    fun setStale()
    fun takeStale(): Boolean

    fun setEditedLocation(coordinates: LatLng)
    fun clearEditedLocation()
}

fun EditableWorksiteProvider.getGroupNode(key: String) =
    formFields.firstOrNull { it.fieldKey == key } ?: EmptyFormFieldNode

fun EditableWorksiteProvider.reset(incidentId: Long = EmptyIncident.id) = run {
    incident = EmptyIncident
    incidentBounds = DefaultIncidentBounds
    editableWorksite.value = EmptyWorksite.copy(
        incidentId = incidentId,
    )
    formFields = emptyList()
    formFieldTranslationLookup = emptyMap()

    takeStale()
    clearEditedLocation()
}

@Singleton
class SingleEditableWorksiteProvider @Inject constructor() : EditableWorksiteProvider,
    WorksiteLocationEditor {
    override var incident = EmptyIncident
    override var incidentBounds = DefaultIncidentBounds
    override val editableWorksite = MutableStateFlow(EmptyWorksite)
    override var formFields = emptyList<FormFieldNode>()
    override var formFieldTranslationLookup = emptyMap<String, String>()

    override var isStale = false
        private set

    private val editedLocation: AtomicReference<Pair<Double, Double>?> = AtomicReference()

    override fun setStale() {
        isStale = true
    }

    override fun takeStale(): Boolean {
        if (isStale) {
            isStale = false
            return true
        }
        return false
    }

    private fun setCoordinates(coordinates: LatLng? = null) {
        val latLngPair = if (coordinates == null) null
        else Pair(coordinates.latitude, coordinates.longitude)
        editedLocation.set(latLngPair)
    }

    override fun setEditedLocation(coordinates: LatLng) = setCoordinates(coordinates)

    override fun clearEditedLocation() = setCoordinates(null)

    override fun takeEditedLocation(): Pair<Double, Double>? = editedLocation.getAndSet(null)
}

internal val DefaultIncidentBounds = IncidentBounds(emptyList(), MapViewCameraBoundsDefault.bounds)

@Module
@InstallIn(SingletonComponent::class)
interface EditableWorksiteModule {
    @Binds
    @Singleton
    fun bindsEditableWorksiteProvider(
        provider: SingleEditableWorksiteProvider
    ): EditableWorksiteProvider

    @Binds
    @Singleton
    fun bindsWorksiteLocationEditor(editor: SingleEditableWorksiteProvider): WorksiteLocationEditor
}