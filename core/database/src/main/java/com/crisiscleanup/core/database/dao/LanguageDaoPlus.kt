package com.crisiscleanup.core.database.dao

import androidx.room.withTransaction
import com.crisiscleanup.core.database.CrisisCleanupDatabase
import com.crisiscleanup.core.database.model.LanguageTranslationEntity
import javax.inject.Inject

class LanguageDaoPlus @Inject constructor(
    private val db: CrisisCleanupDatabase,
) {
    suspend fun saveLanguages(languages: Collection<LanguageTranslationEntity>) {
        db.withTransaction {
            val languageDao = db.languageDao()
            languages.forEach {
                languageDao.insertIgnoreLanguage(it.key, it.name)
            }
        }
    }
}