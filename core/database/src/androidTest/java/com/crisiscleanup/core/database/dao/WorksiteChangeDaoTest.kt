package com.crisiscleanup.core.database.dao

import com.crisiscleanup.core.database.TestCrisisCleanupDatabase
import com.crisiscleanup.core.database.TestUtil
import com.crisiscleanup.core.database.TestUtil.testAppLogger
import com.crisiscleanup.core.database.TestUtil.testAppVersionProvider
import com.crisiscleanup.core.database.TestUtil.testChangeSerializer
import com.crisiscleanup.core.database.TestUtil.testSyncLogger
import com.crisiscleanup.core.database.TestUtil.testUuidGenerator
import com.crisiscleanup.core.database.WorksiteTestUtil.insertWorksites
import com.crisiscleanup.core.database.WorksiteTestUtil.testIncidents
import com.crisiscleanup.core.database.isNearNow
import com.crisiscleanup.core.database.model.EditWorksiteEntities
import com.crisiscleanup.core.database.model.WorkTypeEntity
import com.crisiscleanup.core.database.model.WorksiteChangeEntity
import com.crisiscleanup.core.database.model.WorksiteFlagEntity
import com.crisiscleanup.core.database.model.WorksiteFormDataEntity
import com.crisiscleanup.core.database.model.WorksiteNoteEntity
import com.crisiscleanup.core.database.model.WorksiteRootEntity
import com.crisiscleanup.core.database.model.asEntities
import com.crisiscleanup.core.model.data.AutoContactFrequency
import com.crisiscleanup.core.model.data.EmptyWorksite
import com.crisiscleanup.core.model.data.WorkType
import com.crisiscleanup.core.model.data.Worksite
import com.crisiscleanup.core.model.data.WorksiteFlag
import com.crisiscleanup.core.model.data.WorksiteFormValue
import com.crisiscleanup.core.model.data.WorksiteNote
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

class WorksiteChangeDaoTest {
    private lateinit var db: TestCrisisCleanupDatabase

    private lateinit var worksiteDao: WorksiteDao
    private lateinit var worksiteDaoPlus: WorksiteDaoPlus
    private lateinit var worksiteChangeDaoPlus: WorksiteChangeDaoPlus

    private val uuidGenerator = testUuidGenerator()
    private val changeSerializer = testChangeSerializer()
    private val appVersionProvider = testAppVersionProvider()
    private val syncLogger = testSyncLogger()
    private val appLogger = testAppLogger()

    private val testIncidentId = testIncidents.last().id

    private val now = Clock.System.now()
    private val createdAtA = now.minus(4.days)
    private val updatedAtA = createdAtA.plus(40.minutes)
    private val createdAtB = createdAtA.plus(1.days)
    private val updatedAtB = createdAtB.plus(51.minutes)
    private val createdAtC = createdAtB.plus(23.hours)

    /**
     * Flags are
     * - 0, reason-a
     * - 33, reason-b
     *
     * Notes are
     * - 0, note-a, atA
     * - 64, note-b, atB
     *
     * Work types are
     * - 0, work-type-a, atA, status-a, null
     * - 57, work-type-b, atB, status-b, 523
     */
    private val worksiteFull = Worksite(
        id = 56,
        address = "address",
        autoContactFrequencyT = AutoContactFrequency.NotOften.literal,
        caseNumber = "case-number",
        city = "city",
        county = "county",
        createdAt = createdAtA,
        email = "email",
        favoriteId = 623,
        flags = listOf(
            testWorksiteFlag(
                0,
                createdAtA,
                "reason-a"
            ),
            testWorksiteFlag(
                33,
                createdAtB,
                "reason-b",
                isHighPriority = true,
            ),
        ),
        formData = mapOf(
            "form-data-bt" to WorksiteFormValue(true, "", true),
            "form-data-sa" to WorksiteFormValue(false, "form-data-value-a"),
            "form-data-bf" to WorksiteFormValue(true, "", false),
        ),
        incidentId = testIncidentId,
        keyWorkType = null,
        latitude = -5.23,
        longitude = -39.35,
        name = "name",
        networkId = -1,
        notes = listOf(
            testWorksiteNote(0, createdAtA, "note-a"),
            testWorksiteNote(64, createdAtB, "note-b"),
        ),
        phone1 = "phone1",
        phone2 = "phone2",
        plusCode = "plus-code",
        postalCode = "postal-code",
        reportedBy = 573,
        state = "state",
        svi = 0.5f,
        updatedAt = updatedAtA,
        what3Words = "what-3-words",
        workTypes = listOf(
            testWorkType(
                0,
                createdAtA,
                null,
                "status-a",
                "work-type-a",
            ),
            testWorkType(
                57,
                createdAtB,
                523,
                "status-b",
                "work-type-b",
            ),
        ),
        isAssignedToOrgMember = true,
    )

