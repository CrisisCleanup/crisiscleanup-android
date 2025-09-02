package com.crisiscleanup.core.database.dao

import com.crisiscleanup.core.database.TestCrisisCleanupDatabase
import com.crisiscleanup.core.database.TestRecentWorksiteDao
import com.crisiscleanup.core.database.TestUtil
import com.crisiscleanup.core.database.TestUtil.testAppLogger
import com.crisiscleanup.core.database.TestUtil.testSyncLogger
import com.crisiscleanup.core.database.TestWorksiteDao
import com.crisiscleanup.core.database.WorksiteTestUtil
import com.crisiscleanup.core.database.WorksiteTestUtil.testIncidents
import com.crisiscleanup.core.database.model.IncidentWorksiteIds
import com.crisiscleanup.core.database.model.RecentWorksiteEntity
import com.crisiscleanup.core.database.model.WorksiteEntity
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.junit.Before
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

class WorksiteSyncReconciliationTest {
    private lateinit var db: TestCrisisCleanupDatabase

    private lateinit var worksiteDao: TestWorksiteDao
    private lateinit var worksiteDaoPlus: WorksiteDaoPlus
    private lateinit var recentWorksiteDao: TestRecentWorksiteDao

    private val syncLogger = testSyncLogger()
    private val appLogger = testAppLogger()

    @Before
    fun createDb() {
        db = TestUtil.getTestDatabase()
        worksiteDao = db.testWorksiteDao()
        worksiteDaoPlus = WorksiteDaoPlus(db, syncLogger, appLogger)
        recentWorksiteDao = db.testRecentWorksiteDao()
    }

    @Before
    fun seedDb() = runTest {
        val incidentDao = db.incidentDao()
        incidentDao.upsertIncidents(testIncidents)

        val worksiteCreatedAt = Clock.System.now().minus(10.days)
        val insertAt = Clock.System.now().minus(1.days)
        insertWorksites(
            listOf(
                testWorksiteFullEntity(
                    534,
                    23,
                    worksiteCreatedAt.plus(1.hours),
                ),
                testWorksiteFullEntity(
                    48,
                    1,
                    worksiteCreatedAt.plus(2.hours),
                ),
                testWorksiteFullEntity(
                    1654,
                    456,
                    worksiteCreatedAt.plus(3.hours),
                ),
                testWorksiteFullEntity(
                    9,
                    23,
                    worksiteCreatedAt.plus(4.hours),
                ),
                testWorksiteFullEntity(
                    987,
                    23,
                    worksiteCreatedAt.plus(5.hours),
                ),
            ),
            insertAt,
        )
    }

    private suspend fun insertWorksites(
        worksites: List<WorksiteEntity>,
        syncedAt: Instant,
    ) = WorksiteTestUtil.insertWorksites(
        db,
        syncedAt,
        *worksites.toTypedArray(),
    )

    @Test
    fun syncNetworkChangedIncidents() = runTest {
        val viewedAt = Instant.fromEpochSeconds(1756835957)
        val recentViews = listOf(
            RecentWorksiteEntity(4, 23, viewedAt),
            RecentWorksiteEntity(1, 23, viewedAt),
        )
        for (recent in recentViews) {
            recentWorksiteDao.upsert(recent)
        }

        fun makeIncidentWorksiteIds(incidentId: Long, networkWorksiteId: Long) =
            IncidentWorksiteIds(
                incidentId = incidentId,
                worksiteId = 0,
                networkWorksiteId = networkWorksiteId,
            )

        val changes = worksiteDaoPlus.syncNetworkChangedIncidents(
            listOf(
                makeIncidentWorksiteIds(1, 534),
                makeIncidentWorksiteIds(1, 987),
                makeIncidentWorksiteIds(23, 1654),
            ),
            stepInterval = 2,
        )

        val expectedChanges = listOf(
            IncidentWorksiteIds(1, 1, 534),
            IncidentWorksiteIds(1, 5, 987),
            IncidentWorksiteIds(23, 3, 1654),
        )
        assertEquals(expectedChanges, changes)

        val orderedChanges = listOf(
            expectedChanges[0],
            IncidentWorksiteIds(1, 2, 48),
            expectedChanges[2],
            IncidentWorksiteIds(23, 4, 9),
            expectedChanges[1],
        )
        val worksiteIdsA = worksiteDao.getWorksiteEntities()
        assertEquals(orderedChanges, worksiteIdsA)
        val worksiteIdsB = worksiteDao.getRootWorksiteEntities()
        assertEquals(orderedChanges, worksiteIdsB)

        val recents = recentWorksiteDao.getRecentWorksites()
        val expectedRecents = listOf(
            RecentWorksiteEntity(1, 1, viewedAt),
            RecentWorksiteEntity(4, 23, viewedAt),
        )
        assertEquals(expectedRecents, recents)
    }

    @Test
    fun syncDeletedWorksites() = runTest {
        worksiteDaoPlus.syncDeletedWorksites(
            listOf(987, 1654, 48),
            stepInterval = 2,
        )

        val worksites = worksiteDao.getWorksites()
        val networkWorksiteIds = worksites.map { it.entity.networkId }
        assertEquals(listOf(9L, 534), networkWorksiteIds)
    }
}
