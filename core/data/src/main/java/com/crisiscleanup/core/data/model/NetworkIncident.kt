package com.crisiscleanup.core.data.model

import com.crisiscleanup.core.database.model.IncidentEntity
import com.crisiscleanup.core.database.model.IncidentFormFieldEntity
import com.crisiscleanup.core.database.model.IncidentIncidentLocationCrossRef
import com.crisiscleanup.core.database.model.IncidentLocationEntity
import com.crisiscleanup.core.network.model.NetworkIncident
import com.crisiscleanup.core.network.model.NetworkIncidentFormField
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

fun NetworkIncident.asEntity() = IncidentEntity(
    id = id,
    startAt = startAt,
    name = name,
    shortName = shortName,
    // Active phone numbers are unique to incidents and not complex in structure so treat as comma delimited string value
    activePhoneNumber = activePhoneNumber?.joinToString(",", transform = String::trim),
    isArchived = isArchived ?: false,
    type = type,
)

fun NetworkIncident.locationsAsEntity(): List<IncidentLocationEntity> =
    locations.map { location -> IncidentLocationEntity(location.id, location.location) }

fun NetworkIncident.incidentLocationCrossReferences() =
    locations.map { IncidentIncidentLocationCrossRef(id, it.id) }

fun NetworkIncidentFormField.asEntity(incidentId: Long): IncidentFormFieldEntity {
    val valuesMap = values
        ?.filter { it.value?.isNotEmpty() == true }
        ?.fold(mutableMapOf<String, String>()) { acc, curr ->
            acc[curr.value!!] = curr.name
            acc
        } ?: emptyMap()
    val valuesJson = if (valuesMap.isNotEmpty()) Json.encodeToString(valuesMap) else null
    val valuesDefaultJson =
        if (valuesJson == null &&
            valuesDefault?.isNotEmpty() == true &&
            !isCheckboxDefaultTrue
        ) {
            Json.encodeToString(valuesDefault)
        } else {
            null
        }
    return IncidentFormFieldEntity(
        incidentId = incidentId,
        label = label,
        htmlType = htmlType,
        dataGroup = dataGroup,
        help = help,
        placeholder = placeholder,
        readOnlyBreakGlass = readOnlyBreakGlass,
        valuesDefaultJson = valuesDefaultJson,
        isCheckboxDefaultTrue = isCheckboxDefaultTrue,
        orderLabel = orderLabel ?: -1,
        validation = validation,
        recurDefault = recurDefault,
        valuesJson = valuesJson,
        isRequired = isRequired,
        isReadOnly = isReadOnly,
        listOrder = listOrder,
        isInvalidated = invalidatedAt != null,
        fieldKey = fieldKey,
        fieldParentKey = fieldParentKey,
        selectToggleWorkType = selectToggleWorkType,
    )
}