    /**
     * Flags are
     * - 1, reason-a
     * - 11, reason-c
     * - 0, reason-d
     *
     * Notes are
     * - 1, note-a, atA
     * - 64, note-b, atB
     * - 0, note-c, atC
     * - 0, note-d, atC
     * - 41, note-e, atB
     *
     * Work types are
     * - 1, work-type-a, atA, status-a-change, null
     * - 23, work-type-c, atC, status-c, 523
     * - 0, work-type-d, atC, status-d, 523
     */
    private val worksiteChanged = worksiteFull.copy(
        address = "address-change",
        autoContactFrequencyT = AutoContactFrequency.Often.literal,
        city = "city-change",
        county = "county-change",
        email = "email-change",
        favoriteId = null,
        flags = listOf(
            // Update createdAt (from full)
            testWorksiteFlag(
                1,
                createdAtC,
                "reason-a"
            ),
            // Delete 33 (from full)
            // Insert and map 11 (network ID)
            testWorksiteFlag(
                11,
                createdAtB,
                "reason-c",
            ),
            // Add
            testWorksiteFlag(
                0,
                createdAtB,
                "reason-d",
            ),
        ),
        formData = mapOf(
            // Delete form-data-bt  (from full)
            // Change  (from full)
            "form-data-sa" to WorksiteFormValue(false, "form-data-value-change-a"),
            // No-op
            "form-data-bf" to WorksiteFormValue(true, "", false),
            // Add
            "form-data-new-c" to WorksiteFormValue(false, "form-data-new-c"),
            "form-data-new-d" to WorksiteFormValue(true, "", false),
        ),
        latitude = 15.23,
        longitude = -319.08,
        name = "name-change",
        networkId = -1,
        notes = listOf(
            // Notes are not mutable
            testWorksiteNote(1, createdAtA, "note-a"),
            testWorksiteNote(64, createdAtB, "note-b"),
            // Add
            testWorksiteNote(0, createdAtC, "note-c"),
            testWorksiteNote(0, createdAtC, "note-d"),
            // Insert and map 41
            testWorksiteNote(41, createdAtB, "note-e"),
        ),
        phone1 = "phone1-change",
        phone2 = "",
        postalCode = "postal-code-change",
        state = "state-change",
        updatedAt = updatedAtB,
        workTypes = listOf(
            // Update (from full)
            testWorkType(
                1,
                createdAtA,
                null,
                "status-a-change",
                "work-type-a",
            ),
            // Delete 57  (from full)
            // Insert and map 23
            testWorkType(
                23,
                createdAtC,
                523,
                "status-c",
                "work-type-c",
            ),
            // Add
            testWorkType(
                0,
                createdAtC,
                523,
                "status-d",
                "work-type-d",
            ),
        ),
        isAssignedToOrgMember = false,
    )

    @Before
    fun setUp() {
        MockKAnnotations.init(this)

        every { appLogger.logDebug(*anyVararg()) } returns Unit
    }

    @Before
    fun createDb() {
        db = TestUtil.getTestDatabase()
        worksiteDao = db.worksiteDao()
        worksiteDaoPlus = WorksiteDaoPlus(db, syncLogger)
        worksiteChangeDaoPlus = WorksiteChangeDaoPlus(
            db,
            uuidGenerator,
            changeSerializer,
            appVersionProvider,
            appLogger,
            syncLogger,
        )
    }

    @Before
    fun seedDb() = runTest {
        db.incidentDao().upsertIncidents(testIncidents)
    }

