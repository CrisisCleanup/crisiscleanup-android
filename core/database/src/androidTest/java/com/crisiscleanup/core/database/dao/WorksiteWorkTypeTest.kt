package com.crisiscleanup.core.database.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.crisiscleanup.core.database.CrisisCleanupDatabase
import com.crisiscleanup.core.database.WorksiteTestUtil
import com.crisiscleanup.core.database.model.WorksiteEntity
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

/**
 * Sync worksites with work types
 */
class WorksiteWorkTypeTest {
    private lateinit var db: CrisisCleanupDatabase

    private lateinit var worksiteDao: WorksiteDao
    private lateinit var worksiteDaoPlus: WorksiteDaoPlus

    private suspend fun insertWorksites(
        worksites: List<WorksiteEntity>,
        syncedAt: Instant,
    ): List<WorksiteEntity> {
        return WorksiteTestUtil.insertWorksites(db, worksites, syncedAt)
    }

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context,
            CrisisCleanupDatabase::class.java
        ).build()
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
        db.testTargetWorkTypeDao().insertWorkTypes(
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
        val syncingWorkTypes = listOf(
            listOf(
                // Update
                testWorkTypeEntity(1, worksiteId = 1, workType = "synced"),
                // New
                testWorkTypeEntity(15, worksiteId = 1, workType = "synced"),
                testWorkTypeEntity(22, worksiteId = 1, workType = "synced"),
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
        var actualWorkTypes = listOf(
            testWorkTypeEntity(1, worksiteId = 1).copy(id = 1, workType = "synced"),
            testWorkTypeEntity(15, worksiteId = 1).copy(id = 4, workType = "synced"),
            testWorkTypeEntity(22, worksiteId = 1).copy(id = 5, workType = "synced"),
        )
        assertEquals(actualWorkTypes, actual.workTypes)

        actual = worksiteDao.getWorksite(2)
        actualWorkTypes = listOf(
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
        assertEquals(actualWorkTypes, actual.workTypes)
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
        db.testTargetWorksiteDao().setLocallyModified(2, updatedAtA)

        db.testTargetWorkTypeDao().insertWorkTypes(
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
}