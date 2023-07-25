package com.crisiscleanup.core.database.dao

import com.crisiscleanup.core.database.TestCrisisCleanupDatabase
import com.crisiscleanup.core.database.TestUtil
import com.crisiscleanup.core.database.WorksiteTestUtil.testIncidents
import com.crisiscleanup.core.database.model.WorksiteEntities
import com.crisiscleanup.core.database.model.WorksiteFlagEntity
import com.crisiscleanup.core.database.model.WorksiteFormDataEntity
import com.crisiscleanup.core.database.model.WorksiteNoteEntity
import com.crisiscleanup.core.database.model.WorksiteRootEntity
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

class WorksiteSyncFillTest {
    private lateinit var db: TestCrisisCleanupDatabase

    private lateinit var worksiteDao: WorksiteDao
    private lateinit var worksiteDaoPlus: WorksiteDaoPlus
    private lateinit var flagDao: WorksiteFlagDao
    private lateinit var formDataDao: WorksiteFormDataDao
    private lateinit var noteDao: WorksiteNoteDao
    private lateinit var workTypeDao: WorkTypeDao

    @Before
    fun createDb() {
        db = TestUtil.getTestDatabase()
        worksiteDao = db.worksiteDao()
        worksiteDaoPlus = WorksiteDaoPlus(db, TestUtil.testSyncLogger())
        flagDao = db.worksiteFlagDao()
        formDataDao = db.worksiteFormDataDao()
        noteDao = db.worksiteNoteDao()
        workTypeDao = db.workTypeDao()
    }

    @Before
    fun seedDb() = runTest {
        val incidentDao = db.incidentDao()
        incidentDao.upsertIncidents(testIncidents)
    }

    private val now = Clock.System.now()
    private val epoch0 = Instant.fromEpochSeconds(0)

