package com.crisiscleanup.core.database.dao

import com.crisiscleanup.core.database.TestCrisisCleanupDatabase
import com.crisiscleanup.core.database.TestUtil
import com.crisiscleanup.core.database.WorksiteTestUtil
import com.crisiscleanup.core.database.model.WorksiteEntity
import com.crisiscleanup.core.database.model.asExternalModel
import com.crisiscleanup.core.model.data.WorkType
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.seconds

/**
 * Sync worksites with work types
 */
class WorksiteWorkTypeTest {
    private lateinit var db: TestCrisisCleanupDatabase

    private lateinit var worksiteDao: WorksiteDao
    private lateinit var worksiteDaoPlus: WorksiteDaoPlus

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
        worksiteDaoPlus = WorksiteDaoPlus(db)
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

    @Test(expected = Exception::class)
    fun syncingWorksitesMustHaveWorkTypes() = runTest {
        val syncedAt = previousSyncedAt.plus(487.seconds)
        val syncingWorksites = listOf(
            testWorksiteShortEntity(111, 1, createdAtA),
        )
        worksiteDaoPlus.syncWorksites(
            1,
            syncingWorksites,
            emptyList(),
            syncedAt,
        )
    }

    /**
     * Syncing work types overwrites local (where unchanged)
     */
    @Test
    fun syncWorksiteWorkTypes() = runTest {
        // Insert existing
        var existingWorksites = listOf(
            testWorksiteEntity(1, 1, "address", updatedAtA),
            testWorksiteEntity(2, 1, "address", updatedAtA),
        )
        existingWorksites = insertWorksites(existingWorksites, previousSyncedAt)
        db.workTypeDao().insert(
            listOf(
                testWorkTypeEntity(1, worksiteId = 1),
                testWorkTypeEntity(11, worksiteId = 1),
            )
        )

        // Sync
        val syncingWorksites = listOf(
            testWorksiteEntity(1, 1, "sync-address", updatedAtB),
            testWorksiteEntity(2, 1, "sync-address", updatedAtB),
        )
        val createdAtC = now.plus(200.seconds)
        val nextRecurAtC = createdAtC.plus(1.days)
        val syncingWorkTypes = listOf(
            listOf(
                // Update
                testWorkTypeEntity(
                    1,
                    worksiteId = 1,
                    workType = "work-type-synced-update",
                    status = "status-synced-update",
                    orgClaim = 5498,
                    createdAt = createdAtC,
                    nextRecurAt = nextRecurAtC,
                    phase = 84,
                    recur = "recur-synced-update",
                ),
                // Delete 11
                // New
                testWorkTypeEntity(15, worksiteId = 1, workType = "synced"),
                testWorkTypeEntity(
                    22,
                    worksiteId = 1,
                    workType = "work-type-synced-new",
                    status = "status-synced-new",
                    orgClaim = 8456,
                    createdAt = createdAtC,
                    nextRecurAt = nextRecurAtC,
                    phase = 93,
                    recur = "recur-synced-new",
                ),
            ),
            listOf(
                testWorkTypeEntity(24, worksiteId = 2, workType = "new"),
                testWorkTypeEntity(26, worksiteId = 2, workType = "new"),
            ),
        )
        // Sync new and existing
        val syncedAt = previousSyncedAt.plus(499_999.seconds)
        worksiteDaoPlus.syncWorksites(1, syncingWorksites, syncingWorkTypes, syncedAt)

        // Assert

        var actual = worksiteDao.getWorksite(1)
        assertEquals(
            existingWorksites[0].copy(
                address = "sync-address",
                updatedAt = updatedAtB,
            ),
            actual.entity,
        )
        val expectedWorkTypeEntitiesA = listOf(
            testWorkTypeEntity(
                id = 1,
                networkId = 1,
                worksiteId = 1,
                workType = "work-type-synced-update",
                status = "status-synced-update",
                orgClaim = 5498,
                createdAt = createdAtC,
                nextRecurAt = nextRecurAtC,
                phase = 84,
                recur = "recur-synced-update",
            ),
            testWorkTypeEntity(15, worksiteId = 1).copy(id = 4, workType = "synced"),
            testWorkTypeEntity(
                id = 5,
                networkId = 22,
                worksiteId = 1,
                workType = "work-type-synced-new",
                status = "status-synced-new",
                orgClaim = 8456,
                createdAt = createdAtC,
                nextRecurAt = nextRecurAtC,
                phase = 93,
                recur = "recur-synced-new",
            ),
        )
        assertEquals(expectedWorkTypeEntitiesA, actual.workTypes)

        val expectedWorkTypes = listOf(
            WorkType(
                id = 1,
                workTypeLiteral = "work-type-synced-update",
                statusLiteral = "status-synced-update",
                orgClaim = 5498,
                createdAt = createdAtC,
                nextRecurAt = nextRecurAtC,
                phase = 84,
                recur = "recur-synced-update",
            ),
            WorkType(
                id = 4,
                workTypeLiteral = "synced",
                statusLiteral = "status",
                orgClaim = 201,
                createdAt = null,
                nextRecurAt = null,
                phase = null,
                recur = null,
            ),
            WorkType(
                id = 5,
                workTypeLiteral = "work-type-synced-new",
                statusLiteral = "status-synced-new",
                orgClaim = 8456,
                createdAt = createdAtC,
                nextRecurAt = nextRecurAtC,
                phase = 93,
                recur = "recur-synced-new",
            ),
        )
        assertEquals(expectedWorkTypes, actual.asExternalModel().workTypes)

        actual = worksiteDao.getWorksite(2)
        val expectedWorkTypesB = listOf(
            testWorkTypeEntity(24, worksiteId = 2).copy(id = 6, workType = "new"),
            testWorkTypeEntity(26, worksiteId = 2).copy(id = 7, workType = "new"),
        )
        assertEquals(
            existingWorksites[1].copy(
                address = "sync-address",
                updatedAt = updatedAtB,
            ),
            actual.entity,
        )
        assertEquals(expectedWorkTypesB, actual.workTypes)
    }