    private suspend fun insertWorksite(worksite: Worksite): EditWorksiteEntities {
        val entities = worksite.asEntities(
            uuidGenerator,
            worksite.workTypes[0],
            emptyMap(),
            emptyMap(),
            emptyMap(),
        )
        val inserted = insertWorksites(db, now, entities.core)
        val worksiteId = inserted[0].id
        val flags = entities.flags.map { it.copy(worksiteId = worksiteId) }
        db.worksiteFlagDao().insertIgnore(flags)
        val formData = entities.formData.map { it.copy(worksiteId = worksiteId) }
        db.worksiteFormDataDao().upsert(formData)
        val notes = entities.notes.map { it.copy(worksiteId = worksiteId) }
        db.worksiteNoteDao().insertIgnore(notes)
        val workTypes = entities.workTypes.map { it.copy(worksiteId = worksiteId) }
        db.workTypeDao().insertIgnore(workTypes)

        val worksiteEntity = entities.core.copy(id = worksiteId)
        return EditWorksiteEntities(worksiteEntity, flags, formData, notes, workTypes)
    }

    @Test
    fun skipUnchanged() = runTest {
        val entityData = insertWorksite(worksiteFull)

        worksiteChangeDaoPlus.saveChange(
            worksiteChanged,
            worksiteChanged,
            worksiteChanged.workTypes[0],
            385,
        )

        val worksiteEntity = entityData.core
        val worksiteId = worksiteEntity.id
        val actualWorksite = db.testWorksiteDao().getWorksiteEntity(worksiteId)
        assertEquals(worksiteEntity, actualWorksite)

        var entityIndex = 1L
        val expectedFlags = entityData.flags.map { flag ->
            if (flag.id <= 0) flag.copy(id = entityIndex++)
            else flag
        }
        val actualFlags = db.testFlagDao().getEntities(worksiteId)
        assertEquals(expectedFlags, actualFlags)

        val expectedFormData = entityData.formData.sortedBy(WorksiteFormDataEntity::fieldKey)
        val actualFormData = db.testFormDataDao().getEntities(worksiteId)
        assertEquals(expectedFormData, actualFormData)

        entityIndex = 1
        val expectedNotes = entityData.notes.map { note ->
            if (note.id <= 0) note.copy(id = entityIndex++)
            else note
        }
        val actualNotes = db.testNoteDao().getEntities(worksiteId)
        assertEquals(expectedNotes, actualNotes)

        entityIndex = 1
        val expectedWorkTypes = entityData.workTypes.map { workType ->
            if (workType.id <= 0) workType.copy(id = entityIndex++)
            else workType
        }
        val actualWorkTypes = db.testWorkTypeDao().getEntities(worksiteId)
        assertEquals(expectedWorkTypes, actualWorkTypes)

        verify(exactly = 0) { changeSerializer.serialize(any(), allAny()) }
        verify(exactly = 0) { appLogger.logException(any()) }
    }

