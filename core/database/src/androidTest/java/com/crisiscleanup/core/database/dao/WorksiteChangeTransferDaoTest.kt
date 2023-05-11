package com.crisiscleanup.core.database.dao

import androidx.room.withTransaction
import com.crisiscleanup.core.database.TestCrisisCleanupDatabase
import com.crisiscleanup.core.database.TestUtil
import com.crisiscleanup.core.database.WorksiteTestUtil
import com.crisiscleanup.core.database.isNearNow
import com.crisiscleanup.core.database.model.EditWorksiteEntities
import com.crisiscleanup.core.database.model.WorkTypeEntity
import com.crisiscleanup.core.database.model.WorkTypeTransferRequestEntity
import com.crisiscleanup.core.database.model.WorksiteChangeEntity
import com.crisiscleanup.core.database.model.WorksiteFlagEntity
import com.crisiscleanup.core.database.model.WorksiteNoteEntity
import com.crisiscleanup.core.database.model.WorksiteRootEntity
import com.crisiscleanup.core.database.model.asEntities
import com.crisiscleanup.core.model.data.AutoContactFrequency
import com.crisiscleanup.core.model.data.EmptyWorksite
import com.crisiscleanup.core.model.data.WorkType
import com.crisiscleanup.core.model.data.WorkTypeStatus
import com.crisiscleanup.core.model.data.Worksite
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
import kotlin.time.Duration.Companion.minutes

private val now = Clock.System.now()

class WorksiteChangeTransferDaoTest {
    private lateinit var db: TestCrisisCleanupDatabase

    private lateinit var worksiteDao: WorksiteDao
    private lateinit var worksiteDaoPlus: WorksiteDaoPlus
    private lateinit var worksiteChangeDaoPlus: WorksiteChangeDaoPlus

    private val uuidGenerator = TestUtil.testUuidGenerator()
    private val changeSerializer = TestUtil.testChangeSerializer()
    private val appVersionProvider = TestUtil.testAppVersionProvider()
    private val syncLogger = TestUtil.testSyncLogger()
    private val appLogger = TestUtil.testAppLogger()

    private val testIncidentId = WorksiteTestUtil.testIncidents.last().id

    private val createdAtA = now.minus(4.days)
    private val updatedAtA = createdAtA.plus(40.minutes)

