package com.crisiscleanup.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.crisiscleanup.core.database.model.LanguageTranslationEntity
import com.crisiscleanup.core.database.model.PopulatedLanguage
import com.crisiscleanup.core.database.model.PopulatedLanguageTranslation
import kotlinx.coroutines.flow.Flow

@Dao
interface LanguageDao {
    @Transaction
    @Query("SELECT COUNT(*) FROM language_translations")
    fun getLanguageCount(): Int

    @Transaction
    @Query("SELECT key, name FROM language_translations")
    fun streamLanguages(): Flow<List<PopulatedLanguage>>

    @Transaction
    @Query("SELECT * FROM language_translations WHERE key=:key")
    fun streamLanguageTranslations(key: String): Flow<PopulatedLanguageTranslation?>

    @Upsert
    fun upsertLanguageTranslation(translations: LanguageTranslationEntity)

    @Transaction
    @Query(
        """
        INSERT OR IGNORE INTO language_translations(key, name)
        VALUES(:key, :name)
        """
    )
    fun insertIgnoreLanguage(
        key: String,
        name: String,
    )
}