    @Test
    fun newWorksite() = runTest {
        val newFlags = worksiteFull.flags!!.map { it.copy(id = 0) }
        val newNotes = worksiteFull.notes.map { it.copy(id = 0) }
        val newWorkTypes = worksiteFull.workTypes.map { it.copy(id = 0) }
        val newWorksite = worksiteFull.copy(
            id = 0,
            networkId = -1,
            flags = newFlags,
            notes = newNotes,
            workTypes = newWorkTypes,
        )

        val primaryWorkType = newWorksite.workTypes[0]
        val entities = newWorksite.asEntities(
            uuidGenerator,
            primaryWorkType,
            emptyMap(),
            emptyMap(),
            emptyMap(),
        )

        every {
            changeSerializer.serialize(
                EmptyWorksite,
                newWorksite.copy(
                    id = 1,
                    flags = newFlags.mapIndexed { index, flag -> flag.copy(id = index + 1L) },
                    notes = newNotes.mapIndexed { index, note -> note.copy(id = index + 1L) },
                    workTypes = newWorkTypes.mapIndexed { index, workType -> workType.copy(id = index + 1L) },
                ),
                allAny(),
            )
        } returns Pair(2, "serialized-new-worksite-changes")

        worksiteChangeDaoPlus.saveChange(
            EmptyWorksite,
            newWorksite,
            primaryWorkType,
            385,
            localModifiedAt = now,
        )

        val worksiteEntity = entities.core.copy(id = 1)
        val worksiteId = worksiteEntity.id

        val actualRoot = db.testWorksiteDao().getRootEntity(worksiteId)
        val expectedRoot = WorksiteRootEntity(
            id = 1,
            syncUuid = "uuid-13",
            localModifiedAt = now,
            syncedAt = Instant.fromEpochSeconds(0),
            localGlobalUuid = "uuid-14",
            isLocalModified = true,
            syncAttempt = 0,
            networkId = -1,
            incidentId = testIncidentId,
        )
        assertEquals(expectedRoot, actualRoot)

        val actualWorksite = db.testWorksiteDao().getWorksiteEntity(worksiteId)
        assertEquals(worksiteEntity, actualWorksite)

        var entityIndex = 1L
        var localGlobalIndex = 7L

        val expectedFlags = entities.flags.map {
            it.copy(
                id = entityIndex++,
                worksiteId = 1,
                localGlobalUuid = "uuid-${localGlobalIndex++}",
            )
        }
        val actualFlags = db.testFlagDao().getEntities(worksiteId)
        assertEquals(expectedFlags, actualFlags)

        val expectedFormData = entities.formData
            .map { it.copy(worksiteId = 1) }
            .sortedBy(WorksiteFormDataEntity::fieldKey)
        val actualFormData = db.testFormDataDao().getEntities(worksiteId)
        assertEquals(expectedFormData, actualFormData)

        entityIndex = 1
        localGlobalIndex = 9
        val expectedNotes = entities.notes.map {
            it.copy(
                id = entityIndex++,
                worksiteId = 1,
                localGlobalUuid = "uuid-${localGlobalIndex++}",
            )
        }
        val actualNotes =
            db.testNoteDao().getEntities(worksiteId).sortedBy(WorksiteNoteEntity::createdAt)
        assertEquals(expectedNotes, actualNotes)

        entityIndex = 1
        localGlobalIndex = 11
        val expectedWorkTypes = entities.workTypes.map {
            it.copy(
                id = entityIndex++,
                worksiteId = 1,
                localGlobalUuid = "uuid-${localGlobalIndex++}",
            )
        }
        val actualWorkTypes = db.testWorkTypeDao().getEntities(worksiteId)
        assertEquals(expectedWorkTypes, actualWorkTypes)

        val actualChanges = db.testWorksiteChangeDao().getEntities(worksiteId)
        val expectedWorksiteChange = WorksiteChangeEntity(
            id = 1,
            appVersion = 81,
            organizationId = 385,
            worksiteId = worksiteId,
            syncUuid = "uuid-15",
            changeModelVersion = 2,
            changeData = "serialized-new-worksite-changes",
            createdAt = actualChanges.first().createdAt,
        )
        assertEquals(listOf(expectedWorksiteChange), actualChanges)
        assertTrue(actualChanges.first().createdAt.isNearNow())

        verify(exactly = 0) { appLogger.logException(any()) }
    }

    private fun editWorksiteEntities(worksiteId: Long): EditWorksiteEntities {
        val savedWorksite = db.testWorksiteDao().getWorksiteEntity(worksiteId)
        val flags = db.testFlagDao().getEntities(worksiteId)
        val formData = db.testFormDataDao().getEntities(worksiteId)
        val notes = db.testNoteDao().getEntities(worksiteId)
        val workTypes = db.testWorkTypeDao().getEntities(worksiteId)

        return EditWorksiteEntities(
            savedWorksite!!,
            flags,
            formData,
            notes,
            workTypes,
        )
    }