    private val testWorksite = Worksite(
        id = 56,
        address = "address",
        autoContactFrequencyT = AutoContactFrequency.NotOften.literal,
        caseNumber = "case-number",
        city = "city",
        county = "county",
        createdAt = createdAtA,
        email = "email",
        favoriteId = 623,
        incidentId = testIncidentId,
        keyWorkType = testWorkType(
            57,
            createdAtA,
            523,
            "status-b",
            "work-type-b",
        ),
        latitude = -5.23,
        longitude = -39.35,
        name = "name",
        networkId = 556,
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
                3,
                createdAtA,
                null,
                "status-a",
                "work-type-a",
            ),
            testWorkType(
                57,
                createdAtA,
                523,
                "status-b",
                "work-type-b",
            ),
            testWorkType(
                58,
                createdAtA,
                null,
                "status-c",
                "work-type-c",
            ),
            testWorkType(
                59,
                createdAtA,
                481,
                "status-d",
                "work-type-d",
            ),
        ),
        isAssignedToOrgMember = true,
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
        db.incidentDao().upsertIncidents(WorksiteTestUtil.testIncidents)
        insertWorksite(testWorksite)
        db.worksiteFlagDao().insertIgnore(
            listOf(
                WorksiteFlagEntity(
                    id = 5,
                    networkId = 55,
                    worksiteId = testWorksite.id,
                    action = "action",
                    createdAt = createdAtA,
                    isHighPriority = false,
                    notes = "notes",
                    reasonT = "reason",
                    requestedAction = "",
                )
            )
        )
        db.worksiteNoteDao().insertIgnoreNote(
            WorksiteNoteEntity(
                id = 21,
                localGlobalUuid = "",
                networkId = 221,
                worksiteId = testWorksite.id,
                createdAt = createdAtA,
                isSurvivor = false,
                note = "note",
            )

        )
        db.workTypeDao().insertIgnoreWorkType(
            WorkTypeEntity(
                id = 36,
                networkId = 336,
                worksiteId = testWorksite.id,
                createdAt = createdAtA,
                orgClaim = 167,
                nextRecurAt = null,
                phase = 2,
                recur = null,
                status = "status-existing",
                workType = "work-type-existing",
            )
        )
        db.workTypeDao().updateNetworkId(3, 353)
        db.workTypeDao().updateNetworkId(57, 357)
    }

    private suspend fun insertWorksite(worksite: Worksite): EditWorksiteEntities {
        val entities = worksite.asEntities(
            uuidGenerator,
            worksite.workTypes[0],
            emptyMap(),
            emptyMap(),
            emptyMap(),
        )
        db.withTransaction {
            db.worksiteDao().insertRoot(
                WorksiteRootEntity(
                    id = worksite.id,
                    syncUuid = "",
                    localModifiedAt = now,
                    syncedAt = now,
                    localGlobalUuid = "",
                    isLocalModified = false,
                    syncAttempt = 0,
                    networkId = worksite.networkId,
                    incidentId = worksite.incidentId,
                )
            )
            db.worksiteDao().insert(entities.core)
        }
        val worksiteId = worksite.id
        val flags = entities.flags.map { it.copy(worksiteId = worksiteId) }
        db.worksiteFlagDao().insertIgnore(flags)
        val formData = entities.formData.map { it.copy(worksiteId = worksiteId) }
        db.worksiteFormDataDao().upsert(formData)
        val notes = entities.notes.map { it.copy(worksiteId = worksiteId) }
        db.worksiteNoteDao().insertIgnore(notes)
        val workTypes = entities.workTypes.map { it.copy(worksiteId = worksiteId) }
        db.workTypeDao().insertIgnore(workTypes)

        return EditWorksiteEntities(entities.core, flags, formData, notes, workTypes)
    }

    @Test
    fun requestNotSaved() = runTest {
        worksiteChangeDaoPlus.saveWorkTypeRequests(EmptyWorksite, 1, "reason", listOf("request"))
        worksiteChangeDaoPlus.saveWorkTypeRequests(testWorksite, 0, "reason", listOf("request"))
        worksiteChangeDaoPlus.saveWorkTypeRequests(testWorksite, 1, "", listOf("request"))
        worksiteChangeDaoPlus.saveWorkTypeRequests(testWorksite, 1, "reason", emptyList())
        worksiteChangeDaoPlus.saveWorkTypeRequests(
            testWorksite,
            152,
            "reason",
            listOf("work-type-a", "work-type-c", "work-type-none"),
        )

        val requestCount = db.testWorkTypeRequestDao().getEntities().size
        assertEquals(0, requestCount)

        verify(exactly = 4) { appLogger.logDebug("Not saving work type requests. Invalid data.") }
        verify(exactly = 0) { changeSerializer.serialize(any(), allAny()) }
        verify(exactly = 0) { appLogger.logException(any()) }
    }

    @Test
    fun requestClaimedUnclaimed() = runTest {
        every {
            changeSerializer.serialize(
                EmptyWorksite,
                testWorksite,
                mapOf(5L to 55),
                mapOf(21L to 221),
                mapOf(
                    3L to 353,
                    36L to 336,
                    57L to 357,
                ),
                "reason",
                listOf("work-type-b", "work-type-d"),
                "",
                emptyList(),
            )
        } returns Pair(2, "serialized-work-type-requests")

        worksiteChangeDaoPlus.saveWorkTypeRequests(
            testWorksite,
            152,
            "reason",
            listOf("work-type-b", "work-type-c", "work-type-d"),
            now,
        )

        val expected = listOf(
            testWorkTypeTransferRequestEntity(
                id = 1,
                workType = "work-type-b",
                toOrg = 523,
            ),
            testWorkTypeTransferRequestEntity(
                id = 2,
                workType = "work-type-d",
                toOrg = 481,
            ),
        )
        val actual = db.testWorkTypeRequestDao().getEntities()
        assertEquals(expected, actual)

        val actualChanges = db.testWorksiteChangeDao().getEntities(testWorksite.id)
        val expectedWorksiteChange = WorksiteChangeEntity(
            id = 1,
            appVersion = 81,
            organizationId = 152,
            worksiteId = testWorksite.id,
            syncUuid = "uuid-1",
            changeModelVersion = 2,
            changeData = "serialized-work-type-requests",
            createdAt = actualChanges.first().createdAt,
        )
        assertEquals(listOf(expectedWorksiteChange), actualChanges)
        assertTrue(actualChanges.first().createdAt.isNearNow())

        verify(exactly = 0) { appLogger.logDebug(any()) }
        verify(exactly = 0) { appLogger.logException(any()) }
    }

    @Test
    fun releaseNotSaved() = runTest {
        worksiteChangeDaoPlus.saveWorkTypeReleases(EmptyWorksite, 1, "reason", listOf("request"))
        worksiteChangeDaoPlus.saveWorkTypeReleases(testWorksite, 0, "reason", listOf("request"))
        worksiteChangeDaoPlus.saveWorkTypeReleases(testWorksite, 1, "", listOf("request"))
        worksiteChangeDaoPlus.saveWorkTypeReleases(testWorksite, 1, "reason", emptyList())
        worksiteChangeDaoPlus.saveWorkTypeReleases(
            testWorksite,
            152,
            "reason",
            listOf("work-type-a", "work-type-c", "work-type-none"),
        )

        val requestCount = db.testWorkTypeRequestDao().getEntities().size
        assertEquals(0, requestCount)

        verify(exactly = 4) { appLogger.logDebug("Not saving work type releases. Invalid data.") }
        verify(exactly = 0) { changeSerializer.serialize(any(), allAny()) }
        verify(exactly = 0) { appLogger.logException(any()) }
    }


    @Test
    fun releaseClaimedUnclaimed() = runTest {
        var workTypeInsertId = 60L
        val worksiteChangeSerialize = testWorksite.copy(
            keyWorkType = testWorksite.keyWorkType?.copy(
                id = 60,
                orgClaim = null,
                statusLiteral = WorkTypeStatus.OpenUnassigned.literal,
                phase = null,
                createdAt = now,
            ),
            workTypes = testWorksite.workTypes.map { workType ->
                if (workType.orgClaim == null) workType
                else {
                    WorkType(
                        id = workTypeInsertId++,
                        createdAt = now,
                        statusLiteral = WorkTypeStatus.OpenUnassigned.literal,
                        workTypeLiteral = workType.workTypeLiteral,
                    )
                }
            },
        )
        every {
            changeSerializer.serialize(
                EmptyWorksite,
                worksiteChangeSerialize,
                mapOf(5L to 55),
                mapOf(21L to 221),
                mapOf(
                    3L to 353,
                    36L to 336,
                ),
                "",
                emptyList(),
                "reason",
                listOf("work-type-b", "work-type-d"),
            )
        } returns Pair(2, "serialized-work-type-releases")

        worksiteChangeDaoPlus.saveWorkTypeReleases(
            testWorksite,
            152,
            "reason",
            listOf("work-type-b", "work-type-c", "work-type-d"),
            now,
        )

        fun expectedWorkType(
            id: Long,
            status: String,
            workType: String,
            networkId: Long = -1,
            orgClaim: Long? = null,
            createdAt: Instant = createdAtA,
            phase: Int? = null,
        ) = testWorkTypeEntity(
            networkId,
            status,
            workType,
            orgClaim,
            testWorksite.id,
            createdAt,
            phase = phase,
            id = id,
        )

        val expectedWorkTypes = listOf(
            expectedWorkType(
                3,
                "status-a",
                "work-type-a",
                353,
                phase = 2,
            ),
            expectedWorkType(
                60,
                WorkTypeStatus.OpenUnassigned.literal,
                "work-type-b",
                createdAt = now,
            ),
            expectedWorkType(
                58,
                "status-c",
                "work-type-c",
                -1,
                phase = 2,
            ),
            expectedWorkType(
                61,
                WorkTypeStatus.OpenUnassigned.literal,
                "work-type-d",
                createdAt = now,
            ),
            expectedWorkType(
                36,
                "status-existing",
                "work-type-existing",
                336,
                167,
                phase = 2,
            ),
        )
        val actualWorkTypes = db.testWorkTypeDao().getEntities(testWorksite.id)
        assertEquals(expectedWorkTypes, actualWorkTypes)

        val actualChanges = db.testWorksiteChangeDao().getEntitiesOrderId(testWorksite.id)
        val expectedWorksiteChange = WorksiteChangeEntity(
            id = 1,
            appVersion = 81,
            organizationId = 152,
            worksiteId = testWorksite.id,
            syncUuid = "uuid-1",
            changeModelVersion = 2,
            changeData = "serialized-work-type-releases",
            createdAt = actualChanges.first().createdAt,
        )
        assertEquals(listOf(expectedWorksiteChange), actualChanges)
        assertTrue(actualChanges.first().createdAt.isNearNow())

        verify(exactly = 0) { appLogger.logDebug(any()) }
        verify(exactly = 0) { appLogger.logException(any()) }
    }
}

private fun testWorkTypeTransferRequestEntity(
    id: Long = 0,
    workType: String,
    toOrg: Long,
    byOrg: Long = 152,
    reason: String = "reason",
    worksiteId: Long = 56,
    createdAt: Instant = now,
    networkId: Long = -1,
) = WorkTypeTransferRequestEntity(
    id,
    networkId = networkId,
    worksiteId = worksiteId,
    workType = workType,
    reason = reason,
    byOrg = byOrg,
    toOrg = toOrg,
    createdAt = createdAt,
)