    @Test
    fun syncFillWorksite() = runTest {
        val incidentId = testIncidents[0].id
        val rootA = testWorksiteRootEntity(25, incidentId)
        val rootB = testWorksiteRootEntity(26, incidentId)
        val rootD = testWorksiteRootEntity(27, incidentId)
        worksiteDao.insertRoot(rootA)
        worksiteDao.insertRoot(rootB)
        worksiteDao.insertRoot(rootD)

        val coreA = testWorksiteFullEntity(rootA.networkId, incidentId, now, id = rootA.id).copy(
        )
        val coreB = coreA.copy(
            id = rootB.id,
            networkId = rootB.networkId,
            autoContactFrequencyT = null,
            caseNumber = "",
            email = null,
            favoriteId = null,
            phone1 = null,
            phone2 = null,
            plusCode = null,
            svi = null,
            reportedBy = null,
            what3Words = null,
        )
        val coreD = coreB.copy(
            id = rootD.id,
            networkId = rootD.networkId,
            caseNumber = coreA.caseNumber,
            phone1 = "0",
        )
        worksiteDao.insert(coreA)
        worksiteDao.insert(coreB)
        worksiteDao.insert(coreD)

        flagDao.insertIgnore(
            listOf(
                testWorksiteFlagEntity("reason-a", rootA.id, 34),
                testWorksiteFlagEntity("reason-b", rootB.id, 35),
                testWorksiteFlagEntity("reason-c", rootA.id, 36),
                testWorksiteFlagEntity("reason-d", rootA.id, 37),
            )
        )

        formDataDao.upsert(
            listOf(
                testWorksiteFormDataEntity(rootA.id, "field-a", "value-a"),
                testWorksiteFormDataEntity(rootA.id, "field-b", "value-b"),
                testWorksiteFormDataEntity(rootB.id, "field-c", "value-c"),
            )
        )

        noteDao.insertIgnore(
            listOf(
                testWorksiteNoteEntity("note-a", rootA.id, 57, (-1).hours, 13),
                testWorksiteNoteEntity("note-b", rootA.id, 58, 1.hours, 14),
                testWorksiteNoteEntity("note-c", rootA.id, 59, (-1).hours, 15),
                testWorksiteNoteEntity("note-d", rootA.id, 60, 1.hours, 16),
                testWorksiteNoteEntity("note-e", rootB.id, 61, (-1).hours, 17),
                testWorksiteNoteEntity("note-f", rootB.id, 62, 1.hours, 18),
            )
        )

        workTypeDao.insertIgnore(
            listOf(
                testWorkTypeEntity(35, "status-a", "type-a", 25, rootA.id),
                testWorkTypeEntity(36, "status-b", "type-b", 26, rootA.id),
                testWorkTypeEntity(37, "status-c", "type-c", 26, rootA.id),
                testWorkTypeEntity(38, "status-d", "type-d", 25, rootB.id),
            )
        )

        val updateCore = coreA.copy(
            incidentId = incidentId,
            address = "${coreA.address}-update",
            autoContactFrequencyT = "${coreA.autoContactFrequencyT}-update",
            caseNumber = "${coreA.caseNumber}-update",
            city = "${coreA.city}-update",
            county = "${coreA.county}-update",
            createdAt = coreA.createdAt!!.plus(1.hours),
            email = "${coreA.email}-update",
            favoriteId = 854,
            keyWorkTypeType = "${coreA.keyWorkTypeType}-update",
            keyWorkTypeOrgClaim = coreA.keyWorkTypeOrgClaim,
            keyWorkTypeStatus = "${coreA.keyWorkTypeStatus}-update",
            latitude = coreA.latitude + 0.1,
            longitude = coreA.longitude + 0.1,
            name = "${coreA.name}-update",
            phone1 = "${coreA.phone1}-update",
            phone2 = "${coreA.phone2}-update",
            plusCode = "${coreA.plusCode}-update",
            postalCode = "${coreA.postalCode}-update",
            reportedBy = 7835,
            state = "${coreA.state}-update",
            svi = coreA.svi!! * 2,
            what3Words = "${coreA.what3Words}-update",
            updatedAt = coreA.updatedAt.plus(99.seconds),
        )
        val entitiesA = WorksiteEntities(
            updateCore.copy(
                networkId = coreA.networkId,
            ),
            listOf(
                testWorksiteFlagEntity(
                    "reason-a",
                    rootA.id,
                    networkId = 162,
                    action = "action-change",
                ),
                testWorksiteFlagEntity(
                    "reason-b",
                    rootA.id,
                    networkId = 163,
                    action = "action-change",
                ),
                testWorksiteFlagEntity(
                    "reason-d",
                    rootA.id,
                    networkId = 81,
                    action = "action-change",
                ),
            ),
            listOf(
                testWorksiteFormDataEntity(rootA.id, "field-a", "value-a-change"),
                testWorksiteFormDataEntity(rootA.id, "field-c", "value-c-change"),
            ),
            listOf(
                testWorksiteNoteEntity("note-a", rootA.id, 77, 9.hours),
                testWorksiteNoteEntity("note-b", rootA.id, 78, 9.hours),
                testWorksiteNoteEntity("note-c-change", rootA.id, 59, 9.hours),
                testWorksiteNoteEntity("note-e", rootA.id, 79, 9.hours),
                testWorksiteNoteEntity("note-f", rootA.id, 80, 9.hours),
            ),
            listOf(
                testWorkTypeEntity(71, "status-a-change", "type-a", 25, rootA.id),
                testWorkTypeEntity(72, "status-b-change", "type-b", 26, rootA.id),
                testWorkTypeEntity(37, "status-c-change", "type-c", 26, rootA.id),
                testWorkTypeEntity(73, "status-d-change", "type-d", 25, rootA.id),
            ),
        )

        val entitiesB = WorksiteEntities(
            updateCore.copy(
                id = coreB.id,
                networkId = coreB.networkId,
            ),
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList(),
        )
        val expectedCoreUpdate = coreA.copy(
            id = coreB.id,
            networkId = coreB.networkId,
            autoContactFrequencyT = updateCore.autoContactFrequencyT,
            caseNumber = "case52-update",
            email = updateCore.email,
            favoriteId = updateCore.favoriteId!!,
            phone1 = updateCore.phone1,
            phone2 = updateCore.phone2,
            plusCode = updateCore.plusCode,
            svi = updateCore.svi!!,
            reportedBy = updateCore.reportedBy,
            what3Words = updateCore.what3Words,
        )

        val entitiesD = WorksiteEntities(
            updateCore.copy(
                id = coreD.id,
                caseNumber = "c",
                networkId = coreD.networkId,
            ),
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList(),
        )
        val expectedCoreD = expectedCoreUpdate.copy(
            id = coreD.id,
            caseNumber = coreA.caseNumber,
            networkId = coreD.networkId,
        )

        val actualA = worksiteDaoPlus.syncFillWorksite(entitiesA)
        assertTrue(actualA)

        val testWorksiteDao = db.testWorksiteDao()
        val actualCoreA = testWorksiteDao.getWorksiteEntity(rootA.id)!!
        assertEquals(coreA, actualCoreA)

        val actualB = worksiteDaoPlus.syncFillWorksite(entitiesB)
        assertTrue(actualB)

        val actualCoreB = testWorksiteDao.getWorksiteEntity(rootB.id)!!
        assertEquals(expectedCoreUpdate, actualCoreB)

        val actualD = worksiteDaoPlus.syncFillWorksite(entitiesD)
        assertTrue(actualD)

        val actualCoreD = testWorksiteDao.getWorksiteEntity(rootD.id)!!
        assertEquals(expectedCoreD, actualCoreD)

        val expectedFlagsA = listOf(
            testWorksiteFlagEntity("reason-a", rootA.id, 34),
            testWorksiteFlagEntity("reason-c", rootA.id, 36),
            testWorksiteFlagEntity("reason-d", rootA.id, 37),
            testWorksiteFlagEntity(
                "reason-b",
                rootA.id,
                38,
                networkId = 163,
                action = "action-change",
            ),
        )

        val actualFlagsA = db.testFlagDao().getEntities(rootA.id)
        assertEquals(expectedFlagsA, actualFlagsA)

        val expectedFlagsB = listOf(testWorksiteFlagEntity("reason-b", rootB.id, 35))
        val actualFlagsB = db.testFlagDao().getEntities(rootB.id)
        assertEquals(expectedFlagsB, actualFlagsB)

        val expectedFormDataA = listOf(
            testWorksiteFormDataEntity(rootA.id, "field-a", "value-a"),
            testWorksiteFormDataEntity(rootA.id, "field-b", "value-b"),
            testWorksiteFormDataEntity(rootA.id, "field-c", "value-c-change"),
        )
        val actualFormDataA = db.testFormDataDao().getEntities(rootA.id)
        assertEquals(expectedFormDataA, actualFormDataA)

        val actualFormDataB = db.testFormDataDao().getEntities(rootB.id)
        val expectedFormDataB = listOf(
            testWorksiteFormDataEntity(rootB.id, "field-c", "value-c"),
        )
        assertEquals(expectedFormDataB, actualFormDataB)

        val expectedNotesA = listOf(
            testWorksiteNoteEntity("note-a", rootA.id, 57, (-1).hours, 13),
            testWorksiteNoteEntity("note-b", rootA.id, 58, 1.hours, 14),
            testWorksiteNoteEntity("note-c", rootA.id, 59, (-1).hours, 15),
            testWorksiteNoteEntity("note-d", rootA.id, 60, 1.hours, 16),
            testWorksiteNoteEntity("note-a", rootA.id, 77, 9.hours, 19),
            testWorksiteNoteEntity("note-e", rootA.id, 79, 9.hours, 21),
            testWorksiteNoteEntity("note-f", rootA.id, 80, 9.hours, 22),
        )
        val actualNotesA = db.testNoteDao().getEntities(rootA.id)
        assertEquals(expectedNotesA, actualNotesA)

        val expectedNotesB = listOf(
            testWorksiteNoteEntity("note-e", rootB.id, 61, (-1).hours, 17),
            testWorksiteNoteEntity("note-f", rootB.id, 62, 1.hours, 18),
        )
        val actualNotesB = db.testNoteDao().getEntities(rootB.id)
        assertEquals(expectedNotesB, actualNotesB)

        val expectedWorkTypesA = listOf(
            testWorkTypeEntity(35, "status-a", "type-a", 25, rootA.id, id = 1),
            testWorkTypeEntity(36, "status-b", "type-b", 26, rootA.id, id = 2),
            testWorkTypeEntity(37, "status-c", "type-c", 26, rootA.id, id = 3),
            testWorkTypeEntity(73, "status-d-change", "type-d", 25, rootA.id, id = 5),
        )
        val actualWorkTypesA = db.testWorkTypeDao().getEntities(rootA.id)
        assertEquals(expectedWorkTypesA, actualWorkTypesA)

        val expectedWorkTypesB = listOf(
            testWorkTypeEntity(38, "status-d", "type-d", 25, rootB.id, id = 4),
        )
        val actualWorkTypesB = db.testWorkTypeDao().getEntities(rootB.id)
        assertEquals(expectedWorkTypesB, actualWorkTypesB)
    }