    /**
     * Establishes initial conditions for [editSyncedWorksite]
     *
     * Maps flags
     * - 1 to 201
     * - 11 to 211, reason-c
     * - 21 to 221
     *
     * Notes
     * - 64 to 264
     * - 41 to 241, note-e, atB
     *
     * Work types
     * - 1 to 301
     * - 23 to 223, work-type-c, atC, ...
     */
    private fun editSyncedWorksite_initialConditions(
        worksite: Worksite,
        worksiteLocalGlobalUuid: String = "",
    ): EditWorksiteEntities {
        val entities = worksite.asEntities(
            uuidGenerator,
            worksite.workTypes[0],
            emptyMap(),
            emptyMap(),
            emptyMap(),
        )
        db.worksiteDao().insertRoot(
            WorksiteRootEntity(
                worksite.id,
                "sync-uuid",
                worksite.updatedAt!!,
                worksite.updatedAt!!,
                worksiteLocalGlobalUuid,
                false,
                0,
                worksite.networkId,
                worksite.incidentId,
            )
        )
        db.worksiteDao().insert(entities.core)
        db.worksiteFlagDao().insertIgnore(entities.flags)
        db.worksiteFormDataDao().upsert(entities.formData)
        db.worksiteNoteDao().insertIgnore(entities.notes)
        db.workTypeDao().insertIgnore(entities.workTypes)

        val worksiteId = if (worksite.id > 0) worksite.id else 1

        // For mapping
        db.testFlagDao().updateNetworkId(1, 201)
        db.worksiteFlagDao().insertIgnore(
            listOf(
                WorksiteFlagEntity(
                    11,
                    "",
                    211,
                    worksiteId,
                    "",
                    createdAtB,
                    false,
                    "",
                    "reason-c",
                    "",
                )
            )
        )
        db.worksiteFlagDao().updateNetworkId(21, 221)

        db.testNoteDao().updateNetworkId(64, 264)
        db.worksiteNoteDao().insertIgnore(
            listOf(
                WorksiteNoteEntity(
                    41,
                    "",
                    241,
                    worksiteId,
                    createdAtB,
                    false,
                    "note-e",
                )
            )
        )

        db.workTypeDao().updateNetworkId(1, 301)
        db.workTypeDao().insertIgnore(
            listOf(
                WorkTypeEntity(
                    23,
                    "",
                    223,
                    worksiteId,
                    createdAt = createdAtC,
                    523,
                    null,
                    2,
                    null,
                    "status-c",
                    "work-type-c",
                )
            )
        )
        db.workTypeDao().updateNetworkId(37, 237)

        return editWorksiteEntities(worksiteId)
    }

