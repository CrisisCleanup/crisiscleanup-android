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
        incidentIncidentLocationCrossRefs: List<IncidentIncidentLocationCrossRef>,
    ) {
        db.withTransaction {
            val incidentDao = db.incidentDao()
            incidentDao.upsertIncidents(incidents)
            incidentDao.upsertIncidentLocations(incidentLocations)
            incidentDao.insertIgnoreIncidentIncidentLocationCrossRefs(
                incidentIncidentLocationCrossRefs
            )
        }
    }
}