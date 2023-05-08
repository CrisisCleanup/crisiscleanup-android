package com.crisiscleanup.core.database.model

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.crisiscleanup.core.model.data.Incident

data class PopulatedIncident(
    @Embedded
    val entity: IncidentEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = IncidentIncidentLocationCrossRef::class,
            parentColumn = "incident_id",
            entityColumn = "incident_location_id",
        )
    )
    val locations: List<IncidentLocationEntity>
)

fun PopulatedIncident.asExternalModel() = with(entity) {
    Incident(
        id = id,
        name = name,
        shortName = shortName,
        activePhoneNumbers = activePhoneNumber?.split(",")?.map { it.trim() }
            ?.filter(String::isNotEmpty)
            ?: emptyList(),
        locations = locations.map(IncidentLocationEntity::asExternalModel),
        formFields = emptyList(),
        turnOnRelease = turnOnRelease,
        disasterLiteral = type,
    )
}

data class PopulatedFormFieldsIncident(
    @Embedded
    val entity: PopulatedIncident,
    @Relation(
        parentColumn = "id",
        entityColumn = "incident_id",
    )
    val formFields: List<IncidentFormFieldEntity>,
)

fun PopulatedFormFieldsIncident.asExternalModel() = entity.asExternalModel().copy(
    formFields = formFields.map(IncidentFormFieldEntity::asExternalModel)
        .filter { !(it.isInvalidated || it.isDivEnd) }
)