    @Test
    fun editSyncedWorksite() = runTest {
        val worksiteSynced = worksiteFull.copy(
            networkId = 515,
            flags = worksiteFull.flags?.toMutableList()?.apply {
                add(
                    // Is mapped to 221 in initialConditions
                    testWorksiteFlag(
                        21,
                        createdAtB,
                        "reason-network-synced-local-deleted",
                        isHighPriority = true,
                    )
                )
            },
            workTypes = worksiteFull.workTypes.toMutableList().apply {
                add(
                    testWorkType(
                        37,
                        createdAtB,
                        128,
                        "status-network-synced-local-deleted",
                        "work-type-c",
                    ),
                )
            }
        )
        /*
         * Flags
         * 1 to 201, reason-a
         * 33, reason-b
         * 11 to 211, reason-c
         * 21 to 221, reason-network-synced-local-deleted
         *
         * Notes
         * 1 to -1
         * 41 to 241
         * 64 to 264
         *
         * Work types
         * 1 to 301
         * 23 to 223
         * 37 to 327
         * 57 to -1
         */
        val initialEntities = editSyncedWorksite_initialConditions(worksiteSynced)

        val worksiteModified = worksiteChanged.copy(networkId = worksiteSynced.networkId)

        every {
            changeSerializer.serialize(
                worksiteSynced,
                worksiteModified.copy(
                    flags = worksiteModified.flags!!.map {
                        if (it.id == 0L) it.copy(id = 34)
                        else it
                    },
                    notes = worksiteModified.notes.mapIndexed { index, note ->
                        if (note.id == 0L) note.copy(id = index + 63L)
                        else note
                    },
                    workTypes = worksiteModified.workTypes.map {
                        if (it.id == 0L) it.copy(id = 58)
                        else it
                    }
                ),
                mapOf(
                    1L to 201,
                    11L to 211,
                    21L to 221,
                ),
                mapOf(
                    41L to 241,
                    64L to 264,
                ),
                mapOf(
                    1L to 301,
                    23L to 223,
                    37L to 237,
                ),
            )
        } returns Pair(3, "serialized-edit-worksite-changes")

        val primaryWorkType = worksiteModified.workTypes[0]

        val entities = worksiteModified.asEntities(
            uuidGenerator,
            primaryWorkType,
            emptyMap(),
            emptyMap(),
            emptyMap(),
        )

        worksiteChangeDaoPlus.saveChange(
            worksiteSynced,
            worksiteModified,
            primaryWorkType,
            385,
            localModifiedAt = now,
        )

        val worksiteEntity = entities.core
        val worksiteId = worksiteEntity.id

        val actualRoot = db.testWorksiteDao().getRootEntity(worksiteId)
        val expectedRoot = WorksiteRootEntity(
            id = 56,
            syncUuid = "uuid-25",
            localModifiedAt = now,
            syncedAt = worksiteSynced.updatedAt!!,
            localGlobalUuid = "",
            isLocalModified = true,
            syncAttempt = 0,
            networkId = 515,
            incidentId = testIncidentId,
        )
        assertEquals(expectedRoot, actualRoot)

        val actualWorksite = db.testWorksiteDao().getWorksiteEntity(worksiteId)
        assertEquals(worksiteEntity, actualWorksite)

        fun expectedFlagEntity(
            id: Long,
            networkId: Long,
            reasonT: String,
            createdAt: Instant = createdAtB,
            localGlobalUuid: String = "",
        ) =
            testFlagEntity(
                id = id,
                localGlobalUuid = localGlobalUuid,
                networkId = networkId,
                worksiteId = 56,
                createdAt = createdAt,
                reasonT = reasonT,
                isHighPriority = false,
                action = "",
                notes = "",
                requestedAction = "",
            )

        val expectedFlags = listOf(
            expectedFlagEntity(1, 201, "reason-a", createdAtC),
            expectedFlagEntity(11, 211, "reason-c"),
            expectedFlagEntity(34, -1, "reason-d", localGlobalUuid = "uuid-20"),
        )
        val actualFlags = db.testFlagDao().getEntities(worksiteId)
            .sortedBy(WorksiteFlagEntity::id)
        assertEquals(expectedFlags, actualFlags)

        val expectedFormData = initialEntities.formData
            .toMutableList()
            .also {
                val deleteIndex = it.indexOfFirst { entity -> entity.fieldKey == "form-data-bt" }
                it.removeAt(deleteIndex)

                val updateIndex = it.indexOfFirst { entity -> entity.fieldKey == "form-data-sa" }
                it[updateIndex] = it[updateIndex].copy(valueString = "form-data-value-change-a")

                entities.formData.forEach { entity ->
                    if (it.find { preEntity -> preEntity.fieldKey == entity.fieldKey } == null) {
                        it.add(entity)
                    }
                }
            }
            .sortedBy(WorksiteFormDataEntity::fieldKey)
            .map { it.copy(worksiteId = 56) }
        val actualFormData = db.testFormDataDao().getEntities(worksiteId)
            .sortedBy(WorksiteFormDataEntity::fieldKey)
        assertEquals(expectedFormData, actualFormData)

        fun expectedNote(
            id: Long,
            networkId: Long,
            note: String,
            createdAt: Instant = createdAtB,
            localGlobalUuid: String = "",
        ) = testNotesEntity(
            id = id,
            networkId = networkId,
            worksiteId = 56,
            createdAt = createdAt,
            note = note,
            localGlobalUuid = localGlobalUuid,
        )

        val expectedNotes = listOf(
            expectedNote(1, -1, "note-a", createdAtA, "uuid-4"),
            expectedNote(41, 241, "note-e", createdAtB),
            expectedNote(64, 264, "note-b", createdAtB),
            expectedNote(65, -1, "note-c", createdAtC, "uuid-22"),
            expectedNote(66, -1, "note-d", createdAtC, "uuid-23"),
        )
        val actualNotes = db.testNoteDao().getEntities(worksiteId)
            .sortedBy(WorksiteNoteEntity::id)
        assertEquals(expectedNotes, actualNotes)

        fun expectedWorkType(
            id: Long,
            networkId: Long,
            workType: String,
            status: String,
            orgClaim: Long? = 523,
            createdAt: Instant? = createdAtC,
            localGlobalUuid: String = "",
            phase: Int? = 2,
            nextRecurAt: Instant? = null,
            recur: String? = null,
        ) = testWorkTypeEntity(
            worksiteId = 56,
            id = id,
            networkId = networkId,
            workType = workType,
            status = status,
            orgClaim = orgClaim,
            createdAt = createdAt,
            localGlobalUuid = localGlobalUuid,
            phase = phase,
            nextRecurAt = nextRecurAt,
            recur = recur,
        )

        val expectedWorkTypes = listOf(
            expectedWorkType(1, 301, "work-type-a", "status-a-change", null, createdAtA),
            expectedWorkType(23, 223, "work-type-c", "status-c"),
            expectedWorkType(58, -1, "work-type-d", "status-d", localGlobalUuid = "uuid-24"),
        )
        val actualWorkTypes = db.testWorkTypeDao().getEntities(worksiteId)
            .sortedBy(WorkTypeEntity::id)
        assertEquals(expectedWorkTypes, actualWorkTypes)

        val actualChanges = db.testWorksiteChangeDao().getEntities(worksiteId)
        val expectedWorksiteChange = WorksiteChangeEntity(
            id = 1,
            appVersion = 81,
            organizationId = 385,
            worksiteId = worksiteId,
            syncUuid = "uuid-26",
            changeModelVersion = 3,
            changeData = "serialized-edit-worksite-changes",
            createdAt = actualChanges.first().createdAt,
        )
        assertEquals(listOf(expectedWorksiteChange), actualChanges)
        assertTrue(actualChanges.first().createdAt.isNearNow())

        verify(exactly = 0) { appLogger.logException(any()) }
    }


