package com.crisiscleanup.core.database.dao

import com.crisiscleanup.core.database.TestCrisisCleanupDatabase
import com.crisiscleanup.core.database.TestUtil
import com.crisiscleanup.core.database.TestUtil.testSyncLogger
import com.crisiscleanup.core.database.TestWorksiteDao
import com.crisiscleanup.core.database.WorksiteTestUtil
import com.crisiscleanup.core.database.model.WorksiteEntities
import com.crisiscleanup.core.database.model.WorksiteEntity
import com.crisiscleanup.core.database.model.WorksiteFlagEntity
import com.crisiscleanup.core.database.model.WorksiteFormDataEntity
import com.crisiscleanup.core.database.model.WorksiteNoteEntity
import com.crisiscleanup.core.database.model.asExternalModel
import com.crisiscleanup.core.model.data.WorksiteFlag
import com.crisiscleanup.core.model.data.WorksiteFormValue
import com.crisiscleanup.core.model.data.WorksiteNote
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

class WorksiteFormDataFlagNoteTest {
    private lateinit var db: TestCrisisCleanupDatabase

    private lateinit var worksiteDao: WorksiteDao
    private lateinit var worksiteDaoPlus: WorksiteDaoPlus
    private lateinit var testWorksiteDao: TestWorksiteDao

    private val syncLogger = testSyncLogger()
    private val appLogger = TestUtil.testAppLogger()

    private suspend fun insertWorksites(
        worksites: List<WorksiteEntity>,
        syncedAt: Instant,
    ) = WorksiteTestUtil.insertWorksites(
        db,
        syncedAt,
        *worksites.toTypedArray(),
    )

    @Before
    fun createDb() {
        db = TestUtil.getTestDatabase()
        worksiteDao = db.worksiteDao()
        worksiteDaoPlus = WorksiteDaoPlus(db, syncLogger, appLogger)
        testWorksiteDao = db.testWorksiteDao()
    }

    @Before
    fun seedDb() = runTest {
        val incidentDao = db.incidentDao()
        incidentDao.upsertIncidents(WorksiteTestUtil.testIncidents)
    }

    private val now = Clock.System.now()

    private val previousSyncedAt = now.plus((-999_999).seconds)

    private val createdAtA = previousSyncedAt.plus((-4_812).seconds)
    private val updatedAtA = createdAtA.plus(18_458.seconds)

    private val updatedAtB = updatedAtA.plus(49_841.seconds)

    private val myOrgId = 217L

