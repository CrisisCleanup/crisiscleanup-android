package com.crisiscleanup.core.database.dao

import androidx.room.withTransaction
import com.crisiscleanup.core.database.CrisisCleanupDatabase
import com.crisiscleanup.core.database.model.WorksiteNoteEntity
import javax.inject.Inject

class WorksiteNoteDaoPlus @Inject constructor(
    private val db: CrisisCleanupDatabase,
) {
    suspend fun syncUpsert(workTypes: List<WorksiteNoteEntity>) = db.withTransaction {
        val noteDao = db.worksiteNoteDao()
        workTypes.forEach { note ->
            val id = noteDao.insertIgnoreNote(note)
            if (id < 0) {
                noteDao.syncUpdateNote(
                    worksiteId = note.worksiteId,
                    networkId = note.networkId,
                    createdAt = note.createdAt,
                    isSurvivor = note.isSurvivor,
                    note = note.note,
                )
            }
        }
    }
}