    /**
     * Establishes initial conditions for [editSyncedWorksite_deleteExistingFlags]
     */
    private fun editSyncedWorksite_deleteExistingFlags_initialConditions(
        worksite: Worksite,
        worksiteLocalGlobalUuid: String = "",
    ): EditWorksiteEntities {
        val entities = worksite.asEntities(
            uuidGenerator,
            worksite.workTypes[0],
            emptyMap(),
            emptyMap(),
            emptyMap(),
        )
        db.worksiteDao().insertRoot(
            WorksiteRootEntity(
                worksite.id,
                "sync-uuid",
                worksite.updatedAt!!,
                worksite.updatedAt!!,
                worksiteLocalGlobalUuid,
                false,
                0,
                worksite.networkId,
                worksite.incidentId,
            )
        )
        db.worksiteDao().insert(entities.core)
        db.worksiteFlagDao().insertIgnore(entities.flags)
        db.worksiteFormDataDao().upsert(entities.formData)
        db.worksiteNoteDao().insertIgnore(entities.notes)
        db.workTypeDao().insertIgnore(entities.workTypes)

        val worksiteId = if (worksite.id > 0) worksite.id else 1

        db.testFlagDao().updateNetworkId(1, 201)
        db.worksiteFlagDao().updateNetworkId(21, 221)

        return editWorksiteEntities(worksiteId)
    }