    @Test
    fun syncNewWorksite() = runTest {
        // Sync
        val syncingWorksite = testWorksiteEntity(1, 1, "sync-address", updatedAtA)
        val syncingFormData = listOf(
            testFormDataEntity(
                1,
                "form-field-c",
                value = "doesn't matter",
                isBoolValue = true,
                valueBool = false,
            ),
        )
        val syncingFlags = listOf(
            testFullFlagEntity(432, 1, updatedAtB, true, "new-a"),
        )
        val syncingNotes = listOf(
            testNotesEntity(34, 1, updatedAtB, "note-new-a", isSurvivor = true),
            testNotesEntity(45, 1, updatedAtA, "note-new-b"),
        )
        // Sync existing
        val syncedAt = previousSyncedAt.plus(499_999.seconds)
        val entities = WorksiteEntities(
            syncingWorksite,
            syncingFlags,
            syncingFormData,
            syncingNotes,
            emptyList(),
        )
        val actual = worksiteDaoPlus.syncWorksite(entities, syncedAt)

        // Assert

        assertEquals(Pair(true, 1L), actual)

        val actualPopulatedWorksite = testWorksiteDao.getLocalWorksite(1)
        assertEquals(
            testWorksiteEntity(1, 1, "sync-address", updatedAtA, id = 1),
            actualPopulatedWorksite.entity,
        )

        val expectedFormDataEntities = listOf(
            WorksiteFormDataEntity(
                1,
                "form-field-c",
                true,
                "doesn't matter",
                false,
            ),
        )
        assertEquals(expectedFormDataEntities, actualPopulatedWorksite.formData)

        val expectedFlagEntities = listOf(
            WorksiteFlagEntity(
                1,
                432,
                1,
                "action-new-a",
                updatedAtB,
                true,
                "notes-new-a",
                "reason-new-a",
                "requested-action-new-a",
            ),
        )
        assertEquals(expectedFlagEntities, actualPopulatedWorksite.flags)

        val expectedNoteEntities = listOf(
            WorksiteNoteEntity(
                1,
                "",
                34,
                1,
                updatedAtB,
                true,
                "note-new-a",
            ),
            WorksiteNoteEntity(
                2,
                "",
                45,
                1,
                updatedAtA,
                false,
                "note-new-b",
            ),
        )
        assertEquals(expectedNoteEntities, actualPopulatedWorksite.notes)

        val actualWorksite =
            actualPopulatedWorksite.asExternalModel(myOrgId, WorksiteTestUtil.TestTranslator)

        val expectedFormData = mapOf(
            "form-field-c" to WorksiteFormValue(true, "doesn't matter", false),
        )
        assertEquals(expectedFormData, actualWorksite.worksite.formData)

        val expectedFlags = listOf(
            WorksiteFlag(
                1,
                "action-new-a",
                updatedAtB,
                true,
                "notes-new-a",
                "reason-new-a",
                "reason-new-a-translated",
                "requested-action-new-a",
                attr = null,
            ),
        )
        assertEquals(expectedFlags, actualWorksite.worksite.flags)

        val expectedNotes = listOf(
            WorksiteNote(2, updatedAtA, false, "note-new-b"),
            WorksiteNote(1, updatedAtB, true, "note-new-a"),
        )
        assertEquals(expectedNotes, actualWorksite.worksite.notes)
    }

    /**
     * Syncing form data, flags, and notes overwrite local (where unchanged)
     */
    @Test
    fun syncExistingWorksite() = runTest {
        // Insert existing
        var existingWorksites = listOf(
            testWorksiteEntity(1, 1, "address", updatedAtA),
        )
        existingWorksites = insertWorksites(existingWorksites, previousSyncedAt)

        db.worksiteFormDataDao().upsert(
            listOf(
                testFormDataEntity(1, "form-field-a"),
                testFormDataEntity(1, "form-field-b"),
                testFormDataEntity(1, "form-field-c", isBoolValue = true, valueBool = true),
            ),
        )
        db.worksiteFlagDao().insertIgnore(
            listOf(
                testFlagEntity(11, 1, createdAtA, "flag-a"),
                testFlagEntity(12, 1, createdAtA, "flag-b"),
            ),
        )
        db.worksiteNoteDao().insertIgnore(
            listOf(
                testNotesEntity(21, 1, createdAtA, "note-a"),
                testNotesEntity(22, 1, createdAtA, "note-b"),
            ),
        )

        // Sync
        val syncingWorksite = testWorksiteEntity(1, 1, "sync-address", updatedAtB)
        val syncingFormData = listOf(
            // Update
            testFormDataEntity(1, "form-field-b", "updated-value"),
            testFormDataEntity(
                1,
                "form-field-c",
                value = "doesn't matter",
                isBoolValue = true,
                valueBool = false,
            ),
            // Delete form-field-a
            // New
            testFormDataEntity(1, "form-field-new-a", "value-new"),
        )
        val syncingFlags = listOf(
            // New
            testFullFlagEntity(432, 1, updatedAtA, false, "new-a"),
            // Delete 11
            // Update
            testFlagEntity(
                12,
                1,
                updatedAtA,
                "flag-b",
                action = "updated-flag-b",
                isHighPriority = true,
                notes = "updated-notes-flag-b",
                requestedAction = "updated-requested-action-flag-b",
            ),
        )
        val syncingNotes = listOf(
            // Update
            testNotesEntity(21, 1, updatedAtA, "note-update-a", isSurvivor = true),
            // New
            testNotesEntity(34, 1, updatedAtA, "note-new-a", isSurvivor = true),
            testNotesEntity(45, 1, updatedAtA, "note-new-b"),
            // Delete 22
        )
        // Sync existing
        val syncedAt = previousSyncedAt.plus(499_999.seconds)
        val entities = WorksiteEntities(
            syncingWorksite,
            syncingFlags,
            syncingFormData,
            syncingNotes,
            emptyList(),
        )
        val actual = worksiteDaoPlus.syncWorksite(entities, syncedAt)

        // Assert

        assertEquals(Pair(true, existingWorksites[0].id), actual)

        val actualPopulatedWorksite = testWorksiteDao.getLocalWorksite(1)
        assertEquals(
            existingWorksites[0].copy(
                address = "sync-address",
                updatedAt = updatedAtB,
            ),
            actualPopulatedWorksite.entity,
        )

        val actualWorksite =
            actualPopulatedWorksite.asExternalModel(myOrgId, WorksiteTestUtil.TestTranslator)

        val expectedFormData = mapOf(
            "form-field-b" to WorksiteFormValue(false, "updated-value", false),
            "form-field-c" to WorksiteFormValue(true, "doesn't matter", false),
            "form-field-new-a" to WorksiteFormValue(false, "value-new", false),
        )
        assertEquals(expectedFormData, actualWorksite.worksite.formData)

        val expectedFlags = listOf(
            WorksiteFlag(
                2,
                "updated-flag-b",
                updatedAtA,
                true,
                "updated-notes-flag-b",
                "flag-b",
                "flag-b-translated",
                "updated-requested-action-flag-b",
                attr = null,
            ),
            WorksiteFlag(
                3,
                "action-new-a",
                updatedAtA,
                false,
                "notes-new-a",
                "reason-new-a",
                "reason-new-a-translated",
                "requested-action-new-a",
                attr = null,
            ),
        )
        assertEquals(expectedFlags, actualWorksite.worksite.flags)

        val expectedNotes = listOf(
            WorksiteNote(5, updatedAtA, false, "note-new-b"),
            WorksiteNote(4, updatedAtA, true, "note-new-a"),
            WorksiteNote(1, updatedAtA, true, "note-update-a"),
        )
        assertEquals(expectedNotes, actualWorksite.worksite.notes)
    }

