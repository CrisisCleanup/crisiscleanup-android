package com.crisiscleanup.core.database.dao

import androidx.room.withTransaction
import com.crisiscleanup.core.database.CrisisCleanupDatabase
import com.crisiscleanup.core.database.model.IncidentEntity
import com.crisiscleanup.core.database.model.IncidentIncidentLocationCrossRef
import com.crisiscleanup.core.database.model.IncidentLocationEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IncidentDaoPlus @Inject constructor(
    private val db: CrisisCleanupDatabase
) {
    // Tested in IncidentDaoTest
    suspend fun saveIncidents(
        incidents: List<IncidentEntity>,
        incidentLocations: List<IncidentLocationEntity>,
        idMap: Map<Long, Set<Long>>
    ) {
        val incidentCrossRefs = mutableListOf<IncidentIncidentLocationCrossRef>()
        for ((incidentId, locationIds) in idMap.entries) {
            for (locationId in locationIds) {
                incidentCrossRefs.add(
                    IncidentIncidentLocationCrossRef(incidentId, locationId)
                )
            }
        }

        db.withTransaction {
            db.incidentDao().upsertIncidents(incidents)
            db.incidentDao().upsertIncidentLocations(incidentLocations)
            db.incidentDao().insertIgnoreIncidentIncidentLocationCrossRefs(incidentCrossRefs)
        }
    }
}