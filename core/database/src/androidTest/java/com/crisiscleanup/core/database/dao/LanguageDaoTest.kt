package com.crisiscleanup.core.database.dao

import com.crisiscleanup.core.common.epochZero
import com.crisiscleanup.core.database.CrisisCleanupDatabase
import com.crisiscleanup.core.database.TestUtil
import com.crisiscleanup.core.database.model.LanguageTranslationEntity
import com.crisiscleanup.core.database.model.asExternalModel
import com.crisiscleanup.core.model.data.Language
import com.crisiscleanup.core.model.data.LanguageTranslations
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.days

class LanguageDaoTest {
    private lateinit var db: CrisisCleanupDatabase

    private lateinit var languageDao: LanguageDao
    private lateinit var languageDaoPlus: LanguageDaoPlus

    @Before
    fun createDb() {
        db = TestUtil.getDatabase()
        languageDao = db.languageDao()
        languageDaoPlus = LanguageDaoPlus(db)
    }

    private val languages1 = listOf(
        testLanguageEntity("en-US", "English"),
        testLanguageEntity("sp-MX", "Spanish Mexico"),
        testLanguageEntity("sp-ES", "Spanish Spain"),
    )

    @Test
    fun updateLanguage() = runTest {
        languageDaoPlus.saveLanguages(languages1)

        val syncedAt = Clock.System.now().minus(1.days)
        languageDao.upsertLanguageTranslation(
            LanguageTranslationEntity(
                key = languages1[0].key,
                name = languages1[0].name,
                translationsJson = Json.encodeToString(
                    mapOf("a" to "b"),
                ),
                syncedAt = syncedAt,
            ),
        )

        languageDaoPlus.saveLanguages(
            listOf(
                testLanguageEntity("sp-MX", "Spanish Mexico"),
                testLanguageEntity("en-GB", "British"),
            ),
        )

        val languages2 = languages1.toMutableList().also {
            it.add(testLanguageEntity("en-GB", "British"))
        }
        languages2.forEach {
            val translations = languageDao.streamLanguageTranslations(it.key).first()
            val expected = if (it.key == "en-US") {
                LanguageTranslations(
                    Language("en-US", "English"),
                    translations = mapOf("a" to "b"),
                    syncedAt = syncedAt,
                )
            } else {
                LanguageTranslations(
                    Language(it.key, it.name),
                    translations = emptyMap(),
                    syncedAt = Instant.epochZero,
                )
            }
            assertEquals(expected, translations!!.asExternalModel())
        }
    }
}

private fun testLanguageEntity(key: String, name: String) = LanguageTranslationEntity(
    key = key,
    name = name,
    translationsJson = null,
    syncedAt = null,
)