    @Test
    fun syncSkipLocallyModified() = runTest {
        // Insert existing
        var existingWorksites = listOf(
            testWorksiteEntity(1, 1, "address", updatedAtA),
            testWorksiteEntity(2, 1, "address", updatedAtA),
        )
        existingWorksites = insertWorksites(existingWorksites, previousSyncedAt)
        db.testWorksiteDao().setLocallyModified(2, updatedAtA)

        // Sync

        val syncingWorksite = testWorksiteEntity(1, 1, "sync-address", updatedAtB)
        val syncingFormData = listOf(
            testFormDataEntity(
                1,
                "form-field-a",
                "doesn't-matter",
                isBoolValue = true,
                valueBool = false,
            ),
        )
        val syncingFlags = listOf(
            testFullFlagEntity(432, 1, updatedAtA, false, "flag-a"),
        )
        val syncingNotes = listOf(
            testNotesEntity(34, 1, updatedAtA, "note-a", isSurvivor = true),
        )

        // Sync locally unchanged
        val syncedAt = previousSyncedAt.plus(499_999.seconds)
        val entities = WorksiteEntities(
            syncingWorksite,
            syncingFlags,
            syncingFormData,
            syncingNotes,
            emptyList(),
        )
        val actualSyncWorksite = worksiteDaoPlus.syncWorksite(entities, syncedAt)

        // Sync locally changed
        val syncingWorksiteB = testWorksiteEntity(2, 1, "sync-address", updatedAtB)
        val syncingFormDataB = listOf(
            testFormDataEntity(2, "form-field-b", "updated-value"),
        )
        val syncingFlagsB = listOf(
            testFullFlagEntity(12, 2, updatedAtA, true, "flag-b"),
        )
        val syncingNotesB = listOf(
            testNotesEntity(45, 2, updatedAtA, "note-b"),
        )
        // Sync existing
        val entitiesB = WorksiteEntities(
            syncingWorksiteB,
            syncingFlagsB,
            syncingFormDataB,
            syncingNotesB,
            emptyList(),
        )
        val actualSyncChangeWorksite = worksiteDaoPlus.syncWorksite(entitiesB, syncedAt)

        // Assert

        assertEquals(Pair(true, existingWorksites[0].id), actualSyncWorksite)

        val actualPopulatedWorksite = testWorksiteDao.getLocalWorksite(1)
        assertEquals(
            existingWorksites[0].copy(
                address = "sync-address",
                updatedAt = updatedAtB,
            ),
            actualPopulatedWorksite.entity,
        )

        val actualWorksite =
            actualPopulatedWorksite.asExternalModel(myOrgId, WorksiteTestUtil.TestTranslator)

        val expectedFormData = mapOf(
            "form-field-a" to WorksiteFormValue(true, "doesn't-matter", false),
        )
        assertEquals(expectedFormData, actualWorksite.worksite.formData)

        val expectedFlags = listOf(
            WorksiteFlag(
                1,
                "action-flag-a",
                updatedAtA,
                false,
                "notes-flag-a",
                "reason-flag-a",
                "reason-flag-a-translated",
                "requested-action-flag-a",
                attr = null,
            ),
        )
        assertEquals(expectedFlags, actualWorksite.worksite.flags)

        val expectedNotes = listOf(WorksiteNote(1, updatedAtA, true, "note-a"))
        assertEquals(expectedNotes, actualWorksite.worksite.notes)

        // Locally changed did not sync

        assertEquals(Pair(false, -1L), actualSyncChangeWorksite)
        val actualPopulatedWorksiteB = testWorksiteDao.getLocalWorksite(2)
        assertEquals(existingWorksites[1], actualPopulatedWorksiteB.entity)
        val actualWorksiteB =
            actualPopulatedWorksiteB.asExternalModel(myOrgId, WorksiteTestUtil.TestTranslator)
        assertEquals(emptyMap(), actualWorksiteB.worksite.formData)
        assertEquals(emptyList(), actualWorksiteB.worksite.flags)
        assertEquals(emptyList(), actualWorksiteB.worksite.notes)
    }
}

