package com.crisiscleanup.core.database.dao

import com.crisiscleanup.core.database.TestCrisisCleanupDatabase
import com.crisiscleanup.core.database.TestUtil
import com.crisiscleanup.core.database.TestWorksiteDao
import com.crisiscleanup.core.database.WorksiteTestUtil
import com.crisiscleanup.core.database.model.*
import com.crisiscleanup.core.model.data.WorksiteFlag
import com.crisiscleanup.core.model.data.WorksiteFormValue
import com.crisiscleanup.core.model.data.WorksiteNote
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class WorksiteFormDataFlagNoteTest {
    private lateinit var db: TestCrisisCleanupDatabase

    private lateinit var worksiteDao: WorksiteDao
    private lateinit var worksiteDaoPlus: WorksiteDaoPlus
    private lateinit var testWorksiteDao: TestWorksiteDao

    private suspend fun insertWorksites(
        worksites: List<WorksiteEntity>,
        syncedAt: Instant,
    ): List<WorksiteEntity> {
        return WorksiteTestUtil.insertWorksites(db, worksites, syncedAt)
    }

    @Before
    fun createDb() {
        db = TestUtil.getTestDatabase()
        worksiteDao = db.worksiteDao()
        worksiteDaoPlus = WorksiteDaoPlus(db)
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
                valueBool = false
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
        val syncedWorksiteId = worksiteDaoPlus.syncWorksite(
            1,
            syncingWorksite,
            emptyList(),
            syncingFormData,
            syncingFlags,
            syncingNotes,
            syncedAt,
        )

        // Assert

        assertEquals(1, syncedWorksiteId)

        val actualPopulatedWorksite = testWorksiteDao.getLocalWorksite(1)
        assertEquals(
            testWorksiteEntity(1, 1, "sync-address", updatedAtA, id = 1),
            actualPopulatedWorksite.entity,
        )

        val expectedFormDataEntities = listOf(
            WorksiteFormDataEntity(
                1, "form-field-c", true, "doesn't matter", false
            )
        )
        assertEquals(expectedFormDataEntities, actualPopulatedWorksite.formData)

        val expectedFlagEntities = listOf(
            WorksiteFlagEntity(
                1,
                "",
                false,
                432,
                1,
                "action-new-a",
                updatedAtB,
                true,
                "notes-new-a",
                "reason-new-a",
                "requested-action-new-a",
            )
        )
        assertEquals(expectedFlagEntities, actualPopulatedWorksite.flags)

        val expectedNoteEntities = listOf(
            WorksiteNoteEntity(
                1, "", 34, 1, updatedAtB, true, "note-new-a"
            ),
            WorksiteNoteEntity(
                2, "", 45, 1, updatedAtA, false, "note-new-b"
            ),
        )
        assertEquals(expectedNoteEntities, actualPopulatedWorksite.notes)

        val actualWorksite =
            actualPopulatedWorksite.asExternalModel(WorksiteTestUtil.TestTranslator)

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
            ),
        )
        assertEquals(expectedFlags, actualWorksite.worksite.flags)

        val expectedNotes = listOf(
            WorksiteNote(1, updatedAtB, true, "note-new-a"),
            WorksiteNote(2, updatedAtA, false, "note-new-b"),
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

        db.worksiteFormDataDao().syncUpsert(
            listOf(
                testFormDataEntity(1, "form-field-a"),
                testFormDataEntity(1, "form-field-b"),
                testFormDataEntity(1, "form-field-c", isBoolValue = true, valueBool = true),
            )
        )
        db.testWorksiteFlagDao().insertFlags(
            listOf(
                testFlagEntity(11, 1, createdAtA, "flag-a"),
                testFlagEntity(12, 1, createdAtA, "flag-b"),
            )
        )
        db.testWorksiteNoteDao().insertNotes(
            listOf(
                testNotesEntity(21, 1, createdAtA, "note-a"),
                testNotesEntity(22, 1, createdAtA, "note-b"),
            )
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
                valueBool = false
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
            testFullFlagEntity(12, 1, updatedAtA, true, "update-a"),
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
        val syncedWorksiteId = worksiteDaoPlus.syncWorksite(
            1,
            syncingWorksite,
            emptyList(),
            syncingFormData,
            syncingFlags,
            syncingNotes,
            syncedAt,
        )

        // Assert

        assertEquals(existingWorksites[0].id, syncedWorksiteId)

        val actualPopulatedWorksite = testWorksiteDao.getLocalWorksite(1)
        assertEquals(
            existingWorksites[0].copy(
                address = "sync-address",
                updatedAt = updatedAtB,
            ),
            actualPopulatedWorksite.entity,
        )

        val actualWorksite =
            actualPopulatedWorksite.asExternalModel(WorksiteTestUtil.TestTranslator)

        val expectedFormData = mapOf(
            "form-field-b" to WorksiteFormValue(false, "updated-value", false),
            "form-field-c" to WorksiteFormValue(true, "doesn't matter", false),
            "form-field-new-a" to WorksiteFormValue(false, "value-new", false),
        )
        assertEquals(expectedFormData, actualWorksite.worksite.formData)

        val expectedFlags = listOf(
            WorksiteFlag(
                2,
                "action-update-a",
                updatedAtA,
                true,
                "notes-update-a",
                "reason-update-a",
                "reason-update-a-translated",
                "requested-action-update-a",
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
                1, "form-field-a", "doesn't-matter",
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
        val syncedWorksiteId = worksiteDaoPlus.syncWorksite(
            1,
            syncingWorksite,
            emptyList(),
            syncingFormData,
            syncingFlags,
            syncingNotes,
            syncedAt,
        )

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
        val syncedLocallyChangedWorksiteId = worksiteDaoPlus.syncWorksite(
            1,
            syncingWorksiteB,
            emptyList(),
            syncingFormDataB,
            syncingFlagsB,
            syncingNotesB,
            syncedAt,
        )

        // Assert

        assertEquals(existingWorksites[0].id, syncedWorksiteId)

        val actualPopulatedWorksite = testWorksiteDao.getLocalWorksite(1)
        assertEquals(
            existingWorksites[0].copy(
                address = "sync-address",
                updatedAt = updatedAtB,
            ),
            actualPopulatedWorksite.entity,
        )

        val actualWorksite =
            actualPopulatedWorksite.asExternalModel(WorksiteTestUtil.TestTranslator)

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
            ),
        )
        assertEquals(expectedFlags, actualWorksite.worksite.flags)

        val expectedNotes = listOf(WorksiteNote(1, updatedAtA, true, "note-a"))
        assertEquals(expectedNotes, actualWorksite.worksite.notes)

        // Locally changed did not sync

        assertTrue(syncedLocallyChangedWorksiteId < 0)
        val actualPopulatedWorksiteB = testWorksiteDao.getLocalWorksite(2)
        assertEquals(existingWorksites[1], actualPopulatedWorksiteB.entity)
        val actualWorksiteB =
            actualPopulatedWorksiteB.asExternalModel(WorksiteTestUtil.TestTranslator)
        assertEquals(emptyMap(), actualWorksiteB.worksite.formData)
        assertEquals(emptyList(), actualWorksiteB.worksite.flags)
        assertEquals(emptyList(), actualWorksiteB.worksite.notes)
    }

    @Test
    fun skipInvalidData() = runTest {
        val existingWorksites = listOf(
            testWorksiteEntity(1, 1, "address", updatedAtA),
        )
        insertWorksites(existingWorksites, previousSyncedAt)

        db.testWorksiteFlagDao().insertFlags(
            listOf(
                testFullFlagEntity(432, 1, updatedAtA, false, "flag-a", isInvalid = true),
                testFullFlagEntity(12, 1, updatedAtA, true, "flag-a"),
            )
        )

        val actual = testWorksiteDao.getLocalWorksite(1)
            .asExternalModel(WorksiteTestUtil.TestTranslator).worksite.flags
        val expected = listOf(
            WorksiteFlag(
                id = 2,
                createdAt = updatedAtA,
                action = "action-flag-a",
                isHighPriority = true,
                notes = "notes-flag-a",
                reasonT = "reason-flag-a",
                reason = "reason-flag-a-translated",
                requestedAction = "requested-action-flag-a",
            )
        )
        assertEquals(expected, actual)
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
    isInvalid: Boolean = false,
) = WorksiteFlagEntity(
    id = id,
    localGlobalUuid = "",
    isInvalid = isInvalid,
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
    isInvalid: Boolean = false,
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
    isInvalid = isInvalid,
)

internal fun testNotesEntity(
    networkId: Long,
    worksiteId: Long,
    createdAt: Instant,
    note: String,
    isSurvivor: Boolean = false,
    id: Long = 0,
) = WorksiteNoteEntity(
    id = id,
    localGlobalUuid = "",
    networkId = networkId,
    worksiteId = worksiteId,
    createdAt = createdAt,
    isSurvivor = isSurvivor,
    note = note,
)