    /**
     * Locally modified worksites (and associated work types) are not synced
     */
    @Test
    fun syncSkipLocallyModified() = runTest {
        // Insert existing
        var existingWorksites = listOf(
            testWorksiteEntity(1, 1, "address", updatedAtA),
            testWorksiteEntity(2, 1, "address", updatedAtA),
        )
        existingWorksites = insertWorksites(existingWorksites, previousSyncedAt)
        db.testWorksiteDao().setLocallyModified(2, updatedAtA)

        db.workTypeDao().insert(
            listOf(
                testWorkTypeEntity(1, worksiteId = 1),
                testWorkTypeEntity(11, worksiteId = 1),
                testWorkTypeEntity(22, worksiteId = 2),
                testWorkTypeEntity(24, worksiteId = 2),
            )
        )

        // Sync
        val syncingWorksites = listOf(
            testWorksiteEntity(1, 1, "sync-address", updatedAtB),
            testWorksiteEntity(2, 1, "sync-address", updatedAtB),
        )
        val syncingWorkTypes = listOf(
            listOf(
                // Update
                testWorkTypeEntity(1, worksiteId = 1, workType = "synced"),
                // New
                testWorkTypeEntity(15, worksiteId = 1, workType = "synced"),
            ),
            listOf(
                testWorkTypeEntity(22, worksiteId = 2, workType = "should-not-sync"),
                testWorkTypeEntity(24, worksiteId = 2, workType = "should-not-sync"),
                testWorkTypeEntity(26, worksiteId = 2, workType = "should-not-sync"),
            ),
        )
        // Sync new and existing
        val syncedAt = previousSyncedAt.plus(499_999.seconds)
        worksiteDaoPlus.syncWorksites(1, syncingWorksites, syncingWorkTypes, syncedAt)

        // Assert

        // Worksite synced
        var actual = worksiteDao.getWorksite(1)
        assertEquals(
            existingWorksites[0].copy(
                address = "sync-address",
                updatedAt = updatedAtB,
            ),
            actual.entity,
        )
        var actualWorkTypes = listOf(
            testWorkTypeEntity(1, worksiteId = 1).copy(
                id = 1,
                workType = "synced",
            ),
            testWorkTypeEntity(15, worksiteId = 1).copy(
                id = 6,
                workType = "synced",
            ),
        )
        assertEquals(actualWorkTypes, actual.workTypes)

        // Worksite not synced
        actual = worksiteDao.getWorksite(2)
        actualWorkTypes = listOf(
            testWorkTypeEntity(22, worksiteId = 2).copy(id = 3),
            testWorkTypeEntity(24, worksiteId = 2).copy(id = 4),
        )
        assertEquals(existingWorksites[1], actual.entity)
        assertEquals(actualWorkTypes, actual.workTypes)
    }

    @Test
    fun skipInvalidWorkTypes() = runTest {
        val existingWorksites = listOf(
            testWorksiteEntity(1, 1, "address", updatedAtA),
        )
        insertWorksites(existingWorksites, previousSyncedAt)

        db.workTypeDao().insert(
            listOf(
                testWorkTypeEntity(11, worksiteId = 1, isInvalid = true),
                testWorkTypeEntity(
                    1, worksiteId = 1,
                    createdAt = createdAtA,
                    nextRecurAt = updatedAtA,
                    phase = 3,
                    recur = "recur",
                ),
            )
        )

        val actual = worksiteDao.getWorksite(1).asExternalModel().workTypes
        val expected = listOf(
            WorkType(
                id = 2,
                createdAt = createdAtA,
                orgClaim = 201,
                nextRecurAt = updatedAtA,
                phase = 3,
                recur = "recur",
                statusLiteral = "status",
                workTypeLiteral = "work-type-a",
            )
        )
        assertEquals(expected, actual)
    }
}