internal fun testFormDataEntity(
    worksiteId: Long,
    key: String,
    value: String = "value",
    isBoolValue: Boolean = false,
    valueBool: Boolean = false,
) = WorksiteFormDataEntity(
    worksiteId = worksiteId,
    fieldKey = key,
    valueString = value,
    isBoolValue = isBoolValue,
    valueBool = valueBool,
)

internal fun testFlagEntity(
    networkId: Long,
    worksiteId: Long,
    createdAt: Instant,
    reasonT: String,
    action: String? = null,
    isHighPriority: Boolean? = null,
    notes: String? = null,
    requestedAction: String? = null,
    id: Long = 0,
) = WorksiteFlagEntity(
    id = id,
    networkId = networkId,
    worksiteId = worksiteId,
    action = action,
    createdAt = createdAt,
    isHighPriority = isHighPriority,
    notes = notes,
    reasonT = reasonT,
    requestedAction = requestedAction,
)

internal fun testFullFlagEntity(
    networkId: Long,
    worksiteId: Long,
    createdAt: Instant,
    isHighPriority: Boolean?,
    postfix: String,
    id: Long = 0,
) = testFlagEntity(
    networkId,
    worksiteId,
    createdAt,
    reasonT = "reason-$postfix",
    action = "action-$postfix",
    isHighPriority = isHighPriority,
    notes = "notes-$postfix",
    requestedAction = "requested-action-$postfix",
    id = id,
)

internal fun testNotesEntity(
    networkId: Long,
    worksiteId: Long,
    createdAt: Instant,
    note: String,
    isSurvivor: Boolean = false,
    id: Long = 0,
    localGlobalUuid: String = "",
) = WorksiteNoteEntity(
    id = id,
    localGlobalUuid = localGlobalUuid,
    networkId = networkId,
    worksiteId = worksiteId,
    createdAt = createdAt,
    isSurvivor = isSurvivor,
    note = note,
)
