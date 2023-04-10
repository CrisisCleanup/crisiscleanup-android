package com.crisiscleanup.core.database

import androidx.room.withTransaction
import com.crisiscleanup.core.common.KeyTranslator
import com.crisiscleanup.core.database.dao.testIncidentEntity
import com.crisiscleanup.core.database.model.WorksiteEntity
import kotlinx.coroutines.flow.MutableStateFlow
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
        syncedAt: Instant,
        vararg worksites: WorksiteEntity,
    ): List<WorksiteEntity> {
        return db.withTransaction {
            val worksiteDao = db.worksiteDao()
            worksites.map {
                val id =
                    worksiteDao.insertOrRollbackWorksiteRoot(syncedAt, it.networkId, it.incidentId)
                val updated = it.copy(id = id)
                worksiteDao.insert(updated)
                updated
            }
        }
    }

    object TestTranslator : KeyTranslator {
        override val translationCount = MutableStateFlow(0)

        override fun translate(phraseKey: String) = "$phraseKey-translated"
    }
}