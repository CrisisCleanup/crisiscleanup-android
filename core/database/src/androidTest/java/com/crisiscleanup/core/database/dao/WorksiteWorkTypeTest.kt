package com.crisiscleanup.core.database.dao

import com.crisiscleanup.core.database.TestCrisisCleanupDatabase
import com.crisiscleanup.core.database.TestUtil
import com.crisiscleanup.core.database.TestUtil.testSyncLogger
import com.crisiscleanup.core.database.WorksiteTestUtil
import com.crisiscleanup.core.database.model.WorkTypeEntity
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

    private val syncLogger = testSyncLogger()

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
        worksiteDaoPlus = WorksiteDaoPlus(db, syncLogger)
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
        worksiteDaoPlus.syncWorksites(syncingWorksites, emptyList(), syncedAt)
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
        db.workTypeDao().insertIgnore(
            listOf(
                testWorkTypeEntity(1, worksiteId = 1, workType = "work-type-a"),
                testWorkTypeEntity(11, worksiteId = 1, workType = "work-type-b"),
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
                    workType = "work-type-a",
                    status = "status-synced-update",
                    orgClaim = 5498,
                    createdAt = createdAtC,
                    nextRecurAt = nextRecurAtC,
                    phase = 84,
                    recur = "recur-synced-update",
                ),
                // Delete 11
                // New
                testWorkTypeEntity(15, worksiteId = 1, workType = "work-type-synced-c"),
                testWorkTypeEntity(
                    22,
                    worksiteId = 1,
                    workType = "work-type-synced-d",
                    status = "status-synced-new",
                    orgClaim = 8456,
                    createdAt = createdAtC,
                    nextRecurAt = nextRecurAtC,
                    phase = 93,
                    recur = "recur-synced-new",
                ),
            ),
            listOf(
                testWorkTypeEntity(24, worksiteId = 2, workType = "work-type-a"),
                testWorkTypeEntity(26, worksiteId = 2, workType = "work-type-b"),
            ),
        )
        // Sync new and existing
        val syncedAt = previousSyncedAt.plus(499_999.seconds)
        worksiteDaoPlus.syncWorksites(syncingWorksites, syncingWorkTypes, syncedAt)

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
                workType = "work-type-a",
                status = "status-synced-update",
                orgClaim = 5498,
                createdAt = createdAtC,
                nextRecurAt = nextRecurAtC,
                phase = 84,
                recur = "recur-synced-update",
            ),
            testWorkTypeEntity(15, worksiteId = 1)
                .copy(id = 4, workType = "work-type-synced-c"),
            testWorkTypeEntity(
                id = 5,
                networkId = 22,
                worksiteId = 1,
                workType = "work-type-synced-d",
                status = "status-synced-new",
                orgClaim = 8456,
                createdAt = createdAtC,
                nextRecurAt = nextRecurAtC,
                phase = 93,
                recur = "recur-synced-new",
            ),
        )
        assertEquals(expectedWorkTypeEntitiesA, actual.workTypes.sortedBy(WorkTypeEntity::id))

        val expectedWorkTypes = listOf(
            WorkType(
                id = 1,
                workTypeLiteral = "work-type-a",
                statusLiteral = "status-synced-update",
                orgClaim = 5498,
                createdAt = createdAtC,
                nextRecurAt = nextRecurAtC,
                phase = 84,
                recur = "recur-synced-update",
            ),
            WorkType(
                id = 4,
                workTypeLiteral = "work-type-synced-c",
                statusLiteral = "status",
                orgClaim = 201,
                createdAt = null,
                nextRecurAt = null,
                phase = null,
                recur = null,
            ),
            WorkType(
                id = 5,
                workTypeLiteral = "work-type-synced-d",
                statusLiteral = "status-synced-new",
                orgClaim = 8456,
                createdAt = createdAtC,
                nextRecurAt = nextRecurAtC,
                phase = 93,
                recur = "recur-synced-new",
            ),
        )
        assertEquals(
            expectedWorkTypes,
            actual.asExternalModel(515).worksite.workTypes.sortedBy(WorkType::id)
        )

        actual = worksiteDao.getWorksite(2)
        val expectedWorkTypesB = listOf(
            testWorkTypeEntity(24, worksiteId = 2).copy(id = 6, workType = "work-type-a"),
            testWorkTypeEntity(26, worksiteId = 2).copy(id = 7, workType = "work-type-b"),
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

        db.workTypeDao().insertIgnore(
            listOf(
                testWorkTypeEntity(1, worksiteId = 1, workType = "work-type-a"),
                testWorkTypeEntity(11, worksiteId = 1, workType = "work-type-b"),
                testWorkTypeEntity(22, worksiteId = 2, workType = "work-type-a"),
                testWorkTypeEntity(24, worksiteId = 2, workType = "work-type-b"),
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
                testWorkTypeEntity(
                    1,
                    worksiteId = 1,
                    status = "status-synced",
                    workType = "work-type-a",
                ),
                // New
                testWorkTypeEntity(15, worksiteId = 1, workType = "work-type-synced-a"),
            ),
            listOf(
                testWorkTypeEntity(
                    22,
                    worksiteId = 2,
                    status = "status-synced",
                    workType = "work-type-a",
                ),
                testWorkTypeEntity(
                    24,
                    worksiteId = 2,
                    status = "status-synced",
                    workType = "work-type-b",
                ),
                testWorkTypeEntity(
                    26,
                    worksiteId = 2,
                    status = "status-synced",
                    workType = "work-type-c",
                ),
            ),
        )
        // Sync new and existing
        val syncedAt = previousSyncedAt.plus(499_999.seconds)
        worksiteDaoPlus.syncWorksites(syncingWorksites, syncingWorkTypes, syncedAt)

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
        var expectedWorkTypes = listOf(
            testWorkTypeEntity(1, worksiteId = 1).copy(
                id = 1,
                status = "status-synced",
                workType = "work-type-a",
            ),
            testWorkTypeEntity(15, worksiteId = 1).copy(
                id = 6,
                workType = "work-type-synced-a",
            ),
        )
        assertEquals(expectedWorkTypes, actual.workTypes)

        // Worksite not synced
        actual = worksiteDao.getWorksite(2)
        expectedWorkTypes = listOf(
            testWorkTypeEntity(22, worksiteId = 2, workType = "work-type-a").copy(id = 3),
            testWorkTypeEntity(24, worksiteId = 2, workType = "work-type-b").copy(id = 4),
        )
        assertEquals(existingWorksites[1], actual.entity)
        assertEquals(expectedWorkTypes, actual.workTypes)
    }
}