    @Test
    fun editSyncedWorksite_deleteExistingFlags() = runTest {
        val worksiteSynced = worksiteFull.copy(
            networkId = 515,
            flags = worksiteFull.flags?.toMutableList()?.apply {
                add(
                    testWorksiteFlag(
                        21,
                        createdAtB,
                        "reason-network-synced-local-deleted",
                        isHighPriority = true,
                    )
                )
            },
        )
        val initialEntities =
            editSyncedWorksite_deleteExistingFlags_initialConditions(worksiteSynced)

        // Delete all flags. Keep everything else the same.
        val worksiteModified = worksiteChanged.copy(
            networkId = worksiteSynced.networkId,
            flags = emptyList(),
            formData = worksiteSynced.formData,
            notes = worksiteSynced.notes.mapIndexed { index, note ->
                if (note.id == 0L) note.copy(id = index + 1L)
                else note
            },
            workTypes = worksiteSynced.workTypes.map {
                if (it.id == 0L) it.copy(id = 1)
                else it
            },
        )

        every {
            changeSerializer.serialize(
                worksiteSynced,
                worksiteModified,
                mapOf(
                    1L to 201,
                    21L to 221,
                ),
                emptyMap(),
                emptyMap(),
            )
        } returns Pair(3, "serialized-edit-worksite-changes")

        val primaryWorkType = worksiteModified.workTypes[0]

        val entities = worksiteModified.asEntities(
            uuidGenerator,
            primaryWorkType,
            emptyMap(),
            emptyMap(),
            emptyMap(),
        )

        worksiteChangeDaoPlus.saveChange(
            worksiteSynced,
            worksiteModified,
            primaryWorkType,
            385,
            localModifiedAt = now,
        )

        val worksiteEntity = entities.core
        val worksiteId = worksiteEntity.id

        val actualRoot = db.testWorksiteDao().getRootEntity(worksiteId)
        val expectedRoot = WorksiteRootEntity(
            id = 56,
            syncUuid = "uuid-16",
            localModifiedAt = now,
            syncedAt = worksiteSynced.updatedAt!!,
            localGlobalUuid = "",
            isLocalModified = true,
            syncAttempt = 0,
            networkId = 515,
            incidentId = testIncidentId,
        )
        assertEquals(expectedRoot, actualRoot)

        val actualWorksite = db.testWorksiteDao().getWorksiteEntity(worksiteId)
        assertEquals(worksiteEntity, actualWorksite)

        val actualFlags = db.testFlagDao().getEntities(worksiteId)
        assertEquals(emptyList(), actualFlags)

        val expectedFormData = initialEntities.formData
            .map { it.copy(worksiteId = 56) }
        val actualFormData = db.testFormDataDao().getEntities(worksiteId)
        assertEquals(expectedFormData, actualFormData)

        val expectedNotes = initialEntities.notes
        val actualNotes = db.testNoteDao().getEntities(worksiteId)
        assertEquals(expectedNotes, actualNotes)

        var localGlobalIndex = 14
        val expectedWorkTypes = initialEntities.workTypes
            .map {
                it.copy(
                    worksiteId = 56,
                    localGlobalUuid = if (it.networkId > 0) it.localGlobalUuid
                    else "uuid-${localGlobalIndex++}"
                )
            }
            .sortedBy(WorkTypeEntity::id)
        val actualWorkTypes = db.testWorkTypeDao().getEntities(worksiteId)
            .sortedBy(WorkTypeEntity::id)
        assertEquals(expectedWorkTypes, actualWorkTypes)

        val actualChanges = db.testWorksiteChangeDao().getEntities(worksiteId)
        val expectedWorksiteChange = WorksiteChangeEntity(
            id = 1,
            appVersion = 81,
            organizationId = 385,
            worksiteId = worksiteId,
            syncUuid = "uuid-17",
            changeModelVersion = 3,
            changeData = "serialized-edit-worksite-changes",
            createdAt = actualChanges.first().createdAt,
        )
        assertEquals(listOf(expectedWorksiteChange), actualChanges)
        assertTrue(actualChanges.first().createdAt.isNearNow())

        verify(exactly = 0) { appLogger.logException(any()) }
    }

    // TODO Edit unsynced worksite
}

private fun testWorksiteFlag(
    id: Long,
    createdAt: Instant,
    reasonT: String,
    isHighPriority: Boolean = false,
) = WorksiteFlag(
    id = id,
    action = "",
    createdAt = createdAt,
    isHighPriority = isHighPriority,
    notes = "",
    reasonT = reasonT,
    reason = "",
    requestedAction = "",
)

private fun testWorksiteNote(
    id: Long,
    createdAt: Instant,
    note: String,
) = WorksiteNote(
    id = id,
    createdAt = createdAt,
    isSurvivor = false,
    note = note,
)

internal fun testWorkType(
    id: Long,
    createdAt: Instant,
    orgClaim: Long?,
    status: String,
    workType: String,
    phase: Int = 2,
    recur: String? = null,
) = WorkType(
    id = id,
    createdAt = createdAt,
    orgClaim = orgClaim,
    nextRecurAt = null,
    phase = phase,
    recur = recur,
    statusLiteral = status,
    workTypeLiteral = workType,
)
