package com.crisiscleanup.core.database

import androidx.room.withTransaction
import com.crisiscleanup.core.database.dao.testIncidentEntity
import com.crisiscleanup.core.database.model.WorksiteEntity
import kotlinx.datetime.Instant

// Data and util for tests relating to worksites
object WorksiteTestUtil {
    val testIncidents = listOf(
        testIncidentEntity(1, 6525),
        testIncidentEntity(23, 152),
        testIncidentEntity(456, 514),
    )

    suspend fun insertWorksites(
        db: CrisisCleanupDatabase,
        worksites: List<WorksiteEntity>,
        syncedAt: Instant,
    ): List<WorksiteEntity> {
        return db.withTransaction {
            val worksiteDao = db.worksiteDao()
            worksites.map {
                val id =
                    worksiteDao.insertOrRollbackWorksiteRoot(syncedAt, it.networkId, it.incidentId)
                val updated = it.copy(id = id)
                worksiteDao.insertWorksite(updated)
                updated
            }
        }
    }
}