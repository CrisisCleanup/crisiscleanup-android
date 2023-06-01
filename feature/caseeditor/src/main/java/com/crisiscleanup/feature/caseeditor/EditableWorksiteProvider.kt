package com.crisiscleanup.feature.caseeditor

import com.crisiscleanup.core.common.WorksiteLocationEditor
import com.crisiscleanup.core.mapmarker.model.DefaultIncidentBounds
import com.crisiscleanup.core.mapmarker.model.IncidentBounds
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
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

interface EditableWorksiteProvider {
    var incident: Incident
    var incidentBounds: IncidentBounds
    val editableWorksite: MutableStateFlow<Worksite>
    var formFields: List<FormFieldNode>
    var formFieldTranslationLookup: Map<String, String>
    var workTypeTranslationLookup: Map<String, String>

    val isStale: Boolean
    fun setStale()
    fun takeStale(): Boolean

    fun setEditedLocation(coordinates: LatLng)
    fun clearEditedLocation()

    val isAddressChanged: Boolean
    fun setAddressChanged(worksite: Worksite)
    fun takeAddressChanged(): Boolean

    fun translate(key: String): String?
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
    workTypeTranslationLookup = emptyMap()

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
    override var workTypeTranslationLookup = emptyMap<String, String>()

    private val _isStale = AtomicBoolean(false)
    override val isStale: Boolean
        get() = _isStale.get()

    private val editedLocation: AtomicReference<Pair<Double, Double>?> = AtomicReference()

    override fun setStale() {
        _isStale.set(true)
    }

    override fun takeStale() = _isStale.getAndSet(false)

    private fun setCoordinates(coordinates: LatLng? = null) {
        val latLngPair = if (coordinates == null) null
        else Pair(coordinates.latitude, coordinates.longitude)
        editedLocation.set(latLngPair)
    }

    override fun setEditedLocation(coordinates: LatLng) = setCoordinates(coordinates)

    override fun clearEditedLocation() = setCoordinates(null)

    override fun takeEditedLocation(): Pair<Double, Double>? = editedLocation.getAndSet(null)

    private val _isAddressChanged = AtomicBoolean()
    override val isAddressChanged: Boolean
        get() = _isAddressChanged.get()

    override fun setAddressChanged(worksite: Worksite) {
        _isAddressChanged.set(true)
        editableWorksite.value = worksite
    }

    override fun takeAddressChanged() = _isAddressChanged.getAndSet(false)

    override fun translate(key: String) =
        formFieldTranslationLookup[key] ?: workTypeTranslationLookup[key]
}

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