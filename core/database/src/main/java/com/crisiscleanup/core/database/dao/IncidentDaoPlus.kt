package com.crisiscleanup.core.database.dao

import androidx.room.withTransaction
import com.crisiscleanup.core.database.CrisisCleanupDatabase
import com.crisiscleanup.core.database.model.IncidentEntity
import com.crisiscleanup.core.database.model.IncidentFormFieldEntity
import com.crisiscleanup.core.database.model.IncidentIncidentLocationCrossRef
import com.crisiscleanup.core.database.model.IncidentLocationEntity
import javax.inject.Inject

class IncidentDaoPlus @Inject constructor(
    internal val db: CrisisCleanupDatabase,
) {
    // Tested in IncidentDaoTest
    suspend fun saveIncidents(
        incidents: Collection<IncidentEntity>,
        locations: Collection<IncidentLocationEntity>,
        locationXrs: Collection<IncidentIncidentLocationCrossRef>,
    ) {
        db.withTransaction {
            val incidentDao = db.incidentDao()
            incidentDao.upsertIncidents(incidents)
            incidentDao.upsertIncidentLocations(locations)
            incidentDao.deleteIncidentLocationCrossRefs(incidents.map(IncidentEntity::id))
            incidentDao.insertIgnoreIncidentLocationCrossRefs(locationXrs)
        }
    }

    internal suspend fun updateFormFields(
        incidentId: Long,
        formFields: Collection<IncidentFormFieldEntity>,
    ) {
        if (formFields.isEmpty()) {
            return
        }

        val validFields = formFields
            .filterNot { it.isInvalidated }
        val validFieldKeys = validFields
            .map { it.fieldKey }
            .toSet()
        db.withTransaction {
            val incidentDao = db.incidentDao()
            incidentDao.invalidateUnspecifiedFormFields(
                incidentId,
                validFieldKeys,
            )
            incidentDao.upsertFormFields(validFields)
        }
    }

    suspend fun updateFormFields(incidents: Collection<Pair<Long, List<IncidentFormFieldEntity>>>) {
        db.withTransaction {
            incidents.forEach { updateFormFields(it.first, it.second) }
        }
    }
}
