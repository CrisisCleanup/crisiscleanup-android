package com.crisiscleanup.core.database.dao

import androidx.room.withTransaction
import com.crisiscleanup.core.database.CrisisCleanupDatabase
import com.crisiscleanup.core.database.model.CaseHistoryEventAttrEntity
import com.crisiscleanup.core.database.model.CaseHistoryEventEntity
import javax.inject.Inject

class CaseHistoryDaoPlus @Inject constructor(
    private val db: CrisisCleanupDatabase,
) {
    suspend fun saveEvents(
        worksiteId: Long,
        events: List<CaseHistoryEventEntity>,
        eventAttrs: List<CaseHistoryEventAttrEntity>,
    ) {
        db.withTransaction {
            val historyDao = db.caseHistoryDao()
            val eventIds = events.map(CaseHistoryEventEntity::id).toSet()
            historyDao.deleteUnspecified(worksiteId, eventIds)
            historyDao.upsertEvents(events)
            historyDao.upsertAttrs(eventAttrs)
        }
    }
}