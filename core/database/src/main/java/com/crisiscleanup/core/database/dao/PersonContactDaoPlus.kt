package com.crisiscleanup.core.database.dao

import androidx.room.withTransaction
import com.crisiscleanup.core.database.CrisisCleanupDatabase
import com.crisiscleanup.core.database.model.PersonContactEntity
import com.crisiscleanup.core.database.model.PersonOrganizationCrossRef
import javax.inject.Inject

class PersonContactDaoPlus @Inject constructor(
    internal val db: CrisisCleanupDatabase,
) {
    suspend fun savePersons(
        contacts: List<PersonContactEntity>,
        personOrganizations: List<PersonOrganizationCrossRef>,
    ) = db.withTransaction {
        val contactDao = db.personContactDao()
        contactDao.upsert(contacts)

        contactDao.upsertPersonOrganizations(personOrganizations)
    }
}
