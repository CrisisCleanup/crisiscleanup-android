package com.crisiscleanup.core.database.dao

import com.crisiscleanup.core.common.AppVersionProvider
import com.crisiscleanup.core.common.UuidGenerator
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.sync.SyncLogger
import com.crisiscleanup.core.database.TestCrisisCleanupDatabase
import com.crisiscleanup.core.database.TestUtil
import com.crisiscleanup.core.database.WorksiteTestUtil.insertWorksites
import com.crisiscleanup.core.database.WorksiteTestUtil.testIncidents
import com.crisiscleanup.core.database.model.*
import com.crisiscleanup.core.model.data.*
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.junit.Before
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class WorksiteChangeDaoTest {
    private lateinit var db: TestCrisisCleanupDatabase

    private lateinit var worksiteDao: WorksiteDao
    private lateinit var worksiteDaoPlus: WorksiteDaoPlus
    private lateinit var worksiteChangeDaoPlus: WorksiteChangeDaoPlus

    // TODO Change spys to mock when https://github.com/mockk/mockk/issues/1035 is fixed

    private val uuidGenerator: UuidGenerator = spyk(object : UuidGenerator {
        private val counter = AtomicInteger()
        override fun uuid() = "uuid-${counter.incrementAndGet()}"
    })

    private val changeSerializer: WorksiteChangeSerializer =
        spyk(object : WorksiteChangeSerializer {
            override fun serialize(
                worksiteStart: Worksite,
                worksiteChange: Worksite,
                flagIdLookup: Map<Long, Long>,
                noteIdLookup: Map<Long, Long>,
                workTypeIdLookup: Map<Long, Long>
            ) = "test-worksite-change"
        })

    private val appVersionProvider: AppVersionProvider = spyk(object : AppVersionProvider {
        override val version: Pair<Long, String> = Pair(81, "1.0.81")
        override val versionCode: Long = version.first
        override val versionName: String = version.second
    })

    private val syncLogger: SyncLogger = spyk(object : SyncLogger {
        override var type: String = "test"

        override fun log(message: String, details: String, type: String): SyncLogger {
            return this
        }

        override fun flush() {}
    })

    private val appLogger: AppLogger = spyk(object : AppLogger {
        override fun logDebug(vararg logs: Any) {}

        override fun logException(e: Exception) {}
    })

    private val testIncidentId = testIncidents.last().id

    private val now = Clock.System.now()
    private val createdAtA = now.minus(4.days)
    private val updatedAtA = createdAtA.plus(40.minutes)
    private val createdAtB = createdAtA.plus(1.days)
    private val updatedAtB = createdAtB.plus(51.minutes)
    private val createdAtC = createdAtB.plus(23.hours)

    private val worksiteFull = Worksite(
        id = 56,
        address = "address",
        autoContactFrequencyT = AutoContactFrequency.NotOften.literal,
        autoContactFrequency = AutoContactFrequency.NotOften,
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
    private val worksiteChanged = worksiteFull.copy(
        address = "address-change",
        autoContactFrequencyT = AutoContactFrequency.Often.literal,
        autoContactFrequency = AutoContactFrequency.Often,
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

        // TODO Try mocking in a future version where tests are able to run properly
        // every { syncLogger.log(any(), any(), any()) } returns syncLogger

        every { appLogger.logDebug(*anyVararg()) } returns Unit
    }

    @Before
    fun createDb() {
        db = TestUtil.getTestDatabase()
        worksiteDao = db.worksiteDao()
        worksiteDaoPlus = WorksiteDaoPlus(db)
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
        db.worksiteFlagDao().insert(flags)
        val formData = entities.formData.map { it.copy(worksiteId = worksiteId) }
        db.worksiteFormDataDao().upsert(formData)
        val notes = entities.notes.map { it.copy(worksiteId = worksiteId) }
        db.worksiteNoteDao().insert(notes)
        val workTypes = entities.workTypes.map { it.copy(worksiteId = worksiteId) }
        db.workTypeDao().insert(workTypes)

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

        verify(exactly = 0) { changeSerializer.serialize(any(), any(), any(), any(), any()) }
        verify(exactly = 0) { appLogger.logException(any()) }
    }

    @Test
    fun newWorksite() = runTest {
        val newWorksite = worksiteFull.copy(
            id = 0,
            networkId = -1,
            flags = worksiteFull.flags!!.map { it.copy(id = 0) },
            notes = worksiteFull.notes.map { it.copy(id = 0) },
            workTypes = worksiteFull.workTypes.map { it.copy(id = 0) },
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
                newWorksite.copy(id = 1),
                emptyMap(),
                emptyMap(),
                emptyMap(),
            )
        } returns "serialized-new-worksite-changes"

        worksiteChangeDaoPlus.saveChange(
            EmptyWorksite,
            newWorksite,
            primaryWorkType,
            385,
        )

        val worksiteEntity = entities.core.copy(id = 1)
        val worksiteId = worksiteEntity.id

        val actualRoot = db.testWorksiteDao().getRootEntity(worksiteId)
        val expectedRoot = WorksiteRootEntity(
            id = 1,
            syncUuid = "uuid-13",
            localModifiedAt = newWorksite.updatedAt!!,
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
            changeData = "serialized-new-worksite-changes",
            createdAt = actualChanges.first().createdAt,
        )
        assertEquals(listOf(expectedWorksiteChange), actualChanges)
        assertTrue(now.minus(actualChanges.first().createdAt) < 1.seconds)

        verify(exactly = 0) { appLogger.logException(any()) }
    }

    private fun editWorksiteInitialConditions(
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
        db.worksiteFlagDao().insert(entities.flags)
        db.worksiteFormDataDao().upsert(entities.formData)
        db.worksiteNoteDao().insert(entities.notes)
        db.workTypeDao().insert(entities.workTypes)

        val worksiteId = if (worksite.id > 0) worksite.id else 1

        // For mapping
        db.testFlagDao().updateNetworkId(1, 201)
        db.worksiteFlagDao().insert(
            listOf(
                WorksiteFlagEntity(
                    11,
                    "",
                    false,
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

        db.testNoteDao().updateNetworkId(64, 264)
        db.worksiteNoteDao().insert(
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

        db.testWorkTypeDao().updateNetworkId(1, 301)
        db.workTypeDao().insert(
            listOf(
                WorkTypeEntity(
                    23,
                    "",
                    false,
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

    @Test
    fun editSyncedWorksite() = runTest {
        val worksiteSynced = worksiteFull.copy(networkId = 515)
        val initialEntities = editWorksiteInitialConditions(worksiteSynced)

        val worksiteModified = worksiteChanged.copy(networkId = worksiteSynced.networkId)

        every {
            changeSerializer.serialize(
                worksiteSynced,
                worksiteModified,
                mapOf(
                    1L to 201,
                    11L to 211,
                ),
                mapOf(
                    41L to 241,
                    64L to 264,
                ),
                mapOf(
                    1L to 301,
                    23L to 223,
                ),
            )
        } returns "serialized-edit-worksite-changes"

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
        )

        val worksiteEntity = entities.core
        val worksiteId = worksiteEntity.id

        val actualRoot = db.testWorksiteDao().getRootEntity(worksiteId)
        val expectedRoot = WorksiteRootEntity(
            id = 56,
            syncUuid = "uuid-23",
            localModifiedAt = worksiteModified.updatedAt!!,
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

        var localGlobalIndex = 18L

        val expectedFlags = initialEntities.flags
            .toMutableList()
            .also {
                val deleteIndex = it.indexOfFirst { entity -> entity.id == 33L }
                it.removeAt(deleteIndex)

                val updateIndex = it.indexOfFirst { entity -> entity.id == 1L }
                it[updateIndex] = it[updateIndex].copy(createdAt = createdAtC)

                val addedFlag = entities.flags
                    .find { entity -> entity.id == 0L }!!
                    .copy(id = 34)
                it.add(addedFlag)
            }
            .map {
                it.copy(
                    worksiteId = 56,
                    localGlobalUuid = if (it.networkId > 0) it.localGlobalUuid else "uuid-${localGlobalIndex++}",
                )
            }
            .sortedBy(WorksiteFlagEntity::id)
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

        var entityIndex = 65L
        localGlobalIndex = 20
        val expectedNotes = initialEntities.notes
            .toMutableList()
            .also {
                val updateIndex = it.indexOfFirst { entity -> entity.id == 1L }
                it[updateIndex] = it[updateIndex].copy(
                    // Is updated in preconditions
                    localGlobalUuid = "uuid-3",
                )

                entities.notes.forEach { entity ->
                    if (it.find { preEntity -> preEntity.id == entity.id } == null) {
                        it.add(entity.copy(entityIndex++))
                    }
                }
            }
            .map {
                it.copy(
                    worksiteId = 56,
                    localGlobalUuid = if (it.networkId > 0 || it.id == 1L) it.localGlobalUuid
                    else "uuid-${localGlobalIndex++}"
                )
            }
            .sortedBy(WorksiteNoteEntity::id)
        val actualNotes = db.testNoteDao().getEntities(worksiteId)
            .sortedBy(WorksiteNoteEntity::id)
        assertEquals(expectedNotes, actualNotes)

        localGlobalIndex = 22
        val expectedWorkTypes = initialEntities.workTypes
            .toMutableList()
            .also {
                val deleteIndex = it.indexOfFirst { entity -> entity.id == 57L }
                it.removeAt(deleteIndex)

                val updateIndex = it.indexOfFirst { entity -> entity.id == 1L }
                it[updateIndex] = it[updateIndex].copy(status = "status-a-change")

                val added = entities.workTypes
                    .find { entity -> entity.id == 0L }!!
                    .copy(id = 58)
                it.add(added)
            }
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
            changeData = "serialized-edit-worksite-changes",
            createdAt = actualChanges.first().createdAt,
        )
        assertEquals(listOf(expectedWorksiteChange), actualChanges)
        assertTrue(now.minus(actualChanges.first().createdAt) < 1.seconds)

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

private fun testWorkType(
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

internal fun WorksiteFormDataEntity.hasNoValue() =
    isBoolValue && !valueBool || !isBoolValue && valueString.isBlank()