    private fun testWorksiteRootEntity(
        id: Long,
        incidentId: Long,
        networkId: Long = id + 30,
    ) = WorksiteRootEntity(
        id = id,
        syncUuid = "",
        localModifiedAt = now,
        syncedAt = epoch0,
        localGlobalUuid = "",
        isLocalModified = true,
        syncAttempt = 0,
        networkId = networkId,
        incidentId = incidentId,
    )

    private fun testWorksiteFlagEntity(
        reasonT: String,
        worksiteId: Long,
        id: Long = 0,
        networkId: Long = id + 44,
        action: String = "",
    ) = WorksiteFlagEntity(
        id = id,
        networkId = networkId,
        worksiteId = worksiteId,
        action = action,
        createdAt = now,
        isHighPriority = false,
        notes = "",
        reasonT = reasonT,
        requestedAction = "",
    )

    private fun testWorksiteFormDataEntity(
        worksiteId: Long,
        fieldKey: String,
        fieldValue: String,
    ) = WorksiteFormDataEntity(
        worksiteId,
        fieldKey,
        false,
        fieldValue,
        false,
    )

    private fun testWorksiteNoteEntity(
        note: String,
        worksiteId: Long,
        networkId: Long,
        deltaReferenceTime: Duration,
        id: Long = 0,
    ) = WorksiteNoteEntity(
        id = id,
        localGlobalUuid = "",
        networkId = networkId,
        worksiteId = worksiteId,
        createdAt = now.minus(12.hours).plus(deltaReferenceTime),
        isSurvivor = false,
        note = note,
    )
}